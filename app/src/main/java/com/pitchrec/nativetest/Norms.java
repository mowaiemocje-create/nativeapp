package com.pitchrec.nativetest;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

// NORMY — ocena emisji kazdej porcji mowy wg 4 faz (jak analyzePhases w PitchRec PWA i
// kalibracja z train.html):
//   1. LENIWA   — cichy start (lagodne wejscie),
//   2. NARASTANIE — stopniowy wzrost glosnosci (stale okno = czas narastania wzorca),
//   3. PLATEAU  — utrzymanie szczytu,
//   4. OPADANIE — wyciszenie na koncu.
// Parametry (czasy faz, prog ciszy, tolerancja, stosunek szczyt/cisza, ksztalt wzorca) mozna
// ustawic recznie albo skalibrowac z nagrania trenera. Analiza dziala na RMS okien 2048
// probek (~21,5 okna/s) — tej samej rozdzielczosci co kalibracja w train.html.
public class Norms {

    public static final float FPS = LiveAudioData.SAMPLE_RATE / 2048f;

    // ── parametry (domyslne jak w PWA) ──
    public static volatile float ph1 = 0.5f, ph2 = 1.5f, ph3 = 0.5f, quietThresh = 0.55f, tolerance = 0.10f, pkRatio = 5.0f;
    public static volatile float[] shapeRef = null;
    // Kalibracja z probek wzorca: cechy NAJSLABSZEJ probki trenera (wtedy kazda probka wzorca
    // i kazde identyczne powtorzenie dostaje 100%). Ocena kursanta = porownanie z tymi cechami.
    public static volatile boolean calibrated = false;
    public static volatile int calSamples = 0;
    public static volatile float mGrowth = 2f, mPlatRel = 0.6f, mPlatFrac = 0.6f, mFall = 0.4f, mQuiet = 0.3f, mPk = 5f, mRiseT = 0.5f, mFallT = 0.2f;

    public static void load(SharedPreferences p) {
        ph1 = p.getFloat("norm_ph1", 0.5f);
        ph2 = p.getFloat("norm_ph2", 1.5f);
        ph3 = p.getFloat("norm_ph3", 0.5f);
        quietThresh = p.getFloat("norm_quiet", 0.55f);
        tolerance = p.getFloat("norm_tol", 0.10f);
        pkRatio = p.getFloat("norm_pk", 5.0f);
        calibrated = p.getBoolean("norm_cal", false);
        calSamples = p.getInt("norm_cal_n", 0);
        mGrowth = p.getFloat("norm_m_growth", 2f);
        mPlatRel = p.getFloat("norm_m_prel", 0.6f);
        mPlatFrac = p.getFloat("norm_m_pfrac", 0.6f);
        mFall = p.getFloat("norm_m_fall", 0.4f);
        mQuiet = p.getFloat("norm_m_quiet", 0.3f);
        mPk = p.getFloat("norm_m_pk", 5f);
        mRiseT = p.getFloat("norm_m_riset", 0.5f);
        mFallT = p.getFloat("norm_m_fallt", 0.2f);
        String s = p.getString("norm_shape", "");
        shapeRef = null;
        if (!s.isEmpty()) {
            try {
                String[] parts = s.split(",");
                float[] v = new float[parts.length];
                for (int i = 0; i < parts.length; i++) v[i] = Float.parseFloat(parts[i]);
                shapeRef = v;
            } catch (Exception e) { shapeRef = null; }
        }
    }

    public static void save(SharedPreferences p) {
        StringBuilder sb = new StringBuilder();
        if (shapeRef != null) for (int i = 0; i < shapeRef.length; i++) { if (i > 0) sb.append(','); sb.append(shapeRef[i]); }
        p.edit().putFloat("norm_ph1", ph1).putFloat("norm_ph2", ph2).putFloat("norm_ph3", ph3)
                .putFloat("norm_quiet", quietThresh).putFloat("norm_tol", tolerance).putFloat("norm_pk", pkRatio)
                .putString("norm_shape", sb.toString())
                .putBoolean("norm_cal", calibrated).putInt("norm_cal_n", calSamples)
                .putFloat("norm_m_growth", mGrowth).putFloat("norm_m_prel", mPlatRel).putFloat("norm_m_pfrac", mPlatFrac)
                .putFloat("norm_m_fall", mFall).putFloat("norm_m_quiet", mQuiet).putFloat("norm_m_pk", mPk)
                .putFloat("norm_m_riset", mRiseT).putFloat("norm_m_fallt", mFallT).apply();
    }

    public static void resetDefaults() {
        ph1 = 0.5f; ph2 = 1.5f; ph3 = 0.5f; quietThresh = 0.55f; tolerance = 0.10f; pkRatio = 5.0f; shapeRef = null;
        calibrated = false; calSamples = 0;
    }

    // ── wynik oceny jednej porcji ──
    public static class Result {
        public int score;
        public String detail;          // klucz PL do L.t (z ewentualnymi liczbami w args)
        public Object[] args = new Object[0];
        public Boolean hasQuiet;
        public boolean hasGrowth, hasPlateau, hasFall, hasAmplitude, complete;
        public float growthRatio, studentPkRatio;
        public Integer shapeSimilarity;
    }

    public static class Segment {
        public final double start, end;
        public final Result result;
        public volatile int syllables = -1;     // liczba sylab (po analizie), -1 = jeszcze nie policzono
        public volatile float rate = 0f;        // tempo [sylab/min]
        public float[] buf;                     // RMS okien porcji (tylko przy kalibracji)
        public int preLen;
        Segment(double s, double e, Result r) { start = s; end = e; result = r; }
    }

    private Segment finished = null;

    // Porcja zakonczona w ostatnim wywolaniu frame() (do policzenia sylab) — jednorazowo
    public synchronized Segment pollFinished() { Segment s = finished; finished = null; return s; }

    // Poczatek trwajacej porcji mowy [s] albo -1
    public synchronized double currentSpeechStart() { return segStart < 0 ? -1 : segStart + bufPreLen / FPS; }

    // ── analiza na zywo (wolana dla kazdego okna z nagrywania / wczytanego pliku) ──
    private static final int PRE_N = 64;               // ~3 s historii przed startem mowy
    private static final float HOLD_S = 0.67f;         // jak w PWA: porcja konczy sie po 0,67 s ciszy
    private final float[] pre = new float[PRE_N];
    private int preIdx = 0, preCount = 0;
    private final List<Float> buf = new ArrayList<>();
    private int bufPreLen = 0;
    private double segStart = -1, lastVoiceT = -1;
    private final List<Segment> segments = new ArrayList<>();
    public boolean keepBuffers = false;                 // kalibracja: zachowaj RMS porcji
    public volatile Result live = null;                 // wynik biezacej porcji (do podpowiedzi)
    public volatile long liveAtMs = 0L;

    public synchronized void reset() {
        preIdx = 0; preCount = 0; buf.clear(); bufPreLen = 0; segStart = -1; lastVoiceT = -1;
        segments.clear(); live = null; liveAtMs = 0L; finished = null;
    }

    public synchronized List<Segment> segmentsSnapshot() { return new ArrayList<>(segments); }

    public synchronized void frame(double t, float rms, boolean speech) {
        if (speech) {
            if (segStart < 0) {
                float preTime = Math.min(ph1 * 0.8f, PRE_N / FPS * 0.8f);
                int n = Math.min(preCount, Math.round(preTime * FPS) + 2);
                buf.clear();
                for (int i = n; i > 0; i--) buf.add(pre[(preIdx - i + PRE_N * 4) % PRE_N]);
                bufPreLen = buf.size();
                segStart = t - bufPreLen / FPS;
            }
            lastVoiceT = t;
        }
        if (segStart >= 0) {
            buf.add(rms);
            if (speech && buf.size() - bufPreLen > 5) {
                live = analyze(toArray(buf), bufPreLen);
                liveAtMs = System.currentTimeMillis();
            }
            if (!speech && t - lastVoiceT > HOLD_S) {
                // koniec porcji: obcinamy cisze po ostatnim glosie
                int keep = buf.size() - (int) Math.round((t - lastVoiceT) * FPS);
                double dur = lastVoiceT - (segStart + bufPreLen / FPS);
                if (keep > bufPreLen + 3 && dur >= 0.3) {
                    Result r = analyze(toArray(buf.subList(0, keep)), bufPreLen);
                    live = r;
                    liveAtMs = System.currentTimeMillis();
                    Segment sg = new Segment(segStart + bufPreLen / FPS, lastVoiceT, r);
                    if (keepBuffers) { sg.buf = toArray(buf.subList(0, keep)); sg.preLen = bufPreLen; }
                    segments.add(sg);
                    finished = sg;
                    if (segments.size() > 400) segments.remove(0);
                }
                segStart = -1;
                buf.clear();
            }
        }
        pre[preIdx] = rms;
        preIdx = (preIdx + 1) % PRE_N;
        if (preCount < PRE_N) preCount++;
    }

    public synchronized void finish(double t) {
        if (segStart >= 0) frame(t + HOLD_S + 0.1, 0f, false);
    }

    private static float[] toArray(List<Float> l) {
        float[] a = new float[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    private static float avg(float[] a, int s, int e) {
        float sm = 0;
        for (int i = s; i < e; i++) sm += a[i];
        return sm / Math.max(1, e - s);
    }

    // ── cechy porcji (liczone identycznie dla wzorca i kursanta) ──
    public static class Features {
        public float quiet = Float.NaN;   // srednia cisza przed startem / szczyt (NaN = brak danych)
        public float pk = 1f;             // szczyt / cisza
        public float growth = 1f;         // koniec / poczatek okna narastania
        public boolean gradual = false;   // narastanie bez duzych spadkow
        public float platRel = 0f, platFrac = 0f;
        public float fall = 1f;           // srednia w oknie opadania / szczyt
        public boolean hasRise, hasPlat, hasFallWin;
        public float riseT = 0f, fallT = 0f;  // czas narastania 20→80% szczytu i opadania 80→20% [s]
        public int riseEnd;
    }

    private static float[] smooth3(float[] b) {
        float[] o = new float[b.length];
        for (int i = 0; i < b.length; i++) {
            float s = 0; int c = 0;
            for (int j = i - 1; j <= i + 1; j++) if (j >= 0 && j < b.length) { s += b[j]; c++; }
            o[i] = s / c;
        }
        return o;
    }

    public static Features features(float[] raw, int preLen) {
        Features f = new Features();
        float[] b = smooth3(raw);
        int n = b.length;
        float pk = 0;
        int pkI = Math.min(preLen, n - 1);
        for (int i = Math.min(preLen, n); i < n; i++) if (b[i] > pk) { pk = b[i]; pkI = i; }
        if (pk <= 0) return f;
        // koncowka porcji bez glosu (opoznienie wykrywania konca mowy) nie moze udawac wyciszenia
        int nEff = n;
        while (nEff - 1 > pkI && b[nEff - 1] < pk * 0.15f) nEff--;
        n = nEff;
        // czasy narastania i opadania (odporne na to, gdzie VAD uznal start/koniec porcji)
        int r20 = -1, r80 = pkI;
        for (int i = 0; i <= pkI; i++) { if (r20 < 0 && b[i] >= pk * 0.2f) r20 = i; if (b[i] >= pk * 0.8f) { r80 = i; break; } }
        f.riseT = r20 >= 0 ? (r80 - r20) / FPS : 0f;
        int f80 = pkI, f20 = n - 1;
        for (int i = n - 1; i >= pkI; i--) if (b[i] >= pk * 0.8f) { f80 = i; break; }
        for (int i = f80; i < n; i++) if (b[i] < pk * 0.2f) { f20 = i; break; }
        f.fallT = (f20 - f80) / FPS;
        int s2 = Math.round(ph2 * FPS), s3 = Math.round(ph3 * FPS);
        int riseStart = Math.min(preLen, n);
        int riseEnd = Math.min(n, riseStart + Math.max(3, s2));
        int fallStart = Math.max(riseEnd, n - Math.max(3, s3));
        f.riseEnd = riseEnd;
        int minPre = Math.round(0.15f * FPS), liveWin = Math.round(0.5f * FPS);
        float quiet;
        if (preLen >= minPre) quiet = avg(b, 0, preLen);
        else quiet = avg(b, riseStart, Math.min(n, riseStart + liveWin));
        if (preLen >= minPre || n - riseStart >= minPre) f.quiet = quiet / pk;
        f.pk = quiet > 0.0005f ? pk / quiet : 999f;
        if (riseEnd > riseStart + 3) {
            f.hasRise = true;
            int len = riseEnd - riseStart, w = Math.max(1, len / 3);
            float a1 = avg(b, riseStart, riseStart + w), a2 = avg(b, riseStart + w, riseStart + 2 * w), a3 = avg(b, riseStart + 2 * w, riseEnd);
            f.growth = a1 > 0.0005f ? a3 / a1 : 1f;
            f.gradual = a2 >= a1 * 0.7f && a3 >= a2 * 0.7f;
        }
        if (fallStart > riseEnd + 3) {
            f.hasPlat = true;
            float[] ps = java.util.Arrays.copyOfRange(b, riseEnd, fallStart);
            float[] sorted = ps.clone();
            java.util.Arrays.sort(sorted);
            f.platRel = sorted[sorted.length / 2] / pk;
            int above = 0;
            for (float v : ps) if (v > pk * 0.60f) above++;
            f.platFrac = above / (float) ps.length;
        }
        if (n - fallStart >= 3) { f.hasFallWin = true; f.fall = avg(b, fallStart, n) / pk; }
        return f;
    }

    // Ocena porcji: 4 fazy + glosnosc szczytu. Po kalibracji progi = cechy wzorca z tolerancja.
    public static Result analyze(float[] b, int preLen) {
        Result r = new Result();
        int n = b.length;
        float mx = 0;
        for (float v : b) mx = Math.max(mx, v);
        if (n < 6 || mx < 0.001f) { r.score = 20; r.detail = "LENIWA: czekam…"; return r; }
        float tol = tolerance;
        Features f = features(b, preLen);
        float needGrowth;
        if (calibrated) {
            r.hasQuiet = Float.isNaN(f.quiet) ? null : f.quiet <= mQuiet * (1 + 3 * tol) + 0.05f;
            r.hasAmplitude = f.pk >= mPk * (1 - 2 * tol);
            needGrowth = Math.max(1.1f, mGrowth * (1 - 2.5f * tol));
            r.hasGrowth = f.hasRise && f.growth >= needGrowth && f.gradual && f.riseT >= Math.max(0.1f, mRiseT * (1 - 2.5f * tol));
            r.hasPlateau = f.hasPlat && f.platRel >= mPlatRel - 2 * tol && f.platFrac >= mPlatFrac - 2.5f * tol;
            r.hasFall = f.hasFallWin && f.fall <= Math.min(0.97f, mFall + 2 * tol) && f.fallT >= Math.max(0.05f, mFallT * (1 - 2.5f * tol));
        } else {
            r.hasQuiet = Float.isNaN(f.quiet) ? null : f.quiet < (quietThresh + tol * 0.3f);
            r.hasAmplitude = f.pk >= pkRatio * (1 - tol * 1.5f) || pkRatio <= 2;
            needGrowth = Math.max(1.3f, 2.0f - tol * 3);
            r.hasGrowth = f.hasRise && f.growth >= needGrowth && f.gradual && f.riseT >= Math.max(0.2f, ph2 * 0.3f);
            r.hasPlateau = f.hasPlat && f.platRel > (0.55f - tol) && f.platFrac > Math.max(0.30f, 0.55f - tol);
            r.hasFall = f.hasFallWin && f.fall < (0.50f + tol) && f.fallT >= Math.max(0.1f, ph3 * 0.3f);
        }
        r.growthRatio = f.growth;
        r.studentPkRatio = f.pk;

        int score = 20;
        if (Boolean.TRUE.equals(r.hasQuiet)) score += 20;
        if (r.hasGrowth) score += 20;
        if (r.hasPlateau) score += 20;
        if (r.hasFall) score += 10;
        if (r.hasAmplitude) score += 10;
        if (!Boolean.FALSE.equals(r.hasQuiet) && r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude) score = 100;
        else if (r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude) score = Math.max(score, 88);
        else if (r.hasGrowth && r.hasPlateau && r.hasFall) score = Math.max(score, 78);
        else if (r.hasGrowth && r.hasPlateau) score = Math.max(score, 70);
        r.score = score;
        r.complete = r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude;

        float gp = Math.round(r.growthRatio * 10) / 10f;
        int s2 = Math.round(ph2 * FPS);
        float refPk = calibrated ? mPk : pkRatio;
        if (n < preLen + s2 * 0.3f) { r.detail = "LENIWA: czekam…"; }
        else if (!r.hasGrowth) { r.detail = "NARASTANIE: ×{0} (cel ×{1})"; r.args = new Object[]{fmt(gp), fmt(needGrowth)}; }
        else if (!r.hasPlateau && n < f.riseEnd + 6) { r.detail = "NARASTANIE ✓ ×{0} — trzymaj szczyt"; r.args = new Object[]{fmt(gp)}; }
        else if (!r.hasPlateau) r.detail = "PLATEAU: zbyt krótki";
        else if (!r.hasFall) r.detail = "PLATEAU ✓ — zacznij wyciszać";
        else if (!r.hasAmplitude) { r.detail = "KSZTAŁT ✓ — za cicho! (×{0} / wzorzec ×{1})"; r.args = new Object[]{fmt(r.studentPkRatio), fmt(refPk)}; }
        else if (Boolean.FALSE.equals(r.hasQuiet)) r.detail = "EMISJA ✓ — na starcie trochę za głośno";
        else r.detail = "DOBRA EMISJA ✓";

        float[] ref = shapeRef;
        if (ref != null && ref.length > 1) r.shapeSimilarity = Math.round(cosine(ref, shape(b, ref.length)) * 100);
        return r;
    }

    private static String fmt(float v) { return String.format(java.util.Locale.US, "%.1f", v); }

    public static float[] shape(float[] b, int points) {
        float pk = 0;
        for (float v : b) pk = Math.max(pk, Math.abs(v));
        float[] out = new float[points];
        if (pk < 0.0001f || b.length == 0) return out;
        for (int i = 0; i < points; i++) {
            float pos = i / (float) (points - 1) * (b.length - 1);
            int i0 = (int) Math.floor(pos), i1 = Math.min(b.length - 1, i0 + 1);
            float fr = pos - i0;
            out[i] = Math.abs(b[i0] * (1 - fr) + b[i1] * fr) / pk;
        }
        return out;
    }

    private static float cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        double den = Math.sqrt(na) * Math.sqrt(nb);
        return (float) (den == 0 ? 0 : dot / den);
    }

    // ── KALIBRACJA z probek wzorca (1 lub wiecej nagran, w kazdym 1 lub wiecej porcji) ──
    // Porcje sa wyciete TA SAMA metoda co przy ocenie (VAD + te same okna RMS), wiec wzorzec i
    // kursant sa mierzeni identycznie. Czasy faz = srednia z probek; progi = cechy najslabszej probki.
    public static class Calib {
        public int samples;
        public float ph2, ph3, plateau;
        public int[] scoresAfter;
        public String error;
    }

    public static Calib calibrateSegments(List<float[]> bufs, List<Integer> pres) {
        Calib c = new Calib();
        c.samples = bufs.size();
        if (bufs.isEmpty()) { c.error = "none"; return c; }
        float sumRise = 0, sumFall = 0, sumPlat = 0;
        for (int k = 0; k < bufs.size(); k++) {
            float[] b = smooth3(bufs.get(k));
            int pre = pres.get(k), n = b.length;
            float pk = 0; int pkI = pre;
            for (int i = pre; i < n; i++) if (b[i] > pk) { pk = b[i]; pkI = i; }
            while (n - 1 > pkI && b[n - 1] < pk * 0.15f) n--;
            int riseEnd = pkI;
            for (int i = pre; i <= pkI; i++) if (b[i] >= pk * 0.9f) { riseEnd = i; break; }
            int fallStart = pkI;
            for (int i = n - 1; i >= pkI; i--) if (b[i] >= pk * 0.7f) { fallStart = i; break; }
            sumRise += (riseEnd - pre) / FPS;
            sumFall += (n - 1 - fallStart) / FPS;
            sumPlat += (fallStart - riseEnd) / FPS;
        }
        int m = bufs.size();
        ph2 = clamp(r1(sumRise / m), 0.3f, 4.0f);
        ph3 = clamp(r1(sumFall / m), 0.2f, 1.5f);
        c.ph2 = ph2; c.ph3 = ph3; c.plateau = r1(sumPlat / m);
        // cechy z nowymi oknami — najslabsza probka wyznacza prog
        float g = Float.MAX_VALUE, pr = Float.MAX_VALUE, pf = Float.MAX_VALUE, fl = 0, q = 0, pkr = Float.MAX_VALUE, rt = Float.MAX_VALUE, ft = Float.MAX_VALUE;
        boolean anyQ = false;
        float[] shapeSum = new float[60];
        for (int k = 0; k < m; k++) {
            Features f = features(bufs.get(k), pres.get(k));
            g = Math.min(g, f.growth);
            pr = Math.min(pr, f.platRel);
            pf = Math.min(pf, f.platFrac);
            fl = Math.max(fl, f.fall);
            if (!Float.isNaN(f.quiet)) { q = Math.max(q, f.quiet); anyQ = true; }
            pkr = Math.min(pkr, Math.min(f.pk, 200f));
            rt = Math.min(rt, f.riseT);
            ft = Math.min(ft, f.fallT);
            float[] sh = shape(bufs.get(k), 60);
            for (int i = 0; i < 60; i++) shapeSum[i] += sh[i] / m;
        }
        // Gorne limity progow: cisza przed mowa i szum tla zaleza od pomieszczenia, wiec bardzo
        // wysokie wartosci ze studia trenera nie moga blokowac kursanta w zwyklym otoczeniu.
        mGrowth = Math.max(1.05f, Math.min(3.0f, g));
        mPlatRel = pr; mPlatFrac = pf; mFall = Math.max(0.2f, fl);
        mQuiet = Math.max(0.25f, anyQ ? q : 0.5f);
        mPk = Math.max(1.5f, Math.min(8f, pkr));
        mRiseT = Math.min(2.0f, rt);
        mFallT = Math.min(1.0f, ft);
        pkRatio = mPk; quietThresh = mQuiet;
        shapeRef = shapeSum;
        calibrated = true;
        calSamples = m;
        c.scoresAfter = new int[m];
        for (int k = 0; k < m; k++) c.scoresAfter[k] = analyze(bufs.get(k), pres.get(k)).score;
        return c;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float r1(float v) { return Math.round(v * 10) / 10f; }
}
