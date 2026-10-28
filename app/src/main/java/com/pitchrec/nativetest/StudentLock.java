package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

// 🔒 PRZERWA W TRENINGU: trener może w panelu wstrzymać aplikację kursanta (worker /student-lock/mine).
// Gdy wstrzymane — zamiast nagrywania i wysyłania przyjazny komunikat (odsłuch nagrań i dziennik działają).
// Brak sieci / błąd = NIE blokujemy (aplikacja działa normalnie).
public final class StudentLock {

    private StudentLock() { }

    public static volatile boolean locked = false;
    public static volatile String title = "", message = "";

    private static String lang() {
        String l = L.lang();
        return "cs".equals(l) || "sk".equals(l) || "en".equals(l) || "pl".equals(l) ? l : ("pl".equals(l) || l == null ? "pl" : "en");
    }

    // done — zawsze wołane (na wątku głównym), także przy błędzie
    public static void check(Context c, Runnable done) {
        SharedPreferences p = c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String token = p.getString("ns_token", null);
        if (token == null) { locked = false; if (done != null) done.run(); return; }
        String q = "/student-lock/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(p.getString("ns_email", ""))
                + "&ns_server=new&lang=" + lang();
        NsClient.backend("GET", q, null, r -> {
            boolean l = false;
            String t = "", m = "";
            try {
                if (r.ok && r.body != null) {
                    JSONObject o = new JSONObject(r.body);
                    if (o.optBoolean("ok", false) && o.optBoolean("locked", false)) {
                        l = true;
                        t = o.optString("title", "");
                        m = o.optString("message", "");
                    }
                }
            } catch (Exception e) { l = false; }
            locked = l;
            title = t;
            message = m;
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
}
