package com.pitchrec.nativetest;

// GLOSNOSC NAGRANIA ROZMOWY — dwa stopnie, jak w studiu radiowym:
//  1) AGC (automatyczne wzmocnienie): co ~23 ms liczy poziom mowy i podciaga go do celu.
//     Rozmowcy mowia na zmiane, wiec cichy fragment (np. moj glos) dostaje duzo wiekszy
//     zysk niz glosny (rozmowca). Cisza miedzy slowami nie jest pompowana do maksimum.
//  2) LIMITER Z WYPRZEDZENIEM (look-ahead ~6 ms): "widzi" szczyt zanim on nadejdzie i
//     sciszy dokladnie tyle, zeby nie przekroczyc sufitu (-0,8 dBFS). Dzieki temu calosc
//     moze byc bardzo glosna, a nic nie przesteruje (zero obcinania fali).
final class CallAgc {

    interface Sink { void put(short[] s, int n); }

    private static final int FRAME = 1024;               // ~23 ms przy 44,1 kHz
    private static final int LOOK = 256;                 // ~6 ms wyprzedzenia limitera
    private static final float CEIL = 30000f;            // sufit (-0,8 dBFS) — zapas na kodowanie MP3
    private static final float SPEECH_RMS = 90f;         // ponizej = cisza / szum tla
    private static final float REL = 1f - (float) Math.exp(-1.0 / (0.08 * 44100)); // powrot limitera ~80 ms

    private final float target;   // docelowy poziom RMS mowy po AGC
    private final float maxGain;  // najwieksze wzmocnienie AGC
    private float g = 4f;         // biezace wzmocnienie AGC

    private final short[] frame = new short[FRAME];
    private int fill = 0;

    // limiter: opozniona probka + przesuwne minimum wymaganego wzmocnienia (kolejka monotoniczna)
    private final float[] delay = new float[LOOK + 1];
    private final float[] reqG = new float[LOOK + 1];
    private final long[] dqIdx = new long[LOOK + 2];
    private final float[] dqVal = new float[LOOK + 2];
    private int dqHead = 0, dqTail = 0;   // indeksy w buforze kolowym kolejki
    private long t = 0;                   // licznik probek wejsciowych
    private float env = 1f;               // biezace wzmocnienie limitera
    private final float[] hold = new float[LOOK]; // ostatnie minima (do wygladzenia ataku)
    private double holdSum = LOOK;               // suma bufora hold (start: same 1.0)
    private final short[] out = new short[FRAME + LOOK + 8];
    private int outN = 0;

    CallAgc(float target, float maxGain) {
        this.target = target;
        this.maxGain = maxGain;
        java.util.Arrays.fill(hold, 1f);
    }

    void process(short[] in, int n, Sink sink) {
        int i = 0;
        while (i < n) {
            int k = Math.min(FRAME - fill, n - i);
            System.arraycopy(in, i, frame, fill, k);
            fill += k;
            i += k;
            if (fill == FRAME) { doFrame(FRAME, sink); fill = 0; }
        }
    }

    void flush(Sink sink) {
        if (fill > 0) { doFrame(fill, sink); fill = 0; }
        // wypchnij probki zatrzymane w limiterze
        for (int i = 0; i < LOOK; i++) pushLimiter(0f);
        emit(sink);
    }

    private void doFrame(int n, Sink sink) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += (double) frame[i] * frame[i];
        float rms = (float) Math.sqrt(sum / Math.max(1, n));
        float g0 = g;
        if (rms < SPEECH_RMS) {
            // przerwa miedzy slowami: trzymamy wzmocnienie (zeby nastepna sylaba nie byla cicha),
            // tylko bardzo powoli schodzimy do x12, zeby dlugiej ciszy nie zamienic w szum
            float quiet = Math.min(maxGain, 12f);
            if (g > quiet) g += (quiet - g) * 0.02f;
        } else {
            float want = Math.max(1f, Math.min(maxGain, target / rms));
            // w dol szybko (glosny rozmowca), w gore lagodnie (~0,3 s); limiter i tak pilnuje szczytow
            if (want < g) g = g + (want - g) * 0.5f;
            else g = g + (want - g) * 0.08f;
        }
        for (int i = 0; i < n; i++) {
            float gi = g0 + (g - g0) * i / n;
            pushLimiter(frame[i] * gi);
        }
        emit(sink);
    }

    // Jedna probka do limitera; na wyjscie trafia probka sprzed LOOK probek, juz sciszona
    private void pushLimiter(float x) {
        int slot = (int) (t % (LOOK + 1));
        float ax = Math.abs(x);
        float rg = ax > CEIL ? CEIL / ax : 1f;
        delay[slot] = x;
        reqG[slot] = rg;
        // przesuwne minimum wymaganego wzmocnienia w oknie [t-LOOK, t]
        int cap = LOOK + 2;
        while (dqTail != dqHead && dqVal[(dqTail - 1 + cap) % cap] >= rg) dqTail = (dqTail - 1 + cap) % cap;
        dqIdx[dqTail] = t; dqVal[dqTail] = rg; dqTail = (dqTail + 1) % cap;
        while (dqIdx[dqHead] < t - LOOK) dqHead = (dqHead + 1) % cap;
        float need = dqVal[dqHead];
        // atak WYGLADZONY: srednia z ostatnich LOOK minimow — sciszanie narasta plynnie przez ~6 ms
        // i na pewno siega celu, zanim szczyt wyjdzie z opoznienia (bez trzaskow i bez przesteru)
        int hs = (int) (t % LOOK);
        holdSum += need - hold[hs];
        hold[hs] = need;
        float avg = (float) (holdSum / LOOK);
        if (avg < env) env = avg; else env += (avg - env) * REL;   // powrot plynny ~80 ms
        if (t >= LOOK) {
            float y = delay[(int) ((t - LOOK) % (LOOK + 1))] * env;
            if (y > 32767f) y = 32767f; else if (y < -32768f) y = -32768f;   // zabezpieczenie
            out[outN++] = (short) Math.round(y);
        }
        t++;
    }

    private void emit(Sink sink) {
        if (outN > 0) { sink.put(out, outN); outN = 0; }
    }
}
