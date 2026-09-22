package com.pitchrec.nativetest;

import java.util.ArrayList;
import java.util.List;

// Współdzielony, bezpieczny wątkowo magazyn danych "na żywo" — serwis nagrywający dopisuje
// nowe próbki/punkty pitch, a widok (PitchWaveView) czyta je do rysowania. Odpowiednik
// wavBuf/pitchPts z PitchRec (JS), tylko po stronie natywnej.
public class LiveAudioData {

    public static final int SAMPLE_RATE = 44100;

    // Downsamplowana obwiednia amplitudy (do rysowania fali) — nie surowe próbki (zbyt duże),
    // tylko wartość maksymalną z każdego "kawałka" o rozmiarze ENVELOPE_CHUNK próbek.
    private static final int ENVELOPE_CHUNK = 256;
    private static final List<Float> envelope = new ArrayList<>();
    private static float envelopeChunkMax = 0f;
    private static int envelopeChunkCount = 0;

    // Punkty pitch — odpowiednik pitchPts z JS: {pozycja w próbkach, częstotliwość albo null
    // gdy przerwa (cisza) między segmentami głosu}.
    public static class PitchPoint {
        public final long sampleIndex;
        public final float freq; // -1 oznacza "null" (przerwa)

        PitchPoint(long sampleIndex, float freq) {
            this.sampleIndex = sampleIndex;
            this.freq = freq;
        }
    }

    private static final List<PitchPoint> pitchPts = new ArrayList<>();
    private static volatile long totalSamplesWritten = 0;

    private static final Object lock = new Object();

    public static synchronized void reset() {
        synchronized (lock) {
            envelope.clear();
            envelopeChunkMax = 0f;
            envelopeChunkCount = 0;
            pitchPts.clear();
            totalSamplesWritten = 0;
        }
    }

    // Wywoływane z pętli odczytu serwisu dla KAŻDEJ nowo przeczytanej porcji próbek PCM
    // (16-bit signed, znormalizowane wewnątrz do [-1,1] przy liczeniu obwiedni).
    public static void appendSamples(short[] buffer, int length) {
        synchronized (lock) {
            for (int i = 0; i < length; i++) {
                float normalized = Math.abs(buffer[i] / 32768f);
                if (normalized > envelopeChunkMax) envelopeChunkMax = normalized;
                envelopeChunkCount++;
                if (envelopeChunkCount >= ENVELOPE_CHUNK) {
                    envelope.add(envelopeChunkMax);
                    envelopeChunkMax = 0f;
                    envelopeChunkCount = 0;
                }
            }
            totalSamplesWritten += length;
        }
    }

    // Wywoływane z serwisu po wykryciu wysokości głosu dla aktualnego okna próbek — freq=-1
    // oznacza brak wykrycia (cisza/brak tonu), co odpowiada "null" w JS (przerywa linię).
    public static void appendPitch(long sampleIndex, float freq) {
        synchronized (lock) {
            if (freq > 0) {
                if (!pitchPts.isEmpty()) {
                    PitchPoint last = pitchPts.get(pitchPts.size() - 1);
                    if (last.freq > 0 && sampleIndex - last.sampleIndex <= SAMPLE_RATE / 20) {
                        // Zbyt blisko poprzedniego punktu — zaktualizuj go zamiast dodawać nowy
                        // (odpowiednik "else pitchPts[last].fq=fSm" w JS).
                        pitchPts.set(pitchPts.size() - 1, new PitchPoint(last.sampleIndex, freq));
                        return;
                    }
                }
                pitchPts.add(new PitchPoint(sampleIndex, freq));
            } else if (!pitchPts.isEmpty() && pitchPts.get(pitchPts.size() - 1).freq > 0) {
                pitchPts.add(new PitchPoint(sampleIndex, -1));
            }
            // Ogranicz rozmiar listy (odpowiednik "if length>18000 splice" w JS)
            if (pitchPts.size() > 18000) {
                for (int i = 0; i < 2000; i++) pitchPts.remove(0);
            }
        }
    }

    // Zwraca KOPIĘ aktualnej obwiedni i punktów pitch — bezpieczne do odczytu z wątku UI
    // podczas rysowania, bez blokowania wątku nagrywania na dłużej niż potrzeba do skopiowania.
    public static float[] snapshotEnvelope() {
        synchronized (lock) {
            float[] result = new float[envelope.size()];
            for (int i = 0; i < result.length; i++) result[i] = envelope.get(i);
            return result;
        }
    }

    public static List<PitchPoint> snapshotPitchPoints() {
        synchronized (lock) {
            return new ArrayList<>(pitchPts);
        }
    }

    public static long getTotalSamplesWritten() {
        return totalSamplesWritten;
    }
}
