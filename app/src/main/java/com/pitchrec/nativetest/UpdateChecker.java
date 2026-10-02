package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// AUTOMATYCZNA AKTUALIZACJA — GitHub Actions po kazdym pushu publikuje wydanie
// (Releases -> "latest") z plikami newspeech.apk i version.json. Aplikacja przy starcie
// (najwyzej co 3 h) sprawdza version.json; gdy jest nowsza wersja, sama pobiera APK w tle
// i proponuje instalacje (Android zawsze wymaga jednego dotkniecia "Zainstaluj").
public final class UpdateChecker {

    static final String BASE = "https://github.com/mowaiemocje-create/nativeapp/releases/latest/download/";
    private static volatile boolean busy = false;

    private UpdateChecker() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("auto_update", true); }

    public static long currentCode(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) { return 0L; }
    }

    public static String currentName(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
    }

    // manual = z przycisku w Ustawieniach (komunikat tez gdy brak nowej wersji)
    public static void check(Activity a, boolean manual) {
        if (busy) return;
        SharedPreferences p = prefs(a);
        if (!manual && (!enabled(a) || System.currentTimeMillis() - p.getLong("upd_checked", 0L) < 3 * 3600 * 1000L)) return;
        busy = true;
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            JSONObject v = null;
            try {
                v = new JSONObject(get(BASE + "version.json?t=" + System.currentTimeMillis()));
            } catch (Exception e) { }
            p.edit().putLong("upd_checked", System.currentTimeMillis()).apply();
            final JSONObject fv = v;
            if (fv == null) {
                busy = false;
                if (manual) main.post(() -> Toast.makeText(a, L.t("Nie udało się sprawdzić aktualizacji — sprawdź internet."), Toast.LENGTH_LONG).show());
                return;
            }
            long remote = fv.optLong("versionCode", 0L);
            if (remote <= currentCode(a)) {
                busy = false;
                if (manual) main.post(() -> Toast.makeText(a, L.t("Masz najnowszą wersję ✓") + " (" + currentName(a) + ")", Toast.LENGTH_LONG).show());
                return;
            }
            // pobierz APK w tle (gdy juz pobrany ten sam numer — nie pobieraj drugi raz)
            File apk = new File(a.getCacheDir(), "updates/newspeech-" + remote + ".apk");
            try {
                if (!apk.exists() || apk.length() < 100000) {
                    File dir = apk.getParentFile();
                    if (dir != null) { File[] old = dir.listFiles(); if (old != null) for (File f : old) f.delete(); dir.mkdirs(); }
                    download(BASE + "newspeech.apk", apk);
                }
            } catch (Exception e) {
                apk.delete();
                busy = false;
                if (manual) main.post(() -> Toast.makeText(a, L.t("Nie udało się pobrać aktualizacji."), Toast.LENGTH_LONG).show());
                return;
            }
            busy = false;
            main.post(() -> offerInstall(a, fv, apk));
        }, "update-check").start();
    }

    private static void offerInstall(Activity a, JSONObject v, File apk) {
        if (a.isFinishing()) return;
        String notes = v.optString("notes", "").trim();
        String msg = L.f("Pobrano nową wersję {0} (masz {1}).", v.optString("versionName", ""), currentName(a))
                + (notes.isEmpty() ? "" : "\n\n" + L.t("Co nowego:") + "\n" + notes)
                + "\n\n" + L.t("Nagrania i ustawienia zostaną zachowane.");
        new AlertDialog.Builder(a)
                .setTitle("⬆ " + L.t("Aktualizacja aplikacji"))
                .setMessage(msg)
                .setPositiveButton(L.t("Zainstaluj"), (d, w) -> install(a, apk))
                .setNegativeButton(L.t("Później"), null)
                .show();
    }

    static void install(Activity a, File apk) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(a, L.t("Zezwól tej aplikacji na instalowanie aktualizacji, potem wróć i dotknij „Zainstaluj” jeszcze raz."), Toast.LENGTH_LONG).show();
                a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
                prefs(a).edit().putLong("upd_checked", 0L).apply(); // po powrocie zapytaj od razu
                return;
            }
            Uri u = androidx.core.content.FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", apk);
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, L.t("Nie udało się uruchomić instalacji."), Toast.LENGTH_LONG).show();
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        // GitHub przekierowuje na inny serwer (https -> https), HttpURLConnection to obsluguje
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "NewSpeech-App");
        int code = c.getResponseCode();
        if (code >= 300 && code < 400) { // na wszelki wypadek reczne przekierowanie
            String loc = c.getHeaderField("Location");
            c.disconnect();
            return open(loc);
        }
        if (code != 200) throw new Exception("HTTP " + code);
        return c;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            return bo.toString("UTF-8");
        } finally { c.disconnect(); }
    }

    private static void download(String url, File out) throws Exception {
        HttpURLConnection c = open(url);
        File tmp = new File(out.getPath() + ".part");
        try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(tmp)) {
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) > 0) fo.write(b, 0, n);
        } finally { c.disconnect(); }
        if (!tmp.renameTo(out)) throw new Exception("rename");
    }
}
