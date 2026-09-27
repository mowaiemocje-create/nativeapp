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

    public static void load(SharedPreferences p) {
        ph1 = p.getFloat("norm_ph1", 0.5f);
        ph2 = p.getFloat("norm_ph2", 1.5f);
        ph3 = p.getFloat("norm_ph3", 0.5f);
        quietThresh = p.getFloat("norm_quiet", 0.55f);
        tolerance = p.getFloat("norm_tol", 0.10f);
        pkRatio = p.getFloat("norm_pk", 5.0f);
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
                .putString("norm_shape", sb.toString()).apply();
    }

    public static void resetDefaults() {
        ph1 = 0.5f; ph2 = 1.5f; ph3 = 0.5f; quietThresh = 0.55f; tolerance = 0.10f; pkRatio = 5.0f; shapeRef = null;
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
        Segment(double s, double e, Result r) { start = s; end = e; result = r; }
    }

    // ── analiza na zywo (wolana dla kazdego okna z nagrywania / wczytanego pliku) ──
    private static final int PRE_N = 64;               // ~3 s historii przed startem mowy
    private static final float HOLD_S = 0.67f;         // jak w PWA: porcja konczy sie po 0,67 s ciszy
    private final float[] pre = new float[PRE_N];
    private int preIdx = 0, preCount = 0;
    private final List<Float> buf = new ArrayList<>();
    private int bufPreLen = 0;
    private double segStart = -1, lastVoiceT = -1;
    private final List<Segment> segments = new ArrayList<>();
    public volatile Result live = null;                 // wynik biezacej porcji (do podpowiedzi)
    public volatile long liveAtMs = 0L;

    public synchronized void reset() {
        preIdx = 0; preCount = 0; buf.clear(); bufPreLen = 0; segStart = -1; lastVoiceT = -1;
        segments.clear(); live = null; liveAtMs = 0L;
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
                    segments.add(new Segment(segStart + bufPreLen / FPS, lastVoiceT, r));
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

    // Wierny port analyzePhases (PWA) — stale okna czasowe z parametrow wzorca.
    public static Result analyze(float[] b, int preLen) {
        Result r = new Result();
        int n = b.length;
        float pk = 0;
        for (float v : b) pk = Math.max(pk, v);
        if (n < 6 || pk < 0.001f) { r.score = 20; r.detail = "LENIWA: czekam…"; return r; }
        float tol = tolerance;
        int s2 = Math.round(ph2 * FPS), s3 = Math.round(ph3 * FPS);
        int riseStart = Math.min(preLen, n);
        int riseEnd = Math.min(n, riseStart + Math.max(3, s2));
        int fallStart = Math.max(riseEnd, n - Math.max(3, s3));
        int platStart = riseEnd, platEnd = Math.max(platStart, fallStart);

        // FAZA 1: leniwa
        int liveWin = Math.round(0.5f * FPS);
        int minPre = Math.round(0.15f * FPS);
        float quietAvg = 0;
        int qs, qe;
        if (preLen >= minPre) { qs = 0; qe = preLen; } else { qs = riseStart; qe = Math.min(n, riseStart + liveWin); }
        if (qe - qs < minPre) r.hasQuiet = null;
        else {
            quietAvg = avg(b, qs, qe);
            r.hasQuiet = quietAvg / pk < (quietThresh + tol * 0.3f);
        }
        float studentQuiet = preLen > 0 ? avg(b, 0, preLen) : avg(b, riseStart, Math.min(n, riseStart + liveWin));
        r.studentPkRatio = studentQuiet > 0.001f ? pk / studentQuiet : 999f;
        float minAccepted = pkRatio * (1 - tol * 1.5f);
        r.hasAmplitude = r.studentPkRatio >= minAccepted || pkRatio <= 2;

        // FAZA 2: narastanie
        r.growthRatio = 1f;
        if (riseEnd > riseStart + 3) {
            int len = riseEnd - riseStart, w = Math.max(1, len / 3);
            float a1 = avg(b, riseStart, riseStart + w), a2 = avg(b, riseStart + w, riseStart + 2 * w), a3 = avg(b, riseStart + 2 * w, riseEnd);
            r.growthRatio = a1 > 0.0005f ? a3 / a1 : 1f;
            boolean gradual = a2 >= a1 * 0.78f && a3 >= a2 * 0.78f;
            float minRatio = Math.max(1.3f, 2.0f - tol * 3);
            r.hasGrowth = r.growthRatio >= minRatio && gradual;
        }
        // FAZA 3: plateau (mediana)
        if (platEnd > platStart + 3) {
            float[] ps = java.util.Arrays.copyOfRange(b, platStart, platEnd);
            float[] sorted = ps.clone();
            java.util.Arrays.sort(sorted);
            float pRel = sorted[sorted.length / 2] / pk;
            int above = 0;
            for (float v : ps) if (v > pk * 0.60f) above++;
            float frac = above / (float) Math.max(1, ps.length);
            r.hasPlateau = pRel > (0.55f - tol) && frac > Math.max(0.30f, 0.55f - tol);
        }
        // FAZA 4: opadanie
        float fallAvg = n - fallStart > 0 ? avg(b, fallStart, n) : pk;
        r.hasFall = (fallAvg / pk) < (0.50f + tol) && n - fallStart >= 3;

        int score = 20;
        if (Boolean.TRUE.equals(r.hasQuiet)) score += 20;
        if (r.hasGrowth) score += 20;
        if (r.hasPlateau) score += 20;
        if (r.hasFall) score += 10;
        if (r.hasAmplitude) score += 10;
        if (Boolean.TRUE.equals(r.hasQuiet) && r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude) score = 100;
        else if (r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude) score = Math.max(score, 88);
        else if (r.hasGrowth && r.hasPlateau && r.hasFall) score = Math.max(score, 78);
        else if (r.hasGrowth && r.hasPlateau) score = Math.max(score, 70);
        r.score = score;
        r.complete = r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude;

        float gp = Math.round(r.growthRatio * 10) / 10f;
        if (n < preLen + s2 * 0.3f) { r.detail = "LENIWA: czekam…"; }
        else if (!r.hasGrowth) { r.detail = "NARASTANIE: ×{0} (cel ×{1})"; r.args = new Object[]{fmt(gp), fmt(2.0f - tol * 3)}; }
        else if (!r.hasPlateau && n < riseEnd + 6) { r.detail = "NARASTANIE ✓ ×{0} — trzymaj szczyt"; r.args = new Object[]{fmt(gp)}; }
        else if (!r.hasPlateau) r.detail = "PLATEAU: zbyt krótki";
        else if (!r.hasFall) r.detail = "PLATEAU ✓ — zacznij wyciszać";
        else if (!r.hasAmplitude) { r.detail = "KSZTAŁT ✓ — za cicho! (×{0} / wzorzec ×{1})"; r.args = new Object[]{fmt(r.studentPkRatio), fmt(pkRatio)}; }
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

    // ── KALIBRACJA z nagrania wzorcowego (port computeCalibrationFromBuf z train.html) ──
    public static class Calib { public float ph1, ph2, ph3, plateau, quiet, pk; public float[] shape; }

    public static Calib calibrate(float[] raw) {
        int n = raw.length;
        if (n < 10) return null;
        float[] b = new float[n];
        for (int i = 0; i < n; i++) {
            float s = 0; int c = 0;
            for (int j = Math.max(0, i - 2); j <= Math.min(n - 1, i + 2); j++) { s += raw[j]; c++; }
            b[i] = s / c;
        }
        float pk = 0; int pkIdx = 0;
        for (int i = 0; i < n; i++) if (b[i] > pk) { pk = b[i]; pkIdx = i; }
        if (pk < 0.002f) return null;
        int qEnd = Math.max(1, (int) (n * 0.15));
        float[] q = java.util.Arrays.copyOfRange(b, 0, qEnd);
        java.util.Arrays.sort(q);
        float qMed = q[q.length / 2];
        if (qMed <= 0) qMed = 0.01f;
        float thr = Math.min(0.85f, Math.max(0.1f, qMed * 2.5f / pk));
        int p1 = 0;
        for (int i = 0; i < n; i++) if (b[i] > pk * 0.25f) { p1 = i; break; }
        if (p1 == 0) p1 = (int) (n * 0.1);
        int platS = pkIdx;
        for (int i = pkIdx; i > p1; i--) if (b[i] < pk * 0.90f) { platS = i; break; }
        int platE = pkIdx;
        for (int i = pkIdx; i < n; i++) if (b[i] < pk * 0.70f) { platE = i; break; }
        if (platE == pkIdx) platE = Math.min(n - 1, pkIdx + (int) (n * 0.05));
        Calib c = new Calib();
        c.ph1 = clamp(p1 / FPS, 0.3f, 3.0f);
        c.ph2 = clamp((platS - p1) / FPS, 0.3f, 4.0f);
        c.plateau = (platE - platS) / FPS;
        c.ph3 = clamp((n - platE) / FPS, 0.1f, 1.5f);
        float qa = avg(b, 0, Math.max(1, p1));
        c.pk = clamp(qa > 0.001f ? pk / qa : 10f, 2f, 50f);
        c.quiet = thr;
        c.shape = shape(b, 60);
        c.ph1 = r1(c.ph1); c.ph2 = r1(c.ph2); c.ph3 = r1(c.ph3); c.plateau = r1(c.plateau); c.pk = r1(c.pk);
        c.quiet = Math.round(c.quiet * 100) / 100f;
        return c;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float r1(float v) { return Math.round(v * 10) / 10f; }
}
