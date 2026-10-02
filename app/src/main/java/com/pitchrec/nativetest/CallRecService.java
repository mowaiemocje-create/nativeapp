package com.pitchrec.nativetest;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import com.pitchrec.backgroundrecorder.Mp3Stream;

import java.io.File;

// NAGRYWANIE ROZMOW TELEFONICZNYCH — ta sama metoda co Cube ACR:
// od Androida 10 zwykla apka w czasie rozmowy dostaje z mikrofonu cisze; WYJATKIEM jest
// wlaczona usluga ulatwien dostepu — ona moze nagrywac mikrofonem (zrodlo "voice recognition").
// Usluga NIC nie czyta z ekranu; sluzy tylko do tego, zeby system pozwolil nagrywac w czasie rozmowy.
//
// Dzialanie: kursant klika ☎ przy osobie z listy (albo "Nagraj następną rozmowę") -> rozmowa
// jest "uzbrojona"; gdy telefon przejdzie w tryb rozmowy — start nagrywania (MP3), po rozlaczeniu —
// zapis do nagran i ekran opisu z gotowa kategoria "Phone do Kursanta/Trenera" i osoba.
public class CallRecService extends AccessibilityService {

    static final String PREFS = "app_settings";
    private static final String CHANNEL = "call_rec";
    private static final int NOTIF_ID = 4801;
    private static final int SAMPLE_RATE = 44100;

    static volatile CallRecService running = null;

    private final Handler h = new Handler(Looper.getMainLooper());
    private volatile boolean recording = false;
    private Thread recThread;
    private long recStart = 0L;
    private String recLabel = "";
    private File recFile;
    private volatile int peak = 0;
    private String usedSource = "";

    // ── API dla apki ──

    public static boolean enabled(Context c) {
        try {
            String s = android.provider.Settings.Secure.getString(c.getContentResolver(), "enabled_accessibility_services");
            if (s == null) return false;
            String me = c.getPackageName() + "/";
            for (String part : s.split(":")) if (part.startsWith(me) && part.contains("CallRecService")) return true;
        } catch (Exception e) { }
        return false;
    }

    public static boolean autoOn(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("callrec_auto", true); }

    // Uzbrojenie: nastepna rozmowa (w ciagu `minutes`) zostanie nagrana i przypisana do osoby `label`
    public static void arm(Context c, String label, int minutes) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("callrec_label", label == null ? "" : label)
                .putLong("callrec_until", System.currentTimeMillis() + minutes * 60_000L).apply();
        CallRecService s = running;
        if (s != null) s.kick();
    }

    public static void disarm(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("callrec_until").apply();
    }

    public static boolean armed(Context c) {
        return System.currentTimeMillis() < c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("callrec_until", 0L);
    }

    public static boolean isRecording() { CallRecService s = running; return s != null && s.recording; }

    // Zrodlo dzwieku: auto (jak Cube ACR) albo wybrane w Ustawieniach do testow na danym telefonie
    static int[] sources(Context c) {
        String pick = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("callrec_src", "auto");
        if ("rec".equals(pick)) return new int[]{MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
        if ("comm".equals(pick)) return new int[]{MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC};
        if ("mic".equals(pick)) return new int[]{MediaRecorder.AudioSource.MIC};
        if ("call".equals(pick)) return new int[]{MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
        if (Build.VERSION.SDK_INT >= 29) return new int[]{MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
        if (Build.VERSION.SDK_INT == 28) return new int[]{MediaRecorder.AudioSource.VOICE_COMMUNICATION, MediaRecorder.AudioSource.MIC};
        return new int[]{MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
    }

    static String sourceName(int s) {
        if (s == MediaRecorder.AudioSource.VOICE_RECOGNITION) return "voice recognition";
        if (s == MediaRecorder.AudioSource.VOICE_COMMUNICATION) return "voice communication";
        if (s == MediaRecorder.AudioSource.VOICE_CALL) return "voice call";
        return "mic";
    }

    // ── usluga ──

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        running = this;
        kick();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (recording) stopRec();
        if (running == this) running = null;
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (running == this) running = null;
        return super.onUnbind(intent);
    }

    void kick() {
        h.removeCallbacks(tick);
        h.post(tick);
    }

    private boolean inCall() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        int m = am.getMode();
        return m == AudioManager.MODE_IN_CALL;
    }

    // Co sekunde sprawdzamy, czy trwa rozmowa (tryb audio telefonu) — bez uprawnien do stanu telefonu
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            boolean call = inCall();
            if (!recording && call && armed(CallRecService.this)) startRec();
            else if (recording && !call) stopRec();
            // gdy nic nie jest uzbrojone i nie nagrywamy — sprawdzamy rzadziej (oszczednie)
            boolean active = recording || armed(CallRecService.this);
            h.postDelayed(this, active ? 1000L : 15000L);
        }
    };

    private void startRec() {
        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        recLabel = p.getString("callrec_label", "");
        disarm(this);
        recStart = System.currentTimeMillis();
        recFile = new File(getCacheDir(), "callrec_" + recStart + ".mp3"); // do nagran trafia dopiero gotowy plik
        peak = 0;
        recording = true;
        notifyRecording();
        final int[] srcs = sources(this);
        recThread = new Thread(() -> {
            AudioRecord ar = null;
            Mp3Stream mp3 = null;
            try {
                int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int buf = Math.max(min * 2, SAMPLE_RATE);
                for (int s : srcs) {
                    try {
                        ar = new AudioRecord(s, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf);
                        if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                            ar.startRecording();
                            if (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) { usedSource = sourceName(s); break; }
                        }
                    } catch (Exception e) { }
                    try { if (ar != null) ar.release(); } catch (Exception e) { }
                    ar = null;
                }
                if (ar == null) throw new Exception("AudioRecord");
                mp3 = new Mp3Stream(recFile, SAMPLE_RATE);
                short[] b = new short[2048];
                while (recording) {
                    int n = ar.read(b, 0, b.length);
                    if (n <= 0) continue;
                    int pk = peak;
                    for (int i = 0; i < n; i++) { int v = Math.abs((int) b[i]); if (v > pk) pk = v; }
                    peak = pk;
                    mp3.feed(b, n);
                }
            } catch (Exception e) {
                recording = false;
                getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("callrec_last_err", "start: " + e.getMessage()).apply();
            } finally {
                try { if (ar != null) { ar.stop(); ar.release(); } } catch (Exception e) { }
            }
            File done = mp3 != null ? mp3.finish() : null;
            h.post(() -> finished(done));
        }, "callrec");
        recThread.start();
    }

    private void stopRec() {
        recording = false; // watek sam domknie plik i wywola finished()
    }

    private void finished(File f) {
        cancelNotif(NOTIF_ID);
        long dur = System.currentTimeMillis() - recStart;
        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString("callrec_last", usedSource + " · " + (dur / 1000) + " s · peak " + peak).apply();
        if (f == null || !f.exists() || dur < 4000) {
            if (f != null) f.delete();
            if (f == null) notifyDone(L.t("Nie udało się nagrać rozmowy"), L.t("Telefon nie pozwolił nagrywać. Sprawdź Ustawienia → Nagrywanie rozmów."), false);
            return;
        }
        File dest = new File(getFilesDir(), "recording_" + recStart + ".mp3");
        if (f.renameTo(dest)) f = dest;
        boolean silent = peak < 300; // praktycznie cisza — telefon blokuje nagrywanie w czasie rozmowy
        p.edit().putString("callrec_pending", f.getAbsolutePath() + "\n" + recLabel).apply();
        String who = recLabel.isEmpty() ? "" : " — " + recLabel;
        if (silent) notifyDone("⚠️ " + L.t("Nagranie rozmowy jest ciche") + who,
                L.t("Telefon mógł zablokować dźwięk. Spróbuj z włączonym głośnikiem albo zmień źródło dźwięku w Ustawieniach."), true);
        else notifyDone("📞 " + L.t("Rozmowa nagrana") + who, L.t("Dotknij, żeby opisać i zapisać nagranie."), true);
        // od razu otwieramy apke na ekranie opisu (usluga ulatwien dostepu moze uruchomic ekran)
        try {
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("open_page", "callrec");
            startActivity(i);
        } catch (Exception e) { }
    }

    private NotificationManager nm() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, L.t("Nagrywanie rozmów"), NotificationManager.IMPORTANCE_LOW));
        return nm;
    }

    private void notifyRecording() {
        NotificationManager nm = nm();
        if (nm == null) return;
        String t = "🔴 " + L.t("Nagrywam rozmowę") + (recLabel.isEmpty() ? "" : " — " + recLabel);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(t).setContentText(L.t("Nagrywanie skończy się samo po rozłączeniu."))
                .setOngoing(true).build();
        try { nm.notify(NOTIF_ID, n); } catch (SecurityException e) { }
    }

    private void notifyDone(String title, String text, boolean open) {
        NotificationManager nm = nm();
        if (nm == null) return;
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true);
        if (open) {
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra("open_page", "callrec");
            b.setContentIntent(PendingIntent.getActivity(this, NOTIF_ID + 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        try { nm.notify(NOTIF_ID + 1, b.build()); } catch (SecurityException e) { }
    }

    private void cancelNotif(int id) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(id);
    }
}
