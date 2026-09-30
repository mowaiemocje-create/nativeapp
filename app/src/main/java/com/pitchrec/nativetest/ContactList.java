package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// LISTA TELEFONOW OD TRENERA — kategoria "Phone do Kursanta/Trenera".
// Trener w panelu tworzy liste kursantow (za ich zgoda na udostepnienie numeru) i dopina ja do
// harmonogramu kursanta. Aplikacja pobiera liste z biezacego okresu (backend /contact-lists/mine),
// zapamietuje ja (dziala tez chwilowo bez internetu) i odswieza najwyzej co 10 min.
public final class ContactList {

    public static final String CATEGORY = "Phone do Kursanta/Trenera";

    public static final class Member {
        public final String id, name, phone, note;
        Member(String id, String name, String phone, String note) { this.id = id; this.name = name; this.phone = phone; this.note = note; }
    }

    public static volatile boolean lastFailed = false;

    private ContactList() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    // Nazwa listy ("" = brak listy)
    public static String listName(Context c) {
        JSONObject l = cached(c);
        return l == null ? "" : l.optString("name", "");
    }

    public static List<Member> members(Context c) {
        List<Member> out = new ArrayList<>();
        JSONObject l = cached(c);
        JSONArray a = l != null ? l.optJSONArray("members") : null;
        for (int i = 0; a != null && i < a.length(); i++) {
            JSONObject m = a.optJSONObject(i);
            if (m == null) continue;
            String ph = m.optString("phone", "").trim();
            if (ph.isEmpty() || "null".equals(ph)) continue;
            String note = m.optString("note", "");
            out.add(new Member(m.optString("ns_student_id", ""), m.optString("name", "").trim(), ph, "null".equals(note) ? "" : note.trim()));
        }
        return out;
    }

    public static boolean loaded(Context c) { return prefs(c).getLong("contact_list_at", 0L) > 0; }

    private static JSONObject cached(Context c) {
        try {
            String s = prefs(c).getString("contact_list_json", "");
            if (s.isEmpty() || "null".equals(s)) return null;
            return new JSONObject(s);
        } catch (Exception e) { return null; }
    }

    public static void refresh(Context c, boolean force, Runnable changed) {
        SharedPreferences p = prefs(c);
        String token = p.getString("ns_token", null);
        if (token == null) return;
        if (!force && System.currentTimeMillis() - p.getLong("contact_list_at", 0L) < 10 * 60 * 1000L) return;
        String q = "/contact-lists/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(p.getString("ns_email", "")) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            lastFailed = true;
            try {
                if (r.ok) {
                    JSONObject o = new JSONObject(r.body);
                    if (o.optBoolean("ok", false)) {
                        JSONObject l = o.optJSONObject("list");
                        p.edit().putString("contact_list_json", l == null ? "" : l.toString()).putLong("contact_list_at", System.currentTimeMillis()).apply();
                        lastFailed = false;
                    }
                }
            } catch (Exception e) { }
            if (changed != null) changed.run(); // zawsze — ekran chowa "Wczytywanie"
        });
    }

    public static void dial(Context c, String phone) {
        try {
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:" + phone.replaceAll("[^0-9+]", "")));
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) { }
    }
}
