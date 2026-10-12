package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Komunikaty od trenera z workera newspeech-backend:
//  • /call-notices/mine — "📞 Zadzwoń do trenera" (powtarza sie ten sam blad w korektach).
//    Kursant NIE zamyka banera sam — znika, gdy trener kliknie "Porozmawialismy" albo zaliczy nagranie.
//  • /focus/mine — "🎯 Dziś pilnuj" (kryteria + wskazowka trenera) i "achieved" (opanowane).
// Ostatni dobry stan jest zapamietany — bez sieci pokazujemy go dalej.
public final class CoachNotices {

    private CoachNotices() { }

    public static final class Notice {
        public String id = "", recordId = "";
        public List<String> criteria = new ArrayList<>();
        public int correctionNo;
        public boolean acked;
    }

    public static final class Focus {
        public List<String> criteria = new ArrayList<>();
        public String note = "";
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    private static volatile boolean busy;

    // wlasny watek sieci — nie czeka za dlugimi wysylkami nagran (NsClient ma jeden watek)
    private static final java.util.concurrent.ExecutorService NET = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    private static void http(String method, String pathAndQuery, String jsonBody, NsClient.Callback cb) {
        NET.execute(() -> {
            NsClient.Result r = new NsClient.Result();
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL("https://newspeech-backend.mowaiemocje.workers.dev" + pathAndQuery).openConnection();
                c.setRequestMethod(method);
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestProperty("Accept", "application/json");
                if (jsonBody != null) {
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json");
                    try (java.io.OutputStream os = c.getOutputStream()) { os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                }
                r.status = c.getResponseCode();
                r.ok = r.status >= 200 && r.status < 300;
                java.io.InputStream is = r.ok ? c.getInputStream() : c.getErrorStream();
                if (is != null) {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] b = new byte[8192]; int k;
                    while ((k = is.read(b)) > 0) bo.write(b, 0, k);
                    is.close();
                    r.body = bo.toString("UTF-8");
                }
                c.disconnect();
            } catch (Exception e) { r.ok = false; r.status = 0; }
            final NsClient.Result fr = r;
            MAIN.post(() -> cb.done(fr));
        });
    }

    private static String auth(SharedPreferences p) {
        return "ns_token=" + NsClient.enc(p.getString("ns_token", "")) + "&ns_email=" + NsClient.enc(p.getString("ns_email", "")) + "&ns_server=new";
    }

    // Pobiera oba endpointy (razem). done wywolywane zawsze (na watku glownym), takze bez sieci.
    public static void refresh(Context ctx, Runnable done) {
        Context c = ctx.getApplicationContext();
        SharedPreferences p = prefs(c);
        if (p.getString("ns_token", null) == null || busy) { if (done != null) done.run(); return; }
        busy = true;
        String q = "?" + auth(p);
        http("GET", "/call-notices/mine" + q, null, r1 -> {
            try {
                if (r1.ok && r1.body != null) {
                    JSONObject o = new JSONObject(r1.body);
                    if (o.optBoolean("ok", false)) {
                        JSONArray a = o.optJSONArray("notices");
                        p.edit().putString("coach_call_json", a == null ? "[]" : a.toString()).apply();
                    }
                }
            } catch (Exception e) { /* zostaje poprzedni stan */ }
            http("GET", "/focus/mine" + q, null, r2 -> {
                try {
                    if (r2.ok && r2.body != null) {
                        JSONObject o = new JSONObject(r2.body);
                        if (o.optBoolean("ok", false)) {
                            JSONObject f = o.optJSONObject("focus");
                            p.edit().putString("coach_focus_json", f == null ? "" : f.toString()).apply();
                        }
                    }
                } catch (Exception e) { /* zostaje poprzedni stan */ }
                busy = false;
                if (done != null) done.run();
            });
        });
    }

    public static List<Notice> notices(Context c) {
        List<Notice> out = new ArrayList<>();
        Set<String> acked = prefs(c).getStringSet("coach_acked", new HashSet<>());
        try {
            JSONArray a = new JSONArray(prefs(c).getString("coach_call_json", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                Notice n = new Notice();
                n.id = o.optString("id", "");
                n.recordId = o.isNull("record_id") ? "" : o.optString("record_id", "");
                n.correctionNo = o.optInt("correction_no", 0);
                JSONArray cr = o.optJSONArray("criteria");
                for (int k = 0; cr != null && k < cr.length(); k++) { String s = cr.optString(k, "").trim(); if (!s.isEmpty()) n.criteria.add(s); }
                n.acked = (!o.isNull("acked_at") && !o.optString("acked_at", "").isEmpty()) || acked.contains(n.id);
                out.add(n);
            }
        } catch (Exception e) { }
        return out;
    }

    public static Notice forRecord(Context c, String recordId) {
        if (recordId == null || recordId.isEmpty()) return null;
        for (Notice n : notices(c)) if (recordId.equals(n.recordId)) return n;
        return null;
    }

    // liczebnik porzadkowy SLOWNIE (poprawka/korekta — rodzaj zenski), 1–10; dalej cyfra z kropka
    private static final java.util.Map<String, String[]> ORD = new java.util.HashMap<>();
    static {
        ORD.put("pl", new String[]{"pierwsza", "druga", "trzecia", "czwarta", "piąta", "szósta", "siódma", "ósma", "dziewiąta", "dziesiąta"});
        ORD.put("en", new String[]{"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth"});
        ORD.put("cs", new String[]{"první", "druhá", "třetí", "čtvrtá", "pátá", "šestá", "sedmá", "osmá", "devátá", "desátá"});
        ORD.put("sk", new String[]{"prvá", "druhá", "tretia", "štvrtá", "piata", "šiesta", "siedma", "ôsma", "deviata", "desiata"});
        ORD.put("de", new String[]{"erste", "zweite", "dritte", "vierte", "fünfte", "sechste", "siebte", "achte", "neunte", "zehnte"});
        ORD.put("es", new String[]{"primera", "segunda", "tercera", "cuarta", "quinta", "sexta", "séptima", "octava", "novena", "décima"});
        ORD.put("hu", new String[]{"első", "második", "harmadik", "negyedik", "ötödik", "hatodik", "hetedik", "nyolcadik", "kilencedik", "tizedik"});
    }

    public static String ordinal(int n) {
        String[] a = ORD.get(L.lang());
        if (a == null) a = ORD.get("pl");
        return n >= 1 && n <= a.length ? a[n - 1] : n + ".";
    }

    // "to już trzecia poprawka tego nagrania"
    public static String correctionText(int n) {
        return L.f("to już {0} poprawka tego nagrania", ordinal(n));
    }

    // Kategoria nagrania z komunikatu: z listy KOREKTA (cache) albo z nagran w telefonie
    public static String categoryFor(Context c, String recordId, java.util.List<NsClient.FixEntry> fixList) {
        if (recordId == null || recordId.isEmpty()) return "";
        if (fixList != null) for (NsClient.FixEntry fe : fixList)
            if (recordId.equals(fe.id) && fe.categoryName != null && !fe.categoryName.isEmpty()) return fe.categoryName;
        java.io.File[] all = c.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (all != null) for (java.io.File f : all) {
            RecMeta m = RecMeta.load(c, f.getName());
            if ((recordId.equals(m.nsRecordId) || recordId.equals(m.fixRecordId)) && !m.cat.isEmpty()) return m.cat;
        }
        return "";
    }

    // Krotka linia do karty w KOREKCIE (druga linia pod kategoria)
    public static String cardLine(Notice n) {
        StringBuilder sb = new StringBuilder("📞 ").append(L.t("Zadzwoń do trenera"));
        if (n.correctionNo > 0) sb.append(" — ").append(correctionText(n.correctionNo));
        sb.append(".");
        sb.append("\n").append(n.criteria.isEmpty() ? cap(L.t("powtarza się ten sam błąd")) + "."
                : cap(L.t("powtarza się błąd:")) + " " + trJoin(n.criteria));
        return sb.toString();
    }

    // nazwy kryteriow (Ogonek, Intonacja…) w jezyku aplikacji
    public static String trJoin(List<String> names) {
        StringBuilder b = new StringBuilder();
        for (String x : names) { if (b.length() > 0) b.append(", "); b.append(RateNames.tr(x)); }
        return b.toString();
    }

    private static String cap(String s) { return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1); }

    public static String bannerText(Context c, List<Notice> list, java.util.List<NsClient.FixEntry> fixList) {
        StringBuilder sb = new StringBuilder();
        for (Notice n : list) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("📞 ").append(L.t("Zadzwoń do trenera"));
            if (n.correctionNo > 0) sb.append(" — ").append(correctionText(n.correctionNo));
            sb.append(".");
            String cat = categoryFor(c, n.recordId, fixList);
            if (!cat.isEmpty()) sb.append("\n").append(L.t("Kategoria:")).append(" ").append(L.cat(cat));
            sb.append("\n").append(n.criteria.isEmpty() ? cap(L.t("powtarza się ten sam błąd")) + "."
                    : cap(L.t("powtarza się błąd:")) + " " + trJoin(n.criteria));
        }
        Focus f = focus(c);
        if (f != null && !f.note.isEmpty()) sb.append("\n💬 ").append(f.note);
        sb.append("\n").append(L.t("Zanim wyślesz kolejną poprawkę, porozmawiaj z trenerem."));
        return sb.toString();
    }

    // "Kursant widzial" — wysylane przy pierwszym wyswietleniu banera
    public static void ackShown(Context ctx, List<Notice> list) {
        Context c = ctx.getApplicationContext();
        SharedPreferences p = prefs(c);
        for (Notice n : list) {
            if (n.acked || n.id.isEmpty()) continue;
            Set<String> sending = SENDING;
            synchronized (sending) { if (!sending.add(n.id)) continue; }
            try {
                JSONObject b = new JSONObject();
                b.put("ns_token", p.getString("ns_token", ""));
                b.put("ns_email", p.getString("ns_email", ""));
                b.put("ns_server", "new");
                b.put("id", n.id);
                http("POST", "/call-notices/ack", b.toString(), r -> {
                    synchronized (SENDING) { SENDING.remove(n.id); }
                    if (r.ok) {
                        Set<String> s = new HashSet<>(p.getStringSet("coach_acked", new HashSet<>()));
                        s.add(n.id);
                        p.edit().putStringSet("coach_acked", s).apply();
                    }
                });
            } catch (Exception e) { synchronized (SENDING) { SENDING.remove(n.id); } }
        }
    }

    private static final Set<String> SENDING = new HashSet<>();

    public static Focus focus(Context c) {
        String s = prefs(c).getString("coach_focus_json", "");
        if (s.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(s);
            Focus f = new Focus();
            JSONArray cr = o.optJSONArray("criteria");
            for (int k = 0; cr != null && k < cr.length(); k++) {
                Object v = cr.opt(k);
                String nm = v instanceof JSONObject ? ((JSONObject) v).optString("name", "") : String.valueOf(v);
                if (!nm.trim().isEmpty()) f.criteria.add(nm.trim());
            }
            // wskazowka trenera: pole "note" (albo podobne nazwy); dodatkowo wskazowki przy
            // poszczegolnych kryteriach ("Ogonek: ..."), gdy trener wpisal je osobno
            StringBuilder nb = new StringBuilder(textOf(o));
            for (int k = 0; cr != null && k < cr.length(); k++) {
                JSONObject co = cr.optJSONObject(k);
                if (co == null) continue;
                String cn = textOf(co);
                if (cn.isEmpty() || nb.indexOf(cn) >= 0) continue;
                if (nb.length() > 0) nb.append("\n");
                nb.append(co.optString("name", "").trim()).append(": ").append(cn);
            }
            f.note = nb.toString().trim();
            return f.criteria.isEmpty() ? null : f;
        } catch (Exception e) { return null; }
    }

    private static final String[] NOTE_KEYS = {"note", "notes", "hint", "tip", "comment", "message", "text", "description", "advice", "guidance"};

    private static String textOf(JSONObject o) {
        for (String k : NOTE_KEYS) {
            if (!o.has(k) || o.isNull(k)) continue;
            Object v = o.opt(k);
            String t = v instanceof JSONObject ? textOf((JSONObject) v) : String.valueOf(v);
            t = t.trim();
            if (!t.isEmpty() && !"null".equals(t)) return t;
        }
        return "";
    }

    // Nowo opanowane kryteria (porownanie z zapamietanymi). Przy pierwszym pobraniu tylko
    // zapamietujemy stan — gratulacje dopiero za nowe pozycje.
    public static List<String> takeNewAchieved(Context c) {
        List<String> out = new ArrayList<>();
        SharedPreferences p = prefs(c);
        String s = p.getString("coach_focus_json", "");
        if (s.isEmpty()) return out;
        try {
            JSONArray a = new JSONObject(s).optJSONArray("achieved");
            if (a == null) return out;
            boolean first = !p.getBoolean("coach_ach_init", false);
            Set<String> seen = new HashSet<>(p.getStringSet("coach_ach_seen", new HashSet<>()));
            boolean changed = false;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String nm = o.optString("name", "").trim();
                if (nm.isEmpty()) continue;
                String key = nm + "|" + o.optString("at", "");
                if (seen.add(key)) { changed = true; if (!first) out.add(nm); }
            }
            if (changed || first) p.edit().putStringSet("coach_ach_seen", seen).putBoolean("coach_ach_init", true).apply();
        } catch (Exception e) { }
        return out;
    }

    public static void clear(Context c) {
        prefs(c).edit().remove("coach_call_json").remove("coach_focus_json").remove("coach_acked")
                .remove("coach_ach_seen").remove("coach_ach_init").apply();
    }
}
