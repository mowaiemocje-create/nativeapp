package com.pitchrec.nativetest;

import java.util.ArrayList;
import java.util.List;

// Współdzielony, bezpieczny wątkowo magazyn danych "na żywo" — serwis nagrywający dopisuje
// nowe próbki/punkty pitch, a widok (PitchWaveView) czyta je do rysowania.
//
// WYDAJNOŚĆ (v2): obwiednia jest teraz surową tablicą float[] (rosnącą manualnie, jak
// ArrayList, ale bez pakowania/boxing) — poprzednia wersja (List<Float>) tworzyła nowy
// obiekt Float dla KAŻDEJ pojedynczej próbki audio (tysiące na sekundę), co generowało
// ogromny ruch dla Garbage Collectora i było prawdopodobnie głównym źródłem zacięć
// widocznych podczas nagrywania — pauzy GC wpływają na CAŁY wątek UI, nie tylko na
// przetwarzanie audio.
public class LiveAudioData {

    public static final int SAMPLE_RATE = 44100;
    private static final int ENVELOPE_CHUNK = 256;

    private static float[] envelope = new float[4096];
    private static int envelopeSize = 0;
    private static float envelopeChunkMax = 0f;
    private static int envelopeChunkCount = 0;

    public static class PitchPoint {
        public final long sampleIndex;
        public final float freq;

        PitchPoint(long sampleIndex, float freq) {
            this.sampleIndex = sampleIndex;
            this.freq = freq;
        }
    }

    private static final List<PitchPoint> pitchPts = new ArrayList<>();
    private static volatile long totalSamplesWritten = 0;
    private static volatile long lastSampleUpdateWallClockMs = 0L;

    private static final Object lock = new Object();

    // Wspolny mnoznik gain — ustawiany przez suwak w MainActivity, odczytywany przez petle
    // odczytu w BackgroundRecorderService. Wczesniej suwak TYLKO zmienial wyswietlany tekst,
    // nigdy faktycznie nie wplywal na dzwiek — to byl prawdziwy blad.
    public static volatile float gainMultiplier = 1f;
    public static volatile float pitchLineWidthDp = 4f;
    public static volatile int pitchLineColor = 0xFFFF3B30; // czerwony (ARGB)
    public static volatile float gridLineWidthDp = 1f;
    public static volatile int gridLineColor = 0x20FFFFFF; // ARGB, domyslnie subtelny
    public static volatile int dawBackgroundColor = 0xFF1A1A1A; // ARGB, tlo wykresu DAW

    // Bramka szumów — jeśli włączona, próbki o RMS poniżej progu są wyciszane (ustawiane
    // na zero) przed zapisem/analizą. Domyślnie wyłączona.
    public static volatile boolean noiseGateEnabled = false;
    public static volatile float noiseGateThreshold = 0.02f; // RMS, 0.0-1.0

    public static void reset() {
        synchronized (lock) {
            envelope = new float[4096];
            envelopeSize = 0;
            envelopeChunkMax = 0f;
            envelopeChunkCount = 0;
            pitchPts.clear();
            totalSamplesWritten = 0;
        }
    }

    private static void appendEnvelopeValue(float value) {
        if (envelopeSize >= envelope.length) {
            float[] bigger = new float[envelope.length * 2];
            System.arraycopy(envelope, 0, bigger, 0, envelope.length);
            envelope = bigger;
        }
        envelope[envelopeSize] = value;
        envelopeSize++;
    }

    public static void appendSamples(short[] buffer, int length) {
        synchronized (lock) {
            for (int i = 0; i < length; i++) {
                float normalized = Math.abs(buffer[i] / 32768f);
                if (normalized > envelopeChunkMax) envelopeChunkMax = normalized;
                envelopeChunkCount++;
                if (envelopeChunkCount >= ENVELOPE_CHUNK) {
                    appendEnvelopeValue(envelopeChunkMax);
                    envelopeChunkMax = 0f;
                    envelopeChunkCount = 0;
                }
            }
            totalSamplesWritten += length;
            lastSampleUpdateWallClockMs = System.currentTimeMillis();
        }
    }

    public static void appendPitch(long sampleIndex, float freq) {
        synchronized (lock) {
            if (freq > 0) {
                if (!pitchPts.isEmpty()) {
                    PitchPoint last = pitchPts.get(pitchPts.size() - 1);
                    if (last.freq > 0 && sampleIndex - last.sampleIndex <= SAMPLE_RATE / 20) {
                        pitchPts.set(pitchPts.size() - 1, new PitchPoint(last.sampleIndex, freq));
                        return;
                    }
                }
                pitchPts.add(new PitchPoint(sampleIndex, freq));
            } else if (!pitchPts.isEmpty() && pitchPts.get(pitchPts.size() - 1).freq > 0) {
                pitchPts.add(new PitchPoint(sampleIndex, -1));
            }
            if (pitchPts.size() > 18000) {
                for (int i = 0; i < 2000; i++) pitchPts.remove(0);
            }
        }
    }

    // Jedno, POŁĄCZONE zapytanie o wszystko potrzebne do narysowania jednej klatki —
    // WYDAJNOŚĆ: zamiast 3-4 osobnych wywołań synchronized (envelope, pitch, total samples)
    // na klatkę, jedno wejście w blokadę. Mniej rywalizacji o zamek z wątkiem nagrywania,
    // który dopisuje dane bardzo częstotliwie.
    public static class FrameSnapshot {
        public float[] envelope;
        public int envelopeStartIndex; // pozycja pierwszego punktu tablicy w PELNEJ historii
        public List<PitchPoint> pitchPoints;
        public long visibleStartSample;
        public long totalSamples;
    }

    public static FrameSnapshot snapshotForDrawing(int envelopeTailCount) {
        synchronized (lock) {
            int start = Math.max(0, envelopeSize - envelopeTailCount);
            return snapshotForDrawingAtIndex(start, envelopeTailCount);
        }
    }

    // Jak snapshotForDrawing, ale od DOWOLNEJ pozycji (nie tylko najnowszych) — potrzebne do
    // przewijania palcem w trybie statycznym (wczytany plik), gdzie użytkownik może chcieć
    // zobaczyć wcześniejszy fragment, nie tylko koniec.
    public static FrameSnapshot snapshotForDrawingAtSample(long startSample, int envelopeChunkCount) {
        synchronized (lock) {
            int startIdx = Math.max(0, (int) (startSample / ENVELOPE_CHUNK));
            return snapshotForDrawingAtIndex(startIdx, envelopeChunkCount);
        }
    }

    // UWAGA: musi być wołane WEWNĄTRZ synchronized(lock) — nie synchronizuje samo, żeby
    // uniknąć podwójnego wejścia w blokadę z metod publicznych powyżej.
    private static FrameSnapshot snapshotForDrawingAtIndex(int startIdx, int envelopeChunkCount) {
        FrameSnapshot snap = new FrameSnapshot();
        int clampedStart = Math.max(0, Math.min(startIdx, envelopeSize));
        int len = Math.min(envelopeChunkCount, envelopeSize - clampedStart);
        snap.envelope = new float[Math.max(0, len)];
        if (len > 0) System.arraycopy(envelope, clampedStart, snap.envelope, 0, len);
        snap.envelopeStartIndex = clampedStart;

        snap.visibleStartSample = (long) clampedStart * ENVELOPE_CHUNK;
        snap.totalSamples = totalSamplesWritten;

        int lo = 0, hi = pitchPts.size();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (pitchPts.get(mid).sampleIndex < snap.visibleStartSample) lo = mid + 1;
            else hi = mid;
        }
        int pitchStart = Math.max(0, lo - 1);
        snap.pitchPoints = new ArrayList<>(pitchPts.subList(pitchStart, pitchPts.size()));

        return snap;
    }

    public static float[] snapshotEnvelopeTail(int maxCount) {
        synchronized (lock) {
            int start = Math.max(0, envelopeSize - maxCount);
            int len = envelopeSize - start;
            float[] result = new float[len];
            System.arraycopy(envelope, start, result, 0, len);
            return result;
        }
    }

    public static int getEnvelopeSize() {
        synchronized (lock) {
            return envelopeSize;
        }
    }

    public static long getTotalSamplesWritten() {
        return totalSamplesWritten;
    }

    // Zwraca PRZEWIDYWANA (interpolowana) aktualna liczbe probek, zakladajac ciagle
    // nagrywanie od czasu ostatniej faktycznej aktualizacji z wątku audio. Bez tego,
    // podziałka/siatka "skakala" widocznie za każdym razem gdy nadchodzil nowy bufor
    // audio (co dzieje sie rzadziej niz odswiezanie ekranu), bo rysowanie co klatke samo
    // w sobie nie pomaga, jesli źrodlowa wartosc zmienia sie rzadziej.
    public static long getExtrapolatedTotalSamples() {
        if (!isRecordingActive) return totalSamplesWritten; // bez ekstrapolacji, gdy nic sie nie nagrywa
        long elapsedMs = System.currentTimeMillis() - lastSampleUpdateWallClockMs;
        if (elapsedMs <= 0 || elapsedMs > 500) return totalSamplesWritten; // zabezpieczenie przy dlugich przerwach
        long extrapolatedSamples = (long) (elapsedMs * (SAMPLE_RATE / 1000.0));
        return totalSamplesWritten + extrapolatedSamples;
    }

    public static volatile boolean isRecordingActive = false;
}
