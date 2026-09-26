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
        public boolean isAuthError() { return status == 401 || status == 403; }
        public boolean isLimitError() { return err != null && err.toLowerCase(Locale.ROOT).contains("limit"); }
    }

    public interface Callback { void done(Result r); }

    private static final ExecutorService NET = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static void runAsync(java.util.concurrent.Callable<Result> job, Callback cb) {
        NET.execute(() -> {
            Result r;
            try { r = job.call(); }
            catch (Exception e) { r = new Result(); r.ok = false; r.status = 0; r.err = "Brak połączenia z serwerem NewSpeech"; }
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
            try { msg = new JSONObject(r.body).optString("message", null); } catch (Exception e) { }
            r.err = (msg != null && !msg.isEmpty()) ? msg : ("Błąd " + r.status);
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
                if (tok.isEmpty()) { r.ok = false; r.err = "Serwer nie zwrócił tokenu logowania"; return r; }
                r.token = tok;
                String em = d.optString("email", "");
                if (em.isEmpty() && d.optJSONObject("user") != null) em = d.optJSONObject("user").optString("email", "");
                r.email = em.isEmpty() ? email : em;
                String id = d.optString("id", "");
                if (id.isEmpty() && d.optJSONObject("user") != null) id = d.optJSONObject("user").optString("id", "");
                r.userId = id;
            } else if (r.status == 401 || r.status == 422 || r.status == 400) {
                r.err = "Nieprawidłowy email lub hasło";
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
                else if (st < 200 || st >= 300) err = "Błąd " + st;
                else {
                    JSONObject d = new JSONObject(body);
                    if (!d.optBoolean("ok", false)) err = d.optString("error", "Błąd serwera");
                    JSONArray arr = d.optJSONArray("entries");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;
                            FixEntry fe = new FixEntry();
                            fe.id = o.optString("id", "");
                            fe.categoryName = o.optString("category_name", "");
                            fe.reviewedAt = o.optLong("reviewed_at", 0L);
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
                                    sb.append("• ").append(w.optString("category_name", "")).append(" — ").append(unit);
                                }
                            }
                            fe.weakText = sb.toString();
                            if (!fe.id.isEmpty()) list.add(fe);
                        }
                    }
                }
            } catch (Exception e) {
                err = "Brak połączenia z serwerem";
            }
            final String fErr = err;
            MAIN.post(() -> cb.done(list, fErr));
        });
    }

    private static void writeFilePart(DataOutputStream out, String boundary, String field, File f, String mime) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.write(("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + f.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.writeBytes("Content-Type: " + mime + "\r\n\r\n");
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
        out.writeBytes("\r\n");
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
            String boundary = "----PitchRec" + System.currentTimeMillis();
            HttpURLConnection c = open("/records", "POST", token, email);
            c.setDoOutput(true);
            c.setChunkedStreamingMode(64 * 1024);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                writeFilePart(out, boundary, "record_file", file, mimeOf(file));
                writeField(out, boundary, "record_category_id", categoryId);
                writeField(out, boundary, "date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
                out.writeBytes("--" + boundary + "--\r\n");
            }
            return finish(c);
        }, cb);
    }

    // PUT /records/{id} — POPRAWKA istniejącego nagrania (bez dziennego limitu).
    public static void correctRecording(String token, String email, File file, String recordId, String categoryId, Callback cb) {
        runAsync(() -> {
            String boundary = "----PitchRec" + System.currentTimeMillis();
            HttpURLConnection c = open("/records/" + recordId, "PUT", token, email);
            c.setDoOutput(true);
            c.setChunkedStreamingMode(64 * 1024);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                writeField(out, boundary, "id", recordId);
                writeField(out, boundary, "record_category_id", categoryId == null ? "" : categoryId);
                writeField(out, boundary, "Content-Type", mimeOf(file));
                writeFilePart(out, boundary, "record_file", file, mimeOf(file));
                out.writeBytes("--" + boundary + "--\r\n");
            }
            return finish(c);
        }, cb);
    }
}
