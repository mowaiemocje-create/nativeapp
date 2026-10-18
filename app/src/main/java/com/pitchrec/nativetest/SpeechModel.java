package com.pitchrec.nativetest;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// 🗣 MODEL ROZPOZNAWANIA MOWY W TELEFONIE (Vosk, polski, offline, ok. 50 MB).
// Pobierany raz z Ustawień (albo przy pierwszym liczeniu tempa), rozpakowany do pamięci aplikacji.
// Bez internetu i bez serwera — działa też bez logowania.
public final class SpeechModel {

    private SpeechModel() { }

    static final String NAME = "vosk-model-small-pl-0.22";
    static final String URL_ZIP = "https://alphacephei.com/vosk/models/" + NAME + ".zip";
    static final int SIZE_MB = 50;

    public interface Progress { void update(int pct, String err, boolean done); }

    private static volatile boolean downloading = false;
    private static volatile int lastPct = 0;
    private static org.vosk.Model model;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static File root(Context c) { return new File(c.getFilesDir(), "vosk"); }
    static File dir(Context c) { return new File(root(c), NAME); }

    public static boolean ready(Context c) { return new File(dir(c), ".ok").exists(); }
    public static boolean busy() { return downloading; }
    public static int pct() { return lastPct; }

    public static void download(Context c, Progress cb) {
        if (downloading) return;
        downloading = true;
        lastPct = 0;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            String err = null;
            File tmp = new File(app.getFilesDir(), "vosk-tmp");
            try {
                deleteRec(tmp);
                tmp.mkdirs();
                HttpURLConnection con = (HttpURLConnection) new URL(URL_ZIP).openConnection();
                con.setConnectTimeout(20000);
                con.setReadTimeout(60000);
                con.setInstanceFollowRedirects(true);
                if (con.getResponseCode() / 100 != 2) throw new Exception("HTTP " + con.getResponseCode());
                long total = con.getContentLengthLong();
                final long[] read = {0};
                InputStream raw = con.getInputStream();
                InputStream counting = new java.io.FilterInputStream(raw) {
                    @Override public int read(byte[] b, int off, int len) throws java.io.IOException {
                        int n = super.read(b, off, len);
                        if (n > 0) {
                            read[0] += n;
                            int p = total > 0 ? (int) (read[0] * 100 / total) : (int) Math.min(99, read[0] / (SIZE_MB * 10486L));
                            if (p != lastPct) { lastPct = p; post(cb, p, null, false); }
                        }
                        return n;
                    }
                };
                try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(counting, 1 << 16))) {
                    ZipEntry e;
                    byte[] buf = new byte[1 << 16];
                    String base = tmp.getCanonicalPath() + File.separator;
                    while ((e = zin.getNextEntry()) != null) {
                        File out = new File(tmp, e.getName());
                        if (!out.getCanonicalPath().startsWith(base)) continue; // ochrona przed "../"
                        if (e.isDirectory()) { out.mkdirs(); continue; }
                        out.getParentFile().mkdirs();
                        try (OutputStream os = new FileOutputStream(out)) {
                            int n;
                            while ((n = zin.read(buf)) > 0) os.write(buf, 0, n);
                        }
                    }
                }
                File unpacked = new File(tmp, NAME);
                if (!new File(unpacked, "am").exists() && !new File(unpacked, "conf").exists()) throw new Exception(L.t("Niepełny plik modelu"));
                deleteRec(root(app));
                root(app).mkdirs();
                if (!unpacked.renameTo(dir(app))) throw new Exception(L.t("Nie udało się zapisać modelu"));
                new FileOutputStream(new File(dir(app), ".ok")).close();
            } catch (Throwable t) {
                err = t.getMessage() == null ? t.toString() : t.getMessage();
            } finally {
                deleteRec(tmp);
                downloading = false;
            }
            post(cb, 100, err, true);
        }).start();
    }

    private static void post(Progress cb, int p, String err, boolean done) {
        if (cb != null) MAIN.post(() -> cb.update(p, err, done));
    }

    public static synchronized void delete(Context c) {
        if (model != null) { try { model.close(); } catch (Throwable t) { } model = null; }
        deleteRec(root(c));
    }

    // Model wczytany raz (trwa kilka sekund) — kolejne nagrania liczą się szybciej
    static synchronized org.vosk.Model model(Context c) throws java.io.IOException {
        if (model == null) {
            try { org.vosk.LibVosk.setLogLevel(org.vosk.LogLevel.WARNINGS); } catch (Throwable t) { }
            model = new org.vosk.Model(dir(c).getAbsolutePath());
        }
        return model;
    }

    static void deleteRec(File f) {
        if (f == null || !f.exists()) return;
        File[] l = f.listFiles();
        if (l != null) for (File x : l) deleteRec(x);
        f.delete();
    }
}
