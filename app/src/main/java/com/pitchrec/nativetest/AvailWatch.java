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
    private static final String CHANNEL = "avail_talk2"; // nowy kanal: WAZNE (wyskakuje na ekranie)
    private static final long AVAIL_TTL = 2 * 60 * 60 * 1000L;   // dostepnosc trwa 2 h
    private static volatile boolean running = false;
    private static volatile long runningSince = 0L;

    private AvailWatch() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    // Zawsze wlaczone (bez przelacznika w Ustawieniach) — jedyny wyjatek: koniec harmonogramu
    public static boolean enabled(Context c) { return true; }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 2201, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Budzik co ok. 15 min. setAndAllowWhileIdle dziala tez w trybie uspienia (Doze) — wczesniejszy
    // "nieprecyzyjny powtarzalny" budzik RTC na wielu telefonach (Xiaomi, Samsung) przychodzil
    // dopiero po odblokowaniu ekranu albo wcale. Kazde wywolanie ustawia NASTEPNY budzik;
    // w nocy (22–7) kolejne sprawdzenie dopiero o 7:00.
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c);
        boolean callRec = prefs(c).getBoolean("callrec_wanted", false); // pilnowanie uslugi nagrywania rozmow
        if (!callRec && prefs(c).getString("ns_token", null) == null) { am.cancel(p); return; }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        long next = System.currentTimeMillis() + 15 * 60 * 1000L;
        int h = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (!callRec && (h >= 22 || h < 7)) {
            java.util.Calendar m = java.util.Calendar.getInstance();
            if (h >= 22) m.add(java.util.Calendar.DAY_OF_YEAR, 1);
            m.set(java.util.Calendar.HOUR_OF_DAY, 7); m.set(java.util.Calendar.MINUTE, 0); m.set(java.util.Calendar.SECOND, 0);
            next = m.getTimeInMillis();
        }
        try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, p); }
        catch (Exception e) { am.set(AlarmManager.RTC_WAKEUP, next, p); }
    }

    public static void check(Context c, Runnable done) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        String token = prefs(ctx).getString("ns_token", null);
        // "running" zawieszone (zerwane polaczenie / blad w trakcie) blokowalo sprawdzanie az do
        // zamkniecia aplikacji przez system — po minucie odblokowujemy
        if (running && System.currentTimeMillis() - runningSince > 60000L) running = false;
        if (!enabled(ctx) || token == null || running) { if (done != null) done.run(); return; }
        running = true;
        runningSince = System.currentTimeMillis();
        String email = prefs(ctx).getString("ns_email", "");
        // lekki GET /map/avail (nowszy proxy); gdy go nie ma — pelne /map/points
        NsClient.request("GET", "/map/avail", token, email, null, null, r -> {
            JSONArray list = r.ok ? parse(r.body) : null;
            if (list != null) {
                try { handle(ctx, list, false); } catch (Exception e) { }
                running = false; if (done != null) done.run(); return;
            }
            NsClient.request("GET", "/map/points", token, email, null, null, r2 -> {
                try { if (r2.ok) { JSONArray l2 = parse(r2.body); if (l2 != null) handle(ctx, l2, false); } } catch (Exception e) { }
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

    // Powiadomienie PUSH (PushService) — jedna osoba, od razu. Te same zasady co przy sprawdzaniu.
    public static void fromPush(Context c, java.util.Map<String, String> d) {
        try {
            JSONObject o = new JSONObject();
            o.put("uid", d.get("uid") == null ? "" : d.get("uid"));
            o.put("name", d.get("name") == null ? "" : d.get("name"));
            o.put("city", d.get("city") == null ? "" : d.get("city"));
            String ph = d.get("phone");
            o.put("phone", ph == null || ph.isEmpty() ? JSONObject.NULL : ph);
            long ts = 0L;
            try { ts = Long.parseLong(d.get("ts")); } catch (Exception e) { }
            o.put("ts", ts > 0 ? ts : System.currentTimeMillis());
            handle(c, new JSONArray().put(o), true);
        } catch (Exception e) { }
    }

    // partial = lista niepelna (jedna osoba z push) — nie kasujemy z "widzianych" pozostalych osob
    private static void handle(Context c, JSONArray list, boolean partial) {
        SharedPreferences p = prefs(c);
        // seen: "osoba|ts" — osoba = uid (nowy proxy) albo imie. Ta sama osoba z nowszym ts w ciagu
        // 2 h (np. dosłana pozycja GPS) NIE daje drugiego powiadomienia.
        Set<String> seen = new HashSet<>(p.getStringSet("avail_seen", new HashSet<>()));
        java.util.Map<String, Long> seenTs = new java.util.HashMap<>();
        for (String k : seen) {
            int i = k.lastIndexOf('|');
            if (i > 0) try { seenTs.put(k.substring(0, i), Long.parseLong(k.substring(i + 1))); } catch (Exception e) { }
        }
        String me = p.getString("map_self_name", "").trim();
        String myUid = p.getString("ns_user_id", "").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-zA-Z0-9_-]", "_");
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
            String uid = o.optString("uid", "");
            if (!myUid.isEmpty() && myUid.equals(uid)) continue;                       // to ja
            if (!me.isEmpty() && me.equalsIgnoreCase(name)) continue;
            if (!myPhone.isEmpty() && myPhone.equals(phone.replaceAll("\\D", ""))) continue;
            String who = uid.isEmpty() ? name : uid;
            Long prev = seenTs.get(who);
            boolean known = prev != null && ts - prev < AVAIL_TTL;
            long keyTs = known ? prev : ts;
            keep.add(who + "|" + keyTs);
            if (known) continue;
            seenTs.put(who, ts);
            if (night) continue; // bez nocnych powiadomien (osoba zostaje "widziana")
            notifyAvail(c, name, o.optString("city", "").trim(), phone, ts, Math.abs(who.hashCode() % 10000) + 3000);
        }
        if (partial) {
            Set<String> merged = new HashSet<>(p.getStringSet("avail_seen", new HashSet<>()));
            merged.addAll(keep);
            p.edit().putStringSet("avail_seen", merged).apply();
        } else p.edit().putStringSet("avail_seen", keep).apply(); // zostaja tylko osoby nadal dostepne
    }

    private static void notifyAvail(Context c, String name, String city, String phone, long ts, int id) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        try { nm.deleteNotificationChannel("avail_talk"); } catch (Exception e) { } // stary, cichy kanal
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("Chcę porozmawiać"), NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(L.t("Kursanci, którzy właśnie chcą porozmawiać przez telefon"));
        nm.createNotificationChannel(ch);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("open_page", "map");
        PendingIntent cp = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = "📞 " + L.f("{0} chce porozmawiać", name);
        StringBuilder text = new StringBuilder();
        if (!city.isEmpty()) text.append("📍 ").append(city);
        if (!phone.isEmpty()) { if (text.length() > 0) text.append("  ·  "); text.append("☎ ").append(phone); }
        if (text.length() > 0) text.append("\n");
        String until = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(ts + AVAIL_TTL));
        text.append(L.f("Zadzwoń i poćwicz rozmowę nową mową — dostępny do {0}.", until));
        Notification.Builder b = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(title)
                .setContentText(text.toString())
                .setStyle(new Notification.BigTextStyle().bigText(text.toString()))
                .setContentIntent(cp)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setTimeoutAfter(Math.max(60000L, ts + AVAIL_TTL - System.currentTimeMillis())) // znika, gdy dostepnosc minie
                .setAutoCancel(true);
        if (!phone.isEmpty()) {
            Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone.replaceAll("[^0-9+]", "")))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent dp = PendingIntent.getActivity(c, id + 1, dial, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(c, android.R.drawable.ic_menu_call), "☎ " + L.t("Zadzwoń"), dp).build());
        }
        // "Chcę porozmawiać" — ZAWSZE, niezaleznie od harmonogramu (inne powiadomienia milkna po jego koncu)
        try { nm.notify(id, b.build()); } catch (SecurityException e) { }
    }
}
