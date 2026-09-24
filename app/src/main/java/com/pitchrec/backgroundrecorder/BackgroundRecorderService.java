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
            currentOutputFormat = "wav"; // podczas nagrywania ZAWSZE wav, konwersja przy Stop

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

                            try {
                                writeAudioChunk(buffer, read);
                            } catch (IOException ioe) { /* kontynuuj */ }

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

                                    if (freq > 70 && freq < 1000 && rms > 0.006f) {
                                        // Wygladzanie wykladnicze (fSm), dokladnie jak w
                                        // oryginalnym JS — bez tego kazde okno dawalo
                                        // "surowy" wynik YIN, co przy naturalnym szumie
                                        // analizy wygladalo jak nagle skoki.
                                        float smoothed = lastSmoothedFreq > 0
                                                ? lastSmoothedFreq * 0.88f + freq * 0.12f
                                                : freq;
                                        lastSmoothedFreq = smoothed;
                                        LiveAudioData.appendPitch(windowStartSample, smoothed);
                                    } else {
                                        LiveAudioData.appendPitch(windowStartSample, -1);
                                    }
                                    yinFillCount = 0;
                                }
                            }
                            samplePos += read;
                        }
                    }
                }
            }, "PitchRecAudioReadThread");
            recordThread.start();
        } catch (Exception e) {
            currentStatus = "NONE";
            LiveAudioData.isRecordingActive = false;
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

    private void handleStop() {
        if ("NONE".equals(currentStatus)) {
            RecordingResultHolder.rejectStop("RECORDING_HAS_NOT_STARTED", null);
            stopForegroundCompat();
            stopSelf();
            return;
        }
        try {
            recording = false;
            if (recordThread != null) {
                try { recordThread.join(2000); } catch (InterruptedException ie) { }
            }
            cleanupAudioResources();

            boolean hasData = outputFile != null && outputFile.exists() && outputFile.length() > 0;
            if (!hasData) {
                RecordingResultHolder.rejectStop("EMPTY_RECORDING", null);
            } else {
                File finalFile = outputFile;
                String mime = "audio/wav";
                if ("mp3".equals(outputFormat)) {
                    // Konwersja calego, gotowego pliku WAV do MP3 — dzieje sie TERAZ,
                    // dopiero po Stop (nie na zywo podczas nagrywania), zeby podglad
                    // podczas pauzy dzialal zawsze (WAV), niezaleznie od wybranego
                    // formatu koncowego.
                    File mp3File = convertWavToMp3(outputFile);
                    if (mp3File != null) {
                        finalFile = mp3File;
                        mime = "audio/mpeg";
                    }
                }
                long durationMs = System.currentTimeMillis() - recordingStartedAt - pausedAccumMs;
                byte[] bytes = readFileBytes(finalFile);
                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                RecordingResultHolder.resolveStop(base64, durationMs, mime);
            }
        } catch (Exception e) {
            RecordingResultHolder.rejectStop("FAILED_TO_FETCH_RECORDING", e.getMessage());
        } finally {
            currentStatus = "NONE";
            LiveAudioData.isRecordingActive = false;
            releaseWakeLock();
            releaseAudioFocus();
            releaseMediaSession();
            stopForegroundCompat();
            stopSelf();
        }
    }

    // Konwertuje caly, gotowy plik WAV do MP3 (wsadowo, nie na zywo) — uzywane dopiero
    // przy Stop, jesli MP3 zostalo wybrane jako format koncowy.
    private File convertWavToMp3(File wavFile) {
        try {
            File mp3File = new File(getCacheDir(), "converted_" + System.currentTimeMillis() + ".mp3");
            javax.sound.sampled.AudioFormat lameFormat =
                    new javax.sound.sampled.AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            LameEncoder encoder = new LameEncoder(lameFormat, 192, MPEGMode.MONO, Lame.QUALITY_HIGHEST, false);
            byte[] encodeBuffer = new byte[encoder.getPCMBufferSize()];

            try (RandomAccessFile in = new RandomAccessFile(wavFile, "r");
                 FileOutputStream out = new FileOutputStream(mp3File)) {
                in.seek(44); // pomijamy naglowek WAV
                byte[] pcmChunk = new byte[8192];
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
