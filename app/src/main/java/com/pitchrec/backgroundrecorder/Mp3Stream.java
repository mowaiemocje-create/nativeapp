package com.pitchrec.backgroundrecorder;

import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.MPEGMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// MP3 KODOWANE NA BIEZACO, W TRAKCIE NAGRYWANIA (osobny watek, nie blokuje odczytu z mikrofonu).
// Po STOP zostaje tylko domkniecie pliku — zapis jest natychmiastowy, jak w PWA.
public final class Mp3Stream {

    public final File file;
    private final LameEncoder enc;
    private final FileOutputStream out;
    private final byte[] obuf;
    private final int pcmMax;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private volatile boolean failed = false;

    public Mp3Stream(File file, int sampleRate) throws Exception {
        this.file = file;
        javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(sampleRate, 16, 1, true, false);
        // jakosc 7 + 128 kbps mono — dla mowy brzmi tak samo jak najwyzsza, a koduje sie wielokrotnie szybciej
        enc = new LameEncoder(fmt, 128, MPEGMode.MONO, 7, false);
        pcmMax = Math.max(1024, enc.getPCMBufferSize()) & ~1;
        obuf = new byte[Math.max(enc.getPCMBufferSize(), 16384) * 2];
        out = new FileOutputStream(file);
    }

    public void feed(short[] s, int n) {
        if (failed || n <= 0) return;
        final byte[] b = new byte[n * 2];
        for (int i = 0; i < n; i++) { b[2 * i] = (byte) (s[i] & 0xff); b[2 * i + 1] = (byte) ((s[i] >> 8) & 0xff); }
        try {
            ex.execute(() -> {
                if (failed) return;
                try {
                    for (int off = 0; off < b.length; off += pcmMax) {
                        int l = Math.min(pcmMax, b.length - off);
                        int k = enc.encodeBuffer(b, off, l, obuf);
                        if (k > 0) out.write(obuf, 0, k);
                    }
                } catch (Exception e) { failed = true; }
            });
        } catch (Exception e) { failed = true; }
    }

    // Domyka plik (czeka na zakodowanie reszty). Zwraca gotowy plik albo null przy bledzie.
    public File finish() {
        try {
            ex.execute(() -> {
                try {
                    int k = enc.encodeFinish(obuf);
                    if (k > 0) out.write(obuf, 0, k);
                } catch (Exception e) { failed = true; }
                try { out.close(); } catch (Exception e) { failed = true; }
                try { enc.close(); } catch (Exception e) { }
            });
            ex.shutdown();
            if (!ex.awaitTermination(120, TimeUnit.SECONDS)) failed = true;
        } catch (Exception e) { failed = true; }
        if (failed) { file.delete(); return null; }
        return file;
    }

    public void abort() {
        failed = true;
        ex.shutdownNow();
        try { out.close(); } catch (Exception e) { }
        file.delete();
    }
}
