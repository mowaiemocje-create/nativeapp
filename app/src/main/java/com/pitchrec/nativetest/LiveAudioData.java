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
    public static volatile float pitchLineWidthDp = 3f;
    public static volatile int pitchLineColor = 0xFFFFE600; // zolty jak w PitchRec (ARGB)
    public static volatile float gridLineWidthDp = 1f; // wspolna grubosc linii poziomych i pionowych (0.5-10)
    // AUTO 0 dB — po Stop nagranie jest podglasniane do 0 dB (szczyt -0,1 dBFS)
    public static volatile boolean autoNormalize = true;
    // Dodatkowe wzmocnienie po zapisie (×1 = tylko 0 dB, ×2…×10 z miekkim limiterem)
    public static volatile int outputBoost = 1;
    public static volatile int gridLineColor = 0x4DFFFFFF; // ARGB, domyslnie subtelny
    public static volatile int dawBackgroundColor = 0xFF050510; // ARGB, tlo wykresu DAW
    public static volatile int waveColor = 0xFFFFB23C; // ARGB, kolor fali (obwiedni amplitudy)

    // Bramka szumów — jeśli włączona, próbki o RMS poniżej progu są wyciszane (ustawiane
    // na zero) przed zapisem/analizą. Domyślnie wyłączona.
    public static volatile boolean noiseGateEnabled = false;
    public static volatile float noiseGateThreshold = 0.02f; // RMS, 0.0-1.0

    // ── Wykrywanie glosu i pauz (VAD, jak w PitchRec PWA v288) ──
    public static final VadDetector vad = new VadDetector();
    public static final PitchTracker tracker = new PitchTracker();
    public static final Norms norms = new Norms();
    // Podpowiedzi na wykresie — wlaczane/wylaczane w Ustawieniach
    public static volatile boolean showPauses = true, showNorms = true, showArrows = true, showTempo = true;
    private static final PitchTracker.Sink PITCH_SINK = LiveAudioData::appendPitch;

    // JEDNO miejsce analizy okna (2048 probek) — wspolne dla nagrywania na zywo i wczytanego
    // pliku: VAD + pauzy, ciagla linia pitch, ocena emisji wg norm.
    public static void processFrame(long windowStartSample, float rms, float strictF0, float relaxedF0) {
        long end = windowStartSample + 2048;
        boolean speech = analyzeVoice(end, rms, strictF0);
        tracker.frame(windowStartSample, rms, strictF0, relaxedF0, PITCH_SINK);
        float vf = strictF0 > 0 ? strictF0 : relaxedF0;  // niski glos: YIN czesto daje tylko wynik "luzniejszy"
        if (vf > 60 && vf < 600 && rms > 0.004f) {
            synchronized (lock) {
                if (voicedN >= voicedT.length) {
                    if (voicedN > 60000) { System.arraycopy(voicedT, voicedN - 30000, voicedT, 0, 30000); voicedN = 30000; }
                    else voicedT = java.util.Arrays.copyOf(voicedT, voicedT.length * 2);
                }
                voicedT[voicedN++] = (windowStartSample + 1024) / (double) SAMPLE_RATE;
            }
        }
        double t = end / (double) SAMPLE_RATE;
        norms.frame(t, rms, speech);
        handleSyllables(t);
    }

    public static void finishAnalysis() {
        tracker.flush(PITCH_SINK);
        norms.finish(getTotalSamplesWritten() / (double) SAMPLE_RATE);
        handleSyllables(getTotalSamplesWritten() / (double) SAMPLE_RATE);
        liveSyllables = null;
    }

    // ── SYLABY I TEMPO ──
    private static final List<SyllableDetector.Syl> syllables = new ArrayList<>();
    public static volatile List<SyllableDetector.Syl> liveSyllables = null; // trwajaca porcja
    public static volatile float liveRate = 0f;         // tempo biezacej porcji [syl/min]
    private static double[] voicedT = new double[4096];  // srodki okien z wyraznym tonem krtaniowym (surowy YIN)
    private static int voicedN = 0;
    private static int sylTotal = 0;
    private static double sylTime = 0;
    private static int frameNo = 0;

    public static float averageRate() { synchronized (lock) { return sylTime > 0.5 ? (float) (sylTotal / sylTime * 60) : 0f; } }

    public static List<SyllableDetector.Syl> syllablesSnapshot() {
        synchronized (lock) {
            List<SyllableDetector.Syl> l = new ArrayList<>(syllables);
            List<SyllableDetector.Syl> cur = liveSyllables;
            if (cur != null) l.addAll(cur);
            return l;
        }
    }

    public static volatile float arrowThresholdSt = 1.5f; // prog strzalki [poltony]

    private static void handleSyllables(double now) {
        Norms.Segment done = norms.pollFinished();
        // sylaby sa potrzebne do tempa ORAZ do strzalek (po jednej na sylabe)
        if (!showTempo && !showArrows) { liveSyllables = null; return; }
        if (done != null) {
            List<SyllableDetector.Syl> s;
            if (done.fourPhase && done.unitEnd > 0) {
                // sylaba 4-fazowa liczy sie jako JEDNA sylaba; dalej zwykle sylaby
                // (dwie 4-fazowe pod rzad = dwie sylaby 4-fazowe)
                s = new ArrayList<>();
                double[] fe = done.fourEnds != null && done.fourEnds.length > 0 ? done.fourEnds : new double[]{done.unitEnd};
                double a = done.start;
                for (int k = 0; k < fe.length; k++) {
                    SyllableDetector.Syl u = new SyllableDetector.Syl(a, k == 0 ? done.unitPeak : (a + fe[k]) / 2, fe[k]);
                    u.four = true;
                    s.add(u);
                    a = fe[k];
                }
                s.addAll(afterFour(done.start, a, done.end));
            } else s = countSyllables(done.start, done.end);
            double dur = Math.max(0.2, done.end - done.start);
            done.syllables = s.size();
            done.rate = (float) (s.size() / dur * 60);
            synchronized (lock) {
                syllables.addAll(s);
                if (syllables.size() > 5000) syllables.subList(0, 1000).clear();
                sylTotal += s.size();
                sylTime += dur;
            }
            liveSyllables = null;
            liveRate = done.rate;
            return;
        }
        double st = norms.currentSpeechStart();
        if (st >= 0 && (++frameNo % 4 == 0) && now - st > 0.4) {
            // Pierwsza sylaba porcji moze byc 4-fazowa (dluga, z wahaniami glosnosci) — liczymy ja
            // dopiero, gdy sie skonczy, i wtedy jako JEDNA sylabe; inaczej pokazywalo 3-4 sylaby.
            double ue = norms.liveUnitEnd;
            if (ue < 0) return;
            List<SyllableDetector.Syl> s;
            if (norms.liveFour) {
                s = new ArrayList<>();
                SyllableDetector.Syl u = new SyllableDetector.Syl(st, (st + ue) / 2, ue);
                u.four = true;
                s.add(u);
                s.addAll(afterFour(st, ue, now));
            } else s = countSyllables(st, now);
            liveSyllables = s;
            liveRate = (float) (s.size() / (now - st) * 60);
        }
    }

    // Sylaby PO sylabie 4-fazowej: prog glosnosci liczony z CALEJ porcji (razem z 4-fazowa),
    // a nie z samej koncowki — inaczej cichy "ogon" wyciszenia 4-fazowej liczyl sie jako
    // osobna sylaba (1 sylaba pokazywala sie jako 2).
    static List<SyllableDetector.Syl> afterFour(double portionStart, double unitEnd, double end) {
        List<SyllableDetector.Syl> out = new ArrayList<>();
        if (end - unitEnd <= 0.15) return out;
        double prev = unitEnd;
        for (SyllableDetector.Syl sy : countSyllables(portionStart, end)) {
            if (sy.nucleus <= unitEnd + 0.05) continue;
            out.add(new SyllableDetector.Syl(prev, sy.nucleus, sy.end));
            prev = sy.end;
        }
        return out;
    }

    public static List<SyllableDetector.Syl> countSyllables(double start, double end) {
        float[] env;
        long envStart;
        double[] voiced;
        synchronized (lock) {
            int e0 = Math.max(0, (int) ((start - 0.1) * SAMPLE_RATE / ENVELOPE_CHUNK));
            int e1 = Math.min(envelopeSize, (int) ((end + 0.1) * SAMPLE_RATE / ENVELOPE_CHUNK) + 1);
            if (e1 - e0 < 10) return new ArrayList<>();
            env = new float[e1 - e0];
            System.arraycopy(envelope, e0, env, 0, e1 - e0);
            envStart = (long) e0 * ENVELOPE_CHUNK;
            double va = start - 0.2, vb = end + 0.2;
            int i0 = voicedN;
            while (i0 > 0 && voicedT[i0 - 1] >= va) i0--;
            int i1 = i0;
            while (i1 < voicedN && voicedT[i1] <= vb) i1++;
            voiced = java.util.Arrays.copyOfRange(voicedT, i0, i1);
        }
        return SyllableDetector.detect(env, envStart, SAMPLE_RATE, start, end, voiced);
    }
    public static volatile float pauseMinS = 1.0f, pauseMaxS = 2.5f; // prawidlowy zakres pauzy (Ustawienia)
    public static class Pause {
        public final double start, end;
        Pause(double s, double e) { start = s; end = e; }
        public double dur() { return end - start; }
        public boolean ok() { return dur() >= pauseMinS && dur() <= pauseMaxS; }
    }
    private static final List<Pause> pauses = new ArrayList<>();

    // Wolane dla kazdego okna analizy (YIN, 2048 probek) — z nagrywania i z wczytanego pliku.
    public static boolean analyzeVoice(long windowEndSample, float rms, float f0) {
        boolean speech = vad.frame(windowEndSample / (double) SAMPLE_RATE, rms, rms > 0.003f ? f0 : -1f);
        double[] pp = vad.pendingPause;
        if (pp == null) return speech;
        vad.pendingPause = null;
        float[] env;
        int e0;
        synchronized (lock) {
            e0 = Math.max(0, (int) ((pp[0] - 1.0) * SAMPLE_RATE / ENVELOPE_CHUNK));
            int e1 = Math.min(envelopeSize, (int) (pp[1] * SAMPLE_RATE / ENVELOPE_CHUNK) + 1);
            if (e1 - e0 < 8) return speech;
            env = new float[e1 - e0];
            System.arraycopy(envelope, e0, env, 0, e1 - e0);
        }
        double[] r = VadDetector.refine(env, (long) e0 * ENVELOPE_CHUNK, SAMPLE_RATE, pp[0], pp[1]);
        if (r != null && r[1] - r[0] >= 0.2 && r[1] - r[0] < 60) {
            synchronized (lock) {
                pauses.add(new Pause(r[0], r[1]));
                if (pauses.size() > 400) pauses.remove(0);
            }
        }
        return speech;
    }

    public static List<Pause> pausesSnapshot() {
        synchronized (lock) { return new ArrayList<>(pauses); }
    }

    public static void reset() {
        vad.reset();
        liveSyllables = null;
        liveRate = 0f;
        synchronized (lock) { syllables.clear(); sylTotal = 0; sylTime = 0; frameNo = 0; voicedN = 0; }
        tracker.reset();
        norms.reset();
        synchronized (lock) {
            pauses.clear();
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
                pitchPts.add(new PitchPoint(sampleIndex, freq));
            } else if (!pitchPts.isEmpty() && pitchPts.get(pitchPts.size() - 1).freq > 0) {
                pitchPts.add(new PitchPoint(sampleIndex, -1));
            }
            if (pitchPts.size() > 40000) {
                pitchPts.subList(0, 4000).clear();
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
