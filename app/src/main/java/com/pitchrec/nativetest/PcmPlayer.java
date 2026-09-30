package com.pitchrec.nativetest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.io.File;
import java.io.RandomAccessFile;

// ODSLUCH W PAUZIE NAGRYWANIA — prosto z nagrywanego pliku WAV (16 bit, mono, 44,1 kHz),
// bez kopiowania calego pliku i bez MediaPlayera. Pozycja odtwarzania jest DOKLADNA (licznik
// probek AudioTracka), wiec biala linia i przewijanie wykresu ida rowno z dzwiekiem.
public final class PcmPlayer {

    public interface Done { void done(); }

    private final File file;
    private final long startSample;
    private volatile boolean stopped = false;
    private volatile boolean finished = false;
    private AudioTrack track;
    private Thread thread;
    private long endSample;

    public PcmPlayer(File wav, long startSample) {
        this.file = wav;
        this.startSample = Math.max(0, startSample);
    }

    public long totalSamples() { return endSample; }

    public void start(Done onDone) throws Exception {
        long dataBytes = Math.max(0, file.length() - 44);
        endSample = dataBytes / 2;
        final long from = Math.min(startSample, endSample);
        int sr = LiveAudioData.SAMPLE_RATE;
        int min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(min * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        track.play();
        final AudioTrack t = track;
        thread = new Thread(() -> {
            try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
                in.seek(44 + from * 2);
                byte[] b = new byte[8192];
                long left = (endSample - from) * 2;
                while (!stopped && left > 0) {
                    int n = in.read(b, 0, (int) Math.min(b.length, left));
                    if (n <= 0) break;
                    int off = 0;
                    while (!stopped && off < n) {
                        int w = t.write(b, off, n - off);
                        if (w < 0) { stopped = true; break; }
                        off += w;
                    }
                    left -= n;
                }
                // poczekaj, az bufor sie odegra
                long total = endSample - from;
                while (!stopped && (t.getPlaybackHeadPosition() & 0xffffffffL) < total) Thread.sleep(20);
            } catch (Exception e) { /* koniec */ }
            finished = true;
            if (!stopped && onDone != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone::done);
        }, "pcm-preview");
        thread.start();
    }

    public boolean isPlaying() {
        try { return track != null && !finished && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING; } catch (Exception e) { return false; }
    }

    public void pause() { try { if (track != null) track.pause(); } catch (Exception e) { } }

    public void resume() { try { if (track != null && !finished) track.play(); } catch (Exception e) { } }

    // aktualna pozycja w probkach NAGRANIA
    public long positionSample() {
        try { return Math.min(endSample, startSample + (track.getPlaybackHeadPosition() & 0xffffffffL)); }
        catch (Exception e) { return startSample; }
    }

    public void release() {
        stopped = true;
        try { if (track != null) { track.pause(); track.flush(); track.release(); } } catch (Exception e) { }
        track = null;
    }
}
