package com.pitchrec.nativetest;

// Wierny port algorytmu YIN z PitchRec (JS, function yin(buf) w index.html) — wykrywanie
// wysokości głosu (fundamentalnej częstotliwości) z bufora próbek PCM.
public class YinPitchDetector {

    private static final int SR = 44100;
    // Bufory robocze — osobne dla kazdego watku (wczytywanie pliku liczy pitch rownolegle
    // na wszystkich rdzeniach telefonu).
    public static final class Work {
        final float[] yd = new float[700];
        final float[] yc = new float[700];
        public float relaxed = -1;
    }
    private static final Work MAIN = new Work();

    // Zwraca wykrytą częstotliwość w Hz, albo -1 jeśli nie wykryto (cisza / brak tonu).
    // buf: próbki znormalizowane do zakresu [-1, 1] (tak jak Web Audio Float32 time-domain data).
    // Wynik "luzniejszy" z ostatniego wywolania detect(): gdy scisly prog YIN (0,20) nie
    // znalazl tonu, bierzemy globalne minimum, jesli jest wyrazne (< 0,35) — standardowy
    // wariant YIN. Uzywane tylko do rysowania ciaglej linii pitch (NIE do wykrywania mowy).
    public static float lastRelaxed = -1;
    static final int WIN = 256;

    public static synchronized float detect(float[] buf) {
        float f = detect(buf, MAIN);
        lastRelaxed = MAIN.relaxed;
        return f;
    }

    // Wersja dla dowolnego watku. Liczy funkcje roznicowa tylko do pierwszego minimum
    // ponizej progu (wynik identyczny jak pelne liczenie, a dla mowy ~2x szybciej).
    public static float detect(float[] buf, Work w) {
        w.relaxed = -1;
        final float[] yd = w.yd, yc = w.yc;
        int n = buf.length;
        float rms = 0;
        for (int i = 0; i < n; i++) rms += buf[i] * buf[i];
        rms = (float) Math.sqrt(rms / n);
        if (rms < 0.0005f) return -1;

        int minL = (int) Math.round(SR / 900.0);
        int maxL = (int) Math.round(SR / 70.0);
        // Okno calkowania 256 probek (jak w PWA) — dluzsze okna sprawdzone na nagraniach
        // pogarszaly liczenie sylab i wykrywanie mowy.
        int wn = Math.min(WIN, n - maxL - 1);
        if (wn < 32) return -1;

        yc[0] = 1;
        float run = 0;
        for (int tau = 1; tau < minL; tau++) { yd[tau] = 0; yc[tau] = tau == 0 ? 1 : 0; }
        for (int tau = minL; tau <= maxL; tau++) {
            float sd = 0;
            for (int j = 0; j < wn; j++) {
                float d = buf[j] - buf[j + tau];
                sd += d * d;
            }
            yd[tau] = sd;
            run += sd;
            yc[tau] = (tau * sd) / (run == 0 ? 1 : run);
            // pierwszy tau ponizej progu (sprawdzany, gdy znamy juz sasiada z prawej)
            int t = tau - 1;
            if (t >= minL && yc[t] < 0.20f) return interp(yc, t, minL, maxL, w);
        }
        if (yc[maxL] < 0.20f) return interp(yc, maxL, minL, maxL, w);
        int best = -1;
        float bestV = 0.35f;
        for (int tau = minL + 1; tau < maxL; tau++) {
            if (yc[tau] < bestV && yc[tau] <= yc[tau - 1] && yc[tau] <= yc[tau + 1]) { bestV = yc[tau]; best = tau; }
        }
        if (best > 0) {
            float a = yc[best - 1], b = yc[best], c = yc[best + 1];
            float denom = 2 * (2 * b - a - c);
            w.relaxed = denom > 0 ? SR / (best - (c - a) / denom) : (float) SR / best;
        }
        return -1;
    }

    private static float interp(float[] yc, int tau, int minL, int maxL, Work w) {
        if (tau > minL && tau < maxL) {
            float a = yc[tau - 1], b = yc[tau], c = yc[tau + 1];
            float denom = 2 * (2 * b - a - c);
            if (denom > 0) { w.relaxed = SR / (tau - (c - a) / denom); return w.relaxed; }
        }
        w.relaxed = (float) SR / tau;
        return w.relaxed;
    }

    // Odpowiednik freqNote(f) z JS — nazwa nuty, oktawa, odchylenie w centach, częstotliwość
    // zaokrąglona do najbliższego Hz. Zwraca null jeśli częstotliwość jest nieprawidłowa.
    public static class NoteInfo {
        public final String note;
        public final int octave;
        public final int cents;
        public final int hz;

        NoteInfo(String note, int octave, int cents, int hz) {
            this.note = note;
            this.octave = octave;
            this.cents = cents;
            this.hz = hz;
        }
    }

    private static final String[] NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static NoteInfo freqToNote(float f) {
        if (f <= 0 || f < 50) return null;
        double st = 12 * (Math.log(f / 440.0) / Math.log(2)) + 57;
        long rounded = Math.round(st);
        int noteIdx = (int) (((rounded % 12) + 12) % 12);
        int oct = (int) Math.floor(rounded / 12.0);
        int cents = (int) Math.round((st - rounded) * 100);
        int hz = (int) Math.round(f);
        return new NoteInfo(NOTES[noteIdx], oct, cents, hz);
    }
}
