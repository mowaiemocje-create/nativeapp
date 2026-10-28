package com.pitchrec.nativetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONObject;

// 🔒 PRZERWA W TRENINGU: trener może w panelu wstrzymać aplikację kursanta (worker /student-lock/mine).
//  • ekran blokady zamiast nagrywania i wysyłania (odsłuch nagrań i dziennik działają),
//  • wszystkie powiadomienia aplikacji wyciszone (ustawienia zostają, tylko ich nie pokazujemy),
//  • poziom 1: zachęta raz na 7 dni (lokalne powiadomienie z grafiką); poziom 2: żadnych powiadomień.
// Brak sieci = ostatnio znany stan; gdy nigdy nie było odpowiedzi — bez blokady.
public final class StudentLock {

    private StudentLock() { }

    public static volatile boolean locked = false;
    public static volatile String title = "", message = "";
    public static volatile int level = 1;

    private static final String BACKEND = "https://newspeech-backend.mowaiemocje.workers.dev";

    private static SharedPreferences p(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    private static String lang() {
        String l = L.lang();
        return "cs".equals(l) || "sk".equals(l) || "en".equals(l) || "pl".equals(l) ? l : (l == null ? "pl" : "en");
    }

    private static void apply(JSONObject o) {
        locked = o != null && o.optBoolean("locked", false);
        title = locked ? o.optString("title", "") : "";
        message = locked ? o.optString("message", "") : "";
        level = locked ? o.optInt("level", 1) : 1;
    }

    // stan zapisany w telefonie (działa też w tle i bez sieci)
    private static JSONObject saved(Context c) {
        try {
            String s = p(c).getString("student_lock_json", null);
            if (s == null || p(c).getString("ns_token", null) == null) return null;
            return new JSONObject(s);
        } catch (Exception e) { return null; }
    }

    public static void loadSaved(Context c) { apply(saved(c)); }

    // Czy wyciszyć powiadomienia aplikacji (przypomnienia, harmonogram, zadania, dziennik, „Chcę porozmawiać”…)
    public static boolean muted(Context c) {
        JSONObject o = saved(c);
        return o != null && o.optBoolean("locked", false);
    }

    // done — zawsze wołane (na wątku głównym), także przy błędzie
    public static void check(Context c, Runnable done) {
        String token = p(c).getString("ns_token", null);
        if (token == null) { apply(null); if (done != null) done.run(); return; }
        String q = "/student-lock/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(p(c).getString("ns_email", ""))
                + "&ns_server=new&lang=" + lang();
        final Context ac = c.getApplicationContext();
        NsClient.backend("GET", q, null, r -> {
            JSONObject o = null;
            try { if (r.ok && r.body != null) { o = new JSONObject(r.body); if (!o.optBoolean("ok", false)) o = null; } } catch (Exception e) { o = null; }
            if (o != null) {
                boolean was = muted(ac);
                p(ac).edit().putString("student_lock_json", o.toString()).apply();
                if (was && !o.optBoolean("locked", false)) p(ac).edit().remove("lock_screen_day").apply();
                apply(o);
            } else apply(saved(ac)); // brak sieci / błąd — ostatnio znany stan (gdy nie było — bez blokady)
            if (done != null) done.run();
        });
    }

    // numer trenera (z dni telefonu do trenera), "" gdy brak
    public static String trainerPhone(Context c) {
        try {
            for (CallDays.Day d : CallDays.days(c)) if (d.phone != null && !d.phone.trim().isEmpty() && !"null".equals(d.phone)) return d.phone.trim();
        } catch (Exception e) { }
        return "";
    }

    // ── zdarzenia dla trenera: lock_screen / nudge_shown / nudge_opened ──
    public static void event(Context c, String ev) {
        try {
            String token = p(c).getString("ns_token", null);
            if (token == null) return;
            JSONObject b = new JSONObject();
            b.put("ns_token", token);
            b.put("ns_email", p(c).getString("ns_email", ""));
            b.put("ns_server", "new");
            b.put("event", ev);
            NsClient.backend("POST", "/student-lock/event", b.toString(), r -> { });
        } catch (Exception e) { }
    }

    // ekran blokady pokazany — raz dziennie wystarczy
    public static void screenShown(Context c) {
        String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        if (day.equals(p(c).getString("lock_screen_day", ""))) return;
        p(c).edit().putString("lock_screen_day", day).apply();
        event(c, "lock_screen");
    }

    // ── 🌱 ZACHĘTA raz na 7 dni (tylko poziom 1) — wołane w tle (sprawdzanie co kilkanaście minut) ──
    public static void backgroundTick(Context c, Runnable done) {
        final Context ac = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        if (p(ac).getString("ns_token", null) == null) { if (done != null) done.run(); return; }
        check(ac, () -> { try { maybeNudge(ac); } catch (Throwable t) { } if (done != null) done.run(); });
    }

    static void maybeNudge(Context c) {
        JSONObject o = saved(c);
        if (o == null || !o.optBoolean("locked", false)) return;
        JSONObject n = o.optJSONObject("nudge");
        if (n == null) return; // poziom 2 — żadnych powiadomień
        long every = Math.max(1, n.optInt("every_days", 7)) * 86400000L;
        long last = Math.max(p(c).getLong("lock_nudge_at", 0L), n.optLong("last_shown", 0L) * 1000L);
        long now = System.currentTimeMillis();
        if (last > 0 && now - last < every) return;
        if (last == 0) {
            // pierwsza zachęta dopiero tydzień po wstrzymaniu (nie od razu)
            long since = o.optLong("since", 0L) * 1000L;
            if (since > 0 && now - since < every) return;
        }
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (h < 10 || h >= 20) return; // nie budzimy rano ani wieczorem
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel("lock_nudge", L.t("Zachęta do treningu"), NotificationManager.IMPORTANCE_DEFAULT));
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("open_page", "lock");
        PendingIntent pi = PendingIntent.getActivity(c, 5101, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Bitmap big = null, small = null;
        try { big = BitmapFactory.decodeResource(c.getResources(), R.drawable.lock_nudge); } catch (Throwable t) { }
        try { small = BitmapFactory.decodeResource(c.getResources(), R.drawable.lock_nudge_small); } catch (Throwable t) { }
        String t = n.optString("title", ""), x = n.optString("text", "");
        Notification.Builder b = new Notification.Builder(c, "lock_nudge")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(t)
                .setContentText(x)
                .setContentIntent(pi)
                .setAutoCancel(true);
        if (small != null) b.setLargeIcon(small);
        if (big != null) b.setStyle(new Notification.BigPictureStyle().bigPicture(big).setSummaryText(x));
        else b.setStyle(new Notification.BigTextStyle().bigText(x));
        try { nm.notify(5101, b.build()); } catch (SecurityException e) { return; }
        p(c).edit().putLong("lock_nudge_at", now).apply();
        event(c, "nudge_shown");
    }
}
