package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// STATUS NAGRAN W NS (pewne dane): id rekordu -> "ok" (zaliczone), "bad" (do poprawy),
// "wait" (czeka na ocene; tez po podmianie pliku). Odswiezane najwyzej co 2 minuty.
public final class NsStatus {

    private static final Map<String, String> MAP = new HashMap<>();
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
            if (token == null || busy || System.currentTimeMillis() - fetchedAt < 120000L) return;
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
                            boolean re = reviewed && !upd.isEmpty() && upd.compareTo(o.optString("reviewed_at", "")) > 0;
                            String st = !reviewed || re ? "wait" : o.optBoolean("is_correct", false) ? "ok" : "bad";
                            if (!st.equals(MAP.put(id, st))) diff = true;
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
}
