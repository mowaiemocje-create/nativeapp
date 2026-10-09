package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

// POWIADOMIENIA PUSH (Firebase Cloud Messaging) — tylko wersja kursantow.
// Telefon zglasza swoj token do proxy (/push/register). Gdy ktos kliknie "Chcę porozmawiać",
// proxy od razu wysyla wiadomosc do wszystkich innych telefonow (PushService ja odbiera).
// Sprawdzanie w tle co ~15 min (AvailWatch) zostaje jako zapas.
public final class Push {

    private Push() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    // ten sam identyfikator co przy "Chcę porozmawiać" (MainActivity.postAvail)
    static String uid(Context c) {
        SharedPreferences p = prefs(c);
        String id = p.getString("ns_user_id", "");
        if (id.isEmpty()) id = p.getString("ns_email", "");
        if (id.isEmpty()) id = p.getString("map_self_name", "");
        return id;
    }

    // przy starcie aplikacji i po zalogowaniu
    public static void register(Context c) {
        final Context ctx = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        if (prefs(ctx).getString("ns_token", null) == null) return;
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult() != null) send(ctx, t.getResult());
            });
        } catch (Throwable e) { /* brak Google Play Services itp. — zostaje sprawdzanie co 15 min */ }
    }

    static void send(Context c, String token) {
        SharedPreferences p = prefs(c);
        if (p.getString("ns_token", null) == null) return;
        String uid = uid(c);
        if (uid.isEmpty() || token == null || token.isEmpty()) return;
        // ten sam token i konto zgloszone w ostatnich 24 h — nie wysylamy ponownie
        String key = uid + "|" + token;
        if (key.equals(p.getString("push_sent_key", "")) && System.currentTimeMillis() - p.getLong("push_sent_at", 0L) < 24 * 3600 * 1000L) return;
        try {
            JSONObject b = new JSONObject().put("userId", uid).put("token", token).put("name", p.getString("map_self_name", ""));
            NsClient.request("POST", "/push/register", null, null, "application/json", b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), r -> {
                if (r.ok) p.edit().putString("push_sent_key", key).putLong("push_sent_at", System.currentTimeMillis()).apply();
            });
        } catch (Exception e) { }
    }

    // przy wylogowaniu (wywolac PRZED usunieciem danych konta)
    public static void unregister(Context c) {
        String uid = uid(c);
        prefs(c).edit().remove("push_sent_key").remove("push_sent_at").apply();
        if (uid.isEmpty()) return;
        try {
            JSONObject b = new JSONObject().put("userId", uid).put("remove", true);
            NsClient.request("POST", "/push/register", null, null, "application/json", b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), r -> { });
        } catch (Exception e) { }
    }
}
