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
    public static volatile float mLazyT = 0.5f, mLazyL = 0.4f, mRiseT = 0.3f, mPlatT = 0.2f, mFallT = 0.2f, mPk = 5f;

    public static void load(SharedPreferences p) {
        ph1 = p.getFloat("norm_ph1", 0.5f);
        ph2 = p.getFloat("norm_ph2", 1.5f);
        ph3 = p.getFloat("norm_ph3", 0.5f);
        quietThresh = p.getFloat("norm_quiet", 0.55f);
        tolerance = p.getFloat("norm_tol", 0.10f);
        pkRatio = p.getFloat("norm_pk", 5.0f);
        calibrated = p.getBoolean("norm_cal", false);
        calSamples = p.getInt("norm_cal_n", 0);
        mLazyT = p.getFloat("norm4_lazyt", 0.5f);
        mLazyL = p.getFloat("norm4_lazyl", 0.4f);
        mRiseT = p.getFloat("norm4_riset", 0.3f);
        mPlatT = p.getFloat("norm4_platt", 0.2f);
        mFallT = p.getFloat("norm4_fallt", 0.2f);
        mPk = p.getFloat("norm4_pk", 5f);
        if (!p.contains("norm4_lazyt")) calibrated = false; // stara kalibracja (inna metoda) — do powtorzenia
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
                .putFloat("norm4_lazyt", mLazyT).putFloat("norm4_lazyl", mLazyL).putFloat("norm4_riset", mRiseT)
                .putFloat("norm4_platt", mPlatT).putFloat("norm4_fallt", mFallT).putFloat("norm4_pk", mPk).apply();
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
        public double unitEnd = -1, unitPeak = -1; // oceniana (pierwsza) sylaba: koniec i szczyt [s]
        public boolean fourPhase = false;           // czy pierwsza sylaba miala ksztalt 4-fazowy
        public float[] buf;                     // RMS okien sylaby 4-fazowej (tylko przy kalibracji)
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
    private boolean unitDone = false;
    public volatile double liveUnitEnd = -1;   // koniec pierwszej sylaby trwajacej porcji (-1 = jeszcze trwa)
    public volatile boolean liveFour = false;  // czy ta pierwsza sylaba jest 4-fazowa
    public boolean keepBuffers = false;                 // kalibracja: zachowaj RMS porcji
    public volatile Result live = null;                 // wynik biezacej porcji (do podpowiedzi)
    public volatile long liveAtMs = 0L;

    public synchronized void reset() {
        preIdx = 0; preCount = 0; buf.clear(); bufPreLen = 0; segStart = -1; lastVoiceT = -1; unitDone = false;
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
                // Na zywo: dopoki trwa sylaba 4-fazowa (pierwsza w porcji) — ocena biezaca;
                // po jej wyciszeniu wynik zostaje zamrozony (dalej sa juz sylaby zwykle).
                if (!unitDone) {
                    float[] arr = toArray(buf);
                    int ue = unitEnd(arr, bufPreLen);
                    if (ue < 0) { live = analyze(arr, bufPreLen); }
                    else {
                        unitDone = true;
                        liveFour = isFourPhase(arr, bufPreLen, ue);
                        if (!liveFour) ue = firstSyllableEnd(arr, bufPreLen);
                        liveUnitEnd = segStart + ue / FPS;
                        live = analyze(java.util.Arrays.copyOf(arr, ue), bufPreLen);
                    }
                    liveAtMs = System.currentTimeMillis();
                }
            }
            if (!speech && t - lastVoiceT > HOLD_S) {
                // koniec porcji: obcinamy cisze po ostatnim glosie
                int keep = buf.size() - (int) Math.round((t - lastVoiceT) * FPS);
                double dur = lastVoiceT - (segStart + bufPreLen / FPS);
                if (keep > bufPreLen + 3 && dur >= 0.3) {
                    float[] arr = toArray(buf.subList(0, keep));
                    int ue = unitEnd(arr, bufPreLen);
                    if (ue < 0) ue = arr.length;
                    boolean four = isFourPhase(arr, bufPreLen, ue);
                    if (!four) ue = firstSyllableEnd(arr, bufPreLen);
                    float[] unit = java.util.Arrays.copyOf(arr, ue);
                    Result r = analyze(unit, bufPreLen);
                    live = r;
                    liveAtMs = System.currentTimeMillis();
                    Segment sg = new Segment(segStart + bufPreLen / FPS, lastVoiceT, r);
                    sg.fourPhase = four;
                    sg.unitEnd = segStart + ue / FPS;
                    sg.unitPeak = segStart + peakIndex(unit, bufPreLen) / FPS;
                    if (keepBuffers && four) { sg.buf = unit; sg.preLen = bufPreLen; }
                    segments.add(sg);
                    finished = sg;
                    if (segments.size() > 400) segments.remove(0);
                }
                segStart = -1;
                unitDone = false;
                liveUnitEnd = -1; liveFour = false;
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

    // ── SYLABA 4-FAZOWA = pierwsza sylaba porcji mowy: cichy start, narastanie, szczyt,
    // wyciszenie. Po niej moga byc "zwykle" sylaby (ciszej) — one nie sa oceniane wg 4 faz.
    // Koniec sylaby = pierwszy dolek po najglosniejszym miejscu porcji (spadek < 30% szczytu).
    static int peakIndex(float[] raw, int preLen) {
        float[] b = smooth3(raw);
        int pkI = Math.min(preLen, b.length - 1);
        for (int i = Math.min(preLen, b.length); i < b.length; i++) if (b[i] > b[pkI]) pkI = i;
        return pkI;
    }

    // Indeks konca sylaby 4-fazowej (wylacznie) albo -1, gdy jeszcze sie nie wyciszyla
    public static int unitEnd(float[] raw, int preLen) {
        float[] b = smooth3(raw);
        int n = b.length;
        int pkI = peakIndex(raw, preLen);
        float pk = b[pkI];
        if (pk <= 0.0005f) return -1;
        int j = -1;
        for (int i = pkI + 1; i < n; i++) if (b[i] < pk * 0.3f) { j = i; break; }
        if (j < 0) return -1;
        while (j + 1 < n && b[j + 1] < b[j]) j++;       // do najnizszego punktu dolka
        return Math.min(n, j + 1);
    }

    // Czy to sylaba 4-fazowa: dlugi, cichy start (faza leniwa) i wyrazny szczyt?
    public static boolean isFourPhase(float[] raw, int preLen, int end) {
        if (end <= preLen + 3 || (end - preLen) / FPS < 0.6f) return false;
        Features f = features(java.util.Arrays.copyOf(raw, end), preLen);
        return f.lazyT >= 0.35f && f.lazyLevel <= 0.55f;
    }

    // Koniec pierwszej sylaby (gdy nie jest 4-fazowa): pierwszy wyrazny szczyt i dolek po nim
    public static int firstSyllableEnd(float[] raw, int preLen) {
        float[] b = smooth3(raw);
        int n = b.length, st = Math.min(preLen, n - 1);
        float gpk = 0;
        for (int i = st; i < n; i++) gpk = Math.max(gpk, b[i]);
        int m1 = -1;
        for (int i = st + 1; i < n - 1; i++) if (b[i] >= gpk * 0.3f && b[i] >= b[i - 1] && b[i] >= b[i + 1]) { m1 = i; break; }
        if (m1 < 0) return n;
        int j = -1;
        for (int i = m1 + 1; i < n; i++) if (b[i] < b[m1] * 0.5f) { j = i; break; }
        if (j < 0) return n;
        while (j + 1 < n && b[j + 1] < b[j]) j++;
        return Math.min(n, j + 1);
    }

    // ── CECHY SYLABY 4-FAZOWEJ (liczone identycznie dla wzorca i kursanta) ──
    // Wszystko wzgledem szczytu i jego progow — NIE zalezy od tego, ile trwa cicha faza
    // leniwa ani kiedy dokladnie wykrywanie uznalo start mowy:
    //   faza leniwa  = od startu do przekroczenia 50% szczytu: czas i poziom (mediana / szczyt)
    //   narastanie   = od ostatniego miejsca < 50% do osiagniecia 90% szczytu
    //   szczyt       = jak dlugo glos trzyma >= 80% szczytu
    //   opadanie     = od ostatniego >= 80% do spadku < 30% szczytu
    public static class Features {
        public float lazyT = 0f, lazyLevel = 1f, riseT = 0f, platT = 0f, fallT = 0f;
        public float pk = 1f;             // szczyt / cisza przed startem
        public float quiet = Float.NaN;   // (zgodnosc) = lazyLevel
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
        int st = Math.min(preLen, n - 1);
        int pkI = st;
        for (int i = st; i < n; i++) if (b[i] > b[pkI]) pkI = i;
        float pk = b[pkI];
        if (pk <= 0.0005f) return f;
        while (n - 1 > pkI && b[n - 1] < pk * 0.15f) n--;
        // faza leniwa
        int t50 = pkI;
        for (int i = st; i <= pkI; i++) if (b[i] >= pk * 0.5f) { t50 = i; break; }
        f.lazyT = (t50 - st) / FPS;
        if (t50 - st >= 2) {
            float[] lz = java.util.Arrays.copyOfRange(b, st, t50);
            java.util.Arrays.sort(lz);
            f.lazyLevel = lz[lz.length / 2] / pk;
        } else f.lazyLevel = b[st] / pk;
        f.quiet = f.lazyLevel;
        // narastanie
        int r90 = pkI;
        for (int i = st; i <= pkI; i++) if (b[i] >= pk * 0.9f) { r90 = i; break; }
        int r50 = st;
        for (int i = r90; i >= st; i--) if (b[i] < pk * 0.5f) { r50 = i; break; }
        f.riseT = (r90 - r50) / FPS;
        // szczyt
        int p0 = pkI, p1 = pkI;
        while (p0 - 1 >= st && b[p0 - 1] >= pk * 0.8f) p0--;
        while (p1 + 1 < n && b[p1 + 1] >= pk * 0.8f) p1++;
        f.platT = (p1 - p0 + 1) / FPS;
        // opadanie
        int f30 = n - 1;
        for (int i = p1; i < n; i++) if (b[i] < pk * 0.3f) { f30 = i; break; }
        f.fallT = (f30 - p1) / FPS;
        // glosnosc szczytu wzgledem ciszy przed mowa
        float q = preLen >= 3 ? avg(b, 0, preLen) : b[st];
        f.pk = q > 0.0005f ? pk / q : 999f;
        return f;
    }

    // Ocena sylaby 4-fazowej. Po kalibracji progi = cechy wzorca (najslabsza probka) z tolerancja.
    public static Result analyze(float[] b, int preLen) {
        Result r = new Result();
        int n = b.length;
        float mx = 0;
        for (float v : b) mx = Math.max(mx, v);
        if (n < 6 || mx < 0.001f) { r.score = 20; r.detail = "LENIWA: czekam…"; return r; }
        float tol = tolerance, k = 1 - 2.5f * tol;
        Features f = features(b, preLen);
        float nLazyT, nLazyL, nRise, nPlat, nFall, nPk;
        if (calibrated) {
            nLazyT = mLazyT * k; nLazyL = Math.min(0.9f, mLazyL + 2 * tol + 0.05f);
            nRise = mRiseT * k; nPlat = mPlatT * k; nFall = mFallT * k; nPk = mPk * (1 - 2 * tol);
        } else {
            nLazyT = Math.max(0.2f, ph1 * 0.5f); nLazyL = quietThresh + tol * 0.3f;
            nRise = Math.max(0.15f, ph2 * 0.2f); nPlat = 0.1f; nFall = Math.max(0.1f, ph3 * 0.3f); nPk = pkRatio * (1 - tol * 1.5f);
        }
        r.hasQuiet = f.lazyT >= nLazyT && f.lazyLevel <= nLazyL;
        r.hasGrowth = f.riseT >= nRise;
        r.hasPlateau = f.platT >= nPlat;
        r.hasFall = f.fallT >= nFall;
        r.hasAmplitude = f.pk >= nPk || (!calibrated && pkRatio <= 2);
        r.growthRatio = f.riseT;
        r.studentPkRatio = f.pk;

        int score = 20;
        if (Boolean.TRUE.equals(r.hasQuiet)) score += 20;
        if (r.hasGrowth) score += 20;
        if (r.hasPlateau) score += 20;
        if (r.hasFall) score += 10;
        if (r.hasAmplitude) score += 10;
        r.score = score;
        r.complete = r.hasGrowth && r.hasPlateau && r.hasFall && r.hasAmplitude;

        if (!Boolean.TRUE.equals(r.hasQuiet)) {
            if (f.lazyT < nLazyT) { r.detail = "FAZA LENIWA: za krótka ({0} s / wzorzec {1} s)"; r.args = new Object[]{fmt(f.lazyT), fmt(nLazyT)}; }
            else { r.detail = "FAZA LENIWA: za głośno — zacznij ciszej"; }
        }
        else if (!r.hasGrowth) { r.detail = "NARASTANIE: za szybkie ({0} s / wzorzec {1} s)"; r.args = new Object[]{fmt(f.riseT), fmt(nRise)}; }
        else if (!r.hasPlateau) { r.detail = "SZCZYT: za krótki ({0} s / wzorzec {1} s)"; r.args = new Object[]{fmt(f.platT), fmt(nPlat)}; }
        else if (!r.hasFall) { r.detail = "OPADANIE: za szybkie ({0} s / wzorzec {1} s)"; r.args = new Object[]{fmt(f.fallT), fmt(nFall)}; }
        else if (!r.hasAmplitude) { r.detail = "KSZTAŁT ✓ — za cicho! (×{0} / wzorzec ×{1})"; r.args = new Object[]{fmt(f.pk), fmt(nPk)}; }
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

    // ── KALIBRACJA z probek wzorca (sylaby 4-fazowe z 1 lub wiecej nagran) ──
    // Probki wycinane TA SAMA metoda co przy ocenie. Progi = najslabsza probka, wiec kazda
    // probka wzorca i kazde identyczne powtorzenie dostaje 100%.
    public static class Calib {
        public int samples;
        public float lazyT, riseT, platT, fallT;
        public int[] scoresAfter;
        public int rejected;
        public String error;
    }

    public static Calib calibrateSegments(List<float[]> bufs, List<Integer> pres) {
        Calib c = new Calib();
        c.samples = bufs.size();
        if (bufs.isEmpty()) { c.error = "none"; return c; }
        int m = bufs.size();
        Features[] fs = new Features[m];
        for (int i = 0; i < m; i++) fs[i] = features(bufs.get(i), pres.get(i));
        // Odrzucenie probek wyraznie innych niz reszta (np. nieudana probka): probka jest
        // "inna", gdy co najmniej 2 z 4 czasow faz odbiegaja o > 50% od mediany wszystkich.
        boolean[] in = new boolean[m];
        float medL = median(fs, 0), medR = median(fs, 1), medP = median(fs, 2), medF = median(fs, 3);
        int inliers = 0;
        for (int i = 0; i < m; i++) {
            int off = 0;
            if (far(fs[i].lazyT, medL)) off++;
            if (far(fs[i].riseT, medR)) off++;
            if (far(fs[i].platT, medP)) off++;
            if (far(fs[i].fallT, medF)) off++;
            in[i] = m < 3 || off < 2;
            if (in[i]) inliers++;
        }
        if (inliers == 0) { java.util.Arrays.fill(in, true); inliers = m; }
        c.rejected = m - inliers;
        float lt = Float.MAX_VALUE, ll = 0, rt = Float.MAX_VALUE, pt = Float.MAX_VALUE, ft = Float.MAX_VALUE, pkr = Float.MAX_VALUE;
        float sl = 0, sr = 0, sp = 0, sf = 0;
        float[] shapeSum = new float[60];
        for (int i = 0; i < m; i++) {
            if (!in[i]) continue;
            Features f = fs[i];
            lt = Math.min(lt, f.lazyT); ll = Math.max(ll, f.lazyLevel);
            rt = Math.min(rt, f.riseT); pt = Math.min(pt, f.platT); ft = Math.min(ft, f.fallT);
            pkr = Math.min(pkr, Math.min(f.pk, 200f));
            sl += f.lazyT; sr += f.riseT; sp += f.platT; sf += f.fallT;
            float[] sh = shape(bufs.get(i), 60);
            for (int j = 0; j < 60; j++) shapeSum[j] += sh[j] / inliers;
        }
        m = inliers;
        mLazyT = lt; mLazyL = ll; mRiseT = rt; mPlatT = pt; mFallT = ft;
        // cisza przed mowa zalezy od pomieszczenia — gorny limit, zeby nie blokowac kursanta
        mPk = Math.max(1.5f, Math.min(6f, pkr));
        ph1 = r1(sl / m); ph2 = r1(sr / m); ph3 = r1(sf / m);
        c.lazyT = r1(sl / m); c.riseT = r1(sr / m); c.platT = r1(sp / m); c.fallT = r1(sf / m);
        shapeRef = shapeSum;
        calibrated = true;
        calSamples = m;
        c.scoresAfter = new int[bufs.size()];
        for (int i = 0; i < bufs.size(); i++) c.scoresAfter[i] = analyze(bufs.get(i), pres.get(i)).score;
        return c;
    }

    private static float median(Features[] fs, int which) {
        float[] v = new float[fs.length];
        for (int i = 0; i < fs.length; i++) v[i] = which == 0 ? fs[i].lazyT : which == 1 ? fs[i].riseT : which == 2 ? fs[i].platT : fs[i].fallT;
        java.util.Arrays.sort(v);
        return v[v.length / 2];
    }

    private static boolean far(float v, float med) {
        if (med <= 0.05f) return v > 0.3f;
        return Math.abs(v - med) / med > 0.5f;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static float r1(float v) { return Math.round(v * 10) / 10f; }
}
