package com.pitchrec.nativetest;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Klient serwera NewSpeech — ten sam mechanizm co w PitchRec PWA: wszystkie zapytania idą
// przez nasze proxy (Cloudflare Worker), które dokłada wymagane przez serwer nagłówki
// (Origin itp.). Sieć działa na osobnym wątku, wynik wraca na wątek UI przez callback.
public class NsClient {

    public static final String NS_BASE = "https://pitchrec-proxy.mowaiemocje.workers.dev";

    // Kategorie nagrań (nazwa wyświetlana -> ID w NS) — ta sama lista co w PWA.
    public static final String[] CATEGORIES = {
            "Sklepy", "Rodzina", "Telefon rodzina/znajomi", "Wystąpienie", "Praca / Szkoła",
            "Monolog / Czytanie", "Przechodzień", "Miasto - inne", "Przyjaciele", "Special",
            "Telefon do miasta", "W grupie", "Zerówka", "Phone do Kursanta/Trenera",
            "Restauracja Kelner", "McDrive, DriveThru"
    };
    private static final Map<String, String> CAT_MAP = new LinkedHashMap<>();
    static {
        CAT_MAP.put("Sklepy", "58c2042d-b605-4794-ab97-053f5b2ccc80");
        CAT_MAP.put("Rodzina", "cf4e385b-a930-403e-9213-ea7a0bda4d42");
        CAT_MAP.put("Telefon rodzina/znajomi", "43e7d2e6-d94d-4d1f-baac-2d01915c5e03");
        CAT_MAP.put("Wystapienie", "8d5b0c84-a83f-4484-91fd-6d52db0d0e98");
        CAT_MAP.put("Praca / Szkola", "fbbef91a-8fbf-4221-be65-d370576cbdfd");
        CAT_MAP.put("Monolog / Czytanie", "e032b74a-39f0-4d37-b1f1-de6917a9dd49");
        CAT_MAP.put("Przechodzien", "f916676c-7bf3-45dd-8eec-55fc970e1375");
        CAT_MAP.put("Miasto - inne", "5ce9b402-f573-4c01-a252-768ec4b240fb");
        CAT_MAP.put("Przyjaciele", "836de6e6-ae9e-4087-9b48-34d1c5491cb2");
        CAT_MAP.put("Special", "182e14d9-f206-49b1-9858-0a1f96887f35");
        CAT_MAP.put("Telefon do miasta", "5f9d8ef9-321b-43f4-aec1-ce6548a5f937");
        CAT_MAP.put("W grupie", "4a9d6a90-2419-4c82-90fa-3d546e49e215");
        CAT_MAP.put("Zerowka", "7366633b-49e8-4be1-acb7-878083b1d613");
        CAT_MAP.put("Phone do Kursanta/Trenera", "afbca62a-e6a8-4e20-b8ca-7c73f27b5887");
        CAT_MAP.put("Restauracja Kelner", "f4c67148-af37-40e1-a557-e557a8503f45");
        CAT_MAP.put("McDrive, DriveThru", "f4c605b7-eb81-403f-b799-52b70143a5e8");
    }

    // Porównanie bez polskich znaków i wielkości liter ("Wystąpienie" == "Wystapienie").
    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('ł', 'l').replace('Ł', 'L').toLowerCase(Locale.ROOT).trim();
    }

    public static String categoryId(String displayName) {
        String k = norm(displayName);
        synchronized (CAT_MAP) {
            for (Map.Entry<String, String> e : CAT_MAP.entrySet()) {
                if (norm(e.getKey()).equals(k)) return e.getValue();
            }
        }
        return null;
    }

    public static class Result {
        public boolean ok;
        public int status;          // kod HTTP (0 = brak połączenia)
        public String err;          // komunikat błędu dla użytkownika
        public String token, email, userId;
        public String body;
        public boolean mute;        // nie wyslano: w nagraniu dluga calkowita cisza (MuteCheck)
        public boolean isAuthError() { return status == 401 || status == 403; }
        // 429 = dzienny limit NOWYCH nagran w NS (serwer nie zawsze dodaje opis ze slowem "limit")
        public boolean isLimitError() { return status == 429 || (err != null && err.toLowerCase(Locale.ROOT).contains("limit")); }
    }

    public interface Callback { void done(Result r); }

    private static final ExecutorService NET = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // POSTEP WYSYLKI nagrania (klucz: nazwa oryginalnego pliku) — dla przycisku "Wysyłanie…".
    // {calosc 0..1, etap (0 = zmniejszanie, 1 = wysylanie, 2 = serwer przetwarza), postep etapu 0..1}
    public static final java.util.concurrent.ConcurrentHashMap<String, float[]> PROGRESS = new java.util.concurrent.ConcurrentHashMap<>();

    private static void prog(String key, int stage, double f, boolean shrunk) {
        float pf = (float) Math.max(0, Math.min(1, f));
        float all;
        if (stage == 3) all = 0f;           // sprawdzanie nagrania (cisza)
        else if (stage == 0) all = 0.5f * pf;
        else if (stage == 1) all = shrunk ? 0.5f + 0.5f * pf : pf;
        else all = 1f;
        PROGRESS.put(key, new float[]{all, stage, pf});
    }

    // kontekst aplikacji (pamiec wynikow sprawdzania ciszy) — ustawiany przy starcie aplikacji
    public static volatile android.content.Context appCtx;

    private static void runAsync(java.util.concurrent.Callable<Result> job, Callback cb) {
        NET.execute(() -> {
            Result r;
            try { r = job.call(); }
            catch (Exception e) { r = new Result(); r.ok = false; r.status = 0; r.err = L.t("Brak połączenia z serwerem NewSpeech"); }
            final Result fr = r;
            MAIN.post(() -> cb.done(fr));
        });
    }

    private static HttpURLConnection open(String path, String method, String token, String email) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(NS_BASE + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(120000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("X-NS-Server", "test");
        if (token != null) c.setRequestProperty("Authorization", "Token token=" + token);
        if (email != null) c.setRequestProperty("Email", email);
        return c;
    }

    private static String readBody(HttpURLConnection c) {
        try {
            InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in == null) return "";
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            in.close();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static Result finish(HttpURLConnection c) throws Exception {
        Result r = new Result();
        r.status = c.getResponseCode();
        r.body = readBody(c);
        r.ok = r.status >= 200 && r.status < 300;
        if (!r.ok) {
            String msg = null;
            try { JSONObject eo = new JSONObject(r.body); msg = eo.optString("message", ""); if (msg.isEmpty()) msg = eo.optString("error", ""); } catch (Exception e) { }
            r.err = (msg != null && !msg.isEmpty()) ? msg : (L.t("Błąd ") + r.status);
            if (r.status == 413) r.err = L.t("Nagranie jest za duże dla serwera NewSpeech");
        }
        return r;
    }

    // POST /session — logowanie emailem i hasłem.
    public static void login(String email, String password, Callback cb) {
        runAsync(() -> {
            HttpURLConnection c = open("/session", "POST", null, null);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            Result r = finish(c);
            if (r.ok) {
                JSONObject d = new JSONObject(r.body);
                String tok = d.optString("auth_token", "");
                if (tok.isEmpty()) tok = d.optString("token", "");
                if (tok.isEmpty()) tok = d.optString("jwt", "");
                if (tok.isEmpty() && d.optJSONObject("data") != null) tok = d.optJSONObject("data").optString("auth_token", "");
                if (tok.isEmpty()) { r.ok = false; r.err = L.t("Serwer nie zwrócił tokenu logowania"); return r; }
                r.token = tok;
                String em = d.optString("email", "");
                if (em.isEmpty() && d.optJSONObject("user") != null) em = d.optJSONObject("user").optString("email", "");
                r.email = em.isEmpty() ? email : em;
                String id = d.optString("id", "");
                if (id.isEmpty() && d.optJSONObject("user") != null) id = d.optJSONObject("user").optString("id", "");
                r.userId = id;
            } else if (r.status == 401 || r.status == 422 || r.status == 400) {
                r.err = L.t("Nieprawidłowy email lub hasło");
            }
            return r;
        }, cb);
    }

    // Sprawdza, czy zapisany token jest nadal ważny (401/403 = sesja wygasła).
    public static void verify(String token, String email, String userId, Callback cb) {
        runAsync(() -> {
            String path = (userId != null && !userId.isEmpty()) ? "/users/" + userId : "/record_categories";
            HttpURLConnection c = open(path, "GET", token, email);
            c.setReadTimeout(15000);
            return finish(c);
        }, cb);
    }

    // Pobiera kategorie z serwera i dopisuje brakujące do mapy (jak nsLoadTasks w PWA).
    public static void loadCategories(String token, String email) {
        runAsync(() -> {
            HttpURLConnection c = open("/record_categories", "GET", token, email);
            Result r = finish(c);
            if (r.ok) {
                JSONArray arr;
                String b = r.body.trim();
                if (b.startsWith("[")) arr = new JSONArray(b);
                else {
                    JSONObject o = new JSONObject(b);
                    arr = o.optJSONArray("collection");
                    if (arr == null) arr = o.optJSONArray("record_categories");
                    if (arr == null) arr = o.optJSONArray("data");
                }
                if (arr != null) {
                    synchronized (CAT_MAP) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject t = arr.optJSONObject(i);
                            if (t == null) continue;
                            String id = t.optString("id", "");
                            if (id.isEmpty()) continue;
                            String pl = t.optString("name_pl", "");
                            String nm = t.optString("name", "");
                            if (!pl.isEmpty() && categoryId(pl) == null) CAT_MAP.put(pl, id);
                            if (!nm.isEmpty() && categoryId(nm) == null) CAT_MAP.put(nm, id);
                        }
                    }
                }
            }
            return r;
        }, r -> { });
    }

    // Lista nagran DO POPRAWY — szybkie zrodlo (nowy backend, jak w PitchRec PWA).
    public static class FixEntry {
        public String id, categoryName, weakText;
        public long reviewedAt;
        public long recordedAt, firstSentAt;   // unix [s]; 0 = brak (stary cache backendu)
        public String recordDate = "";
        public boolean allGood;
    }

    public interface FixCallback { void done(java.util.List<FixEntry> list, String err); }

    public static void fetchNeedsCorrection(String token, String email, boolean force, FixCallback cb) {
        NET.execute(() -> {
            java.util.List<FixEntry> list = new java.util.ArrayList<>();
            String err = null;
            try {
                String q = "ns_token=" + java.net.URLEncoder.encode(token, "UTF-8")
                        + "&ns_email=" + java.net.URLEncoder.encode(email == null ? "" : email, "UTF-8")
                        + "&ns_server=new" + (force ? "&force=1" : "");
                HttpURLConnection c = (HttpURLConnection) new URL("https://newspeech-backend.mowaiemocje.workers.dev/needs-correction/from-ns?" + q).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestProperty("Accept", "application/json");
                int st = c.getResponseCode();
                String body = readBody(c);
                if (st == 401 || st == 403) err = "AUTH";
                else if (st < 200 || st >= 300) err = L.t("Błąd ") + st;
                else {
                    JSONObject d = new JSONObject(body);
                    if (!d.optBoolean("ok", false)) err = d.optString("error", L.t("Błąd serwera"));
                    JSONArray arr = d.optJSONArray("entries");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            FixEntry fe = new FixEntry();
                            fe.id = o.optString("id", "");
                            fe.categoryName = o.optString("category_name", "");
                            fe.reviewedAt = o.optLong("reviewed_at", 0L);
                            fe.recordedAt = o.isNull("recorded_at") ? 0L : o.optLong("recorded_at", 0L);
                            fe.firstSentAt = o.isNull("first_sent_at") ? 0L : o.optLong("first_sent_at", 0L);
                            fe.recordDate = o.isNull("record_date") ? "" : o.optString("record_date", "");
                            fe.allGood = o.optBoolean("all_good", false);
                            StringBuilder sb = new StringBuilder();
                            JSONArray wk = o.optJSONArray("weak_areas");
                            if (wk != null) {
                                for (int j = 0; j < Math.min(4, wk.length()); j++) {
                                    JSONObject w = wk.optJSONObject(j);
                                    if (w == null) continue;
                                    String unit = w.optString("unit_name", "");
                                    if (unit.isEmpty()) unit = w.optInt("percent_mark", 0) + "%";
                                    if (sb.length() > 0) sb.append("\n");
                                    sb.append("• ").append(RateNames.tr(w.optString("category_name", ""))).append(" — ").append(RateNames.tr(unit));
                                }
                            }
                            fe.weakText = sb.toString();
                            if (!fe.id.isEmpty()) list.add(fe);
                        }
                    }
                }
            } catch (Exception e) {
                err = L.t("Brak połączenia z serwerem");
            }
            // szybkie zrodlo nie dziala (np. blad 500 bazy) — lista wprost z NewSpeech
            if (err != null && !"AUTH".equals(err)) {
                java.util.List<FixEntry> fb = fixFromNs(token, email);
                if (!fb.isEmpty() || nsReachable(token, email)) { list.clear(); list.addAll(fb); err = null; }
            }
            final String fErr = err;
            MAIN.post(() -> cb.done(list, fErr));
        });
    }


    // ── Ogolne zapytanie do NS (JSON / formularz) — uzywane przez Dziennik i Statystyki ──
    public static void request(String method, String path, String token, String email, String contentType, byte[] body, Callback cb) {
        runAsync(() -> {
            HttpURLConnection c = open(path, method, token, email);
            c.setReadTimeout(30000);
            if (body != null) {
                c.setDoOutput(true);
                if (contentType != null) c.setRequestProperty("Content-Type", contentType);
                try (OutputStream os = c.getOutputStream()) { os.write(body); }
            }
            return finish(c);
        }, cb);
    }

    // Zapytanie do naszego nowego backendu (Cloudflare Worker "newspeech-backend")
    public static void backend(String method, String pathAndQuery, String jsonBody, Callback cb) {
        runAsync(() -> {
            Result br;
            try { br = backendNow(method, pathAndQuery, jsonBody); }
            catch (Exception e) { br = new Result(); br.ok = false; br.status = 0; br.err = L.t("Brak połączenia z serwerem"); }
            // Awaria szybkiego zrodla (np. 500 z bazy) — statystyki harmonogramu liczymy wprost z NS
            if (!br.ok && br.status != 401 && br.status != 403 && pathAndQuery.startsWith("/harmonogram-stats/from-ns")) {
                Result fb = harmoStatsFromNs(pathAndQuery);
                if (fb != null) return fb;
            }
            return br;
        }, cb);
    }

    private static Result backendNow(String method, String pathAndQuery, String jsonBody) throws Exception {
        {
            HttpURLConnection c = (HttpURLConnection) new URL("https://newspeech-backend.mowaiemocje.workers.dev" + pathAndQuery).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept", "application/json");
            if (jsonBody != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = c.getOutputStream()) { os.write(jsonBody.getBytes(StandardCharsets.UTF_8)); }
            }
            return finish(c);
        }
    }

    private static boolean nsReachable(String token, String email) {
        try { HttpURLConnection c = open("/records?page=1&page_size=1", "GET", token, email); c.setReadTimeout(20000); return c.getResponseCode() == 200; }
        catch (Exception e) { return false; }
    }

    private static String qp(String pathAndQuery, String key) {
        int i = pathAndQuery.indexOf('?');
        if (i < 0) return "";
        for (String kv : pathAndQuery.substring(i + 1).split("&")) {
            int e = kv.indexOf('=');
            if (e > 0 && kv.substring(0, e).equals(key)) {
                try { return java.net.URLDecoder.decode(kv.substring(e + 1), "UTF-8"); } catch (Exception x) { return kv.substring(e + 1); }
            }
        }
        return "";
    }

    // Nagrania kursanta wprost z NS (najnowsze, do maxPages*100) — zapas, gdy backend nie dziala
    private static java.util.List<JSONObject> nsRecords(String token, String email, String minDay, int maxPages) {
        java.util.List<JSONObject> out = new java.util.ArrayList<>();
        try {
            for (int page = 1; page <= maxPages; page++) {
                HttpURLConnection c = open("/records?page=" + page + "&page_size=100&sort_by=date&sort_order=desc", "GET", token, email);
                c.setReadTimeout(45000);
                if (c.getResponseCode() != 200) break;
                String b = readBody(c).trim();
                JSONArray arr = b.startsWith("[") ? new JSONArray(b) : new JSONObject(b).optJSONArray("collection");
                if (arr == null || arr.length() == 0) break;
                boolean older = false;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    out.add(o);
                    String day = o.optString("date", o.optString("created_at", ""));
                    if (minDay != null && day.length() >= 10 && day.substring(0, 10).compareTo(minDay) < 0) older = true;
                }
                if (arr.length() < 100 || older) break;
            }
        } catch (Exception e) { }
        return out;
    }

    private static String catNameOf(JSONObject rec) {
        JSONObject c = rec.optJSONObject("record_category");
        if (c == null) return "";
        String n = c.optString("name_pl", "");
        return n.isEmpty() || "null".equals(n) ? c.optString("name", "") : n;
    }

    private static boolean pendingReReview(JSONObject rec) {
        String rv = rec.optString("reviewed_at", ""), fu = rec.optString("record_file_updated_by_author_at", "");
        if (fu.isEmpty() || "null".equals(fu)) fu = rec.optString("updated_by_author_at", "");
        if (rv.isEmpty() || "null".equals(rv) || fu.isEmpty() || "null".equals(fu)) return false;
        return isoSec(fu) > isoSec(rv);
    }

    static long isoSec(String iso) {
        if (iso == null || iso.length() < 19) return 0L;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            long t = f.parse(iso.substring(0, 19)).getTime() / 1000L;
            // przesuniecie strefy z napisu (np. +02:00)
            String rest = iso.substring(19).replaceAll("^\\.\\d+", "");
            if (rest.length() >= 6 && (rest.charAt(0) == '+' || rest.charAt(0) == '-')) {
                int sg = rest.charAt(0) == '+' ? 1 : -1;
                t -= sg * (Integer.parseInt(rest.substring(1, 3)) * 3600L + Integer.parseInt(rest.substring(4, 6)) * 60L);
            }
            return t;
        } catch (Exception e) { return 0L; }
    }

    private static boolean has(JSONObject o, String k) { String v = o.optString(k, ""); return !v.isEmpty() && !"null".equals(v); }

    // Zapas dla /harmonogram-stats/from-ns: te same liczby (wyslane / ocenione / zaliczone per kategoria)
    private static Result harmoStatsFromNs(String pathAndQuery) {
        String token = qp(pathAndQuery, "ns_token"), email = qp(pathAndQuery, "ns_email");
        String from = qp(pathAndQuery, "date_from"), to = qp(pathAndQuery, "date_to");
        if (token.isEmpty()) return null;
        if (from.isEmpty()) from = "1970-01-01";
        if (to.isEmpty()) to = "2999-12-31";
        java.util.List<JSONObject> recs = nsRecords(token, email, from, 10);
        if (recs.isEmpty()) return null;
        try {
            java.util.Map<String, int[]> byName = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<String>> ids = new java.util.HashMap<>();
            int s = 0, rv = 0, ok = 0;
            for (JSONObject r : recs) {
                String day = r.optString("created_at", "");
                if (day.length() < 10) continue;
                day = day.substring(0, 10);
                if (day.compareTo(from.substring(0, Math.min(10, from.length()))) < 0 || day.compareTo(to.substring(0, Math.min(10, to.length()))) > 0) continue;
                JSONObject cat = r.optJSONObject("record_category");
                String cid = cat != null ? cat.optString("id", "") : r.optString("record_category_id", "");
                String nm = catNameOf(r);
                String key = nm.isEmpty() ? cid : nm;
                int[] t = byName.get(key);
                if (t == null) { t = new int[3]; byName.put(key, t); ids.put(key, new java.util.ArrayList<>()); }
                if (!cid.isEmpty() && !ids.get(key).contains(cid)) ids.get(key).add(cid);
                boolean reviewed = has(r, "reviewed_at") && !pendingReReview(r);
                boolean correct = reviewed && r.optBoolean("is_correct", false);
                t[0]++; if (reviewed) t[1]++; if (correct) t[2]++;
                s++; if (reviewed) rv++; if (correct) ok++;
            }
            JSONObject by = new JSONObject();
            for (java.util.Map.Entry<String, int[]> e : byName.entrySet()) {
                for (String id : ids.get(e.getKey())) {
                    JSONObject o = new JSONObject();
                    o.put("category_name", e.getKey());
                    o.put("sent", e.getValue()[0]);
                    o.put("reviewed", e.getValue()[1]);
                    o.put("correct", e.getValue()[2]);
                    by.put(id, o);
                }
            }
            JSONObject res = new JSONObject();
            res.put("ok", true);
            res.put("fallback", true);
            res.put("overall", new JSONObject().put("sent", s).put("reviewed", rv).put("correct", ok));
            res.put("byCategory", by);
            Result r = new Result();
            r.ok = true; r.status = 200; r.body = res.toString();
            return r;
        } catch (Exception e) { return null; }
    }

    // Zapas dla listy KOREKTA: odrzucone (i jeszcze nie poprawione) nagrania wprost z NS
    private static java.util.List<FixEntry> fixFromNs(String token, String email) {
        java.util.List<FixEntry> list = new java.util.ArrayList<>();
        for (JSONObject o : nsRecords(token, email, null, 3)) {
            if (!has(o, "reviewed_at") || o.optBoolean("is_correct", true) || pendingReReview(o)) continue;
            if (o.isNull("is_correct")) continue;
            FixEntry fe = new FixEntry();
            fe.id = o.optString("id", "");
            fe.categoryName = catNameOf(o);
            fe.reviewedAt = isoSec(o.optString("reviewed_at", ""));
            fe.recordedAt = isoSec(has(o, "record_file_updated_by_author_at") ? o.optString("record_file_updated_by_author_at", "") : o.optString("created_at", ""));
            fe.firstSentAt = isoSec(o.optString("created_at", ""));
            fe.recordDate = has(o, "date") ? o.optString("date", "") : "";
            StringBuilder sb = new StringBuilder();
            JSONArray rates = o.optJSONArray("record_rates");
            int weak = 0;
            for (int j = 0; rates != null && j < rates.length(); j++) {
                JSONObject rr = rates.optJSONObject(j);
                if (rr == null) continue;
                JSONObject unit = rr.optJSONObject("record_rate_unit"), rc = rr.optJSONObject("record_rate_category");
                if (unit == null || unit.optInt("percent_mark", 100) >= 100) continue;
                weak++;
                if (weak > 4) continue;
                String cn = rc == null ? "" : (has(rc, "name_pl") ? rc.optString("name_pl", "") : rc.optString("name", ""));
                String un = has(unit, "name_pl") ? unit.optString("name_pl", "") : has(unit, "name") ? unit.optString("name", "") : unit.optInt("percent_mark", 0) + "%";
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(RateNames.tr(cn)).append(" — ").append(RateNames.tr(un));
            }
            fe.weakText = sb.toString();
            fe.allGood = weak == 0;
            if (!fe.id.isEmpty()) list.add(fe);
        }
        return list;
    }

    // Wyslanie formularza (np. Google Forms) — "wyslij i zapomnij", bledy ignorowane.
    // ── GOOGLE FORMS (kwestionariusz nagrania + dziennik) ──
    // Wlasny watek (nie czeka za dlugimi wysylkami nagran), naglowki jak przegladarka,
    // sprawdzenie kodu odpowiedzi, a przy bledzie / braku internetu — kolejka zapisana w
    // telefonie i ponowienie przy nastepnym uruchomieniu lub nastepnej wysylce (jak w PWA).
    private static final ExecutorService GF_NET = Executors.newSingleThreadExecutor();
    private static android.content.Context gfCtx;
    public interface FormCallback { void done(boolean ok, String info); }

    public static void initForms(android.content.Context c) {
        gfCtx = c.getApplicationContext();
        flushFormQueue();
    }

    public static void postForm(String url, String body) { postForm(url, body, null); }

    public static void postForm(String url, String body, FormCallback cb) {
        GF_NET.execute(() -> {
            String info = sendFormNow(url, body);
            boolean ok = info == null;
            if (ok) flushQueueInline();
            else enqueueForm(url, body);
            if (cb != null) MAIN.post(() -> cb.done(ok, ok ? "" : info));
        });
    }

    // null = wyslano; inaczej opis bledu
    private static String sendFormNow(String url, String body) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*");
            c.setRequestProperty("Origin", "https://docs.google.com");
            c.setRequestProperty("Referer", url.replace("/formResponse", "/viewform"));
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = c.getOutputStream()) { os.write(data); }
            int st = c.getResponseCode();
            try { readBody(c); } catch (Exception e) { }
            if (st >= 200 && st < 400) return null;
            return "HTTP " + st;
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        } finally {
            if (c != null) try { c.disconnect(); } catch (Exception e) { }
        }
    }

    private static synchronized void enqueueForm(String url, String body) {
        if (gfCtx == null) return;
        try {
            android.content.SharedPreferences p = gfCtx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
            JSONArray q = new JSONArray(p.getString("gf_queue", "[]"));
            JSONObject o = new JSONObject();
            o.put("u", url); o.put("b", body);
            q.put(o);
            while (q.length() > 200) q.remove(0);
            p.edit().putString("gf_queue", q.toString()).apply();
        } catch (Exception e) { }
    }

    public static void flushFormQueue() { GF_NET.execute(NsClient::flushQueueInline); }

    private static synchronized void flushQueueInline() {
        if (gfCtx == null) return;
        try {
            android.content.SharedPreferences p = gfCtx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
            JSONArray q = new JSONArray(p.getString("gf_queue", "[]"));
            if (q.length() == 0) return;
            JSONArray left = new JSONArray();
            for (int i = 0; i < q.length(); i++) {
                JSONObject o = q.optJSONObject(i);
                if (o == null) continue;
                if (left.length() > 0 || sendFormNow(o.optString("u", ""), o.optString("b", "")) != null) left.put(o);
            }
            p.edit().putString("gf_queue", left.toString()).apply();
        } catch (Exception e) { }
    }

    public static int formQueueSize() {
        if (gfCtx == null) return 0;
        try { return new JSONArray(gfCtx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE).getString("gf_queue", "[]")).length(); }
        catch (Exception e) { return 0; }
    }

    public static String enc(String s) {
        try { return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }

    private static void writeFilePart(DataOutputStream out, String boundary, String field, File f, String mime) throws Exception {
        writeFilePart(out, boundary, field, f, mime, null);
    }

    private static void writeFilePart(DataOutputStream out, String boundary, String field, File f, String mime, UploadShrink.Prog p) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.write(("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + f.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.writeBytes("Content-Type: " + mime + "\r\n\r\n");
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            long done = 0, total = Math.max(1, f.length());
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                if (p != null) p.at(done / (double) total);
            }
        }
        out.writeBytes("\r\n");
    }

    // DLUGOSC calego multipart (do Content-Length) — liczona dokladnie tak, jak pisza writeFilePart/writeField
    private static long filePartLen(String boundary, String field, File f, String mime) {
        return ("--" + boundary + "\r\n").length()
                + ("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + f.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8).length
                + ("Content-Type: " + mime + "\r\n\r\n").length() + f.length() + 2;
    }

    private static long fieldLen(String boundary, String name, String value) {
        return ("--" + boundary + "\r\n").length() + ("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").length()
                + value.getBytes(StandardCharsets.UTF_8).length + 2;
    }

    // Wysylka duzego pliku: znana dlugosc (serwery i proxy lepiej to znosza niz "chunked") i dlugi
    // czas oczekiwania na odpowiedz — serwer po odebraniu dlugiego nagrania potrzebuje chwili.
    private static void prepUpload(HttpURLConnection c, long len) {
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(len);
        c.setReadTimeout(10 * 60 * 1000);
    }

    private static void writeField(DataOutputStream out, String boundary, String name, String value) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private static String mimeOf(File f) {
        return f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
    }

    // POST /records — NOWE nagranie (podlega dziennemu limitowi).
    public static void uploadRecording(String token, String email, File file, String categoryId, Callback cb) {
        runAsync(() -> {
            // dlugie nagranie → mniejsza kopia (limit wielkosci pliku na serwerze NS)
            final String key = file.getName();
            // dluga calkowita cisza (mikrofon przestal nagrywac) -> NIE wysylamy
            MuteCheck.Res mute = MuteCheck.check(appCtx, file, f -> prog(key, 3, f, false));
            if (mute.bad()) { PROGRESS.remove(key); Result mr = new Result(); mr.ok = false; mr.mute = true; mr.err = MuteCheck.describe(mute); return mr; }
            final boolean shrink = UploadShrink.needed(file);
            File up = UploadShrink.forUpload(file, f -> prog(key, 0, f, true));
            UploadShrink.Prog sendP = f -> prog(key, 1, f, shrink);
            sendP.at(0);
            try {
                String boundary = "----PitchRec" + System.currentTimeMillis();
                HttpURLConnection c = open("/records", "POST", token, email);
                String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                String cat = categoryId == null ? "" : categoryId;
                long len = filePartLen(boundary, "record_file", up, mimeOf(up)) + fieldLen(boundary, "record_category_id", cat)
                        + fieldLen(boundary, "date", day) + ("--" + boundary + "--\r\n").length();
                prepUpload(c, len);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                    writeFilePart(out, boundary, "record_file", up, mimeOf(up), sendP);
                    writeField(out, boundary, "record_category_id", cat);
                    writeField(out, boundary, "date", day);
                    out.writeBytes("--" + boundary + "--\r\n");
                }
                prog(key, 2, 1, shrink); // wyslane — czekamy na odpowiedz serwera
                return finish(c);
            } finally { UploadShrink.cleanup(file, up); PROGRESS.remove(key); }
        }, cb);
    }

    // PUT /records/{id} — POPRAWKA istniejącego nagrania (bez dziennego limitu).
    public static void correctRecording(String token, String email, File file, String recordId, String categoryId, Callback cb) {
        runAsync(() -> {
            // dlugie nagranie → mniejsza kopia (limit wielkosci pliku na serwerze NS)
            final String key = file.getName();
            // dluga calkowita cisza (mikrofon przestal nagrywac) -> NIE wysylamy
            MuteCheck.Res mute = MuteCheck.check(appCtx, file, f -> prog(key, 3, f, false));
            if (mute.bad()) { PROGRESS.remove(key); Result mr = new Result(); mr.ok = false; mr.mute = true; mr.err = MuteCheck.describe(mute); return mr; }
            final boolean shrink = UploadShrink.needed(file);
            File up = UploadShrink.forUpload(file, f -> prog(key, 0, f, true));
            UploadShrink.Prog sendP = f -> prog(key, 1, f, shrink);
            sendP.at(0);
            try {
                String boundary = "----PitchRec" + System.currentTimeMillis();
                HttpURLConnection c = open("/records/" + recordId, "PUT", token, email);
                String cat = categoryId == null ? "" : categoryId;
                long len = fieldLen(boundary, "id", recordId) + fieldLen(boundary, "record_category_id", cat) + fieldLen(boundary, "Content-Type", mimeOf(up))
                        + filePartLen(boundary, "record_file", up, mimeOf(up)) + ("--" + boundary + "--\r\n").length();
                prepUpload(c, len);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                    writeField(out, boundary, "id", recordId);
                    writeField(out, boundary, "record_category_id", cat);
                    writeField(out, boundary, "Content-Type", mimeOf(up));
                    writeFilePart(out, boundary, "record_file", up, mimeOf(up), sendP);
                    out.writeBytes("--" + boundary + "--\r\n");
                }
                prog(key, 2, 1, shrink); // wyslane — czekamy na odpowiedz serwera
                return finish(c);
            } finally { UploadShrink.cleanup(file, up); PROGRESS.remove(key); }
        }, cb);
    }
}
