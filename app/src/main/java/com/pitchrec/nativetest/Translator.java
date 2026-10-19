package com.pitchrec.nativetest;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// 📥 TRYB TŁUMACZA (konto trenera/admina): telefon bierze z serwera nagrania kursantów, które nie mają jeszcze
// tekstu (najnowsze najpierw), rozpoznaje mowę wbudowanym rozpoznawaniem Androida, mierzy pauzy z dźwięku
// i odsyła tekst do bazy tekstów. Czego nie rozpozna — oddaje serwerowi (przepisze je Whisper).
// Działa, dopóki aplikacja jest uruchomiona (najlepiej na ładowarce i Wi-Fi — domyślnie tylko wtedy).
public final class Translator {

    private Translator() { }

    private static final String BACKEND = "https://newspeech-backend.mowaiemocje.workers.dev";
    private static volatile Thread thread;
    public static volatile String status = "";
    public static volatile int waiting = -1;
    public interface Listener { void changed(); }
    public static volatile Listener listener;

    private static SharedPreferences p(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static boolean on(Context c) { return !BuildConfig.PLAY && p(c).getBoolean("translator_on", false); }
    public static boolean onlyCharging(Context c) { return p(c).getBoolean("tr_charging", true); }
    public static boolean onlyWifi(Context c) { return p(c).getBoolean("tr_wifi", true); }
    public static int done(Context c) { return p(c).getInt("tr_done", 0); }
    public static int doneToday(Context c) {
        String d = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        return d.equals(p(c).getString("tr_day", "")) ? p(c).getInt("tr_today", 0) : 0;
    }

    public static void setOn(Context c, boolean v) {
        p(c).edit().putBoolean("translator_on", v).apply();
        if (v) ensureRunning(c); else setStatus("");
    }
    public static void setOnlyCharging(Context c, boolean v) { p(c).edit().putBoolean("tr_charging", v).apply(); }
    public static void setOnlyWifi(Context c, boolean v) { p(c).edit().putBoolean("tr_wifi", v).apply(); }

    private static void setStatus(String s) {
        status = s;
        Listener l = listener;
        if (l != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(l::changed);
    }

    public static synchronized void ensureRunning(Context c) {
        if (!on(c) || !AndroidAsr.supported(c)) return;
        if (thread != null && thread.isAlive()) return;
        final Context ac = c.getApplicationContext();
        thread = new Thread(() -> loop(ac), "translator");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void loop(Context c) {
        while (on(c)) {
            long sleep;
            try {
                if (onlyCharging(c) && !charging(c)) { setStatus("🔌 " + L.t("Czeka na ładowarkę")); sleep = 60000; }
                else if (onlyWifi(c) && !wifi(c)) { setStatus("📶 " + L.t("Czeka na Wi-Fi")); sleep = 60000; }
                else sleep = batch(c);
            } catch (Throwable t) {
                setStatus("⚠️ " + (t.getMessage() == null ? t.toString() : t.getMessage()));
                sleep = 120000;
            }
            try { Thread.sleep(sleep); } catch (InterruptedException e) { break; }
        }
        setStatus("");
    }

    // jedna porcja kolejki; zwraca czas do następnej
    private static long batch(Context c) throws Exception {
        String token = p(c).getString("ns_token", null), email = p(c).getString("ns_email", "");
        if (token == null) { setStatus(L.t("Zaloguj się kontem trenera.")); return 300000; }
        setStatus("⏳ " + L.t("Sprawdzam nagrania bez tekstu…"));
        String q = "/transcripts/phone-queue?limit=2&ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email) + "&ns_server=new";
        JSONObject d = http("GET", q, null);
        if (d.has("error")) {
            if (String.valueOf(d.optString("error", "")).contains("trener")) { p(c).edit().putBoolean("translator_on", false).apply(); setStatus("⛔ " + L.t("Tryb tłumacza działa tylko na koncie trenera/admina.")); return 600000; }
            throw new Exception(d.optString("error", ""));
        }
        waiting = d.optInt("waiting", 0);
        JSONArray items = d.optJSONArray("items");
        if (items == null || items.length() == 0) { setStatus("✅ " + L.t("Wszystkie nagrania mają tekst. Sprawdzę ponownie za 5 min.")); return 300000; }
        for (int i = 0; i < items.length() && on(c); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            String rid = it.optString("record_id", "");
            setStatus("🗣 " + L.f("Tłumaczę nagranie ({0}) · w kolejce: {1}", L.cat(it.optString("category", "")), Math.max(0, waiting - i)));
            File tmp = null;
            boolean ok = false;
            try {
                String name = it.optString("file", "a.mp3");
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT) : ".mp3";
                if (ext.length() > 6) ext = ".mp3";
                tmp = new File(c.getCacheDir(), "tr_" + rid + ext);
                download(it.optString("audio_url", ""), tmp);
                JSONObject r = LocalTempo.analyze(c, tmp, it.optString("lang", "pl"));
                String text = r == null ? "" : r.optString("text", "").trim();
                if (!text.isEmpty()) {
                    JSONObject b = new JSONObject();
                    b.put("ns_token", token);
                    b.put("ns_email", email);
                    b.put("ns_server", "new");
                    b.put("translator", true);
                    b.put("record_id", rid);
                    b.put("text", text);
                    b.put("dur", r.optDouble("dur", 0));
                    b.put("lang", it.optString("lang", "pl"));
                    b.put("words", LocalTempo.wordTimes(text, r.optJSONArray("runs")));
                    JSONObject res = http("POST", "/transcripts/phone", b.toString());
                    ok = res.optBoolean("ok", false);
                }
            } catch (Throwable t) {
                ok = false;
            } finally {
                if (tmp != null) tmp.delete();
            }
            if (ok) {
                String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                int today = day.equals(p(c).getString("tr_day", "")) ? p(c).getInt("tr_today", 0) : 0;
                p(c).edit().putInt("tr_done", done(c) + 1).putString("tr_day", day).putInt("tr_today", today + 1).apply();
            } else {
                // nie rozpoznano / błąd — oddajemy nagranie serwerowi (przepisze je sam)
                try {
                    JSONObject b = new JSONObject();
                    b.put("ns_token", token); b.put("ns_email", email); b.put("ns_server", "new"); b.put("record_id", rid);
                    http("POST", "/transcripts/phone-release", b.toString());
                } catch (Throwable t) { }
            }
        }
        return 3000;
    }

    private static boolean charging(Context c) {
        try {
            Intent b = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return b != null && b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        } catch (Throwable t) { return true; }
    }

    private static boolean wifi(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Throwable t) { return true; }
    }

    private static JSONObject http(String method, String path, String body) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(BACKEND + path).openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(60000);
        con.setRequestMethod(method);
        if (body != null) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = con.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        int code = con.getResponseCode();
        InputStream in = code >= 400 ? con.getErrorStream() : con.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        try { return new JSONObject(sb.toString()); }
        catch (Exception e) { JSONObject o = new JSONObject(); o.put("error", "HTTP " + code); return o; }
    }

    private static void download(String url, File out) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(120000);
        con.setInstanceFollowRedirects(true);
        if (con.getResponseCode() / 100 != 2) throw new Exception("HTTP " + con.getResponseCode());
        try (InputStream in = con.getInputStream(); OutputStream os = new FileOutputStream(out)) {
            byte[] b = new byte[1 << 16];
            int n;
            while ((n = in.read(b)) > 0) os.write(b, 0, n);
        }
    }
}
