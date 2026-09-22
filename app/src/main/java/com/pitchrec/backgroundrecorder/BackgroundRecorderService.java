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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;

// Foreground Service — nagrywanie przez AudioRecord (ręczna pętla odczytu), żeby mieć dostęp
// do surowych próbek PCM potrzebnych do liczenia pitch (YIN) i rysowania fali w czasie
// rzeczywistym. Potwierdzone: w pełni natywna architektura (bez WebView) przetrwała
// nagrywanie z zablokowanym ekranem na SDK 36 nawet z MediaRecorder — AudioRecord powinien
// zadziałać identycznie dobrze, to nie API audio było wcześniej problemem, tylko WebView.
public class BackgroundRecorderService extends Service {

    public static final String ACTION_START = "com.pitchrec.backgroundrecorder.START";
    public static final String ACTION_PAUSE = "com.pitchrec.backgroundrecorder.PAUSE";
    public static final String ACTION_RESUME = "com.pitchrec.backgroundrecorder.RESUME";
    public static final String ACTION_STOP = "com.pitchrec.backgroundrecorder.STOP";
    public static final String CHANNEL_ID = "pitchrec_recording_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static volatile String currentStatus = "NONE"; // "NONE" | "RECORDING" | "PAUSED"

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int YIN_WINDOW = 2048; // rozmiar okna do liczenia pitch, jak AN.frequencyBinCount w JS

    private AudioRecord audioRecord;
    private Thread recordThread;
    private volatile boolean recording = false;
    private volatile boolean paused = false;
    private RandomAccessFile outputStream;
    private File outputFile;
    private long pcmBytesWritten = 0L;
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
        if (ACTION_START.equals(action)) handleStart();
        else if (ACTION_PAUSE.equals(action)) handlePause();
        else if (ACTION_RESUME.equals(action)) handleResume();
        else if (ACTION_STOP.equals(action)) handleStop();
        return START_STICKY;
    }

    private void handleStart() {
        if ("RECORDING".equals(currentStatus)) return;

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
            outputFile = new File(getCacheDir(), "bg_recording_" + System.currentTimeMillis() + ".wav");
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING);
            if (minBufferSize <= 0) throw new IOException("AudioRecord.getMinBufferSize failed: " + minBufferSize);
            int bufferSize = minBufferSize * 4;

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNELS, ENCODING, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IOException("AudioRecord nie zainicjalizowany poprawnie");
            }

            outputStream = new RandomAccessFile(outputFile, "rw");
            writeWavHeaderPlaceholder(outputStream);
            pcmBytesWritten = 0L;

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

            final int finalBufferSize = bufferSize;
            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO); } catch (Exception e) { }
                    short[] buffer = new short[finalBufferSize / 2];
                    float[] yinWindow = new float[YIN_WINDOW];
                    int yinFillCount = 0;
                    long samplePos = 0;

                    while (recording) {
                        if (paused) {
                            try { Thread.sleep(50); } catch (InterruptedException ie) { }
                            continue;
                        }
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            // Zapis do pliku WAV (16-bit little-endian, tak jak nagrywalismy dotad)
                            try {
                                byte[] bytes = shortsToBytesLE(buffer, read);
                                outputStream.write(bytes);
                                pcmBytesWritten += bytes.length;
                            } catch (IOException ioe) { /* kontynuuj, jak wczesniej */ }

                            // Obwiednia dla rysowania fali
                            LiveAudioData.appendSamples(buffer, read);

                            // Wypelnij okno YIN, licz pitch gdy okno pelne
                            for (int i = 0; i < read; i++) {
                                yinWindow[yinFillCount] = buffer[i] / 32768f;
                                yinFillCount++;
                                if (yinFillCount >= YIN_WINDOW) {
                                    float freq = YinPitchDetector.detect(yinWindow);
                                    long windowStartSample = samplePos + i - YIN_WINDOW + 1;
                                    if (freq > 70 && freq < 1000) {
                                        LiveAudioData.appendPitch(windowStartSample, freq);
                                    } else {
                                        LiveAudioData.appendPitch(windowStartSample, -1);
                                    }
                                    // Przesuwamy okno o pol dlugosci (overlap), nie zerujemy calkowicie —
                                    // prostsze podejscie: po prostu zaczynamy nowe okno od zera (mniej
                                    // czestа aktualizacja pitch, ale wystarczajaca dla wizualizacji).
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
            cleanupAudioResources();
            releaseWakeLock();
            releaseAudioFocus();
            releaseMediaSession();
            RecordingResultHolder.rejectStop("FAILED_TO_RECORD", e.getMessage());
            stopForegroundCompat();
            stopSelf();
        }
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
        updatePlaybackState(PlaybackState.STATE_PAUSED);
        updateNotification("Pauza");
    }

    private void handleResume() {
        if (!"PAUSED".equals(currentStatus)) return;
        paused = false;
        lastResumeAt = System.currentTimeMillis();
        currentStatus = "RECORDING";
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

            if (outputFile == null || !outputFile.exists() || pcmBytesWritten == 0L) {
                RecordingResultHolder.rejectStop("EMPTY_RECORDING", null);
            } else {
                long durationMs = System.currentTimeMillis() - recordingStartedAt - pausedAccumMs;
                byte[] bytes = readFileBytes(outputFile);
                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                RecordingResultHolder.resolveStop(base64, durationMs, "audio/wav");
                // UWAGA: NIE usuwamy pliku od razu — zostaje na dysku pod outputFile.getAbsolutePath(),
                // żeby MainActivity mogła go odtworzyć bez konieczności dekodowania base64 z powrotem.
            }
        } catch (Exception e) {
            RecordingResultHolder.rejectStop("FAILED_TO_FETCH_RECORDING", e.getMessage());
        } finally {
            currentStatus = "NONE";
            releaseWakeLock();
            releaseAudioFocus();
            releaseMediaSession();
            stopForegroundCompat();
            stopSelf();
        }
    }

    public File getLastOutputFile() {
        return outputFile;
    }

    private void cleanupAudioResources() {
        try { if (audioRecord != null) { audioRecord.stop(); audioRecord.release(); } } catch (Exception e) { }
        audioRecord = null;
        try {
            if (outputStream != null) {
                finalizeWavHeader(outputStream, pcmBytesWritten);
                outputStream.close();
            }
        } catch (Exception e) { }
        outputStream = null;
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
