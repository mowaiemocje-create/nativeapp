package com.pitchrec.nativetest;

// Wykrywanie glosu czlowieka (VAD) + dokladny pomiar pauz — ten sam algorytm co w PitchRec
// PWA v288 (przetestowany na nagraniach z ZNANYMI pauzami: 12/12 wykrytych, blad ~30 ms
// w ciszy/szumie biurowym, odporny na stuki, klawiature, buczenie i szum uliczny).
//
// Zasada: mowa zaczyna sie dopiero, gdy jest energia ponad szumem tla ORAZ stabilny ton
// krtaniowy (70-600 Hz, ±12% miedzy kolejnymi oknami). Szum, stuki i buczenie nie maja
// stabilnego tonu, wiec nie licza sie jako mowa. Granice pauzy sa potem liczone dokladnie
// z obwiedni nagranego dzwieku (rozdzielczosc ~6 ms), nie z opoznionego wygladzania.
public class VadDetector {

    private static final float CAL_S = 1.5f, BRIDGE_S = 0.12f, MIN_RUN_S = 0.08f, MIN_PAUSE_S = 0.2f;

    private float env = 0f;
    private double lastT = -1, t0 = -1;
    private final float[] cal = new float[400];
    private int calN = 0;
    private boolean calDone = false;
    private float minEnv = Float.MAX_VALUE;
    private float floor = 0.004f, thr = 0.02f, ratio = 3f;
    private boolean inSpeech = false;
    private double runStart = 0, lastSpeechT = 0, lastRunEnd = -1, lastRunDur = 0;
    private final float[] pitchHist = new float[3];

    // Wynik: przyblizone granice pauzy do doprecyzowania (albo null)
    public double[] pendingPause = null;

    public synchronized void reset() {
        env = 0f; lastT = -1; t0 = -1; calN = 0; calDone = false; minEnv = Float.MAX_VALUE;
        floor = 0.004f; thr = 0.02f; ratio = 3f;
        inSpeech = false; runStart = 0; lastSpeechT = 0; lastRunEnd = -1; lastRunDur = 0;
        pitchHist[0] = pitchHist[1] = pitchHist[2] = 0;
        pendingPause = null;
    }

    public boolean isCalibrated() { return calDone; }

    // t = czas nagrania [s], rms = RMS okna, f0 = ton z YIN [Hz] albo <=0 (brak tonu).
    // Zwraca true, gdy trwa mowa.
    public synchronized boolean frame(double t, float rms, float f0) {
        double dt = lastT < 0 ? 0.046 : Math.max(0.001, Math.min(0.2, t - lastT));
        lastT = t;
        if (t0 < 0) t0 = t;
        float tau = rms > env ? 0.015f : 0.06f;
        env += (rms - env) * (float) (1 - Math.exp(-dt / tau));
        pitchHist[0] = pitchHist[1];
        pitchHist[1] = pitchHist[2];
        pitchHist[2] = (f0 > 70 && f0 < 600) ? f0 : 0;

        if (!calDone) {
            // Kalibracja szumu tla w pierwszych 1,5 s — ale mowa MOZE juz trwac (ktos zaczyna
            // mowic od razu po REC): wykrywamy ja od razu z progu tymczasowego (najcichszy
            // moment × 4), a do kalibracji bierzemy tylko okna BEZ mowy.
            if (t - t0 > 0.1) minEnv = Math.min(minEnv, env);
            if (minEnv < Float.MAX_VALUE) { floor = Math.max(0.0015f, minEnv); ratio = 4f; thr = floor * ratio; }
            if (t - t0 > 0.3 && !inSpeech && calN < cal.length) cal[calN++] = env;
            if (t - t0 >= CAL_S) {
                if (calN > 5) {
                    float[] s = java.util.Arrays.copyOf(cal, calN);
                    java.util.Arrays.sort(s);
                    float fl = Math.max(0.0015f, s[(int) (s.length * 0.3)]);
                    float hi = s[(int) (s.length * 0.9)];
                    floor = fl;
                    ratio = Math.max(2.5f, Math.min(6f, (hi / fl) * 1.6f));
                    thr = floor * ratio;
                }
                calDone = true;
            }
        } else if (!inSpeech && env < thr) {
            // Szum tla sledzony na biezaco (tylko w ciszy) — prog sam sie dopasowuje do otoczenia
            float k = (float) (1 - Math.exp(-dt / (env < floor ? 0.5 : 4.0)));
            floor += (env - floor) * k;
            floor = Math.max(0.0015f, floor);
            thr = floor * ratio;
        }
        boolean pitchedRecent = false;
        for (int i = 1; i < 3; i++) {
            if (pitchHist[i] > 0 && pitchHist[i - 1] > 0 && Math.abs(pitchHist[i] - pitchHist[i - 1]) / pitchHist[i] < 0.12f) pitchedRecent = true;
        }
        if (!inSpeech) {
            if (env > thr && pitchedRecent) {
                double gap = lastRunEnd >= 0 ? t - lastRunEnd : 1e9;
                if (gap < MIN_PAUSE_S * 0.8) { inSpeech = true; lastSpeechT = t; return true; } // ta sama porcja
                if (lastRunEnd >= 0 && lastRunDur >= MIN_RUN_S) pendingPause = new double[]{lastRunEnd, t};
                inSpeech = true; runStart = t; lastSpeechT = t;
            }
        } else {
            if (env > Math.max(thr * 0.5f, floor * 2.2f)) lastSpeechT = t;
            if (t - lastSpeechT > BRIDGE_S) {
                inSpeech = false; lastRunEnd = lastSpeechT; lastRunDur = lastSpeechT - runStart;
            }
        }
        return inSpeech;
    }

    // Doprecyzowanie granic pauzy na obwiedni (max |probka| w oknach po 256 probek).
    // env[0] odpowiada probce envStartSample. Zwraca {start, end} [s] albo null.
    public static double[] refine(float[] e, long envStartSample, int sampleRate, double a, double b) {
        int chunk = 256;
        double hopS = chunk / (double) sampleRate;
        int n = e.length;
        if (n < 8) return null;
        int fa = (int) Math.max(0, Math.floor((a * sampleRate - envStartSample) / chunk));
        int fb = (int) Math.min(n - 1, Math.floor((b * sampleRate - envStartSample) / chunk));
        if (fb - fa < 4) return null;
        float[] seg = java.util.Arrays.copyOfRange(e, fa, fb + 1);
        java.util.Arrays.sort(seg);
        float noise = Math.max(1e-5f, seg[(int) (seg.length * 0.2)]);
        float T = noise * 3f;
        // srodek pauzy = najcichsze miejsce (wygladzone)
        int core = fa;
        float best = Float.MAX_VALUE;
        for (int f = fa; f <= fb; f++) {
            float sm = 0; int c = 0;
            for (int j = -4; j <= 4; j++) { if (f + j >= 0 && f + j < n) { sm += e[f + j]; c++; } }
            sm /= c;
            if (sm < best) { best = sm; core = f; }
        }
        // Wyrazna mowa = w oknie ~150 ms min. ~60 ms dzwieku ponad szumem (dopuszcza zwarcia
        // w wyrazie, odrzuca pojedynczy stuk/klik).
        int winN = (int) Math.round(0.15 / hopS), needN = (int) Math.round(0.06 / hopS);
        Integer off = null;
        for (int f = core; f >= 0; f--) {
            if (e[f] > T && countAbove(e, f, -1, winN, T * 0.6f) >= needN) { off = f; break; }
        }
        Integer on = null;
        for (int f = core; f < n; f++) {
            if (e[f] > T && countAbove(e, f, 1, winN, T * 0.6f) >= needN) { on = f; break; }
        }
        float te = Math.max(noise * 2f, T * 0.5f);
        if (off != null) { int o = off; while (o + 1 < n && o + 1 < core && e[o + 1] > te) o++; off = o; }
        if (on != null) { int o = on; while (o - 1 > core && e[o - 1] > te) o--; on = o; }
        double start = off != null ? (envStartSample + (long) (off + 1) * chunk) / (double) sampleRate : a;
        double end = on != null ? (envStartSample + (long) on * chunk) / (double) sampleRate : b;
        if (end <= start) return null;
        return new double[]{start, end};
    }

    private static int countAbove(float[] e, int f, int dir, int win, float thr) {
        int c = 0;
        for (int j = 0; j < win; j++) {
            int g = f + dir * j;
            if (g < 0 || g >= e.length) break;
            if (e[g] > thr) c++;
        }
        return c;
    }
}
