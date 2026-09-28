package com.pitchrec.backgroundrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.util.Base64;

import com.pitchrec.nativetest.LiveAudioData;
import com.pitchrec.nativetest.YinPitchDetector;

import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;

// Foreground Service — nagrywanie przez AudioRecord (ręczna pętla odczytu), żeby mieć dostęp
// do surowych próbek PCM potrzebnych do liczenia pitch (YIN) i rysowania fali w czasie
// rzeczywistym. Wspiera dwa formaty zapisu: WAV (surowe PCM) albo prawdziwe MP3 (przez
// czysty port Javy biblioteki LAME — java-lame, bez C++/NDK).
public class BackgroundRecorderService extends Service {

    public static final String ACTION_START = "com.pitchrec.backgroundrecorder.START";
    public static final String ACTION_PAUSE = "com.pitchrec.backgroundrecorder.PAUSE";
    public static final String ACTION_RESUME = "com.pitchrec.backgroundrecorder.RESUME";
    public static final String ACTION_STOP = "com.pitchrec.backgroundrecorder.STOP";
    public static final String EXTRA_FORMAT = "format"; // "wav" albo "mp3"
    public static final String CHANNEL_ID = "pitchrec_recording_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static volatile String currentStatus = "NONE"; // "NONE" | "RECORDING" | "PAUSED"

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int YIN_WINDOW = 2048;

    private AudioRecord audioRecord;
    private Thread recordThread;
    private volatile boolean recording = false;
    private volatile boolean paused = false;
    private String outputFormat = "wav";

    // Sciezka WAV (surowe PCM + naglowek) — nagrywanie na zywo jest teraz ZAWSZE WAV;
    // MP3 (jesli wybrane) to wsadowa konwersja calego pliku dopiero przy Stop
    // (patrz convertWavToMp3), nie kodowanie na zywo.
    private RandomAccessFile wavOutputStream;
    private long pcmBytesWritten = 0L;

    // Wspolna, biezaca sciezka pliku i format — pozwala MainActivity na podglad/odtworzenie
    // fragmentu nagrania PODCZAS pauzy, zanim plik zostanie sfinalizowany przy Stop.
    public static volatile String currentOutputFilePath = null;
    public static volatile String currentOutputFormat = "wav";

    private File outputFile;
    private long recordingStartedAt = 0L;
    private long pausedAccumMs = 0L;
    private long lastResumeAt = 0L;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private AudioFocusRequest audioFocusRequest;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        releaseAudioFocus();
        releaseMediaSession();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_START.equals(action)) {
            String fmt = intent.getStringExtra(EXTRA_FORMAT);
            handleStart(fmt != null ? fmt : "wav");
        }
        else if (ACTION_PAUSE.equals(action)) handlePause();
        else if (ACTION_RESUME.equals(action)) handleResume();
        else if (ACTION_STOP.equals(action)) handleStop();
        return START_STICKY;
    }

    private void handleStart(String format) {
        if ("RECORDING".equals(currentStatus)) return;
        outputFormat = format;

        LiveAudioData.reset();
        createNotificationChannel();
        setupMediaSession();
        Notification notification = buildNotification("Nagrywanie…");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int combinedType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
            startForeground(NOTIFICATION_ID, notification, combinedType);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PitchRec:BackgroundRecorderWakeLock");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(4 * 60 * 60 * 1000L);
        } catch (Exception e) { /* ignorowane */ }

        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attrs)
                        .build();
                am.requestAudioFocus(audioFocusRequest);
            } else {
                //noinspection deprecation
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        } catch (Exception e) { /* ignorowane */ }

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING);
            if (minBufferSize <= 0) throw new IOException("AudioRecord.getMinBufferSize failed: " + minBufferSize);
            int bufferSize = minBufferSize * 4;

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNELS, ENCODING, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IOException("AudioRecord nie zainicjalizowany poprawnie");
            }

            // ZAWSZE nagrywamy na zywo jako WAV — niezaleznie od wybranego formatu
            // koncowego. To wlacza podglad podczas pauzy UNIWERSALNIE (wczesniej nie
            // dzialalo dla MP3, bo czesciowy strumien MP3 nie jest bezpiecznie
            // odtwarzalny w trakcie kodowania). Konwersja do MP3 (jesli wybrana) dzieje
            // sie na CALYM, gotowym pliku dopiero po wcisnieciu Stop.
            outputFile = new File(getCacheDir(), "bg_recording_" + System.currentTimeMillis() + ".wav");
            wavOutputStream = new RandomAccessFile(outputFile, "rw");
            writeWavHeaderPlaceholder(wavOutputStream);
            pcmBytesWritten = 0L;
            currentOutputFilePath = outputFile.getAbsolutePath();
            currentOutputFormat = "wav"; // podczas nagrywania ZAWSZE wav (podglad w pauzie)
            // MP3 kodowane NA BIEZACO obok WAV — po STOP nie trzeba juz nic przeliczac
            mp3Stream = null;
            if ("mp3".equals(outputFormat)) {
                try { mp3Stream = new Mp3Stream(new File(getCacheDir(), "bg_recording_" + System.currentTimeMillis() + ".mp3"), SAMPLE_RATE); }
                catch (Throwable t) { mp3Stream = null; } // awaryjnie: konwersja po STOP jak dawniej
            }
            peakAbs = 0;
            liveBoost = Math.max(1, LiveAudioData.outputBoost);

            audioRecord.startRecording();
            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IOException("AudioRecord.startRecording() nie uruchomilo nagrywania");
            }

            recording = true;
            paused = false;
            recordingStartedAt = System.currentTimeMillis();
            pausedAccumMs = 0L;
            lastResumeAt = recordingStartedAt;
            currentStatus = "RECORDING";
            LiveAudioData.isRecordingActive = true;

            final int finalBufferSize = bufferSize;
            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO); } catch (Exception e) { }
                    short[] buffer = new short[finalBufferSize / 2];
                    float[] yinWindow = new float[YIN_WINDOW];
                    int yinFillCount = 0;
                    long samplePos = 0;
                    float lastSmoothedFreq = 0f; // do wygladzania (fSm), jak w oryginalnym JS
                    float currentAppliedGain = LiveAudioData.gainMultiplier; // do plynnego rampowania gain

                    while (recording) {
                        if (paused) {
                            try { Thread.sleep(50); } catch (InterruptedException ie) { }
                            continue;
                        }
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            // Zastosuj gain PLYNNIE (rampowanie próbka-po-próbce w strone
                            // celu z suwaka) — bez tego, przesuniecie suwaka podczas
                            // nagrywania powodowalo slyszalne "kliknieice/skok" na granicy
                            // buforow, bo caly bufor od razu skakal na nowa wartosc.
                            float targetGain = LiveAudioData.gainMultiplier;
                            for (int i = 0; i < read; i++) {
                                currentAppliedGain += (targetGain - currentAppliedGain) * 0.01f;
                                int amplified = (int) (buffer[i] * currentAppliedGain);
                                if (amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
                                else if (amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;
                                buffer[i] = (short) amplified;
                            }

                            // Bramka szumów — jesli wlaczona, wycisz caly bufor gdy jego
                            // RMS jest ponizej ustawionego progu (tlumi szum tla w cichych
                            // momentach).
                            if (LiveAudioData.noiseGateEnabled) {
                                float sumSq = 0;
                                for (int i = 0; i < read; i++) {
                                    float norm = buffer[i] / 32768f;
                                    sumSq += norm * norm;
                                }
                                float bufRms = (float) Math.sqrt(sumSq / read);
                                if (bufRms < LiveAudioData.noiseGateThreshold) {
                                    for (int i = 0; i < read; i++) buffer[i] = 0;
                                }
                            }

                            // Glosnosc wynikowa ×N — od razu, z miekkim limiterem (bez trzaskow)
                            if (liveBoost > 1) {
                                final double T = 0.6, K = 0.4;
                                for (int i = 0; i < read; i++) {
                                    double x = buffer[i] / 32768.0 * liveBoost, ax = Math.abs(x);
                                    double y = ax <= T ? ax : T + K * Math.tanh((ax - T) / K);
                                    buffer[i] = (short) Math.round(Math.signum(x) * Math.min(y, 1.0) * 0.985 * 32767);
                                }
                            }
                            for (int i = 0; i < read; i++) {
                                int av = buffer[i] < 0 ? -buffer[i] : buffer[i];
                                if (av > peakAbs) peakAbs = av;
                            }
                            try {
                                writeAudioChunk(buffer, read);
                            } catch (IOException ioe) { /* kontynuuj */ }
                            Mp3Stream ms = mp3Stream;
                            if (ms != null) ms.feed(buffer, read);

                            LiveAudioData.appendSamples(buffer, read);

                            for (int i = 0; i < read; i++) {
                                yinWindow[yinFillCount] = buffer[i] / 32768f;
                                yinFillCount++;
                                if (yinFillCount >= YIN_WINDOW) {
                                    // RMS calego okna — brakujacy w poprzedniej wersji prog
                                    // glosnosci (rms>0.006 w oryginalnym JS), ktory zapobiega
                                    // przyjmowaniu przypadkowych, szumowych wykryc podczas
                                    // cichych momentow (naturalne przerwy w mowie) —
                                    // to byla prawdopodobnie glowna przyczyna "skokow mimo
                                    // stabilnego glosu".
                                    float sum = 0;
                                    for (int j = 0; j < YIN_WINDOW; j++) sum += yinWindow[j] * yinWindow[j];
                                    float rms = (float) Math.sqrt(sum / YIN_WINDOW);

                                    float freq = YinPitchDetector.detect(yinWindow);
                                    long windowStartSample = samplePos + i - YIN_WINDOW + 1;
                                    // VAD + pauzy, ciagla linia pitch (PitchTracker) i ocena emisji wg norm
                                    LiveAudioData.processFrame(windowStartSample, rms, freq, YinPitchDetector.lastRelaxed);
                                    yinFillCount = 0;
                                }
                            }
                            samplePos += read;
                        }
                    }
                    LiveAudioData.finishAnalysis(); // domkniecie ostatniej porcji mowy / linii pitch
                }
            }, "PitchRecAudioReadThread");
            recordThread.start();
        } catch (Exception e) {
            currentStatus = "NONE";
            LiveAudioData.isRecordingActive = false;
            if (mp3Stream != null) { mp3Stream.abort(); mp3Stream = null; }
            cleanupAudioResources();
            releaseWakeLock();
            releaseAudioFocus();
            releaseMediaSession();
            RecordingResultHolder.rejectStop("FAILED_TO_RECORD", e.getMessage());
            stopForegroundCompat();
            stopSelf();
        }
    }

    // Zapisuje porcje probek jako surowe PCM (WAV) — nagrywanie na zywo jest teraz ZAWSZE
    // WAV, kodowanie MP3 (jesli wybrane) dzieje sie dopiero przy Stop, na calym pliku.
    private void writeAudioChunk(short[] samples, int count) throws IOException {
        if (wavOutputStream == null) return;
        byte[] bytes = shortsToBytesLE(samples, count);
        wavOutputStream.write(bytes);
        pcmBytesWritten += bytes.length;
    }

    private byte[] shortsToBytesLE(short[] samples, int count) {
        byte[] bytes = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            bytes[i * 2] = (byte) (samples[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
        }
        return bytes;
    }

    private void handlePause() {
        if (!"RECORDING".equals(currentStatus)) return;
        paused = true;
        pausedAccumMs += System.currentTimeMillis() - lastResumeAt;
        currentStatus = "PAUSED";
        LiveAudioData.isRecordingActive = false;
        updatePlaybackState(PlaybackState.STATE_PAUSED);
        updateNotification("Pauza");
        // Wymuszamy zapis na dysk — bez tego podglad/odtworzenie fragmentu podczas pauzy
        // moglby nie widziec najnowszych, jeszcze zbuforowanych danych.
        try {
            if (wavOutputStream != null) wavOutputStream.getFD().sync();
        } catch (Exception e) { /* ignorowane */ }
    }

    private void handleResume() {
        if (!"PAUSED".equals(currentStatus)) return;
        paused = false;
        lastResumeAt = System.currentTimeMillis();
        currentStatus = "RECORDING";
        LiveAudioData.isRecordingActive = true;
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        updateNotification("Nagrywanie…");
    }

    private volatile Mp3Stream mp3Stream = null;
    private volatile int peakAbs = 0;
    private volatile int liveBoost = 1;

    private void handleStop() {
        if ("NONE".equals(currentStatus)) {
            RecordingResultHolder.rejectStop("RECORDING_HAS_NOT_STARTED", null);
            stopForegroundCompat();
            stopSelf();
            return;
        }
        recording = false;
        if (recordThread != null) {
            try { recordThread.join(2000); } catch (InterruptedException ie) { }
        }
        try { cleanupAudioResources(); } catch (Exception e) { }
        final File wav = outputFile;
        final String fmt = outputFormat;
        final long durationMs = System.currentTimeMillis() - recordingStartedAt - pausedAccumMs;
        final boolean norm = LiveAudioData.autoNormalize;
        final int boost = LiveAudioData.outputBoost;
        currentStatus = "NONE";
        LiveAudioData.isRecordingActive = false;
        releaseAudioFocus();
        releaseMediaSession();
        updateNotification("Zapisywanie…");
        // ZAPIS W TLE (wczesniej wszystko szlo na glownym watku: dwa przebiegi glosnosci,
        // MP3 w najwolniejszej jakosci i przepisywanie calego pliku przez Base64 — stad
        // dlugie czekanie). Teraz: jeden przebieg glosnosci, szybkie MP3, plik przekazywany
        // bez kopiowania.
        final Mp3Stream ms = mp3Stream;
        mp3Stream = null;
        final int peak = peakAbs;
        new Thread(() -> {
            try {
                boolean hasData = wav != null && wav.exists() && wav.length() > 44;
                if (!hasData) {
                    if (ms != null) ms.abort();
                    RecordingResultHolder.rejectStop("EMPTY_RECORDING", null);
                    return;
                }
                // AUTO 0 dB: ile podglosnic, zeby szczyt trafil w -0,1 dBFS (cisza — nie wzmacniamy szumu)
                double g = (norm && peak >= 33) ? Math.max(1.0, 32390.0 / peak) : 1.0;
                File finalFile = wav;
                String mime = "audio/wav";
                File mp3 = ms != null ? ms.finish() : null;
                if (mp3 != null) {
                    // MP3 gotowe z nagrywania — podglosnienie bez ponownego kodowania (krok 1,5 dB)
                    int steps = (int) Math.floor(20 * Math.log10(g) / 1.5);
                    if (steps > 0) Mp3Gain.apply(mp3, steps);
                    wav.delete();
                    finalFile = mp3;
                    mime = "audio/mpeg";
                } else {
                    if (g > 1.01) scaleWavInPlace(wav, g);
                    if ("mp3".equals(fmt)) {
                        File conv = convertWavToMp3(wav); // awaryjnie (gdy kodowanie na biezaco sie nie udalo)
                        if (conv != null) { finalFile = conv; mime = "audio/mpeg"; }
                    }
                }
                // fala + pitch z nagrywania od razu do pamieci podrecznej DAW (otwarcie z NAGRAN bez dekodowania)
                try {
                    double gEff = g;
                    if ("audio/mpeg".equals(mime) && mp3 != null) gEff = Math.pow(10, Math.floor(20 * Math.log10(g) / 1.5) * 1.5 / 20);
                    if (com.pitchrec.nativetest.AudioFileLoader.cacheDir == null)
                        com.pitchrec.nativetest.AudioFileLoader.cacheDir = new File(getFilesDir(), ".dawcache");
                    com.pitchrec.nativetest.AudioFileLoader.saveCache(finalFile, LiveAudioData.exportCache((float) gEff));
                } catch (Throwable t) { }
                RecordingResultHolder.resolveStopFile(finalFile.getAbsolutePath(), durationMs, mime);
            } catch (Exception e) {
                RecordingResultHolder.rejectStop("FAILED_TO_FETCH_RECORDING", e.getMessage());
            } finally {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (!"RECORDING".equals(currentStatus) && !"PAUSED".equals(currentStatus)) {
                        releaseWakeLock();
                        stopForegroundCompat();
                        stopSelf();
                    }
                });
            }
        }, "rec-save").start();
    }

    // Liniowe podglosnienie WAV (wzmocnienie ×N jest juz zrobione w trakcie nagrywania)
    private void scaleWavInPlace(File wavFile, double g) {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            if (raf.length() <= 44) return;
            byte[] buf = new byte[1 << 18];
            long pos = 44;
            raf.seek(pos);
            int read;
            while ((read = raf.read(buf)) > 0) {
                int even = read & ~1;
                if (even == 0) break;
                for (int i = 0; i < even; i += 2) {
                    int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                    long nv = Math.round(v * g);
                    if (nv > 32767) nv = 32767; else if (nv < -32768) nv = -32768;
                    buf[i] = (byte) (nv & 0xff);
                    buf[i + 1] = (byte) ((nv >> 8) & 0xff);
                }
                raf.seek(pos);
                raf.write(buf, 0, even);
                pos += even;
                raf.seek(pos);
            }
        } catch (Exception e) { /* zostaje bez zmian */ }
    }

    // GLOSNOSC W JEDNYM PRZEBIEGU: 0 dB (szczyt -0,1 dBFS) i/lub wzmocnienie ×N z miekkim
    // limiterem (powyzej 60% skali tanh — glosne miejsca lagodnie sciskane, bez trzaskow).
    private void applyGainInPlace(File wavFile, boolean normalize, int boost) {
        final double T = 0.6, K = 1.0 - T;
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            long len = raf.length();
            if (len <= 44) return;
            byte[] buf = new byte[1 << 18];
            double gain = 1.0;
            if (normalize) {
                int peak = 0, read;
                raf.seek(44);
                while ((read = raf.read(buf)) > 0) {
                    for (int i = 0; i + 1 < read; i += 2) {
                        int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                        int av = v < 0 ? -v : v;
                        if (av > peak) peak = av;
                    }
                }
                if (peak >= 33) gain = Math.max(1.0, 32390.0 / peak); // cisza — nie wzmacniamy szumu
            }
            final double g = gain * Math.max(1, boost);
            final boolean limit = boost > 1;
            if (g <= 1.01) return;
            long pos = 44;
            raf.seek(pos);
            int read;
            while ((read = raf.read(buf)) > 0) {
                int even = read & ~1;
                if (even == 0) break;
                for (int i = 0; i < even; i += 2) {
                    int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                    long nv;
                    if (!limit) {
                        nv = Math.round(v * g);
                    } else {
                        double x = v / 32768.0 * g, ax = Math.abs(x);
                        double y = ax <= T ? ax : T + K * Math.tanh((ax - T) / K);
                        nv = Math.round(Math.signum(x) * Math.min(y, 1.0) * 0.985 * 32767);
                    }
                    if (nv > 32767) nv = 32767; else if (nv < -32768) nv = -32768;
                    buf[i] = (byte) (nv & 0xff);
                    buf[i + 1] = (byte) ((nv >> 8) & 0xff);
                }
                raf.seek(pos);
                raf.write(buf, 0, even);
                pos += even;
                raf.seek(pos);
            }
        } catch (Exception e) { /* zostaje nagranie bez zmian */ }
    }

    // Konwertuje caly, gotowy plik WAV do MP3 (wsadowo, nie na zywo) — uzywane dopiero
    // przy Stop, jesli MP3 zostalo wybrane jako format koncowy.
    private File convertWavToMp3(File wavFile) {
        try {
            File mp3File = new File(getCacheDir(), "converted_" + System.currentTimeMillis() + ".mp3");
            javax.sound.sampled.AudioFormat lameFormat =
                    new javax.sound.sampled.AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            // jakosc 7 (szybka) i 128 kbps mono — dla mowy brzmi tak samo, a koduje sie kilka razy szybciej
            LameEncoder encoder = new LameEncoder(lameFormat, 128, MPEGMode.MONO, 7, false);
            byte[] encodeBuffer = new byte[encoder.getPCMBufferSize()];

            try (RandomAccessFile in = new RandomAccessFile(wavFile, "r");
                 FileOutputStream out = new FileOutputStream(mp3File)) {
                in.seek(44); // pomijamy naglowek WAV
                byte[] pcmChunk = new byte[encoder.getPCMBufferSize()];
                int read;
                while ((read = in.read(pcmChunk)) != -1) {
                    int bytesEncoded = encoder.encodeBuffer(pcmChunk, 0, read, encodeBuffer);
                    if (bytesEncoded > 0) out.write(encodeBuffer, 0, bytesEncoded);
                }
                int flushed = encoder.encodeFinish(encodeBuffer);
                if (flushed > 0) out.write(encodeBuffer, 0, flushed);
            }
            encoder.close();
            wavFile.delete(); // WAV juz niepotrzebny, mamy MP3
            return mp3File;
        } catch (Exception e) {
            return null; // konwersja nieudana — zostajemy przy WAV (obsluzone przez wywolujacego)
        }
    }

    // Normalizacja szczytu do -0,1 dBFS (wartosc 32390 z 32767). Dwa przejscia po pliku:
    // 1) znajdz najglosniejsza probke, 2) przemnoz wszystkie probki przez wspolny wspolczynnik.
    // Tylko PODGLASNIANIE (glosne nagranie zostaje bez zmian). Blad = zostawiamy oryginal.
    private void normalizeWavInPlace(File wavFile) {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            long len = raf.length();
            if (len <= 44) return;
            byte[] buf = new byte[65536];
            int peak = 0;
            raf.seek(44);
            int read;
            while ((read = raf.read(buf)) > 0) {
                for (int i = 0; i + 1 < read; i += 2) {
                    int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                    int a = v < 0 ? -v : v;
                    if (a > peak) peak = a;
                }
            }
            if (peak < 33) return; // praktycznie cisza — nie wzmacniamy szumu
            double gain = 32390.0 / peak;
            if (gain <= 1.01) return; // juz glosne
            long pos = 44;
            raf.seek(pos);
            while ((read = raf.read(buf)) > 0) {
                int even = read & ~1;
                if (even == 0) break; // pojedynczy nieparzysty bajt na koncu — koniec danych
                for (int i = 0; i < even; i += 2) {
                    int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                    long nv = Math.round(v * gain);
                    if (nv > 32767) nv = 32767; else if (nv < -32768) nv = -32768;
                    buf[i] = (byte) (nv & 0xff);
                    buf[i + 1] = (byte) ((nv >> 8) & 0xff);
                }
                raf.seek(pos);
                raf.write(buf, 0, even);
                pos += even;
                raf.seek(pos);
            }
        } catch (Exception e) { /* zostaje oryginalne nagranie */ }
    }

    // Glosnosc wynikowa ×N: wzmocnienie liniowe, a powyzej 60% skali miekki limiter (tanh),
    // wiec glosne miejsca sa lagodnie sciskane zamiast przesterowane "trzaski".
    private void boostWavInPlace(File wavFile, int boost) {
        final double T = 0.6, K = 1.0 - T, CEIL = 0.985;
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            if (raf.length() <= 44) return;
            byte[] buf = new byte[65536];
            long pos = 44;
            raf.seek(pos);
            int read;
            while ((read = raf.read(buf)) > 0) {
                int even = read & ~1;
                if (even == 0) break;
                for (int i = 0; i < even; i += 2) {
                    int v = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                    double x = v / 32768.0 * boost, ax = Math.abs(x);
                    double y = ax <= T ? ax : T + K * Math.tanh((ax - T) / K);
                    long nv = Math.round(Math.signum(x) * Math.min(y, 1.0) * CEIL * 32767);
                    buf[i] = (byte) (nv & 0xff);
                    buf[i + 1] = (byte) ((nv >> 8) & 0xff);
                }
                raf.seek(pos);
                raf.write(buf, 0, even);
                pos += even;
                raf.seek(pos);
            }
        } catch (Exception e) { /* zostaje nagranie bez dodatkowego wzmocnienia */ }
    }

    private void cleanupAudioResources() {
        currentOutputFilePath = null;
        try { if (audioRecord != null) { audioRecord.stop(); audioRecord.release(); } } catch (Exception e) { }
        audioRecord = null;

        try {
            if (wavOutputStream != null) {
                finalizeWavHeader(wavOutputStream, pcmBytesWritten);
                wavOutputStream.close();
            }
        } catch (Exception e) { }
        wavOutputStream = null;
    }

    private void writeWavHeaderPlaceholder(RandomAccessFile raf) throws IOException {
        byte[] header = new byte[44];
        raf.write(header);
    }

    private void finalizeWavHeader(RandomAccessFile raf, long pcmDataSize) throws IOException {
        long totalDataLen = pcmDataSize + 36;
        int channels = 1;
        int bitsPerSample = 16;
        long byteRate = SAMPLE_RATE * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        raf.seek(0);
        raf.writeBytes("RIFF");
        writeIntLE(raf, (int) totalDataLen);
        raf.writeBytes("WAVE");
        raf.writeBytes("fmt ");
        writeIntLE(raf, 16);
        writeShortLE(raf, (short) 1);
        writeShortLE(raf, (short) channels);
        writeIntLE(raf, SAMPLE_RATE);
        writeIntLE(raf, (int) byteRate);
        writeShortLE(raf, (short) blockAlign);
        writeShortLE(raf, (short) bitsPerSample);
        raf.writeBytes("data");
        writeIntLE(raf, (int) pcmDataSize);
    }

    private void writeIntLE(RandomAccessFile raf, int value) throws IOException {
        raf.write(value & 0xff);
        raf.write((value >> 8) & 0xff);
        raf.write((value >> 16) & 0xff);
        raf.write((value >> 24) & 0xff);
    }

    private void writeShortLE(RandomAccessFile raf, short value) throws IOException {
        raf.write(value & 0xff);
        raf.write((value >> 8) & 0xff);
    }

    private byte[] readFileBytes(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        }
        return bos.toByteArray();
    }

    private void setupMediaSession() {
        if (mediaSession != null) return;
        try {
            MediaSession session = new MediaSession(this, "PitchRecBackgroundRecorder");
            session.setCallback(new MediaSession.Callback() {});
            PlaybackState state = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP)
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .build();
            session.setPlaybackState(state);
            session.setActive(true);
            mediaSession = session;
        } catch (Exception e) { /* ignorowane */ }
    }

    private void updatePlaybackState(int state) {
        try {
            float speed = (state == PlaybackState.STATE_PLAYING) ? 1f : 0f;
            PlaybackState playbackState = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP)
                    .setState(state, 0, speed)
                    .build();
            if (mediaSession != null) mediaSession.setPlaybackState(playbackState);
        } catch (Exception e) { }
    }

    private void releaseMediaSession() {
        try {
            if (mediaSession != null) {
                mediaSession.setActive(false);
                mediaSession.release();
            }
        } catch (Exception e) { }
        mediaSession = null;
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception e) { }
        wakeLock = null;
    }

    private void releaseAudioFocus() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                am.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                //noinspection deprecation
                am.abandonAudioFocus(null);
            }
        } catch (Exception e) { }
        audioFocusRequest = null;
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            //noinspection deprecation
            stopForeground(true);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Nagrywanie w tle", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            //noinspection deprecation
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle("PitchRec")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true);

        if (mediaSession != null) {
            try {
                builder.setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()));
            } catch (Exception e) { }
        }

        return builder.build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(text));
    }
}
