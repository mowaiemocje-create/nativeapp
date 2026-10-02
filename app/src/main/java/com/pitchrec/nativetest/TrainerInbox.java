package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// "OD TRENERA" — jedna lista wszystkiego, co przyszlo od trenera: oceny (zaliczone / do poprawy),
// komentarze glosowe, odpowiedzi na dziennik, dni telefonu, lista kursantow do dzwonienia.
// Zapisywana w telefonie; kazdy wpis ma "przeczytane" — z tego licza sie kropki na dolnym menu.
public final class TrainerInbox {

    public static final String T_OK = "ok", T_BAD = "bad", T_VOICE = "voice", T_DIARY = "diary", T_CALLS = "calls", T_LIST = "list";

    public static final class Ev {
        public String id, type, title, sub, rec, date;
        public long ts;
        public boolean read;
        JSONObject json() {
            JSONObject o = new JSONObject();
            try { o.put("id", id).put("type", type).put("title", title).put("sub", sub).put("rec", rec).put("date", date).put("ts", ts).put("read", read); } catch (Exception e) { }
            return o;
        }
        static Ev of(JSONObject o) {
            Ev e = new Ev();
            e.id = o.optString("id", ""); e.type = o.optString("type", ""); e.title = o.optString("title", "");
            e.sub = o.optString("sub", ""); e.rec = o.optString("rec", ""); e.date = o.optString("date", "");
            e.ts = o.optLong("ts", 0L); e.read = o.optBoolean("read", false);
            return e;
        }
    }

    public interface Listener { void changed(); }
    public static volatile Listener listener;

    private TrainerInbox() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static synchronized List<Ev> all(Context c) {
        List<Ev> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("inbox_json", "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null) out.add(Ev.of(o)); }
        } catch (Exception e) { }
        java.util.Collections.sort(out, (x, y) -> Long.compare(y.ts, x.ts));
        return out;
    }

    private static void save(Context c, List<Ev> l) {
        java.util.Collections.sort(l, (x, y) -> Long.compare(y.ts, x.ts));
        JSONArray a = new JSONArray();
        for (int i = 0; i < Math.min(80, l.size()); i++) a.put(l.get(i).json());
        prefs(c).edit().putString("inbox_json", a.toString()).apply();
        Listener li = listener;
        if (li != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(li::changed);
    }

    public static synchronized boolean has(Context c, String id) {
        for (Ev e : all(c)) if (e.id.equals(id)) return true;
        return false;
    }

    // true = nowy wpis (wczesniej go nie bylo)
    public static synchronized boolean add(Context c, String id, String type, String title, String sub, String rec, String date, long ts, boolean read) {
        List<Ev> l = all(c);
        for (Ev e : l) if (e.id.equals(id)) return false;
        Ev e = new Ev();
        e.id = id; e.type = type; e.title = title == null ? "" : title; e.sub = sub == null ? "" : sub;
        e.rec = rec == null ? "" : rec; e.date = date == null ? "" : date; e.ts = ts > 0 ? ts : System.currentTimeMillis(); e.read = read;
        l.add(e);
        save(c, l);
        return true;
    }

    public static synchronized int unread(Context c, String... types) {
        int n = 0;
        for (Ev e : all(c)) if (!e.read) for (String t : types) if (t.equals(e.type)) { n++; break; }
        return n;
    }

    public static synchronized void markRead(Context c, String rec, String date, String... types) {
        List<Ev> l = all(c);
        boolean ch = false;
        for (Ev e : l) {
            if (e.read) continue;
            boolean t = types.length == 0;
            for (String ty : types) if (ty.equals(e.type)) t = true;
            if (!t) continue;
            if (rec != null && !rec.equals(e.rec)) continue;
            if (date != null && !date.equals(e.date)) continue;
            e.read = true; ch = true;
        }
        if (ch) save(c, l);
    }

    public static synchronized void clear(Context c) { prefs(c).edit().remove("inbox_json").apply(); }
}
