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
    // Nowa porcja mowy musi trwac min. 0,2 s i byc nie cichsza niz ok. -26 dB wzgledem
    // glosnosci ostatniej mowy — krotkie ciche dzwieki w pauzie (buczenie, szelest, oddech,
    // klik) nie przerywaja juz pauzy.
    private static final float CONFIRM_S = 0.2f, REL_LEVEL = 0.05f;
    private float speechRef = 0f;          // typowa glosnosc mowy kursanta (powoli zapominana)
    private boolean confirmed = true;
    private double candStart = -1, candEnd = -1;   // pauza czekajaca na potwierdzenie mowy po niej
    private double prevRunEnd = -1, prevRunDur = 0;
    private float runPeak = 0f;

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
        speechRef = 0f; confirmed = true; candStart = -1; candEnd = -1; prevRunEnd = -1; prevRunDur = 0;
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
        // speechRef: glosnosc potwierdzonej mowy, zapominana powoli (~20 s)
        speechRef *= (float) Math.exp(-dt / 20.0);
        float startThr = Math.max(thr, speechRef * REL_LEVEL);
        if (!inSpeech) {
            if (env > startThr && pitchedRecent) {
                double gap = lastRunEnd >= 0 ? t - lastRunEnd : 1e9;
                if (gap < MIN_PAUSE_S * 0.8) { inSpeech = true; lastSpeechT = t; return true; } // ta sama porcja
                // zapamietujemy poprzednia porcje — gdyby ta okazala sie krotkim dzwiekiem, wracamy do niej
                prevRunEnd = lastRunEnd; prevRunDur = lastRunDur;
                candStart = (lastRunEnd >= 0 && lastRunDur >= MIN_RUN_S) ? lastRunEnd : -1;
                candEnd = t;
                confirmed = false; runPeak = env;
                inSpeech = true; runStart = t; lastSpeechT = t;
            }
        } else {
            if (env > Math.max(thr * 0.5f, floor * 2.2f)) lastSpeechT = t;
            runPeak = Math.max(runPeak, env);
            // potwierdzenie mowy: trwa min. 0,2 s ALBO jest wyraznie glosna (jak mowa kursanta)
            if (!confirmed && (lastSpeechT - runStart >= CONFIRM_S || runPeak >= Math.max(thr * 3f, speechRef * 0.25f))) {
                confirmed = true;
                if (candStart >= 0) pendingPause = new double[]{candStart, candEnd};
            }
            if (confirmed && env > speechRef) speechRef += (env - speechRef) * 0.3f;
            if (t - lastSpeechT > BRIDGE_S) {
                inSpeech = false;
                if (confirmed) { lastRunEnd = lastSpeechT; lastRunDur = lastSpeechT - runStart; }
                else { lastRunEnd = prevRunEnd; lastRunDur = prevRunDur; confirmed = true; } // to byl tylko krotki dzwiek — pauza trwa dalej
            }
        }
        return inSpeech;
    }

    // Doprecyzowanie granic pauzy na obwiedni (max |probka| w oknach po 256 probek).
    // env[0] odpowiada probce envStartSample. Zwraca {start, end} [s] albo null.
    // Pauza = odstep miedzy KONCEM dzwieku polaczonego z poprzednia mowa a POCZATKIEM dzwieku
    // polaczonego z nastepna mowa. Pojedyncze ciche dzwieki w srodku pauzy (szum, oddech, klik,
    // buczenie) nie sa z mowa polaczone, wiec jej nie skracaja. Prog "dzwieku" zalezy od szumu
    // tla ORAZ od glosnosci mowy obok (ok. -30 dB) — w cichym pokoju nie lapie drobnych szumow.
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
        // glosnosc mowy przed i po pauzie
        int w1 = (int) Math.round(1.0 / hopS);
        float pkB = 0, pkA = 0;
        for (int f = Math.max(0, fa - w1); f <= fa; f++) pkB = Math.max(pkB, e[f]);
        for (int f = fb; f < n && f <= fb + w1; f++) pkA = Math.max(pkA, e[f]);
        float ref = pkB > 0 && pkA > 0 ? Math.min(pkB, pkA) : Math.max(pkB, pkA);
        float te = Math.max(noise * 2.5f, ref * 0.03f);
        int maxGap = (int) Math.round(0.06 / hopS), look = (int) Math.round(0.2 / hopS);
        int minLoud = (int) Math.round(0.08 / hopS);
        float loud = Math.max(te * 2f, ref * 0.1f);   // ok. -20 dB wzgledem mowy = to jest mowa
        int lo = Math.max(0, fa - (int) Math.round(1.0 / hopS)), hi = Math.min(n - 1, fb + look);
        // Odcinki dzwieku (ponad progiem; przerwy do 60 ms scalone)
        java.util.List<int[]> runs = new java.util.ArrayList<>();   // {start, end, speechLike}
        int rs = -1, re = -1; float rp = 0;
        for (int f = lo; f <= hi + 1; f++) {
            boolean snd = f <= hi && e[f] > te;
            if (snd) {
                if (rs >= 0 && f - re - 1 > maxGap) { runs.add(run(rs, re, rp, loud, minLoud, fa, fb)); rs = -1; }
                if (rs < 0) { rs = f; rp = 0; }
                re = f; rp = Math.max(rp, e[f]);
            }
        }
        if (rs >= 0) runs.add(run(rs, re, rp, loud, minLoud, fa, fb));
        // MOWA = glosny odcinek (min. 80 ms, ok. -20 dB wzgledem mowy) albo polaczony z mowa
        // przed/po pauzie (granice VAD). Ciche odcinki (szum, oddech, buczenie) naleza do pauzy.
        // Pauza = NAJDLUZSZA przerwa miedzy odcinkami mowy.
        int prevEnd = fa - 1, bestA = -1, bestB = -1, bestLen = -1;
        boolean havePrev = false;
        for (int[] r : runs) {
            if (r[2] == 0) continue;
            if (r[0] <= fa) { prevEnd = havePrev ? Math.max(prevEnd, r[1]) : r[1]; havePrev = true; continue; } // koncowka poprzedniej mowy
            int gA = prevEnd, gB = r[0];
            if (gB - gA > bestLen && gB > gA) { bestLen = gB - gA; bestA = gA; bestB = gB; }
            prevEnd = Math.max(prevEnd, r[1]); havePrev = true;
            if (r[1] >= fb) break; // dalej juz nastepna mowa
        }
        if (fb + 1 - prevEnd > bestLen && fb + 1 > prevEnd) { bestA = prevEnd; bestB = fb; }
        if (bestA < 0) { bestA = fa - 1; bestB = fb; }
        int off = bestA, on = bestB;
        double start = (envStartSample + (long) (off + 1) * chunk) / (double) sampleRate;
        double end = (envStartSample + (long) on * chunk) / (double) sampleRate;
        if (end <= start) return null;
        return new double[]{start, end};
    }

    private static int[] run(int s, int e, float pk, float loud, int minLoud, int fa, int fb) {
        boolean speech = (pk >= loud && e - s + 1 >= minLoud) || s <= fa || e >= fb;
        return new int[]{s, e, speech ? 1 : 0};
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
