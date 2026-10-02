package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

// CEL DNIA z harmonogramu (pewne dane z NS): ile brakuje zaliczonych/wyslanych nagran i ile dni
// zostalo -> ile nagran dziennie, czy tempo wystarcza. Wynik w ustawieniach (goal_*), liczony
// najwyzej co 30 min. Na ekranie DAW: "🎯 Dziś: 1/2 nagrań · w tym tempie zdążysz ✓".
public final class HarmoGoal {

    private HarmoGoal() { }
    private static boolean busy = false;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static void refresh(Context c, Runnable done) {
        SharedPreferences p = prefs(c);
        String token = p.getString("ns_token", null);
        if (token == null || busy || System.currentTimeMillis() - p.getLong("goal_at", 0L) < 30 * 60 * 1000L) { if (done != null) done.run(); return; }
        busy = true;
        String email = p.getString("ns_email", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=asc&date_from=" + today, token, email, null, null, r1 -> {
            JSONObject v = firstWithReqs(r1);
            if (v != null) { withVisit(c, token, email, v, done); return; }
            NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=desc", token, email, null, null, r2 -> {
                JSONObject last = firstWithReqs(r2);
                if (last == null) { save(c, false, 0, 0, 0, "", false); busy = false; if (done != null) done.run(); return; }
                withVisit(c, token, email, last, done);
            });
        });
    }

    private static JSONArray coll(NsClient.Result r) {
        try {
            if (!r.ok) return null;
            String b = r.body.trim();
            return b.startsWith("[") ? new JSONArray(b) : new JSONObject(b).optJSONArray("collection");
        } catch (Exception e) { return null; }
    }

    private static JSONObject firstWithReqs(NsClient.Result r) {
        JSONArray arr = coll(r);
        for (int i = 0; arr != null && i < arr.length(); i++) {
            JSONObject v = arr.optJSONObject(i);
            JSONArray q = v != null ? v.optJSONArray("visit_record_requirements") : null;
            if (q != null && q.length() > 0) return v;
        }
        return null;
    }

    private static void withVisit(Context c, String token, String email, JSONObject visit, Runnable done) {
        String vDate = visit.optString("date", ""), vId = visit.optString("id", "");
        NsClient.request("GET", "/visit_dates?page=1&page_size=10&sort_by=date&sort_order=desc", token, email, null, null, r -> {
            String start = null;
            JSONArray arr = coll(r);
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject v = arr.optJSONObject(i);
                if (v == null || vId.equals(v.optString("id", ""))) continue;
                String dd = v.optString("date", "");
                if (!dd.isEmpty() && dd.compareTo(vDate) < 0) { start = HarmoGoal.dayAfter(dd); break; } // nagrania z dnia poprzedniego harmonogramu naleza do starego okresu
            }
            if (start == null) start = visit.optString("created_at", "1970-01-01");
            String from = start.length() >= 10 ? start.substring(0, 10) : start;
            String to = vDate.length() >= 10 ? vDate.substring(0, 10) : vDate;
            String q = "/harmonogram-stats/from-ns?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email) + "&ns_server=new&date_from=" + from + "&date_to=" + to;
            NsClient.backend("GET", q, null, rs -> {
                try {
                    JSONObject by = null;
                    JSONObject dj = new JSONObject(rs.body);
                    if (dj.optBoolean("ok", false)) by = dj.optJSONObject("byCategory");
                    JSONArray reqs = visit.optJSONArray("visit_record_requirements");
                    int totReq = 0, totCorrect = 0, totSent = 0;
                    for (int i = 0; reqs != null && i < reqs.length(); i++) {
                        JSONObject rq = reqs.optJSONObject(i);
                        if (rq == null) continue;
                        JSONObject cat = rq.optJSONObject("record_category");
                        String cid = cat != null ? cat.optString("id", "") : "";
                        int need = rq.optInt("records_count", 0), sent = 0, correct = 0;
                        if (by != null && by.optJSONObject(cid) != null) { sent = by.optJSONObject(cid).optInt("sent", 0); correct = by.optJSONObject(cid).optInt("correct", 0); }
                        totReq += need; totCorrect += Math.min(correct, need); totSent += Math.min(sent, need);
                    }
                    long deadline = parse(to), now = System.currentTimeMillis();
                    boolean active = deadline + 86400000L > now;
                    int daysLeft = (int) Math.max(1, Math.ceil((deadline + 86400000L - now) / 86400000.0));
                    int missing = Math.max(0, totReq - totCorrect), toSend = Math.max(0, totReq - totSent);
                    int perDay = (int) Math.ceil(Math.max(missing, toSend) / (double) daysLeft);
                    long st = parse(from);
                    int elapsed = (int) Math.max(1, Math.ceil((now - st) / 86400000.0));
                    boolean onTrack = totSent / (double) elapsed >= Math.max(missing, toSend) / (double) daysLeft - 0.01;
                    String dl = to.length() >= 10 ? to.substring(8, 10) + "." + to.substring(5, 7) : to;
                    save(c, active && totReq > 0, perDay, missing, daysLeft, dl, onTrack);
                } catch (Exception e) { }
                busy = false;
                if (done != null) done.run();
            });
        });
    }

    private static long parse(String d) {
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.substring(0, 10)).getTime(); } catch (Exception e) { return 0L; }
    }

    private static void save(Context c, boolean active, int perDay, int missing, int daysLeft, String dl, boolean onTrack) {
        prefs(c).edit().putLong("goal_at", System.currentTimeMillis()).putBoolean("goal_active", active)
                .putInt("goal_per_day", perDay).putInt("goal_missing", missing).putInt("goal_days", daysLeft)
                .putString("goal_deadline", dl).putBoolean("goal_on_track", onTrack).apply();
    }

    // "2026-10-01" -> "2026-10-02"
    public static String dayAfter(String d) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return f.format(new java.util.Date(f.parse(d.substring(0, 10)).getTime() + 86400000L));
        } catch (Exception e) { return d; }
    }

    public static void invalidate(Context c) { prefs(c).edit().putLong("goal_at", 0L).apply(); }
}
