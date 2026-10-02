package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// STATUS NAGRAN W NS (pewne dane): id rekordu -> "ok" (zaliczone), "bad" (do poprawy),
// "wait" (czeka na ocene; tez po podmianie pliku). Odswiezane najwyzej co 15 s (na liscie nagran co 30 s, gdy cos czeka).
public final class NsStatus {

    private static final Map<String, String> MAP = new HashMap<>();
    private static final Map<String, JSONObject> RECS = new HashMap<>(); // pelny rekord (ocena trenera)

    public static synchronized JSONObject record(String id) { return id == null ? null : RECS.get(id); }
    private static long fetchedAt = 0L;
    private static boolean busy = false;
    public static volatile int todayCount = 0; // nagrania z dzisiejsza data w NS

    private NsStatus() { }

    public static synchronized String get(String id) {
        if (id == null || id.isEmpty()) return null;
        return MAP.get(id);
    }

    public static void refresh(Context c, Runnable changed) {
        SharedPreferences p = c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String token = p.getString("ns_token", null);
        synchronized (NsStatus.class) {
            if (token == null || busy || System.currentTimeMillis() - fetchedAt < 15000L) return;
            busy = true;
        }
        NsClient.request("GET", "/records?page_size=100&sort_by=date&sort_order=desc", token, p.getString("ns_email", ""), null, null, r -> {
            boolean diff = false;
            try {
                if (r.ok) {
                    String b = r.body.trim();
                    JSONArray arr = b.startsWith("[") ? new JSONArray(b) : new JSONObject(b).optJSONArray("collection");
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                    int tc = 0;
                    synchronized (NsStatus.class) {
                        for (int i = 0; arr != null && i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            String id = o.optString("id", "");
                            boolean reviewed = !o.isNull("reviewed_at") && !o.optString("reviewed_at", "").isEmpty() && !o.isNull("is_correct");
                            String upd = o.optString("record_file_updated_by_author_at", o.optString("updated_by_author_at", ""));
                            boolean re = reviewed && newer(upd, o.optString("reviewed_at", ""));
                            String st = !reviewed || re ? "wait" : o.optBoolean("is_correct", false) ? "ok" : "bad";
                            if (!st.equals(MAP.put(id, st))) diff = true;
                            RECS.put(id, o);
                            org.json.JSONArray rr = o.optJSONArray("record_rates"); // tlumaczenia nazw kryteriow
                            for (int k = 0; rr != null && k < rr.length(); k++) {
                                JSONObject x = rr.optJSONObject(k);
                                if (x == null) continue;
                                RateNames.learn(c, x.optJSONObject("record_rate_category"));
                                RateNames.learn(c, x.optJSONObject("record_rate_unit"));
                            }
                            if (o.optString("date", "").startsWith(today)) tc++;
                        }
                        fetchedAt = System.currentTimeMillis();
                        if (tc != todayCount) diff = true;
                        todayCount = tc;
                    }
                }
            } catch (Exception e) { }
            synchronized (NsStatus.class) { busy = false; }
            if (diff && changed != null) changed.run();
        });
    }

    public static synchronized void invalidate() { fetchedAt = 0L; }

    public static synchronized int count(String st) { int n = 0; for (String v : MAP.values()) if (st.equals(v)) n++; return n; }

    public static synchronized boolean anyWaiting() { return MAP.containsValue("wait"); }

    // Czy plik podmieniony (upd) jest NOWSZY niz ocena (rev)? Porownanie czasow, nie napisow —
    // NS potrafi zwracac rozne formaty (Z / +02:00 / ulamki sekund / spacja zamiast T).
    public static boolean newer(String upd, String rev) {
        long u = isoMs(upd), r = isoMs(rev);
        return u > 0 && r > 0 && u > r + 1000L;
    }

    public static long isoMs(String s) {
        if (s == null) return 0L;
        s = s.trim();
        if (s.isEmpty() || "null".equals(s)) return 0L;
        if (s.matches("\\d{9,13}")) { long v = Long.parseLong(s); return v < 100000000000L ? v * 1000L : v; }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:[.,](\\d+))?)?\\s*(Z|[+-]\\d{2}:?\\d{2})?").matcher(s);
        if (!m.find()) return 0L;
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)),
                Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), m.group(6) == null ? 0 : Integer.parseInt(m.group(6)));
        long ms = c.getTimeInMillis();
        if (m.group(7) != null) { String f = (m.group(7) + "000").substring(0, 3); ms += Integer.parseInt(f); }
        String tz = m.group(8);
        if (tz == null) ms -= java.util.TimeZone.getDefault().getOffset(ms); // bez strefy = czas lokalny
        else if (!"Z".equals(tz)) {
            String t = tz.replace(":", "");
            int off = Integer.parseInt(t.substring(1, 3)) * 60 + Integer.parseInt(t.substring(3, 5));
            ms -= (t.charAt(0) == '-' ? -1 : 1) * off * 60000L;
        }
        return ms;
    }
}
