package com.pitchrec.nativetest;

import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.MPEGMode;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

// DLUGIE NAGRANIA a LIMIT SERWERA NS: serwer odrzuca pliki powyzej ok. 50-75 MB (413),
// a 80 min w 128 kbps to ok. 77 MB. Przed wysylka plik wiekszy niz LIMIT jest przekodowywany
// do MP3 o nizszym bitrate (mowa w 64 kbps mono brzmi praktycznie tak samo). Dekodowanie
// i kodowanie ida STRUMIENIEM, kawalek po kawalku — bez wczytywania calego nagrania do pamieci.
// Oryginal w telefonie zostaje bez zmian; plik tymczasowy jest kasowany po wysylce.
public final class UploadShrink {

    public static final long LIMIT = 45L * 1024 * 1024;   // bezpiecznie ponizej limitu serwera
    private static final long TARGET = 40L * 1024 * 1024; // docelowa wielkosc po przekodowaniu
    private static final int[] RATES = {112, 96, 80, 64, 56, 48, 40, 32};

    private UploadShrink() { }

    public interface Prog { void at(double f); }

    public static boolean needed(File f) { return f != null && f.length() > LIMIT; }

    // Plik do wyslania: oryginal, jesli jest maly; inaczej przekodowana kopia (ta sama nazwa,
    // rozszerzenie .mp3) w katalogu tymczasowym. Przy bledzie przekodowania zwraca oryginal.
    public static File forUpload(File f) { return forUpload(f, null); }

    public static File forUpload(File f, Prog p) {
        if (!needed(f)) return f;
        File dir = new File(System.getProperty("java.io.tmpdir", "/data/local/tmp"), "ns_upload");
        dir.mkdirs();
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        File out = new File(dir, (dot > 0 ? name.substring(0, dot) : name) + ".mp3");
        try {
            if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) wavToMp3(f, out, p);
            else mp3ToMp3(f, out, p);
            if (out.length() > 0 && out.length() < f.length()) return out;
        } catch (Throwable e) { /* np. brak pamieci / zly plik — wysylamy oryginal */ }
        out.delete();
        return f;
    }

    public static void cleanup(File original, File sent) {
        if (sent != null && sent != original && !sent.equals(original)) sent.delete();
    }

    static int pickKbps(double durSec) {
        if (durSec <= 0) return 64;
        double max = TARGET * 8.0 / durSec / 1000.0;
        for (int r : RATES) if (r <= max) return r;
        return 32;
    }

    // ── Koder: przyjmuje PCM 16-bit mono porcjami ──
    private static final class Enc {
        final LameEncoder lame;
        final FileOutputStream os;
        final byte[] pcm, mp3;
        int n = 0;
        Enc(File out, int sr, int kbps) throws Exception {
            javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(sr, 16, 1, true, false);
            lame = new LameEncoder(fmt, kbps, MPEGMode.MONO, 7, false);
            int chunk = Math.max(2048, lame.getPCMBufferSize()) & ~1;
            pcm = new byte[chunk];
            mp3 = new byte[Math.max(lame.getPCMBufferSize(), 16384) * 2];
            os = new FileOutputStream(out);
        }
        void put(short s) throws IOException {
            pcm[n++] = (byte) (s & 0xff);
            pcm[n++] = (byte) ((s >> 8) & 0xff);
            if (n == pcm.length) flush();
        }
        void flush() throws IOException {
            if (n > 0) { int k = lame.encodeBuffer(pcm, 0, n, mp3); if (k > 0) os.write(mp3, 0, k); n = 0; }
        }
        void finish() throws IOException {
            try {
                flush();
                int k = lame.encodeFinish(mp3);
                if (k > 0) os.write(mp3, 0, k);
            } finally {
                try { os.close(); } catch (Exception e) { }
                try { lame.close(); } catch (Exception e) { }
            }
        }
    }

    // WAV (16-bit PCM) → MP3
    static void wavToMp3(File in, File out, Prog p) throws Exception {
        int ch, sr, dataOff = 44;
        long dataLen;
        try (RandomAccessFile r = new RandomAccessFile(in, "r")) {
            byte[] h = new byte[44];
            r.readFully(h);
            ch = Math.max(1, (h[22] & 0xff) | ((h[23] & 0xff) << 8));
            sr = (h[24] & 0xff) | ((h[25] & 0xff) << 8) | ((h[26] & 0xff) << 16) | ((h[27] & 0xff) << 24);
            if (sr <= 0) sr = LiveAudioData.SAMPLE_RATE;
            dataLen = r.length() - dataOff;
        }
        double dur = dataLen / (2.0 * ch * sr);
        Enc enc = new Enc(out, sr, pickKbps(dur));
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(in), 1 << 16)) {
            long skipped = 0;
            while (skipped < dataOff) { long k = is.skip(dataOff - skipped); if (k <= 0) break; skipped += k; }
            byte[] b = new byte[ch * 2 * 4096];
            int have = 0, k;
            long read = 0;
            while ((k = is.read(b, have, b.length - have)) > 0) {
                have += k;
                read += k;
                if (p != null) p.at(read / (double) Math.max(1, dataLen));
                int frames = have / (2 * ch);
                for (int i = 0; i < frames; i++) {
                    int sum = 0;
                    for (int c = 0; c < ch; c++) { int q = 2 * (i * ch + c); sum += (short) ((b[q] & 0xff) | (b[q + 1] << 8)); }
                    enc.put((short) (sum / ch));
                }
                int used = frames * 2 * ch;
                System.arraycopy(b, used, b, 0, have - used);
                have -= used;
            }
        } finally { enc.finish(); }
    }

    // MP3 (lub inny plik audio czytany przez Androida) → MP3 o nizszym bitrate
    static void mp3ToMp3(File in, File out, Prog p) throws Exception {
        android.media.MediaExtractor ex = new android.media.MediaExtractor();
        android.media.MediaCodec codec = null;
        Enc enc = null;
        try {
            ex.setDataSource(in.getAbsolutePath());
            android.media.MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                android.media.MediaFormat f = ex.getTrackFormat(i);
                String m = f.getString(android.media.MediaFormat.KEY_MIME);
                if (m != null && m.startsWith("audio/")) { ex.selectTrack(i); fmt = f; break; }
            }
            if (fmt == null) throw new IOException("brak audio");
            int ch = fmt.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT) ? Math.max(1, fmt.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)) : 1;
            int sr = fmt.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE) ? fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) : LiveAudioData.SAMPLE_RATE;
            long durUs = fmt.containsKey(android.media.MediaFormat.KEY_DURATION) ? fmt.getLong(android.media.MediaFormat.KEY_DURATION) : 0L;
            // dlugosc nieznana → szacunek z wielkosci pliku (nasze nagrania to 128 kbps)
            double dur = durUs > 0 ? durUs / 1e6 : in.length() * 8.0 / 128000.0;
            enc = new Enc(out, sr, pickKbps(dur));

            codec = android.media.MediaCodec.createDecoderByType(fmt.getString(android.media.MediaFormat.KEY_MIME));
            codec.configure(fmt, null, null, 0);
            codec.start();
            android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
            byte[] tmp = new byte[0];
            boolean inDone = false, outDone = false;
            while (!outDone) {
                boolean progressed = false;
                while (!inDone) {
                    int ii = codec.dequeueInputBuffer(0);
                    if (ii < 0) break;
                    progressed = true;
                    java.nio.ByteBuffer ib = codec.getInputBuffer(ii);
                    int sz = ib != null ? ex.readSampleData(ib, 0) : -1;
                    if (sz < 0) { codec.queueInputBuffer(ii, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true; }
                    else {
                        long t = ex.getSampleTime();
                        codec.queueInputBuffer(ii, 0, sz, t, 0);
                        ex.advance();
                        if (p != null && durUs > 0) p.at(t / (double) durUs);
                    }
                }
                int oi = codec.dequeueOutputBuffer(info, progressed ? 0 : 2000);
                while (oi >= 0 || oi == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (oi == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        try { ch = Math.max(1, codec.getOutputFormat().getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)); } catch (Exception e) { }
                        oi = codec.dequeueOutputBuffer(info, 0);
                        continue;
                    }
                    java.nio.ByteBuffer ob = codec.getOutputBuffer(oi);
                    if (ob != null && info.size > 0) {
                        if (tmp.length < info.size) tmp = new byte[info.size];
                        ob.position(info.offset);
                        ob.get(tmp, 0, info.size);
                        int frames = info.size / (2 * ch);
                        for (int i = 0; i < frames; i++) {
                            int sum = 0;
                            for (int c = 0; c < ch; c++) { int q = 2 * (i * ch + c); sum += (short) ((tmp[q] & 0xff) | (tmp[q + 1] << 8)); }
                            enc.put((short) (sum / ch));
                        }
                    }
                    codec.releaseOutputBuffer(oi, false);
                    if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) { outDone = true; break; }
                    oi = codec.dequeueOutputBuffer(info, 0);
                }
            }
        } finally {
            if (enc != null) enc.finish();
            if (codec != null) { try { codec.stop(); } catch (Exception e) { } try { codec.release(); } catch (Exception e) { } }
            try { ex.release(); } catch (Exception e) { }
        }
    }
}
