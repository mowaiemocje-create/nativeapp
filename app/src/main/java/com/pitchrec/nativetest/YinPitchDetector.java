package com.pitchrec.nativetest;

// Wierny port algorytmu YIN z PitchRec (JS, function yin(buf) w index.html) — wykrywanie
// wysokości głosu (fundamentalnej częstotliwości) z bufora próbek PCM.
public class YinPitchDetector {

    private static final int SR = 44100;
    private static final float[] yd = new float[700];
    private static final float[] yc = new float[700];

    // Zwraca wykrytą częstotliwość w Hz, albo -1 jeśli nie wykryto (cisza / brak tonu).
    // buf: próbki znormalizowane do zakresu [-1, 1] (tak jak Web Audio Float32 time-domain data).
    public static float detect(float[] buf) {
        int n = buf.length;
        float rms = 0;
        for (int i = 0; i < n; i++) rms += buf[i] * buf[i];
        rms = (float) Math.sqrt(rms / n);
        if (rms < 0.0005f) return -1;

        int minL = (int) Math.round(SR / 900.0);
        int maxL = (int) Math.round(SR / 70.0);
        int w = Math.min(256, n - maxL - 1);
        if (w < 32) return -1;

        for (int tau = minL; tau <= maxL; tau++) {
            float s = 0;
            for (int j = 0; j < w; j++) {
                float d = buf[j] - buf[j + tau];
                s += d * d;
            }
            yd[tau] = s;
        }

        yc[0] = 1;
        float run = 0;
        for (int tau = 1; tau <= maxL; tau++) {
            run += yd[tau];
            yc[tau] = (tau * yd[tau]) / (run == 0 ? 1 : run);
        }

        for (int tau = minL; tau <= maxL; tau++) {
            if (yc[tau] < 0.20f) {
                if (tau > minL && tau < maxL) {
                    float a = yc[tau - 1], b = yc[tau], c = yc[tau + 1];
                    float denom = 2 * (2 * b - a - c);
                    if (denom > 0) {
                        return SR / (tau - (c - a) / denom);
                    }
                }
                return (float) SR / tau;
            }
        }
        return -1;
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
