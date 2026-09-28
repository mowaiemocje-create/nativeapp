package com.pitchrec.nativetest;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// ZADANIA SPECJALNE (kategoria "Special") — ta sama lista co w PitchRec PWA. Przy opisie
// nagrania Special kursant wybiera, ktore zadanie wykonal; w Statystykach jest ono podswietlone.
public class SpecialTasks {

    public static class Task {
        public final String name, icon;
        public final String[] systems;   // puste = dla kazdego systemu
        public final int color;
        Task(String n, String[] s, String i, int c) { name = n; systems = s; icon = i; color = c; }
        public String shortName() { int k = name.indexOf(" – "); return k > 0 ? name.substring(0, k) : name; }
        public boolean forSystem(String sys) {
            if (systems.length == 0 || sys == null || sys.isEmpty()) return true;
            for (String s : systems) if (s.equalsIgnoreCase(sys)) return true;
            return false;
        }
    }

    private static String[] s(String... v) { return v; }

    public static final Task[] ALL = {
        new Task("Zamówienie video – zamów w restauracji jedzenie u kelnera i poproś kogoś o nagranie wideo.", s("Basic"), "🍽️", 0xFFE8820C),
        new Task("Wywiad z ciekawym zawodem – nagraj na wideo wywiad z ciekawym zawodem. Przygotuj pytania. Czas ok. 10 minut.", s("U1", "U1K", "FIX"), "🎤", 0xFF3B7DD8),
        new Task("Wystąpienie dla seniorów – wygłoś prezentację w domu seniora na dowolny temat.", s("U1", "U1K", "FIX", "K1"), "👵", 0xFF8B5FBF),
        new Task("Wystąpienie w domu dziecka – przygotuj i wygłoś wystąpienie dla dzieci.", s("U1", "U1K", "FIX", "K1"), "🧸", 0xFFE85D9C),
        new Task("Wystąpienie w klasie – 3 min – krótkie przemówienie w klasie, autoprezentacja.", s("Basic", "U1"), "🏫", 0xFF3FA34D),
        new Task("City Vlog – oprowadź widzów po swoim mieście. Wciel się w przewodnika.", s("U1K", "K1", "K2", "Full"), "🏙️", 0xFF1F9E8C),
        new Task("Spotkanie on-line – zorganizuj spotkanie online z kursantami (minimum 3 osoby).", s("U1K", "FIX", "K1", "K2"), "💻", 0xFF4C5FD5),
        new Task("Rozmowa z burmistrzem / wójtem / prezydentem – przeprowadź rozmowę z władzami lokalnymi.", s(), "🏛️", 0xFF2C3E6B),
        new Task("Wystąpienie w szkole ESBS – przygotuj i wygłoś prezentację o ESBS w szkole (około 6–7 minut).", s("FIX", "K1", "K2", "Full"), "📚", 0xFF8B5A2B),
        new Task("Kuchnia TV – wciel się w kucharza i nagraj wideo z przygotowywania posiłku.", s("U1K", "Full"), "👨‍🍳", 0xFFD6473C),
        new Task("Rozmowa z dziennikarzem – przeprowadź rozmowę z dziennikarzem na dowolny temat.", s("K1", "K2", "Full"), "📰", 0xFF6B7280),
        new Task("City Vlog – ferie / wakacje – nagraj vlog z wyjazdu lub miejscowości wypoczynkowej.", s("U1K", "K1", "K2", "Full"), "🏖️", 0xFF22A6B3),
        new Task("Wynajem mieszkania – umów się na oględziny mieszkania, które planujesz wynająć, i przygotuj pytania do agenta.", s("K2", "Full"), "🏠", 0xFFD4A017),
        new Task("Biuro podróży – przygotuj pytania i odwiedź biuro podróży (10–15 minut).", s("U1K", "K1", "K2", "Full"), "✈️", 0xFF3AAFD9),
        new Task("Wystąpienie w klasie – 5 min – krótkie przemówienie w klasie na dowolny temat.", s("FIX", "K1", "K2", "Full"), "🎓", 0xFF2E9E5B),
        new Task("Wystąpienie w radiu – udział w audycji radiowej.", s("FIX", "K1", "K2", "Full"), "📻", 0xFFE67E22),
        new Task("Salon samochodowy – umów się na jazdę testową w salonie i przygotuj pytania do dealera.", s("K2", "Full"), "🚗", 0xFFB03A2E),
        new Task("Wywiad z szefem / dyrektorem – przeprowadź wywiad z dyrektorem (wideo lub audio).", s("U1K", "FIX", "K1", "K2"), "💼", 0xFF4A5568),
        new Task("Wystąpienie w przedszkolu – krótkie przemówienie dla dzieci, np. wiersz lub autoprezentacja.", s("U1", "U1K", "FIX"), "🧒", 0xFFF0729A),
        new Task("Trening randek dla singli – wykorzystaj platformy online do spotkania i rozmowy.", s(), "💕", 0xFFD6336C),
        new Task("100 kontaktów w 1 dzień – nawiązanie 100 krótkich kontaktów w sklepach i miejscach publicznych.", s(), "🤝", 0xFF65A30D),
        new Task("100 telefonów w 1 dzień – wykonanie 100 telefonów (np. do różnych instytucji lub firm).", s(), "☎️", 0xFF7C3AED),
        new Task("TV – wystąpienie telewizyjne.", s("K1", "K2", "Full"), "📺", 0xFF1E3A8A),
    };

    // KOD ZADANIA w nazwie wysylanego pliku ("-ZS07" = 7. zadanie z listy) — dzieki temu z
    // rekordu w NewSpeech wiadomo, ktore zadanie wykonano, i czy trener je zaliczyl.
    public static int code(Task t) { for (int i = 0; i < ALL.length; i++) if (ALL[i] == t) return i + 1; return 0; }
    public static String codeTag(String name) {
        Task t = find(name);
        return t == null ? "" : String.format(java.util.Locale.US, "ZS%02d", code(t));
    }
    private static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[-_]ZS(\\d{2})(?!\\d)");

    // Stan zadania: "ok" = zaliczone przez trenera (na stale), "pending" = wyslane, czeka na ocene,
    // "rejected" = do poprawy, "" = jeszcze nie robione
    private static final Map<String, String> NS_STATE = new HashMap<>();

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    public static java.util.Set<String> approved(Context c) {
        return new java.util.HashSet<>(prefs(c).getStringSet("special_ok", new java.util.HashSet<>()));
    }

    public static String state(Context c, Task t) {
        if (approved(c).contains(t.name)) return "ok";
        synchronized (NS_STATE) { String s = NS_STATE.get(t.name); if (s != null) return s; }
        return done(c).containsKey(t.name) ? "pending" : "";
    }

    // Pobiera nagrania Special z NewSpeech i odczytuje z nazw plikow kody zadan
    public static void refreshFromNs(Context c, Runnable done) { refreshFromNs(c, done, true); }

    private static void refreshFromNs(Context c, Runnable done, boolean filtered) {
        String token = prefs(c).getString("ns_token", null);
        if (token == null) { if (done != null) done.run(); return; }
        String email = prefs(c).getString("ns_email", "");
        String catId = NsClient.categoryId("Special");
        // najpierw tylko kategoria Special; gdyby serwer nie znal filtra — 200 najnowszych nagran
        String q = filtered && catId != null
                ? "/records?page_size=100&sort_by=date&sort_order=desc&record_category_id=" + catId
                : "/records?page_size=200&sort_by=date&sort_order=desc";
        NsClient.request("GET", q, token, email, null, null, r -> {
            if (!r.ok && filtered && catId != null && r.status != 401) { refreshFromNs(c, done, false); return; }
            if (r.ok && r.body != null) {
                try {
                    String b = r.body.trim();
                    org.json.JSONArray arr = b.startsWith("[") ? new org.json.JSONArray(b) : new org.json.JSONObject(b).optJSONArray("collection");
                    java.util.Set<String> ok = approved(c);
                    Map<String, String> st = new HashMap<>();
                    for (int i = 0; arr != null && i < arr.length(); i++) {
                        org.json.JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        String cid = o.optString("record_category_id", "");
                        org.json.JSONObject cat = o.optJSONObject("record_category");
                        if (cat != null && cid.isEmpty()) cid = cat.optString("id", "");
                        boolean special = (catId != null && catId.equals(cid)) || (cat != null && "Special".equalsIgnoreCase(cat.optString("name", "")));
                        if (!special) continue;
                        java.util.regex.Matcher m = CODE.matcher(o.toString());
                        if (!m.find()) continue;
                        int k = Integer.parseInt(m.group(1));
                        if (k < 1 || k > ALL.length) continue;
                        String nm = ALL[k - 1].name;
                        boolean reviewed = !o.isNull("reviewed_at") && !o.optString("reviewed_at", "").isEmpty();
                        String upd = o.optString("record_file_updated_by_author_at", o.optString("updated_by_author_at", ""));
                        boolean reReview = reviewed && NsStatus.newer(upd, o.optString("reviewed_at", ""));
                        String s;
                        if (reviewed && !reReview && !o.isNull("is_correct") && o.optBoolean("is_correct", false)) s = "ok";
                        else if (reviewed && !reReview && !o.isNull("is_correct")) s = "rejected";
                        else s = "pending";
                        if ("ok".equals(s)) ok.add(nm);
                        String prev = st.get(nm);
                        if (prev == null || "rejected".equals(prev)) st.put(nm, s); // najnowsze nagranie decyduje (lista od najnowszych)
                    }
                    prefs(c).edit().putStringSet("special_ok", ok).apply();
                    synchronized (NS_STATE) { NS_STATE.clear(); NS_STATE.putAll(st); }
                } catch (Exception e) { /* zostaje stan z telefonu */ }
            }
            if (done != null) done.run();
        });
    }

    public static Task find(String name) {
        if (name == null || name.isEmpty()) return null;
        for (Task t : ALL) if (t.name.equals(name) || t.shortName().equals(name)) return t;
        return null;
    }

    // Wykonane zadania: nazwa -> data pierwszego nagrania (z nagran na tym telefonie)
    private static Map<String, Long> doneCache = null;
    private static long doneAt = 0;

    public static synchronized Map<String, Long> done(Context c) {
        if (doneCache != null && System.currentTimeMillis() - doneAt < 2000) return doneCache;
        Map<String, Long> out = new HashMap<>();
        doneCache = out;
        doneAt = System.currentTimeMillis();
        File[] files = c.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files == null) return out;
        for (File f : files) {
            RecMeta m = RecMeta.load(c, f.getName());
            Task t = find(m.special);
            if (t == null) continue;
            Long prev = out.get(t.name);
            if (prev == null || f.lastModified() < prev) out.put(t.name, f.lastModified());
        }
        return out;
    }
}
