package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

// CAŁKOWITA CISZA W NAGRANIU (mikrofon "zamilkł" w trakcie — np. przejęty przez inna aplikacje,
// rozmowe, blad telefonu). To NIE jest zwykla pauza w mowie: szum pomieszczenia (nawet po bramce
// szumow) jest wyraznie glosniejszy. Szukamy najdluzszego odcinka ponizej ok. -85 dBFS.
// Nagranie z taka cisza >= 30 s nie jest wysylane do NS (trener dostalby "puste" nagranie).
// Wynik zapamietywany per plik (rozmiar), zeby ponowna wysylka nie analizowala od nowa.
public final class MuteCheck {

    private MuteCheck() { }

    public static final double LIMIT_S = 30.0;
    private static final double THR = 5.6e-5;   // ok. -85 dBFS (RMS okna 50 ms)

    public static final class Res {
        public double longest;   // najdluzsza calkowita cisza (s)
        public double at;        // od ktorej sekundy nagrania
        public boolean bad() { return longest >= LIMIT_S; }
    }

    // biezacy analizator (okna 50 ms)
    private static final class Scan {
        final int win; int n = 0; double sum = 0, pos = 0, run = 0, runStart = 0; final Res r = new Res(); final double winSec;
        Scan(int sr) { win = Math.max(1, sr / 20); winSec = win / (double) sr; }
        void put(short s) {
            double v = s / 32768.0; sum += v * v;
            if (++n == win) {
                double rms = Math.sqrt(sum / win);
                if (rms < THR) { if (run == 0) runStart = pos; run += winSec; if (run > r.longest) { r.longest = run; r.at = runStart; } }
                else run = 0;
                pos += winSec; n = 0; sum = 0;
            }
        }
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("mute_check", Context.MODE_PRIVATE); }

    public static Res check(Context c, File f, UploadShrink.Prog p) {
        String key = f.getName() + "|" + f.length();
        if (c != null && prefs(c).contains(key + "|l")) {
            Res r = new Res();
            r.longest = prefs(c).getFloat(key + "|l", 0f);
            r.at = prefs(c).getFloat(key + "|a", 0f);
            return r;
        }
        Res r = null;
        try { r = f.getName().toLowerCase(Locale.ROOT).endsWith(".wav") ? scanWav(f, p) : scanCodec(f, p); } catch (Throwable e) { r = null; }
        if (r == null) { r = new Res(); return r; } // nie udalo sie sprawdzic — nie blokujemy
        if (c != null) prefs(c).edit().putFloat(key + "|l", (float) r.longest).putFloat(key + "|a", (float) r.at).apply();
        return r;
    }

    public static String describe(Res r) {
        int at = (int) Math.round(r.at);
        return L.f("W nagraniu jest {0} s całkowitej ciszy (od {1}) — mikrofon przestał nagrywać. Nagranie NIE zostało wysłane — nagraj je ponownie.",
                (int) Math.round(r.longest), String.format(Locale.US, "%d:%02d", at / 60, at % 60));
    }

    private static Res scanWav(File f, UploadShrink.Prog p) throws Exception {
        int ch, sr; long dataLen;
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            byte[] h = new byte[44]; r.readFully(h);
            ch = Math.max(1, (h[22] & 0xff) | ((h[23] & 0xff) << 8));
            sr = (h[24] & 0xff) | ((h[25] & 0xff) << 8) | ((h[26] & 0xff) << 16) | ((h[27] & 0xff) << 24);
            if (sr <= 0) sr = LiveAudioData.SAMPLE_RATE;
            dataLen = r.length() - 44;
        }
        Scan s = new Scan(sr);
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(f), 1 << 16)) {
            long sk = 0; while (sk < 44) { long k = is.skip(44 - sk); if (k <= 0) break; sk += k; }
            byte[] b = new byte[ch * 2 * 4096]; int have = 0, k; long read = 0;
            while ((k = is.read(b, have, b.length - have)) > 0) {
                have += k; read += k;
                int frames = have / (2 * ch);
                for (int i = 0; i < frames; i++) {
                    int sum = 0;
                    for (int c = 0; c < ch; c++) { int q = 2 * (i * ch + c); sum += (short) ((b[q] & 0xff) | (b[q + 1] << 8)); }
                    s.put((short) (sum / ch));
                }
                int used = frames * 2 * ch; System.arraycopy(b, used, b, 0, have - used); have -= used;
                if (p != null) p.at(read / (double) Math.max(1, dataLen));
            }
        }
        return s.r;
    }

    private static Res scanCodec(File f, UploadShrink.Prog p) throws Exception {
        android.media.MediaExtractor ex = new android.media.MediaExtractor();
        android.media.MediaCodec codec = null;
        try {
            ex.setDataSource(f.getAbsolutePath());
            android.media.MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                android.media.MediaFormat t = ex.getTrackFormat(i);
                String m = t.getString(android.media.MediaFormat.KEY_MIME);
                if (m != null && m.startsWith("audio/")) { ex.selectTrack(i); fmt = t; break; }
            }
            if (fmt == null) return null;
            int ch = fmt.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT) ? Math.max(1, fmt.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)) : 1;
            int sr = fmt.containsKey(android.media.MediaFormat.KEY_SAMPLE_RATE) ? fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) : LiveAudioData.SAMPLE_RATE;
            long durUs = fmt.containsKey(android.media.MediaFormat.KEY_DURATION) ? fmt.getLong(android.media.MediaFormat.KEY_DURATION) : 0L;
            Scan s = new Scan(sr);
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
                    else { long t = ex.getSampleTime(); codec.queueInputBuffer(ii, 0, sz, t, 0); ex.advance(); if (p != null && durUs > 0) p.at(t / (double) durUs); }
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
                        ob.position(info.offset); ob.get(tmp, 0, info.size);
                        int frames = info.size / (2 * ch);
                        for (int i = 0; i < frames; i++) {
                            int sum = 0;
                            for (int c = 0; c < ch; c++) { int q = 2 * (i * ch + c); sum += (short) ((tmp[q] & 0xff) | (tmp[q + 1] << 8)); }
                            s.put((short) (sum / ch));
                        }
                    }
                    codec.releaseOutputBuffer(oi, false);
                    if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) { outDone = true; break; }
                    oi = codec.dequeueOutputBuffer(info, 0);
                }
            }
            return s.r;
        } finally {
            if (codec != null) { try { codec.stop(); } catch (Exception e) { } try { codec.release(); } catch (Exception e) { } }
            try { ex.release(); } catch (Exception e) { }
        }
    }
}
