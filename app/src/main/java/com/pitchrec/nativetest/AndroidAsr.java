package com.pitchrec.nativetest;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// 🗣 ROZPOZNAWANIE MOWY WBUDOWANE W ANDROIDA (Google, w telefonie) — tylko po to, by policzyć SŁOWA
// i SYLABY całego nagrania. Czas mówienia i pauzy liczymy sami z dźwięku (ten sam pomiar co pauzy
// na wykresie). Plik podajemy rozpoznawaniu jako strumień PCM 16 kHz (Android 13+).
public final class AndroidAsr {

    private AndroidAsr() { }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // Android 13+ umie rozpoznawać mowę z pliku (a nie tylko z mikrofonu)
    public static boolean supported(Context c) {
        return Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isRecognitionAvailable(c);
    }

    public static String langTag() { return tagFor(L.lang()); }

    public static String tagFor(String l) {
        switch (l == null ? "pl" : l) {
            case "en": return "en-US";
            case "cs": return "cs-CZ";
            case "sk": return "sk-SK";
            case "de": return "de-DE";
            case "es": return "es-ES";
            case "hu": return "hu-HU";
            default: return "pl-PL";
        }
    }

    private static SpeechRecognizer create(Context c) {
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(c))
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(c);
        return SpeechRecognizer.createSpeechRecognizer(c);
    }

    private static Intent baseIntent() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag());
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        return i;
    }

    // ── Czy polski jest pobrany do rozpoznawania w telefonie ──
    public interface Status { void got(String state); } // installed / pending / downloadable / online / none

    public static void checkLanguage(Context c, Status cb) {
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizer.isRecognitionAvailable(c)) { cb.got("none"); return; }
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(c)) { cb.got("online"); return; }
        MAIN.post(() -> {
            final SpeechRecognizer r;
            try { r = SpeechRecognizer.createOnDeviceSpeechRecognizer(c); } catch (Throwable t) { cb.got("online"); return; }
            String lt = langTag(), lang2 = lt.substring(0, 2);
            try {
                r.checkRecognitionSupport(baseIntent(), Runnable::run, new android.speech.RecognitionSupportCallback() {
                    @Override public void onSupportResult(android.speech.RecognitionSupport s) {
                        String st = has(s.getInstalledOnDeviceLanguages(), lang2) ? "installed"
                                : has(s.getPendingOnDeviceLanguages(), lang2) ? "pending"
                                : has(s.getSupportedOnDeviceLanguages(), lang2) ? "downloadable"
                                : has(s.getOnlineLanguages(), lang2) ? "online" : "none";
                        MAIN.post(() -> { try { r.destroy(); } catch (Throwable t) { } cb.got(st); });
                    }
                    @Override public void onError(int error) {
                        MAIN.post(() -> { try { r.destroy(); } catch (Throwable t) { } cb.got("online"); });
                    }
                });
            } catch (Throwable t) { try { r.destroy(); } catch (Throwable t2) { } cb.got("online"); }
        });
    }

    private static boolean has(java.util.List<String> l, String lang2) {
        if (l == null) return false;
        for (String x : l) if (x != null && x.toLowerCase(java.util.Locale.ROOT).startsWith(lang2)) return true;
        return false;
    }

    // Prosi system o pobranie języka do rozpoznawania w telefonie (okienko / powiadomienie Google)
    public static void downloadLanguage(Context c) {
        if (Build.VERSION.SDK_INT < 33) return;
        MAIN.post(() -> {
            try {
                SpeechRecognizer r = SpeechRecognizer.createOnDeviceSpeechRecognizer(c);
                r.triggerModelDownload(baseIntent());
                MAIN.postDelayed(() -> { try { r.destroy(); } catch (Throwable t) { } }, 5000);
            } catch (Throwable t) { }
        });
    }

    // ── Rozpoznanie całego nagrania (wołać z wątku w tle; czeka na wynik) ──
    // pcm: próbki mono w częstotliwości sr. Zwraca rozpoznany tekst (może być pusty) albo rzuca wyjątek.
    public static String recognize(Context c, short[] pcm, int sr) throws Exception { return recognize(c, pcm, sr, langTag()); }

    public static String recognize(Context c, short[] pcm, int sr, String tag) throws Exception {
        if (!supported(c)) throw new Exception(L.t("Rozpoznawanie mowy z pliku wymaga Androida 13 lub nowszego."));
        final byte[] bytes = toPcm16k(pcm, sr);
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final StringBuilder text = new StringBuilder();
        final String[] err = {null};
        final CountDownLatch done = new CountDownLatch(1);
        final SpeechRecognizer[] holder = {null};
        final long[] lastEvent = {System.currentTimeMillis()};
        final boolean[] written = {false};
        // zapis dźwięku do strumienia (rozpoznawanie czyta go tak szybko, jak potrafi)
        Thread writer = new Thread(() -> {
            try (OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                for (int i = 0; i < bytes.length; i += 32000) os.write(bytes, i, Math.min(32000, bytes.length - i));
            } catch (Exception e) { /* rozpoznawanie zakończyło czytanie */ }
            written[0] = true;
            lastEvent[0] = System.currentTimeMillis();
        });
        MAIN.post(() -> {
            try {
                SpeechRecognizer r = create(c);
                holder[0] = r;
                r.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle b) { }
                    @Override public void onBeginningOfSpeech() { }
                    @Override public void onRmsChanged(float v) { }
                    @Override public void onBufferReceived(byte[] b) { }
                    @Override public void onEndOfSpeech() { }
                    @Override public void onPartialResults(Bundle b) { }
                    @Override public void onEvent(int t, Bundle b) { }
                    @Override public void onSegmentResults(Bundle b) { lastEvent[0] = System.currentTimeMillis(); add(text, b); }
                    @Override public void onEndOfSegmentedSession() { done.countDown(); }
                    @Override public void onResults(Bundle b) { add(text, b); done.countDown(); }
                    @Override public void onError(int e) {
                        lastEvent[0] = System.currentTimeMillis();
                        // „nic nie rozpoznano” w jednym fragmencie (np. dłuższa cisza) — sesja trwa dalej
                        if (e == SpeechRecognizer.ERROR_NO_MATCH) return;
                        err[0] = errName(e);
                        done.countDown();
                    }
                });
                Intent i = baseIntent();
                if (tag != null) i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag);
                i.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0]);
                i.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1);
                i.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                i.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000);
                i.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE);
                i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
                r.startListening(i);
                writer.start();
            } catch (Throwable t) {
                err[0] = t.getMessage() == null ? t.toString() : t.getMessage();
                done.countDown();
            }
        });
        long durMs = pcm.length * 1000L / sr, start = System.currentTimeMillis();
        boolean ok = false, quiet = false;
        // koniec: sesja się zakończyła, albo cały dźwięk przekazany i od 15 s nic nie przychodzi
        while (!ok && System.currentTimeMillis() - start < durMs * 2 + 60000) {
            ok = done.await(1000, TimeUnit.MILLISECONDS);
            if (!ok && written[0] && System.currentTimeMillis() - lastEvent[0] > 15000) { quiet = true; break; }
        }
        if (quiet) ok = true;
        MAIN.post(() -> { try { if (holder[0] != null) holder[0].destroy(); } catch (Throwable t) { } });
        try { pipe[0].close(); } catch (Exception e) { }
        if (!ok) throw new Exception(L.t("Rozpoznawanie mowy nie odpowiedziało na czas."));
        if (err[0] != null && text.length() == 0) throw new Exception(err[0]);
        return text.toString().trim();
    }

    private static void add(StringBuilder sb, Bundle b) {
        if (b == null) return;
        ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (l == null || l.isEmpty() || l.get(0) == null) return;
        String t = l.get(0).trim();
        if (t.isEmpty()) return;
        synchronized (sb) { if (sb.length() > 0) sb.append(' '); sb.append(t); }
    }

    private static String errName(int e) {
        switch (e) {
            case SpeechRecognizer.ERROR_NO_MATCH: return L.t("Nie rozpoznano żadnych słów.");
            case SpeechRecognizer.ERROR_NETWORK: case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return L.t("Brak internetu, a język nie jest pobrany do rozpoznawania w telefonie.");
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED: case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: return L.t("Język nie jest dostępny w rozpoznawaniu mowy — pobierz go w Ustawieniach (Podpowiedzi).");
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return L.t("Brak zgody na mikrofon");
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return L.t("Rozpoznawanie mowy jest zajęte — spróbuj za chwilę.");
            default: return L.t("Błąd rozpoznawania mowy") + " (" + e + ")";
        }
    }

    // mono 16 kHz, 16 bit little endian (prosty filtr: średnia z sąsiednich próbek)
    private static byte[] toPcm16k(short[] s, int sr) {
        int n = (int) (s.length * 16000L / sr);
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int a = (int) (i * (double) sr / 16000.0);
            int lo = Math.max(0, a - 1), hi = Math.min(s.length - 1, a + 1);
            int sum = 0;
            for (int j = lo; j <= hi; j++) sum += s[j];
            int v = sum / (hi - lo + 1);
            out[2 * i] = (byte) v;
            out[2 * i + 1] = (byte) (v >> 8);
        }
        return out;
    }
}
