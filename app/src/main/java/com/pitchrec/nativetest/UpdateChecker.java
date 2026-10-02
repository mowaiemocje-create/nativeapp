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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// AUTOMATYCZNA AKTUALIZACJA — najnowsza wersja aplikacji lezy na https://nowamowa.com/apka.apk.
// Aplikacja przy starcie (najwyzej co 3 h) pyta serwer tylko o naglowki pliku (HEAD: ETag /
// Last-Modified / rozmiar). Gdy plik sie zmienil — pobiera go w tle, sprawdza numer wersji
// zapisany W SAMYM APK i gdy jest nowszy, proponuje instalacje (Android wymaga jednego
// dotkniecia "Zainstaluj"). Nie trzeba zadnego dodatkowego pliku z numerem wersji —
// wystarczy podmienic apka.apk na serwerze.
public final class UpdateChecker {

    static final String APK_URL = "https://nowamowa.com/apka.apk";
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
            try {
                // 1) czy plik na serwerze sie zmienil? (bez pobierania calego APK)
                String tag = remoteTag();
                boolean noTag = tag.isEmpty();
                if (!manual && !noTag && tag.equals(p.getString("upd_seen_tag", ""))) {
                    // ten sam plik co ostatnio — gdy kursant odlozyl instalacje, zaproponuj ponownie
                    File pend = new File(a.getCacheDir(), "updates/apka.apk");
                    long pc = p.getLong("upd_pending_code", 0L);
                    done(p);
                    if (pend.exists() && pc > currentCode(a)) { String pn = p.getString("upd_pending_name", ""); main.post(() -> offerInstall(a, pn, pend)); }
                    return;
                }
                if (!manual && noTag && System.currentTimeMillis() - p.getLong("upd_dl_at", 0L) < 24 * 3600 * 1000L) { done(p); return; }
                // 2) pobierz i odczytaj wersje z samego APK
                File dir = new File(a.getCacheDir(), "updates");
                File[] old = dir.listFiles(); if (old != null) for (File f : old) f.delete();
                dir.mkdirs();
                File apk = new File(dir, "apka.apk");
                download(APK_URL + "?t=" + System.currentTimeMillis(), apk);
                p.edit().putLong("upd_dl_at", System.currentTimeMillis()).apply();
                android.content.pm.PackageInfo pi = a.getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
                if (pi == null || !a.getPackageName().equals(pi.packageName)) throw new Exception("bad apk");
                long remote = android.os.Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
                p.edit().putString("upd_seen_tag", tag).apply();
                done(p);
                if (remote <= currentCode(a)) {
                    apk.delete();
                    final String srv = (pi.versionName == null ? String.valueOf(remote) : pi.versionName);
                    if (manual) main.post(() -> Toast.makeText(a, L.t("Masz najnowszą wersję ✓") + "\n" + L.f("Telefon: {0} · serwer: {1}", currentName(a), srv), Toast.LENGTH_LONG).show());
                    return;
                }
                final String rName = pi.versionName == null ? String.valueOf(remote) : pi.versionName;
                p.edit().putLong("upd_pending_code", remote).putString("upd_pending_name", rName).apply();
                main.post(() -> offerInstall(a, rName, apk));
            } catch (Exception e) {
                done(p);
                if (manual) main.post(() -> Toast.makeText(a, L.t("Nie udało się sprawdzić aktualizacji — sprawdź internet."), Toast.LENGTH_LONG).show());
            }
        }, "update-check").start();
    }

    private static void done(SharedPreferences p) {
        p.edit().putLong("upd_checked", System.currentTimeMillis()).apply();
        busy = false;
    }

    // "odcisk" pliku na serwerze: ETag albo data modyfikacji + rozmiar
    private static String remoteTag() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(APK_URL + "?t=" + System.currentTimeMillis()).openConnection();
        c.setRequestMethod("HEAD");
        c.setUseCaches(false);
        c.setRequestProperty("Cache-Control", "no-cache");
        c.setRequestProperty("Pragma", "no-cache");
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "NewSpeech-App");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
            String et = c.getHeaderField("ETag"), lm = c.getHeaderField("Last-Modified");
            long len = c.getContentLength();
            if (et == null && lm == null) return "";
            return (et == null ? "" : et) + "|" + (lm == null ? "" : lm) + "|" + len;
        } finally { c.disconnect(); }
    }

    private static void offerInstall(Activity a, String versionName, File apk) {
        if (a.isFinishing()) return;
        String msg = L.f("Pobrano nową wersję {0} (masz {1}).", versionName, currentName(a))
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
                prefs(a).edit().putLong("upd_checked", 0L).putString("upd_seen_tag", "").apply(); // po powrocie zapytaj od razu
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
        c.setUseCaches(false);
        c.setRequestProperty("Cache-Control", "no-cache");
        c.setRequestProperty("Pragma", "no-cache");
        int code = c.getResponseCode();
        if (code >= 300 && code < 400) { // na wszelki wypadek reczne przekierowanie
            String loc = c.getHeaderField("Location");
            c.disconnect();
            return open(loc);
        }
        if (code != 200) throw new Exception("HTTP " + code);
        return c;
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
