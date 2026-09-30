package com.pitchrec.nativetest;

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
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// DNI TELEFONU DO TRENERA — wyznacza trener w panelu. Aplikacja pobiera je z backendu
// (/call-days/mine), pokazuje w Statystykach (harmonogram) i powiadamia:
//  • o nowych terminach ("Trener wyznaczył dni telefonu: 3.10 18:00, 7.10"),
//  • w dniu telefonu (godzine wczesniej, a bez godziny — od 9:00): "Dziś zadzwoń do trenera".
// Sprawdzane przy otwarciu aplikacji i co ok. 15 min (ten sam budzik co "Chętnie porozmawiam").
public final class CallDays {

    public static final class Day {
        public final String id, day, time, note, trainer, phone;
        Day(JSONObject o) {
            id = o.optString("id", ""); day = s(o, "day"); time = s(o, "time"); note = s(o, "note");
            trainer = s(o, "trainer_name"); phone = s(o, "trainer_phone");
        }
        private static String s(JSONObject o, String k) { String v = o.optString(k, ""); return "null".equals(v) ? "" : v.trim(); }
        public String label() { return day.length() >= 10 ? day.substring(8, 10) + "." + day.substring(5, 7) + (time.isEmpty() ? "" : " " + time) : day; }
    }

    private static final String CHANNEL = "call_days";

    private CallDays() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static List<Day> days(Context c) {
        List<Day> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("call_days_json", "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null) out.add(new Day(o)); }
        } catch (Exception e) { }
        return out;
    }

    public static String today() { return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()); }

    public static void refresh(Context c, Runnable done) {
        SharedPreferences p = prefs(c);
        String token = p.getString("ns_token", null);
        if (token == null) { if (done != null) done.run(); return; }
        String q = "/call-days/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(p.getString("ns_email", "")) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            try {
                if (r.ok) {
                    JSONObject o = new JSONObject(r.body);
                    JSONArray a = o.optJSONArray("days");
                    if (o.optBoolean("ok", false) && a != null) p.edit().putString("call_days_json", a.toString()).putLong("call_days_at", System.currentTimeMillis()).apply();
                }
            } catch (Exception e) { }
            if (done != null) done.run();
        });
    }

    // pobierz + powiadom (nowe terminy, dzisiejszy telefon)
    public static void check(Context c, Runnable done) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        refresh(ctx, () -> { try { notifyIfNeeded(ctx); } catch (Exception e) { } if (done != null) done.run(); });
    }

    private static void notifyIfNeeded(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("call_days_notif", true)) return;
        List<Day> ds = days(c);
        String today = today();
        Set<String> seen = new HashSet<>(p.getStringSet("call_days_seen", new HashSet<>()));
        boolean first = !p.contains("call_days_seen");
        List<String> fresh = new ArrayList<>();
        Set<String> keep = new HashSet<>();
        for (Day d : ds) {
            keep.add(d.id);
            if (!seen.contains(d.id) && d.day.compareTo(today) >= 0) fresh.add(d.label());
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean night = hour >= 22 || hour < 7;
        if (!fresh.isEmpty() && !night) {
            notify(c, 4101, "📞 " + L.t("Trener wyznaczył dni telefonu"), android.text.TextUtils.join(", ", fresh) + "\n" + L.t("Zobaczysz je w Statystykach — Harmonogram."), null);
            seen.addAll(keep);
        } else if (first) seen.addAll(keep);
        if (!night) seen.retainAll(keep);
        p.edit().putStringSet("call_days_seen", seen).apply();
        // dzien telefonu: godzine przed (albo od 9:00, gdy bez godziny) — raz na termin
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        Set<String> done = new HashSet<>(p.getStringSet("call_days_done", new HashSet<>()));
        for (Day d : ds) {
            if (!today.equals(d.day) || done.contains(d.id)) continue;
            int at = 9 * 60;
            if (d.time.matches("\\d{1,2}:\\d{2}")) { String[] t = d.time.split(":"); at = Math.max(7 * 60, Integer.parseInt(t[0]) * 60 + Integer.parseInt(t[1]) - 60); }
            if (nowMin < at || nowMin >= 22 * 60) continue;
            String body = (d.time.isEmpty() ? "" : L.f("o {0}", d.time) + "  ·  ") + (d.trainer.isEmpty() ? "" : d.trainer + "  ") + (d.phone.isEmpty() ? "" : "☎ " + d.phone)
                    + (d.note.isEmpty() ? "" : "\n" + d.note);
            notify(c, 4200 + Math.abs(d.id.hashCode() % 500), "📞 " + L.t("Dziś zadzwoń do trenera"), body.trim(), d.phone);
            done.add(d.id);
        }
        Set<String> ids = new HashSet<>();
        for (Day d : ds) ids.add(d.id);
        done.retainAll(ids);
        p.edit().putStringSet("call_days_done", done).apply();
    }

    private static void notify(Context c, int id, String title, String text, String phone) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Telefony do trenera"), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", "stats");
        PendingIntent cp = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(title).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(cp).setAutoCancel(true);
        if (phone != null && !phone.isEmpty()) {
            Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone.replaceAll("[^0-9+]", ""))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent dp = PendingIntent.getActivity(c, id + 1, dial, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(c, android.R.drawable.ic_menu_call), "☎ " + L.t("Zadzwoń"), dp).build());
        }
        try { nm.notify(id, b.build()); } catch (SecurityException e) { }
    }
}
