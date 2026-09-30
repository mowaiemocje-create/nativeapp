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

import java.util.HashSet;
import java.util.Set;

// "CHĘTNIE POROZMAWIAM" — powiadomienie dla innych kursantów.
// Gdy ktos wlaczy w Ustawieniach "Chetnie porozmawiam", jego imie, miasto i telefon trafiaja na
// wspolna mape (Firebase przez nasz proxy). Ta klasa co ok. 15 min (i przy otwarciu apki) sprawdza
// liste dostepnych i pokazuje powiadomienie o KAZDEJ nowej osobie: "📞 Anna chetnie porozmawia"
// + przycisk "Zadzwoń". Bez nocnych powiadomien (22–7). Wlaczane w Ustawieniach (domyslnie wl.).
public final class AvailWatch {

    static final String ACTION = "com.pitchrec.nativetest.AVAIL_CHECK";
    private static final String CHANNEL = "avail_talk";
    private static final long AVAIL_TTL = 2 * 60 * 60 * 1000L;   // dostepnosc trwa 2 h
    private static final long FRESH = 45 * 60 * 1000L;           // powiadamiamy tylko o swiezych wlaczeniach
    private static volatile boolean running = false;

    private AvailWatch() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("avail_notif", true); }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 2201, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c);
        // budzik sluzy tez dniom telefonu do trenera — dziala, gdy wlaczone jest cokolwiek z tych dwoch
        boolean any = enabled(c) || prefs(c).getBoolean("call_days_notif", true);
        if (!any || prefs(c).getString("ns_token", null) == null) { am.cancel(p); return; }
        // co ok. 15 min, bez budzenia telefonu (oszczedza baterie) — system moze to nieco przesunac
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 60 * 1000L, 15 * 60 * 1000L, p);
    }

    public static void check(Context c, Runnable done) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        String token = prefs(ctx).getString("ns_token", null);
        if (!enabled(ctx) || token == null || running) { if (done != null) done.run(); return; }
        running = true;
        String email = prefs(ctx).getString("ns_email", "");
        // lekki GET /map/avail (nowszy proxy); gdy go nie ma — pelne /map/points
        NsClient.request("GET", "/map/avail", token, email, null, null, r -> {
            JSONArray list = r.ok ? parse(r.body) : null;
            if (list != null) { handle(ctx, list); running = false; if (done != null) done.run(); return; }
            NsClient.request("GET", "/map/points", token, email, null, null, r2 -> {
                try { if (r2.ok) { JSONArray l2 = parse(r2.body); if (l2 != null) handle(ctx, l2); } } catch (Exception e) { }
                running = false;
                if (done != null) done.run();
            });
        });
    }

    private static JSONArray parse(String body) {
        try {
            String b = body == null ? "" : body.trim();
            if (b.startsWith("[")) return new JSONArray(b);
            JSONObject o = new JSONObject(b);
            JSONArray a = o.optJSONArray("available");
            return a != null ? a : o.optJSONArray("avail");
        } catch (Exception e) { return null; }
    }

    private static void handle(Context c, JSONArray list) {
        SharedPreferences p = prefs(c);
        Set<String> seen = new HashSet<>(p.getStringSet("avail_seen", new HashSet<>()));
        String me = p.getString("map_self_name", "").trim();
        String myPhone = p.getString("map_phone", "").replaceAll("\\D", "");
        long now = System.currentTimeMillis();
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        boolean night = hour >= 22 || hour < 7;
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o == null) continue;
            String name = o.optString("name", "").trim();
            long ts = o.optLong("ts", 0L);
            if (name.isEmpty() || ts <= 0 || now - ts > AVAIL_TTL) continue;
            String phone = o.isNull("phone") ? "" : o.optString("phone", "").trim();
            if (!me.isEmpty() && me.equalsIgnoreCase(name)) continue;                 // to ja
            if (!myPhone.isEmpty() && myPhone.equals(phone.replaceAll("\\D", ""))) continue;
            String key = name + "|" + ts;
            keep.add(key);
            if (seen.contains(key)) continue;
            if (night || now - ts > FRESH) { seen.add(key); continue; } // bez nocnych i starych powiadomien
            seen.add(key);
            notifyAvail(c, name, o.optString("city", "").trim(), phone, Math.abs(key.hashCode() % 10000) + 3000);
        }
        seen.retainAll(keep); // zostaja tylko osoby nadal dostepne — lista nie rosnie
        p.edit().putStringSet("avail_seen", seen).apply();
    }

    private static void notifyAvail(Context c, String name, String city, String phone, int id) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Chętnie porozmawiam"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(L.t("Kursanci, którzy właśnie chcą porozmawiać przez telefon"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", "map");
        PendingIntent cp = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = "📞 " + L.f("{0} chętnie porozmawia", name);
        StringBuilder text = new StringBuilder();
        if (!city.isEmpty()) text.append("📍 ").append(city);
        if (!phone.isEmpty()) { if (text.length() > 0) text.append("  ·  "); text.append("☎ ").append(phone); }
        if (text.length() > 0) text.append("\n");
        text.append(L.t("Zadzwoń i poćwicz rozmowę nową mową — dostępność trwa do 2 godzin."));
        Notification.Builder b = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(title)
                .setContentText(text.toString())
                .setStyle(new Notification.BigTextStyle().bigText(text.toString()))
                .setContentIntent(cp)
                .setAutoCancel(true);
        if (!phone.isEmpty()) {
            Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone.replaceAll("[^0-9+]", "")))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent dp = PendingIntent.getActivity(c, id + 1, dial, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(c, android.R.drawable.ic_menu_call), "☎ " + L.t("Zadzwoń"), dp).build());
        }
        try { nm.notify(id, b.build()); } catch (SecurityException e) { }
    }
}
