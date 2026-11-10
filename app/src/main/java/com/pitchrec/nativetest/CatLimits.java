package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// 🚦 LIMIT NAGRAŃ NA KATEGORIĘ: trener ustawia kursantowi dzienny limit nagrań w danej kategorii
// (worker /cat-limits/mine liczy dzisiejsze nagrania z NS, dzień wg Europe/Warsaw).
//  • przed wysłaniem w kategorii z blocked=true — nie wysyłamy, pokazujemy message[lang] ({n} = per_day),
//  • przy kategoriach z limitem licznik „zostało dziś: left”,
//  • brak sieci / błąd endpointu = NIE blokujemy (lastOk=false).
// Poprawki (PUT istniejącego nagrania) nie podlegają limitowi.
public final class CatLimits {

    private CatLimits() { }

    private static final Map<String, JSONObject> byId = new HashMap<>();
    private static JSONObject message = null;
    /** czy OSTATNIE pobranie się udało — tylko wtedy blokujemy */
    public static volatile boolean lastOk = false;

    public interface Done { void done(boolean ok); }

    private static SharedPreferences p(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    /** pobiera świeży stan; done zawsze wołane (wątek główny) */
    public static void fetch(Context c, Done done) {
        String token = p(c).getString("ns_token", null);
        if (token == null) { synchronized (byId) { byId.clear(); } lastOk = false; if (done != null) done.done(false); return; }
        String q = "/cat-limits/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(p(c).getString("ns_email", "")) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            boolean ok = false;
            try {
                if (r.ok && r.body != null) {
                    JSONObject o = new JSONObject(r.body);
                    JSONArray arr = o.optJSONArray("limits");
                    if (o.optBoolean("ok", false) && arr != null) {
                        synchronized (byId) {
                            byId.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject l = arr.optJSONObject(i);
                                if (l != null && !l.optString("category_id", "").isEmpty()) byId.put(l.optString("category_id"), l);
                            }
                        }
                        message = o.optJSONObject("message");
                        ok = true;
                    }
                }
            } catch (Exception e) { ok = false; }
            lastOk = ok;
            if (done != null) done.done(ok);
        });
    }

    public static JSONObject of(String catId) {
        if (catId == null) return null;
        synchronized (byId) { return byId.get(catId); }
    }

    /** blokować wysyłkę? tylko gdy ostatnie pobranie się udało i worker mówi blocked */
    public static boolean blockedNow(String catId) {
        JSONObject l = of(catId);
        return lastOk && l != null && l.optBoolean("blocked", false);
    }

    public static String msg(String catId) {
        JSONObject l = of(catId);
        String lang = L.lang();
        String t = null;
        if (message != null) {
            t = message.optString(lang == null ? "pl" : lang, "");
            if (t.isEmpty()) t = message.optString("en", "");
            if (t.isEmpty()) t = message.optString("pl", "");
        }
        if (t == null || t.isEmpty()) t = L.t("Na dziś masz już komplet nagrań w tej kategorii ({n}). Wróć jutro.");
        return t.replace("{n}", l == null ? "" : String.valueOf(l.optInt("per_day", 0)));
    }

    /** "zostało dziś: 2/3" albo null, gdy kategoria bez limitu */
    public static String leftLabel(String catId) {
        JSONObject l = of(catId);
        if (l == null || !l.has("left")) return null;
        String lang = L.lang();
        String w = "en".equals(lang) ? "left today" : "cs".equals(lang) ? "zbývá dnes" : "sk".equals(lang) ? "zostáva dnes"
                : "de".equals(lang) ? "heute übrig" : "es".equals(lang) ? "quedan hoy" : "hu".equals(lang) ? "ma maradt" : "zostało dziś";
        return (l.optBoolean("blocked", false) ? "⛔ " : "") + w + ": " + l.optInt("left", 0) + "/" + l.optInt("per_day", 0);
    }

    /** po udanej wysyłce — licznik w dół lokalnie (do następnego pobrania) */
    public static void sent(String catId) {
        JSONObject l = of(catId);
        if (l == null) return;
        try {
            int left = Math.max(0, l.optInt("left", 0) - 1);
            l.put("today", l.optInt("today", 0) + 1);
            l.put("left", left);
            l.put("blocked", left <= 0);
        } catch (Exception e) { }
    }
}
