package com.pitchrec.nativetest;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// POWIADOMIENIA O OCENIE TRENERA: co ok. 2 godziny (i przy otwarciu apki) sprawdzamy w NS,
// czy trener ocenil nowe nagrania (zaliczone / do poprawy) albo odpowiedzial na dziennik.
// Tylko pewne dane z NS — nic nie jest zgadywane. Wlaczane w Ustawieniach (domyslnie wl.).
public final class TrainerWatch {

    static final String ACTION = "com.pitchrec.nativetest.TRAINER_CHECK";
    private static final String CHANNEL = "trainer_reviews";
    private static volatile boolean running = false;

    private TrainerWatch() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("trainer_notif", true); }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 2101, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c);
        if (!enabled(c) || prefs(c).getString("ns_token", null) == null) { am.cancel(p); return; }
        // RTC (bez budzenia telefonu) — sprawdzenie przy najblizszym uzyciu telefonu, oszczedza baterie
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 20 * 60 * 1000L, 2 * 60 * 60 * 1000L, p);
    }

    // done: wolane po zakonczeniu (moze byc null)
    public static void check(Context c, Runnable done) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        String token = prefs(ctx).getString("ns_token", null);
        if (!enabled(ctx) || token == null || running) { if (done != null) done.run(); return; }
        running = true;
        String email = prefs(ctx).getString("ns_email", "");
        NsClient.request("GET", "/records?page_size=30&sort_by=date&sort_order=desc", token, email, null, null, r -> {
            try { if (r.ok) checkRecords(ctx, r.body); } catch (Exception e) { }
            checkDiaryReplies(ctx, token, email, () -> { running = false; if (done != null) done.run(); });
        });
    }

    private static JSONArray collection(String body) throws Exception {
        String b = body.trim();
        return b.startsWith("[") ? new JSONArray(b) : new JSONObject(b).optJSONArray("collection");
    }

    private static void checkRecords(Context c, String body) throws Exception {
        JSONArray arr = collection(body);
        if (arr == null) return;
        SharedPreferences p = prefs(c);
        Set<String> seen = new HashSet<>(p.getStringSet("seen_reviews", new HashSet<>()));
        boolean first = !p.getBoolean("seen_reviews_init", false);
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.isNull("reviewed_at") || o.optString("reviewed_at", "").isEmpty() || o.isNull("is_correct")) continue;
            String rev = o.optString("reviewed_at", "");
            String upd = o.optString("record_file_updated_by_author_at", o.optString("updated_by_author_at", ""));
            if (NsStatus.newer(upd, rev)) continue; // plik podmieniony po ocenie — czeka na nowa
            String key = o.optString("id", "") + "|" + rev;
            if (seen.contains(key)) continue;
            seen.add(key);
            if (first) continue;
            JSONObject cat = o.optJSONObject("record_category");
            String cn = cat != null ? (cat.optString("name_pl", "").isEmpty() ? cat.optString("name", "") : cat.optString("name_pl", "")) : "";
            String d = o.optString("date", "");
            String label = L.cat(cn) + (d.length() >= 10 ? " · " + d.substring(8, 10) + "." + d.substring(5, 7) : "");
            if (o.optBoolean("is_correct", false)) ok.add(label); else bad.add(label);
        }
        if (seen.size() > 400) { List<String> l = new ArrayList<>(seen); seen = new HashSet<>(l.subList(l.size() - 300, l.size())); }
        p.edit().putStringSet("seen_reviews", seen).putBoolean("seen_reviews_init", true).apply();
        if (ok.isEmpty() && bad.isEmpty()) return;
        String title, text;
        if (ok.size() + bad.size() == 1) {
            title = ok.isEmpty() ? "↺ " + L.t("Trener prosi o poprawkę") : "✅ " + L.t("Trener zaliczył Twoje nagranie");
            text = ok.isEmpty() ? bad.get(0) + " — " + L.t("zobacz uwagi w Korekcie") : ok.get(0) + " — " + L.t("brawo, tak trzymaj!");
        } else {
            title = "📋 " + L.f("Trener ocenił {0} nagrań", ok.size() + bad.size());
            text = "✅ " + L.t("zaliczone") + ": " + ok.size() + (bad.isEmpty() ? "" : "  ·  ↺ " + L.t("do poprawy") + ": " + bad.size());
        }
        notify(c, 2102, title, text, bad.isEmpty() ? "stats" : "fix");
    }

    // Odpowiedzi trenera na dziennik (tekst lub nagranie) — ostatnie 5 wpisow
    private static void checkDiaryReplies(Context c, String token, String email, Runnable done) {
        String uid = prefs(c).getString("ns_user_id", "");
        if (uid.isEmpty()) { done.run(); return; }
        NsClient.request("GET", "/diaries?sort_by=date&sort_order=desc&page_size=5", token, email, null, null, r -> {
            List<String> dates = new ArrayList<>();
            try {
                JSONArray arr = r.ok ? collection(r.body) : null;
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    String d = arr.optJSONObject(i) != null ? arr.optJSONObject(i).optString("date", "") : "";
                    if (d.length() >= 10) dates.add(d.substring(0, 10));
                }
            } catch (Exception e) { }
            if (dates.isEmpty()) { done.run(); return; }
            final int[] left = {dates.size()};
            final List<String> fresh = new ArrayList<>();
            for (String d : dates) {
                String q = "/diary-reply/get?student_id=" + NsClient.enc(uid) + "&entry_date=" + d + "&ns_token=" + NsClient.enc(token)
                        + "&ns_email=" + NsClient.enc(email) + "&ns_server=new";
                NsClient.backend("GET", q, null, r2 -> {
                    try {
                        if (r2.ok) {
                            JSONObject o = new JSONObject(r2.body);
                            boolean has = (!o.isNull("text_reply") && !o.optString("text_reply", "").isEmpty()) || o.optBoolean("has_audio", false);
                            String upd = o.isNull("updated_at") ? "" : String.valueOf(o.opt("updated_at"));
                            String k = "diary_reply_" + d;
                            String prev = prefs(c).getString(k, null);
                            if (has && !upd.equals(prev)) {
                                boolean init = prev == null && !prefs(c).getBoolean("diary_reply_init", false);
                                prefs(c).edit().putString(k, upd).apply();
                                if (!init) fresh.add(d);
                            }
                        }
                    } catch (Exception e) { }
                    if (--left[0] == 0) {
                        prefs(c).edit().putBoolean("diary_reply_init", true).apply();
                        if (!fresh.isEmpty()) {
                            String d0 = fresh.get(0);
                            notify(c, 2103, "💬 " + L.t("Trener odpowiedział na Twój dziennik"),
                                    L.t("Wpis z dnia") + " " + d0.substring(8, 10) + "." + d0.substring(5, 7) + " — " + L.t("dotknij, aby przeczytać"), "diary");
                        }
                        done.run();
                    }
                });
            }
        });
    }

    private static void notify(Context c, int id, String title, String text, String page) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Oceny trenera"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(L.t("Zaliczenia, poprawki i odpowiedzi trenera"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", page);
        PendingIntent cp = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(cp)
                .setAutoCancel(true)
                .build();
        try { nm.notify(id, n); } catch (SecurityException e) { }
    }
}
