package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 🗣 TEMPO MOWY LICZONE W TELEFONIE — po zakończeniu nagrania, z CAŁEGO nagrania, bez serwera i bez logowania.
//  • SŁOWA: rozpoznawanie mowy wbudowane w Androida (Google, Android 13+) — tekst całego nagrania.
//  • DŹWIĘK (nasz model z wykresu): ten sam VAD co pauzy na wykresie (głos / cisza) i to samo liczenie
//    sylab z głośności co tempo na żywo.
//  • Czas mówienia i pauzy — z dźwięku (nasz pomiar). Sylaby — z tekstu (grupy samogłosek); obok
//    sylaby z dźwięku do porównania (gdy różnią się o > 20% — ostrzeżenie).
// Wynik zapisany przy nagraniu (SharedPreferences "tempo_local", kluczem jest nazwa pliku).
public final class LocalTempo {

    private LocalTempo() { }

    private static final int SR = LiveAudioData.SAMPLE_RATE;
    private static final ExecutorService ONE = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ConcurrentHashMap<String, String> RENAMED = new ConcurrentHashMap<>();
    private static final java.util.Set<String> RUNNING = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public interface Done { void done(JSONObject r, String err); }

    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("tempo_local", Context.MODE_PRIVATE); }
    private static SharedPreferences app(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public static boolean allowed(Context c) { return Access.full(c); }
    public static boolean autoOn(Context c) { return app(c).getBoolean("tempo_auto", true); }
    public static void setAuto(Context c, boolean on) { app(c).edit().putBoolean("tempo_auto", on).apply(); }

    public static JSONObject get(Context c, String fileName) {
        String js = sp(c).getString(fileName, null);
        if (js == null) return null;
        try { return new JSONObject(js); } catch (Exception e) { return null; }
    }

    public static boolean running(String fileName) { return RUNNING.contains(fileName); }

    // zmiana nazwy pliku przy opisie — wynik idzie za plikiem (także, gdy liczenie jeszcze trwa)
    public static void renamed(Context c, String oldName, String newName) {
        if (oldName.equals(newName)) return;
        RENAMED.put(oldName, newName);
        String js = sp(c).getString(oldName, null);
        if (js != null) sp(c).edit().remove(oldName).putString(newName, js).apply();
    }

    public static void forget(Context c, String fileName) { sp(c).edit().remove(fileName).apply(); }

    private static String finalName(String n) {
        for (int i = 0; i < 10 && RENAMED.containsKey(n); i++) n = RENAMED.get(n);
        return n;
    }

    // Po nagraniu: liczy w tle, jeśli model jest pobrany i włączone liczenie automatyczne
    public static void autoAfterRecording(Context c, File f) {
        if (f == null || !allowed(c) || !autoOn(c)) return;
        analyzeAsync(c, f, null);
    }

    public static void analyzeAsync(Context c, File f, Done cb) {
        final Context ac = c.getApplicationContext();
        final String name = f.getName();
        if (!RUNNING.add(name)) return;
        ONE.execute(() -> {
            JSONObject r = null;
            String err = null;
            try {
                // plik mógł dostać nową nazwę (opis nagrania), zanim liczenie ruszyło
                File real = new File(f.getParentFile(), finalName(name));
                r = analyze(ac, real.exists() ? real : f);
                if (r == null) err = L.t("Nagranie za krótkie albo bez mowy.");
                else sp(ac).edit().putString(finalName(name), r.toString()).apply();
            } catch (Throwable t) {
                err = t.getMessage() == null ? t.toString() : t.getMessage();
            } finally {
                RUNNING.remove(name);
            }
            final JSONObject fr = r;
            final String fe = err;
            if (cb != null) MAIN.post(() -> cb.done(fr, fe));
        });
    }

    // ── ANALIZA ──
    static JSONObject analyze(Context c, File f) throws Exception {
        short[] s = AudioFileLoader.decode(f);
        if (s == null || s.length < SR * 5) return null;
        double dur = s.length / (double) SR;
        int G = (int) Math.ceil(dur * 100); // siatka 10 ms

        // 1) DŹWIĘK: VAD (jak pauzy na wykresie), obwiednia RMS (256 próbek), dźwięczne okna (ton krtaniowy)
        final int W = 2048;
        int frames = s.length / W;
        VadDetector vad = new VadDetector();
        YinPitchDetector.Work yw = new YinPitchDetector.Work();
        float[] win = new float[W];
        boolean[] speechG = new boolean[G];
        double[] voiced = new double[Math.max(16, frames)];
        int nv = 0;
        double prevT = 0;
        for (int fr = 0; fr < frames; fr++) {
            double sum = 0;
            for (int j = 0; j < W; j++) { float v = s[fr * W + j] / 32768f; win[j] = v; sum += v * v; }
            float rms = (float) Math.sqrt(sum / W);
            float f0 = YinPitchDetector.detect(win, yw);
            double t = (fr + 1) * W / (double) SR;
            boolean sp = vad.frame(t, rms, rms > 0.003f ? f0 : -1f);
            if (sp) for (int g = (int) (prevT * 100); g < Math.min(G, (int) (t * 100)); g++) speechG[g] = true;
            float vf = f0 > 0 ? f0 : yw.relaxed;
            if (vf > 60 && vf < 600 && rms > 0.004f) voiced[nv++] = (fr * W + W / 2) / (double) SR;
            prevT = t;
        }
        voiced = java.util.Arrays.copyOf(voiced, nv);
        int CH = 256;
        float[] env = new float[s.length / CH];
        for (int i = 0; i < env.length; i++) {
            double sum = 0;
            for (int j = 0; j < CH; j++) { float v = s[i * CH + j] / 32768f; sum += v * v; }
            env[i] = (float) Math.sqrt(sum / CH);
        }
        // porcje mowy z dźwięku (krótkie przerwy < 0,15 s łączymy)
        List<double[]> runs = runsOf(speechG, 15);

        // 2) SŁOWA: rozpoznawanie mowy wbudowane w Androida (całe nagranie, tylko tekst — bez czasów słów)
        String text = null, asrErr = null;
        if (AndroidAsr.supported(c)) {
            try { text = AndroidAsr.recognize(c, s, SR); } catch (Throwable t) { asrErr = t.getMessage() == null ? t.toString() : t.getMessage(); }
        } else asrErr = L.t("Rozpoznawanie mowy z pliku wymaga Androida 13 lub nowszego.");
        List<String> words = new ArrayList<>();
        if (text != null) for (String w : text.split("\\s+")) if (w.replaceAll("[^\\p{L}\\p{N}]", "").length() > 0) words.add(w);
        boolean asr = text != null && !words.isEmpty();
        int sylText = 0;
        for (String w : words) sylText += syl(w);

        // 3) DŹWIĘK: czas mówienia i pauzy z naszego pomiaru (VAD jak pauzy na wykresie),
        //    sylaby z dźwięku (to samo liczenie co tempo na żywo) — do porównania z tekstem
        boolean[] speech = speechG;
        List<SyllableDetector.Syl> sylA = new ArrayList<>();
        for (double[] r : runs) sylA.addAll(sylIn(env, r[0], r[1], voiced));
        int extra = 0, sp0 = 0, cover = 0;
        int sylFinal = asr ? sylText : sylA.size();

        // czas mówienia, pauzy (wewnątrz nagrania — cisza przed pierwszym i po ostatnim słowie się nie liczy)
        int first = -1, last = -1, spk = 0;
        for (int g = 0; g < G; g++) if (speech[g]) { if (first < 0) first = g; last = g; spk++; }
        if (first < 0 || spk < 300) return null; // mniej niż 3 s mowy
        int span = last - first + 1;
        int pauses1 = 0;
        List<double[]> gaps = new ArrayList<>();
        int run = 0;
        for (int g = first; g <= last; g++) {
            if (!speech[g]) run++;
            else { if (run >= 30) gaps.add(new double[]{(g - run) / 100.0, run / 100.0}); if (run >= 100) pauses1++; run = 0; }
        }
        double speechSec = spk / 100.0, spanSec = span / 100.0;
        JSONObject o = new JSONObject();
        o.put("v", 1);
        o.put("at", System.currentTimeMillis());
        o.put("dur", r1(dur));
        o.put("span_sec", r1(spanSec));
        o.put("speech_sec", r1(speechSec));
        o.put("pause_sec", r1(spanSec - speechSec));
        o.put("pause_pct", Math.round((spanSec - speechSec) / spanSec * 100));
        o.put("pauses_1s", pauses1);
        o.put("syl", sylFinal);
        o.put("syl_text", asr ? sylText : -1);
        o.put("syl_audio", sylA.size());
        o.put("syl_audio_extra", extra);
        o.put("words", asr ? words.size() : -1);
        o.put("coverage", -1);
        if (asrErr != null) o.put("asr_err", asrErr);
        o.put("syl_min_speech", Math.round(sylFinal / speechSec * 60));
        o.put("syl_min", Math.round(sylFinal / spanSec * 60));
        if (asr) {
            o.put("wpm_speech", Math.round(words.size() / speechSec * 60));
            o.put("wpm", Math.round(words.size() / spanSec * 60));
            String tx = text;
            o.put("text", tx.length() > 4000 ? tx.substring(0, 4000) + "…" : tx);
        }
        return o;
    }

    private static List<SyllableDetector.Syl> sylIn(float[] env, double a, double b, double[] voiced) {
        List<SyllableDetector.Syl> out = new ArrayList<>();
        final double CHK = 10.0;
        for (double cs = a; cs < b; cs += CHK) {
            double ce = Math.min(b, cs + CHK);
            for (SyllableDetector.Syl y : SyllableDetector.detect2(env, 0, SR, Math.max(a, cs - 1.0), Math.min(b, ce + 1.0), voiced))
                if (y.nucleus >= cs && y.nucleus < ce) out.add(y);
        }
        return out;
    }

    private static List<double[]> runsOf(boolean[] g, int bridge) {
        List<double[]> out = new ArrayList<>();
        int i = 0;
        while (i < g.length) {
            if (!g[i]) { i++; continue; }
            int st = i, en = i;
            while (i < g.length) {
                if (g[i]) { en = i; i++; continue; }
                int j = i;
                while (j < g.length && !g[j] && j - i < bridge) j++;
                if (j < g.length && g[j] && j - i < bridge) { i = j; continue; }
                break;
            }
            out.add(new double[]{st / 100.0, (en + 1) / 100.0});
            i = en + 1;
        }
        return out;
    }

    private static void collect(String json, List<Object[]> out) {
        try {
            JSONArray a = new JSONObject(json).optJSONArray("result");
            for (int i = 0; a != null && i < a.length(); i++) {
                JSONObject w = a.optJSONObject(i);
                if (w == null) continue;
                String word = w.optString("word", "");
                if (word.isEmpty()) continue;
                out.add(new Object[]{word, w.optDouble("start", 0), w.optDouble("end", 0), w.optDouble("conf", 1)});
            }
        } catch (Exception e) { }
    }

    // Sylaby słowa: grupy samogłosek; „i” po spółgłosce przed samogłoską tylko zmiękcza (się, nie);
    // przeciągnięte „dzieeeeń” = 1 sylaba (powtórzone litery łączymy)
    private static final String VOW = "aąeęioóuyáéěíúůýäô";
    static int syl(String w) {
        String x = w.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "").replaceAll("(\\p{L})\\1+", "$1");
        int n = 0;
        for (int i = 0; i < x.length(); i++) {
            char ch = x.charAt(i);
            if (VOW.indexOf(ch) < 0) continue;
            if (ch == 'i' && i > 0 && VOW.indexOf(x.charAt(i - 1)) < 0 && i + 1 < x.length() && VOW.indexOf(x.charAt(i + 1)) >= 0) continue;
            n++;
        }
        return n;
    }

    private static double r1(double v) { return Math.round(v * 10) / 10.0; }

    // ── WIDOK ──
    // Krótka linia na karcie nagrania („🗣 152 syl/min · pauzy 31%  ›” albo „🗣 Policz tempo ›”)
    public static TextView cardLine(Activity a, File f) {
        JSONObject r = get(a, f.getName());
        String t = r != null ? "🗣 " + r.optInt("syl_min_speech", 0) + " " + L.t("syl/min") + (r.optInt("wpm_speech", -1) >= 0 ? " · " + r.optInt("wpm_speech", 0) + " " + L.t("sł/min") : "")
                + " · " + L.t("pauzy") + " " + r.optInt("pause_pct", 0) + "%  ›"
                : running(f.getName()) ? "🗣 " + L.t("Liczę tempo…") : "🗣 " + L.t("Policz tempo mowy") + " ›";
        TextView tv = Ui.text(a, t, 11f, r != null ? R.color.pr_accent : R.color.pr_muted);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, (int) (4 * a.getResources().getDisplayMetrics().density), 0, 0);
        tv.setOnClickListener(v -> showDialog(a, f, null));
        return tv;
    }

    public static void showDialog(Activity a, File f, Runnable onChange) {
        if (!allowed(a)) { PremiumUi.offer(a, L.t("Tempo mowy jest w pełnej wersji."), null); return; }
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (20 * d), (int) (6 * d), (int) (20 * d), (int) (6 * d));
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        AlertDialog dlg = new AlertDialog.Builder(a).setTitle("🗣 " + L.t("Tempo mowy")).setView(sv)
                .setPositiveButton(L.t("Zamknij"), null).create();
        dlg.setOnDismissListener(x -> { if (onChange != null) onChange.run(); });
        fillSection(a, box, f);
        dlg.show();
    }

    // Sekcja tempa (okno tempa i szczegóły nagrania)
    public static void fillSection(Activity a, LinearLayout box, File f) {
        box.removeAllViews();
        float d = a.getResources().getDisplayMetrics().density;
        if (!AndroidAsr.supported(a))
            box.addView(Ui.text(a, "ℹ️ " + L.t("Słowa liczy rozpoznawanie mowy Androida 13+. Na tym telefonie tempo liczone tylko z dźwięku (przybliżone)."), 11f, R.color.pr_pause));
        JSONObject r = get(a, f.getName());
        if (r == null) { compute(a, box, f); return; }
        render(a, box, r);
        Button again = Ui.button(a, "↻ " + L.t("Przelicz ponownie"), R.color.pr_muted, false);
        again.setOnClickListener(v -> compute(a, box, f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (10 * d);
        box.addView(again, lp);
    }

    private static void compute(Activity a, LinearLayout box, File f) {
        box.removeAllViews();
        box.addView(Ui.text(a, "⏳ " + L.t("Liczę tempo z całego nagrania… (przy dłuższym nagraniu do minuty)"), 12f, R.color.pr_muted));
        if (running(f.getName())) {
            // liczy się już w tle (np. zaraz po nagraniu) — sprawdzamy co 2 s
            box.postDelayed(() -> { if (box.isAttachedToWindow()) { if (running(f.getName())) compute(a, box, f); else fillSection(a, box, f); } }, 2000);
            return;
        }
        analyzeAsync(a, f, (r, err) -> {
            if (!box.isAttachedToWindow()) return;
            if (r == null) {
                box.removeAllViews();
                box.addView(Ui.text(a, err == null ? L.t("Nie udało się policzyć tempa.") : err, 12f, R.color.pr_warn));
                return;
            }
            fillSection(a, box, f);
        });
    }

    static void render(Activity a, LinearLayout box, JSONObject r) {
        float d = a.getResources().getDisplayMetrics().density;
        boolean asr = r.optInt("words", -1) >= 0;
        LinearLayout tiles = Ui.row(a);
        tiles.addView(tile(a, L.t("Tempo mowy"), r.optInt("syl_min_speech", 0) + "", L.t("sylab/min") + (asr ? "\n" + r.optInt("wpm_speech", 0) + " " + L.t("słów/min") : "")), Ui.weight(1f, 3 * d));
        tiles.addView(tile(a, L.t("Z pauzami"), r.optInt("syl_min", 0) + "", L.t("sylab/min") + (asr ? "\n" + r.optInt("wpm", 0) + " " + L.t("słów/min") : "")), Ui.weight(1f, 3 * d));
        tiles.addView(tile(a, L.t("Pauzy"), r.optInt("pause_pct", 0) + "%", L.t("nagrania")), Ui.weight(1f, 3 * d));
        box.addView(tiles);
        TextView det = Ui.text(a, L.f("Mowa {0} s · pauzy {1} s (dłuższych niż 1 s: {2})", Math.round(r.optDouble("speech_sec", 0)), Math.round(r.optDouble("pause_sec", 0)), r.optInt("pauses_1s", 0)), 11f, R.color.pr_muted);
        det.setPadding(0, (int) (8 * d), 0, 0);
        box.addView(det);
        // KONFRONTACJA: tekst ↔ dźwięk
        TextView l = Ui.label(a, "🔍 " + L.t("TEKST A DŹWIĘK"));
        l.setPadding(0, (int) (12 * d), 0, (int) (2 * d));
        box.addView(l);
        if (asr) {
            box.addView(Ui.text(a, L.f("Słów: {0} · sylab z tekstu: {1} · sylab z dźwięku: {2}", r.optInt("words", 0), r.optInt("syl_text", 0), r.optInt("syl_audio", 0)), 11f, R.color.pr_text));
            int cov = r.optInt("coverage", -1);
            if (cov >= 0 && false) box.addView(Ui.text(a, L.f("Rozpoznane słowa obejmują {0}% mowy z dźwięku", cov) + (r.optInt("syl_audio_extra", 0) > 0 ? " · " + L.f("dodano {0} sylab z dźwięku tam, gdzie nie rozpoznano słów", r.optInt("syl_audio_extra", 0)) : ""), 11f, cov < 70 ? R.color.pr_warn : R.color.pr_muted));
            int st = r.optInt("syl_text", 0), sa = r.optInt("syl_audio", 0);
            if (st > 0 && sa > 0 && Math.abs(st - sa) > Math.max(st, sa) * 0.2)
                box.addView(Ui.text(a, "⚠️ " + L.t("Tekst i dźwięk różnią się o ponad 20% — posłuchaj nagrania i sprawdź tekst poniżej."), 11f, R.color.pr_warn));
            String tx = r.optString("text", "");
            if (!tx.isEmpty()) {
                TextView tl = Ui.text(a, L.t("Rozpoznany tekst (sprawdź, czy się zgadza):"), 10f, R.color.pr_muted);
                tl.setPadding(0, (int) (8 * d), 0, (int) (2 * d));
                box.addView(tl);
                TextView tt = Ui.text(a, tx, 12f, R.color.pr_text);
                tt.setTextIsSelectable(true);
                box.addView(tt);
            }
        } else {
            box.addView(Ui.text(a, L.f("Sylaby tylko z dźwięku: {0} (bez rozpoznanych słów — wynik przybliżony)", r.optInt("syl_audio", 0)), 11f, R.color.pr_muted));
        }
        String ae = r.optString("asr_err", "");
        if (!ae.isEmpty()) box.addView(Ui.text(a, "⚠️ " + ae, 11f, R.color.pr_warn));
    }

    private static View tile(Activity a, String label, String big, String small) {
        float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout tile = new LinearLayout(a);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding((int) (2 * dp), (int) (8 * dp), (int) (2 * dp), (int) (8 * dp));
        tile.setBackground(Ui.rounded(Ui.col(a, R.color.pr_bg), 0, 0, 10 * dp));
        TextView lb = Ui.text(a, label, 10f, R.color.pr_muted);
        lb.setGravity(Gravity.CENTER);
        tile.addView(lb);
        TextView bg = Ui.text(a, big, 17f, R.color.pr_accent);
        bg.setTypeface(Typeface.DEFAULT_BOLD);
        bg.setGravity(Gravity.CENTER);
        tile.addView(bg);
        TextView sm = Ui.text(a, small, 9f, R.color.pr_muted);
        sm.setGravity(Gravity.CENTER);
        tile.addView(sm);
        return tile;
    }

    // ── STATYSTYKI z telefonu (monologi / czytanie), w kształcie jak z serwera (/speech-rate/mine) ──
    public static JSONObject statsJson(Context c) {
        try {
            File[] files = c.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
            List<JSONObject> items = new ArrayList<>();
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            if (files != null) for (File f : files) {
                JSONObject r = get(c, f.getName());
                if (r == null) continue;
                RecMeta m = RecMeta.load(c, f.getName());
                if (!m.cat.toLowerCase(Locale.ROOT).matches(".*(monolog|czytani).*")) continue;
                if (r.optDouble("dur", 0) < 60) continue;
                JSONObject x = new JSONObject(r.toString());
                x.remove("text");
                x.put("date", df.format(new java.util.Date(f.lastModified())));
                x.put("category", m.cat);
                if (x.optInt("wpm", -1) < 0) { x.put("wpm", 0); x.put("wpm_speech", 0); }
                items.add(x);
            }
            java.util.Collections.sort(items, (p, q) -> q.optString("date", "").compareTo(p.optString("date", "")));
            long now = System.currentTimeMillis();
            String d6 = df.format(new java.util.Date(now - 6 * 86400000L)), d13 = df.format(new java.util.Date(now - 13 * 86400000L)), d29 = df.format(new java.util.Date(now - 29 * 86400000L));
            List<JSONObject> a7 = new ArrayList<>(), p7 = new ArrayList<>(), a30 = new ArrayList<>();
            java.util.TreeMap<String, List<JSONObject>> weeks = new java.util.TreeMap<>();
            for (JSONObject x : items) {
                String dt = x.optString("date", "");
                if (dt.compareTo(d6) >= 0) a7.add(x); else if (dt.compareTo(d13) >= 0) p7.add(x);
                if (dt.compareTo(d29) >= 0) a30.add(x);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(df.parse(dt));
                cal.setFirstDayOfWeek(java.util.Calendar.MONDAY);
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
                String wk = df.format(cal.getTime());
                List<JSONObject> l = weeks.get(wk);
                if (l == null) { l = new ArrayList<>(); weeks.put(wk, l); }
                l.add(x);
            }
            JSONObject out = new JSONObject();
            out.put("ok", true);
            out.put("local", true);
            out.put("d7", sum(a7));
            out.put("prev7", sum(p7));
            out.put("d30", sum(a30));
            out.put("all", sum(items));
            JSONArray W = new JSONArray();
            List<String> keys = new ArrayList<>(weeks.keySet());
            for (String k : keys.subList(Math.max(0, keys.size() - 12), keys.size())) { JSONObject w = sum(weeks.get(k)); w.put("week", k); W.put(w); }
            out.put("weeks", W);
            JSONArray I = new JSONArray();
            for (int i = 0; i < Math.min(30, items.size()); i++) I.put(items.get(i));
            out.put("items", I);
            return out;
        } catch (Exception e) { return null; }
    }

    private static JSONObject sum(List<JSONObject> l) throws Exception {
        JSONObject o = new JSONObject();
        o.put("n", l.size());
        if (l.isEmpty()) return o;
        for (String k : new String[]{"syl_min", "syl_min_speech", "wpm", "wpm_speech", "pause_pct"}) {
            List<Integer> v = new ArrayList<>();
            for (JSONObject x : l) v.add(x.optInt(k, 0));
            java.util.Collections.sort(v);
            int n = v.size();
            o.put(k, n % 2 == 1 ? v.get(n / 2) : Math.round((v.get(n / 2 - 1) + v.get(n / 2)) / 2f));
        }
        return o;
    }
}
