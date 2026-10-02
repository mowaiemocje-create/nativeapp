package com.pitchrec.nativetest;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

// PRZYPOMNIENIE O 20:00 — jesli kursant dzis nic nie wgral do NewSpeech albo nie napisal
// dziennika, dostaje powiadomienie. Sprawdzamy NS (nagrania i dziennik z dzisiejsza data),
// a gdy nie ma internetu — to, co wiadomo na telefonie. Wlaczane w Ustawieniach (domyslnie wl.).
public class ReminderReceiver extends BroadcastReceiver {

    static final String ACTION = "com.pitchrec.nativetest.DAILY_REMINDER";
    private static final String CHANNEL = "daily_reminder";
    private static final int HOUR = 20;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("reminder_on", true); }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 2001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Ustawia (albo kasuje) najblizsze przypomnienie o 20:00
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c);
        if (!enabled(c)) { am.cancel(p); return; }
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, HOUR);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis() + 30000L) next.add(Calendar.DAY_OF_YEAR, 1);
        // okno 10 min (bez uprawnienia do dokladnych alarmow) — przypomnienie 20:00–20:10
        am.setWindow(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), 10 * 60 * 1000L, p);
    }

    static String today() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // Dziennik zapisany dzis z tej aplikacji (albo wykryty w NS przy otwarciu dziennika)
    public static void markDiary(Context c, String date) {
        if (date != null) prefs(c).edit().putString("diary_last_date", date).apply();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        L.init(c);
        String act = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_BOOT_COMPLETED.equals(act) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(act)) {
            schedule(c);
            TrainerWatch.schedule(c);
            AvailWatch.schedule(c);
            CallRecUi.watch(c);
            return;
        }
        if (AvailWatch.ACTION.equals(act)) {
            CallRecUi.watch(c); // usluga nagrywania rozmow wylaczona przez telefon?
            final PendingResult pr = goAsync();
            AvailWatch.check(c, () -> CallDays.check(c, () -> { try { pr.finish(); } catch (Exception e) { } }));
            return;
        }
        if (TrainerWatch.ACTION.equals(act)) {
            final PendingResult pr = goAsync();
            TrainerWatch.check(c, () -> { try { pr.finish(); } catch (Exception e) { } });
            return;
        }
        schedule(c); // kolejny dzien
        if (!enabled(c)) return;
        final String day = today();
        final boolean[] rec = {sentToday(c, day)};
        final boolean[] diary = {day.equals(prefs(c).getString("diary_last_date", ""))};
        String token = prefs(c).getString("ns_token", null);
        String email = prefs(c).getString("ns_email", "");
        if (token == null || (rec[0] && diary[0])) { notifyIfNeeded(c, rec[0], diary[0]); return; }
        final PendingResult pr = goAsync();
        final boolean[] finished = {false};
        final Runnable fin = () -> {
            if (finished[0]) return;
            finished[0] = true;
            notifyIfNeeded(c, rec[0], diary[0]);
            try { pr.finish(); } catch (Exception e) { }
        };
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(fin, 9000);
        NsClient.request("GET", "/records?page_size=10&sort_by=date&sort_order=desc", token, email, null, null, r -> {
            if (hasDate(r, day)) rec[0] = true;
            NsClient.request("GET", "/diaries?sort_by=date&sort_order=desc&page_size=5", token, email, null, null, r2 -> {
                if (hasDate(r2, day)) { diary[0] = true; markDiary(c, day); }
                fin.run();
            });
        });
    }

    private static boolean hasDate(NsClient.Result r, String day) {
        if (r == null || !r.ok || r.body == null) return false;
        try {
            String b = r.body.trim();
            JSONArray arr = b.startsWith("[") ? new JSONArray(b) : new JSONObject(b).optJSONArray("collection");
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                if (o.optString("date", "").startsWith(day)) return true;
                if (o.optString("created_at", "").startsWith(day)) return true;
            }
        } catch (Exception e) { }
        return false;
    }

    // Nagranie z dzisiaj wyslane do NS z tego telefonu
    private static boolean sentToday(Context c, String day) {
        File[] files = c.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files == null) return false;
        Calendar cal = Calendar.getInstance();
        for (File f : files) {
            cal.setTimeInMillis(f.lastModified());
            String k = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            if (k.equals(day) && "sent".equals(RecMeta.load(c, f.getName()).ns)) return true;
        }
        return false;
    }

    private static void notifyIfNeeded(Context c, boolean rec, boolean diary) {
        if (rec && diary) return;
        String title, text;
        if (!rec && !diary) {
            title = L.t("Dziś jeszcze nic nie wgrałeś");
            text = L.t("Nagraj rozmowę i napisz dziennik — masz jeszcze czas do końca dnia.");
        } else if (!rec) {
            title = L.t("Brakuje dzisiejszego nagrania");
            text = L.t("Dziennik jest ✓ — nagraj jeszcze dziś rozmowę i wyślij ją do NewSpeech.");
        } else {
            title = L.t("Brakuje wpisu w dzienniku");
            text = L.t("Nagranie jest ✓ — uzupełnij jeszcze dziennik z dzisiejszego dnia.");
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Przypomnienia"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(L.t("Przypomnienie o 20:00 o nagraniu i dzienniku"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", !rec ? "daw" : "diary");
        PendingIntent cp = PendingIntent.getActivity(c, 2002, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(cp)
                .setAutoCancel(true)
                .build();
        try { nm.notify(2003, n); } catch (SecurityException e) { /* brak zgody na powiadomienia */ }
    }
}
