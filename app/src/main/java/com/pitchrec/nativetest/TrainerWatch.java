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
    private static volatile long lastRun = 0L;

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
        // Jednorazowy budzik co ok. 2 h, dzialajacy tez w uspieniu (Doze); kolejny ustawia odbiornik.
        // Wczesniejszy RTC bez budzenia na czesci telefonow nie przychodzil wcale.
        long next = System.currentTimeMillis() + 2 * 60 * 60 * 1000L;
        try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, p); }
        catch (Exception e) { am.set(AlarmManager.RTC_WAKEUP, next, p); }
    }

    // done: wolane po zakonczeniu (moze byc null)
    public static void check(Context c, Runnable done) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        String token = prefs(ctx).getString("ns_token", null);
        // lista "Od trenera" uzupelnia sie zawsze; powiadomienia tylko gdy wlaczone (patrz notify)
        if (token == null || running || System.currentTimeMillis() - lastRun < 45000L) { if (done != null) done.run(); return; }
        running = true;
        lastRun = System.currentTimeMillis();
        HarmoGoal.refresh(ctx, null); // czy harmonogram trwa (po jego koncu powiadomienia sa wstrzymane) — najwyzej co 30 min
        String email = prefs(ctx).getString("ns_email", "");
        NsClient.request("GET", "/records?page_size=30&sort_by=date&sort_order=desc", token, email, null, null, r -> {
            try { if (r.ok) checkRecords(ctx, r.body); } catch (Exception e) { }
            checkVoice(ctx, token, email, () ->
                checkDiaryReplies(ctx, token, email, () ->
                    // zadania specjalne zaliczone recznie przez trenera w panelu
                    SpecialTasks.refreshCredits(ctx, () -> { running = false; if (done != null) done.run(); })));
        });
    }

    private static List<String> lastRecent = new ArrayList<>();

    // Komentarze GLOSOWE trenera do nagran ocenionych w ostatnich 14 dniach — nowe = wpis + powiadomienie
    private static void checkVoice(Context c, String token, String email, Runnable done) {
        List<String> ids = lastRecent;
        if (ids == null || ids.isEmpty()) { done.run(); return; }
        String q = "/voice-review/check?record_ids=" + NsClient.enc(android.text.TextUtils.join(",", ids)) + "&ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email);
        NsClient.backend("GET", q, null, r -> {
            try {
                if (r.ok) {
                    JSONArray has = new JSONObject(r.body).optJSONArray("has_review");
                    SharedPreferences p = prefs(c);
                    boolean first = !p.getBoolean("voice_seen_init", false);
                    String newRec = null; int n = 0;
                    for (int i = 0; has != null && i < has.length(); i++) {
                        String rid = has.optString(i, "");
                        if (rid.isEmpty()) continue;
                        if (TrainerInbox.add(c, "voice:" + rid, TrainerInbox.T_VOICE, "🎧 " + L.t("Komentarz głosowy trenera"),
                                L.t("Do Twojego nagrania — dotknij, aby posłuchać"), rid, "", System.currentTimeMillis(), first) && !first) { newRec = rid; n++; }
                    }
                    p.edit().putBoolean("voice_seen_init", true).apply();
                    if (n > 0) notify(c, 2104, "🎧 " + L.t("Trener nagrał komentarz głosowy"),
                            n == 1 ? L.t("Do Twojego nagrania — dotknij, aby posłuchać") : L.f("Nowe komentarze głosowe: {0}", n), n == 1 ? "rate" : "inbox", n == 1 ? newRec : null, null);
                }
            } catch (Exception e) { }
            done.run();
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
        List<String> ok = new ArrayList<>(), bad = new ArrayList<>(), okIds = new ArrayList<>(), badIds = new ArrayList<>();
        List<String> recent = new ArrayList<>(); // ocenione w ostatnich 14 dniach — sprawdzenie komentarzy glosowych
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.isNull("reviewed_at") || o.optString("reviewed_at", "").isEmpty() || o.isNull("is_correct")) continue;
            String rev = o.optString("reviewed_at", "");
            String upd = o.optString("record_file_updated_by_author_at", o.optString("updated_by_author_at", ""));
            if (NsStatus.newer(upd, rev)) continue; // plik podmieniony po ocenie — czeka na nowa
            String rid = o.optString("id", "");
            long revMs = NsStatus.isoMs(rev);
            if (revMs > System.currentTimeMillis() - 14 * 86400000L) recent.add(rid);
            String key = rid + "|" + rev;
            JSONObject cat = o.optJSONObject("record_category");
            String cn = cat != null ? (cat.optString("name_pl", "").isEmpty() ? cat.optString("name", "") : cat.optString("name_pl", "")) : "";
            String d = o.optString("date", "");
            String label = L.cat(cn) + (d.length() >= 10 ? " · " + d.substring(8, 10) + "." + d.substring(5, 7) : "");
            boolean good = o.optBoolean("is_correct", false);
            // do listy "Od trenera" (przy pierwszym uruchomieniu jako przeczytane)
            boolean fresh = TrainerInbox.add(c, "rate:" + key, good ? TrainerInbox.T_OK : TrainerInbox.T_BAD,
                    good ? "✅ " + L.t("Zaliczone przez trenera") : "↺ " + L.t("Do poprawy"), label, rid, "", revMs, first);
            if (seen.contains(key)) continue;
            seen.add(key);
            if (first || !fresh) continue;
            if (good) { ok.add(label); okIds.add(rid); } else { bad.add(label); badIds.add(rid); }
        }
        if (seen.size() > 400) { List<String> l = new ArrayList<>(seen); seen = new HashSet<>(l.subList(l.size() - 300, l.size())); }
        p.edit().putStringSet("seen_reviews", seen).putBoolean("seen_reviews_init", true).apply();
        lastRecent = recent;
        if (ok.isEmpty() && bad.isEmpty()) return;
        String title, text;
        if (ok.size() + bad.size() == 1) {
            title = ok.isEmpty() ? "↺ " + L.t("Trener prosi o poprawkę") : "✅ " + L.t("Trener zaliczył Twoje nagranie");
            text = ok.isEmpty() ? bad.get(0) + " — " + L.t("zobacz uwagi w Korekcie") : ok.get(0) + " — " + L.t("brawo, tak trzymaj!");
        } else {
            title = "📋 " + L.f("Trener ocenił {0} nagrań", ok.size() + bad.size());
            text = "✅ " + L.t("zaliczone") + ": " + ok.size() + (bad.isEmpty() ? "" : "  ·  ↺ " + L.t("do poprawy") + ": " + bad.size());
        }
        // jedno nagranie — powiadomienie otwiera od razu jego ocene; kilka — liste nagran / Korekte
        String one = ok.size() + bad.size() == 1 ? (okIds.isEmpty() ? badIds.get(0) : okIds.get(0)) : null;
        notify(c, 2102, title, text, one != null ? "rate" : bad.isEmpty() ? "recs" : "fix", one, null);
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
                                String txt = o.isNull("text_reply") ? "" : o.optString("text_reply", "");
                                TrainerInbox.add(c, "diary:" + d + "|" + upd, TrainerInbox.T_DIARY,
                                        (o.optBoolean("has_audio", false) ? "🎧 " : "💬 ") + L.t("Trener odpowiedział na Twój dziennik"),
                                        L.t("Wpis z dnia") + " " + d.substring(8, 10) + "." + d.substring(5, 7) + (txt.isEmpty() ? "" : " — " + (txt.length() > 80 ? txt.substring(0, 80) + "…" : txt)),
                                        "", d, System.currentTimeMillis(), init);
                                if (!init) fresh.add(d);
                            }
                        }
                    } catch (Exception e) { }
                    if (--left[0] == 0) {
                        prefs(c).edit().putBoolean("diary_reply_init", true).apply();
                        if (!fresh.isEmpty()) {
                            String d0 = fresh.get(0);
                            notify(c, 2103, "💬 " + L.t("Trener odpowiedział na Twój dziennik"),
                                    L.t("Wpis z dnia") + " " + d0.substring(8, 10) + "." + d0.substring(5, 7) + " — " + L.t("dotknij, aby przeczytać"), "diary", null, d0);
                        }
                        done.run();
                    }
                });
            }
        });
    }

    private static void notify(Context c, int id, String title, String text, String page) { notify(c, id, title, text, page, null, null); }

    private static void notify(Context c, int id, String title, String text, String page, String rec, String date) {
        if (!enabled(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Oceny trenera"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(L.t("Zaliczenia, poprawki i odpowiedzi trenera"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", page);
        if (rec != null) open.putExtra("open_rec", rec);
        if (date != null) open.putExtra("open_date", date);
        PendingIntent cp = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(cp)
                .setAutoCancel(true)
                .build();
        if (HarmoGoal.ended(c)) return; // harmonogram sie skonczyl — bez powiadomien
        try { nm.notify(id, n); } catch (SecurityException e) { }
    }
}
