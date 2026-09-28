package com.pitchrec.nativetest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Wczytuje zapisany plik (WAV albo MP3) z powrotem do LiveAudioData, żeby PitchWaveView mógł
// go wyświetlić — dekoduje do PCM, liczy obwiednię i pitch (YIN) dla CAŁEGO pliku,
// "odtwarzając" tę samą analizę, którą normalnie robi serwis podczas nagrywania na żywo.
public class AudioFileLoader {

    private static final int YIN_WINDOW = 2048;

    // ── PAMIEC PODRECZNA (fala + pitch) w <pliki aplikacji>/.dawcache — klucz: rozmiar i data pliku
    //    (zmiana nazwy przy opisie nagrania ich nie zmienia). Wczytanie z niej trwa ulamek sekundy.
    public static volatile File cacheDir = null;

    private static File cacheFor(File f) {
        if (cacheDir == null) return null;
        if (!cacheDir.exists()) cacheDir.mkdirs();
        return new File(cacheDir, f.length() + "_" + f.lastModified() + ".dawc");
    }

    public static void saveCache(File f, LiveAudioData.CacheData c) {
        File cf = cacheFor(f);
        if (cf == null || c == null || c.env == null) return;
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(cf)))) {
            out.writeInt(0x44415731); // "DAW1"
            out.writeLong(c.total);
            out.writeInt(c.env.length);
            for (float v : c.env) out.writeFloat(v);
            for (float v : c.rms) out.writeFloat(v);
            int pn = c.pitchS == null ? 0 : c.pitchS.length;
            out.writeInt(pn);
            for (int i = 0; i < pn; i++) { out.writeLong(c.pitchS[i]); out.writeFloat(c.pitchF[i]); }
        } catch (Exception e) { cf.delete(); }
    }

    public static LiveAudioData.CacheData readCache(File f) {
        File cf = cacheFor(f);
        if (cf == null || !cf.exists()) return null;
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(cf)))) {
            if (in.readInt() != 0x44415731) return null;
            LiveAudioData.CacheData c = new LiveAudioData.CacheData();
            c.total = in.readLong();
            int n = in.readInt();
            if (n < 0 || n > 50_000_000) return null;
            c.env = new float[n]; c.rms = new float[n];
            for (int i = 0; i < n; i++) c.env[i] = in.readFloat();
            for (int i = 0; i < n; i++) c.rms[i] = in.readFloat();
            int pn = in.readInt();
            c.pitchS = new long[Math.max(0, pn)]; c.pitchF = new float[Math.max(0, pn)];
            for (int i = 0; i < pn; i++) { c.pitchS[i] = in.readLong(); c.pitchF[i] = in.readFloat(); }
            return c;
        } catch (Exception e) { return null; }
    }

    // Czy dla pliku jest juz zapamietana linia pitch
    public static boolean cachedPitch(File f) {
        LiveAudioData.CacheData c = readCache(f);
        return c != null && c.pitchS != null && c.pitchS.length > 0;
    }

    // Ostatnio wczytany plik (probki w pamieci) — zeby "zaladuj pitch" nie dekodowal go drugi raz
    private static String lastPath = null;
    private static short[] lastSamples = null;

    public static void loadIntoLiveData(File file) throws IOException {
        loadWave(file, false);
        addPitch(file, true);
    }

    // 1) Sama FALA — bardzo szybko (bez pitch, pauz, norm)
    // Zwraca true, gdy wczytano z pamieci podrecznej razem z linia pitch
    public static boolean loadWave(File file) throws IOException { return loadWave(file, true); }

    public static synchronized boolean loadWave(File file, boolean useCache) throws IOException {
        LiveAudioData.reset();
        LiveAudioData.CacheData cached = useCache ? readCache(file) : null;
        if (cached != null) {
            boolean hasPitch = cached.pitchS != null && cached.pitchS.length > 0;
            LiveAudioData.importCache(cached, hasPitch);
            lastPath = null; lastSamples = null;
            return hasPitch;
        }
        short[] samples = file.getName().endsWith(".mp3") ? decodeMp3(file) : decodeWav(file);
        lastPath = file.getAbsolutePath();
        lastSamples = samples;
        final int CH = 1 << 16;
        for (int off = 0; off < samples.length; off += CH) {
            int len = Math.min(CH, samples.length - off);
            short[] chunk = new short[len];
            System.arraycopy(samples, off, chunk, 0, len);
            LiveAudioData.appendSamples(chunk, len);
        }
        saveCache(file, LiveAudioData.exportCache(1f));
        return false;
    }

    // 2) PITCH dla wczytanej fali. full=false: tylko linia pitch (bez pauz, norm i sylab).
    public static synchronized void addPitch(File file, boolean full) throws IOException {
        short[] samples = file.getAbsolutePath().equals(lastPath) ? lastSamples : null;
        if (samples == null) {
            samples = file.getName().endsWith(".mp3") ? decodeMp3(file) : decodeWav(file);
        }
        // PITCH (YIN) ROWNOLEGLE na wszystkich rdzeniach — okna 2048 probek sa niezalezne.
        // Ciche okna (RMS < 0,003) pomijamy: VAD, linia pitch i sylaby i tak ich nie uzywaja.
        final int frames = samples.length / YIN_WINDOW;
        final float[] rmsA = new float[frames], f0A = new float[frames], relA = new float[frames];
        final short[] src = samples;
        int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        final java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.List<java.util.concurrent.Future<?>> jobs = new java.util.ArrayList<>();
        for (int t = 0; t < threads; t++) {
            jobs.add(pool.submit(() -> {
                YinPitchDetector.Work w = new YinPitchDetector.Work();
                float[] win = new float[YIN_WINDOW];
                int f;
                while ((f = next.getAndIncrement()) < frames) {
                    int base = f * YIN_WINDOW;
                    float sum = 0;
                    for (int j = 0; j < YIN_WINDOW; j++) { float v = src[base + j] / 32768f; win[j] = v; sum += v * v; }
                    float rms = (float) Math.sqrt(sum / YIN_WINDOW);
                    rmsA[f] = rms;
                    if (rms < 0.003f) { f0A[f] = -1; relA[f] = -1; continue; }
                    f0A[f] = YinPitchDetector.detect(win, w);
                    relA[f] = w.relaxed;
                }
            }));
        }
        try { for (java.util.concurrent.Future<?> j : jobs) j.get(); }
        catch (Exception e) { throw new IOException("Analiza pitch: " + e.getMessage()); }
        finally { pool.shutdown(); }

        lastSamples = null; lastPath = null; // pamiec zwalniamy — pitch juz policzony
        if (!full) {
            for (int f = 0; f < frames; f++) LiveAudioData.processPitchOnly((long) f * YIN_WINDOW, rmsA[f], f0A[f], relA[f]);
            LiveAudioData.finishPitchOnly();
            saveCache(file, LiveAudioData.exportCache(1f)); // nastepnym razem pitch od razu
            return;
        }
        // Pelna analiza po kolei (VAD + pauzy, linia pitch, normy, sylaby) — jak na zywo
        LiveAudioData.batch = true;
        try {
            for (int f = 0; f < frames; f++) {
                LiveAudioData.processFrame((long) f * YIN_WINDOW, rmsA[f], f0A[f], relA[f]);
            }
            LiveAudioData.finishAnalysis();
        } finally { LiveAudioData.batch = false; }
    }

    // Porcje mowy z pliku wzorca — wyciete DOKLADNIE jak przy ocenie na zywo (ten sam VAD,
    // te same okna RMS 2048 probek, ta sama segmentacja Norms), z zachowanym RMS kazdej porcji.
    public static java.util.List<Norms.Segment> normSegments(File file) throws IOException {
        short[] s = file.getName().endsWith(".mp3") ? decodeMp3(file) : decodeWav(file);
        VadDetector vad = new VadDetector();
        Norms nm = new Norms();
        nm.keepBuffers = true;
        float[] win = new float[YIN_WINDOW];
        int frames = s.length / YIN_WINDOW;
        for (int f = 0; f < frames; f++) {
            double sum = 0;
            for (int j = 0; j < YIN_WINDOW; j++) { float v = s[f * YIN_WINDOW + j] / 32768f; win[j] = v; sum += v * v; }
            float rms = (float) Math.sqrt(sum / YIN_WINDOW);
            float f0 = YinPitchDetector.detect(win);
            double t = (f + 1) * YIN_WINDOW / (double) LiveAudioData.SAMPLE_RATE;
            boolean speech = vad.frame(t, rms, rms > 0.003f ? f0 : -1f);
            nm.frame(t, rms, speech);
        }
        nm.finish(frames * YIN_WINDOW / (double) LiveAudioData.SAMPLE_RATE);
        java.util.List<Norms.Segment> out = new java.util.ArrayList<>();
        for (Norms.Segment sg : nm.segmentsSnapshot()) if (sg.buf != null && sg.fourPhase) out.add(sg);
        return out;
    }

    // RMS kolejnych okien 2048 probek (~21,5/s) — do kalibracji norm z nagrania wzorcowego
    public static float[] rmsFrames(File file) throws IOException {
        short[] s = file.getName().endsWith(".mp3") ? decodeMp3(file) : decodeWav(file);
        int n = s.length / YIN_WINDOW;
        float[] out = new float[n];
        for (int f = 0; f < n; f++) {
            double sum = 0;
            for (int j = 0; j < YIN_WINDOW; j++) { float v = s[f * YIN_WINDOW + j] / 32768f; sum += v * v; }
            out[f] = (float) Math.sqrt(sum / YIN_WINDOW);
        }
        return out;
    }

    // Dekoduje WAV, wykrywajac liczbe kanalow z naglowka (offset 22-23) — importowane
    // pliki (nie nasze wlasne nagrania) sa czesto STEREO, a wczesniejsza wersja zawsze
    // zakladala mono, co przy stereo myliło kanaly L/R jako sekwencyjne probki mono,
    // dajac zniekształcony, "przesterowany" wyglad wykresu.
    private static short[] decodeWav(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] headerBytes = new byte[44];
            raf.readFully(headerBytes);
            int channels = (headerBytes[22] & 0xff) | ((headerBytes[23] & 0xff) << 8);
            if (channels <= 0) channels = 1; // zabezpieczenie przed nieprawidlowym naglowkiem

            long dataSize = raf.length() - 44;
            byte[] bytes = new byte[(int) dataSize];
            raf.readFully(bytes);
            short[] rawSamples = new short[bytes.length / 2];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawSamples);

            return downmixToMono(rawSamples, channels);
        }
    }

    // Usrednia kanaly stereo (albo wiecej) do mono — YIN i obwiednia analizuja jeden
    // strumien, tak jak nasze wlasne nagrania (ktore sa mono od razu przy zapisie).
    private static short[] downmixToMono(short[] samples, int channels) {
        if (channels <= 1) return samples;
        int monoLen = samples.length / channels;
        short[] mono = new short[monoLen];
        for (int i = 0; i < monoLen; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) sum += samples[i * channels + c];
            mono[i] = (short) (sum / channels);
        }
        return mono;
    }

    // Dekoduje MP3 przez wbudowany w Androida MediaExtractor/MediaCodec — Android wspiera
    // DEKODOWANIE MP3 natywnie (tylko nie kodowanie), więc to bezpieczniejsza, dobrze
    // dokumentowana droga niż zgadywanie niepewnego API zewnętrznego dekodera.
    private static short[] decodeMp3(File file) throws IOException {
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        extractor.setDataSource(file.getAbsolutePath());

        int audioTrackIndex = -1;
        android.media.MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            android.media.MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(android.media.MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrackIndex = i;
                format = f;
                break;
            }
        }
        if (audioTrackIndex < 0 || format == null) {
            throw new IOException("Nie znaleziono strumienia audio w pliku MP3");
        }
        extractor.selectTrack(audioTrackIndex);

        android.media.MediaCodec codec;
        try {
            codec = android.media.MediaCodec.createDecoderByType(format.getString(android.media.MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();
        } catch (Exception e) {
            throw new IOException("Nie udalo sie utworzyc dekodera MP3: " + e.getMessage());
        }

        java.io.ByteArrayOutputStream pcmOut = new java.io.ByteArrayOutputStream();
        android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
        boolean inputDone = false, outputDone = false;

        // Szybkie dekodowanie: wkladamy do dekodera wszystko, co przyjmie (bez czekania),
        // a na wynik czekamy krotko — wczesniej kazda ramka MP3 mogla czekac po 10 ms
        // (tysiace ramek = kilkanascie sekund przy dluzszym nagraniu).
        while (!outputDone) {
            boolean progressed = false;
            while (!inputDone) {
                int inIdx = codec.dequeueInputBuffer(0);
                if (inIdx < 0) break;
                progressed = true;
                {
                    java.nio.ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                    int sampleSize = inBuf != null ? extractor.readSampleData(inBuf, 0) : -1;
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            // czekamy tylko wtedy, gdy nic sie nie ruszylo (wczesniej: do 1 ms na kazda ramke MP3)
            int outIdx = codec.dequeueOutputBuffer(info, progressed ? 0 : 2000);
            while (outIdx >= 0) {
                java.nio.ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                if (outBuf != null && info.size > 0) {
                    byte[] chunk = new byte[info.size];
                    outBuf.get(chunk);
                    pcmOut.write(chunk, 0, chunk.length);
                }
                codec.releaseOutputBuffer(outIdx, false);
                if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                    break;
                }
                outIdx = codec.dequeueOutputBuffer(info, 0);
            }
        }

        codec.stop();
        codec.release();
        extractor.release();

        int channels = format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
        byte[] pcmBytes = pcmOut.toByteArray();
        short[] samples = new short[pcmBytes.length / 2];
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
        return downmixToMono(samples, channels);
    }
}
