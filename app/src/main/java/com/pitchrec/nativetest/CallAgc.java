package com.pitchrec.nativetest;

// AUTOMATYCZNE WYROWNANIE GLOSNOSCI nagrania rozmowy.
// Zrodlo "voice recognition" (jak w Cube ACR) jest surowe i ciche, a na wielu telefonach
// (np. Samsung) wlasny glos nagrywa sie ciszej niz rozmowca. Rozmowcy mowia na zmiane,
// wiec wzmocnienie liczone co ~23 ms podciaga cichy fragment (mnie) mocniej niz glosny
// (rozmowce). Cisza miedzy slowami nie jest pompowana do maksimum (mniej szumu),
// a glosne miejsca lagodnie ogranicza limiter (bez trzaskow).
final class CallAgc {

    private static final int FRAME = 1024;
    private final float target;     // docelowy poziom RMS mowy
    private final float maxGain;
    private static final float SPEECH_RMS = 90f;   // ponizej = cisza/szum tla
    private static final float LIMIT = 26000f;

    private float g = 4f;            // biezace wzmocnienie (start: rozsadnie glosno)
    private final short[] frame = new short[FRAME];
    private int fill = 0;

    CallAgc(float target, float maxGain) {
        this.target = target;
        this.maxGain = maxGain;
    }

    // Przetwarza n probek z `in`; wynik (moze byc krotszy/dluzszy — ramkami) oddaje do sink
    interface Sink { void put(short[] s, int n); }

    void process(short[] in, int n, Sink sink) {
        int i = 0;
        while (i < n) {
            int k = Math.min(FRAME - fill, n - i);
            System.arraycopy(in, i, frame, fill, k);
            fill += k;
            i += k;
            if (fill == FRAME) { doFrame(FRAME); sink.put(frame, FRAME); fill = 0; }
        }
    }

    void flush(Sink sink) {
        if (fill > 0) { doFrame(fill); sink.put(frame, fill); fill = 0; }
    }

    private void doFrame(int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += (double) frame[i] * frame[i];
        float rms = (float) Math.sqrt(sum / Math.max(1, n));
        float want;
        if (rms < SPEECH_RMS) {
            // cisza: powoli schodzimy do umiarkowanego wzmocnienia, zeby nie pompowac szumu
            want = Math.min(g, Math.max(1f, Math.min(maxGain, 6f)));
        } else {
            want = Math.max(1f, Math.min(maxGain, target / rms));
        }
        float g0 = g;
        // szybko w dol (glosny rozmowca nie przester), wolniej w gore (~0.4 s)
        if (want < g) g = g + (want - g) * 0.6f;
        else g = g + (want - g) * 0.06f;
        for (int i = 0; i < n; i++) {
            float gi = g0 + (g - g0) * i / n;          // plynne przejscie w ramce
            float x = frame[i] * gi;
            float ax = Math.abs(x);
            if (ax > LIMIT) {                          // miekki limiter
                float over = ax - LIMIT;
                ax = LIMIT + over / (1f + over / (32767f - LIMIT));
                x = x < 0 ? -ax : ax;
            }
            if (x > 32767f) x = 32767f; else if (x < -32768f) x = -32768f;
            frame[i] = (short) x;
        }
    }
}
