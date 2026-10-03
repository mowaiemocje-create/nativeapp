package com.pitchrec.nativetest;

import com.pitchrec.backgroundrecorder.Mp3Stream;

import java.io.File;
import java.io.FileInputStream;

// OBROBKA NAGRANIA ROZMOWY PO ROZLACZENIU (jak w studiu radiowym), na surowym dzwieku z rozmowy:
//  1) filtr gornoprzepustowy 80 Hz — usuwa dudnienie i stuki, glos zostaje naturalny,
//  2) WYROWNANIE (leveler) — powolne wzmocnienie liczone osobno dla kazdego fragmentu mowy:
//     cichy rozmowca dostaje duzo wiecej (do +30 dB), glosny wlasny glos mniej; cisza miedzy
//     slowami jest lekko sciszana (ekspander), zeby nie podbijac szumu,
//  3) KOMPRESJA dynamiki — wyrownuje glosne i ciche sylaby (prog -22 dBFS, 3:1, miekkie kolano),
//  4) NORMALIZACJA — srednia glosnosc mowy do -16 dBFS (wyraznie glosno, jak radio/podcast),
//  5) LIMITER z wyprzedzeniem 5 ms — szczyty nigdy ponad -1 dBFS, bez przesteru.
// Tresc nagrania sie nie zmienia — zmienia sie tylko glosnosc w czasie.
// Dziala strumieniowo (3 przejscia po pliku), wiec nawet godzinna rozmowa nie zajmuje duzo pamieci.
final class CallPost {

    static final int SR = 44100;
    private static final int F = 882;     // ramka 20 ms
    private static final int B = 220;     // blok kompresora 5 ms
    private static final int LOOK = 220;  // wyprzedzenie limitera 5 ms

    // tryb: "auto" (domyslnie), "strong" (mocniej), "off" (tylko podglosnienie do -1 dBFS)
    static final class Params {
        float target = -20f, maxGain = 30f, norm = -16f, ratio = 3f, thr = -22f;
        boolean level = true;
        static Params of(String mode) {
            Params p = new Params();
            if ("strong".equals(mode)) { p.target = -18f; p.maxGain = 36f; p.norm = -14f; p.ratio = 4f; p.thr = -24f; }
            else if ("off".equals(mode)) p.level = false;
            return p;
        }
    }

    // wynik (do diagnostyki w Ustawieniach)
    static String lastInfo = "";

    static boolean process(File pcm, File mp3, String mode) {
        Params P = Params.of(mode);
        try {
            long n = pcm.length() / 2;
            if (n < SR) return false;
            int frames = (int) (n / F) + 1;

            // ── PRZEJSCIE 1: poziom ramek 20 ms (po filtrze 80 Hz) ──
            float[] db = new float[frames];
            {
                Hpf h = new Hpf();
                double sum = 0; int cnt = 0, fi = 0;
                try (Pcm in = new Pcm(pcm)) {
                    for (long t = 0; t < n; t++) {
                        double x = h.f(in.next() / 32768.0);
                        sum += x * x;
                        if (++cnt == F) { db[fi++] = toDb(Math.sqrt(sum / cnt)); sum = 0; cnt = 0; }
                    }
                }
                if (cnt > 0) db[fi++] = toDb(Math.sqrt(sum / cnt));
                frames = fi;
            }
            // poziom szumu = 10. percentyl ramek; mowa = co najmniej 12 dB nad szumem
            float[] sorted = java.util.Arrays.copyOf(db, frames);
            java.util.Arrays.sort(sorted);
            float noise = sorted[Math.max(0, frames / 10)];
            float speech = Math.max(noise + 12f, -66f);

            // ── wzmocnienie WYROWNUJACE dla kazdej ramki ──
            float[] g = new float[frames];
            if (P.level) {
                float lev = -60f, cur = 0f;
                for (int i = 0; i < frames; i++) {
                    if (db[i] > speech) {
                        lev += (db[i] - lev) * (db[i] > lev ? 0.25f : 0.08f);
                        float want = Math.min(P.maxGain, Math.max(-6f, P.target - lev));
                        cur += (want - cur) * (want < cur ? 0.3f : 0.05f);
                    } else {
                        cur += (Math.min(cur, 12f) - cur) * 0.05f;   // w ciszy nie pompujemy szumu
                    }
                    float exp = Math.min(18f, Math.max(0f, speech - db[i])); // ekspander: cisza jeszcze ciszej
                    g[i] = cur - exp;
                }
                // wygladzenie (5 ramek = 100 ms), zeby nie bylo slychac zmian
                float[] s = new float[frames];
                for (int i = 0; i < frames; i++) {
                    float a = 0; int k = 0;
                    for (int j = Math.max(0, i - 2); j <= Math.min(frames - 1, i + 2); j++) { a += g[j]; k++; }
                    s[i] = a / k;
                }
                g = s;
            }

            // ── PRZEJSCIE 2: glosnosc mowy po wyrownaniu i kompresji -> wzmocnienie normalizacji ──
            double normGain;
            {
                Chain c = new Chain(P, g);
                double sum = 0; long cnt = 0; double fsum = 0; int fc = 0, fi = 0; double peak = 0;
                try (Pcm in = new Pcm(pcm)) {
                    for (long t = 0; t < n; t++) {
                        double y = c.step(in.next() / 32768.0, t);
                        fsum += y * y;
                        double ay = Math.abs(y); if (ay > peak) peak = ay;
                        if (++fc == F) {
                            if (fi < frames && db[fi] > speech) { sum += fsum; cnt += fc; }
                            fi++; fsum = 0; fc = 0;
                        }
                    }
                }
                if (P.level && cnt > 0) {
                    double rmsDb = toDb(Math.sqrt(sum / cnt));
                    normGain = Math.pow(10, (P.norm - rmsDb) / 20.0);
                } else {
                    normGain = peak > 1e-6 ? Math.pow(10, -1 / 20.0) / peak : 1.0; // "tylko 0 dB"
                }
                normGain = Math.min(normGain, 1000.0);
            }

            // ── PRZEJSCIE 3: wszystko + limiter -> MP3 ──
            Chain c = new Chain(P, g);
            Limiter lim = new Limiter();
            Mp3Stream out = new Mp3Stream(mp3, SR);
            short[] buf = new short[4096];
            int bn = 0;
            int outPeak = 0;
            try (Pcm in = new Pcm(pcm)) {
                for (long t = 0; t < n + LOOK; t++) {
                    double y = t < n ? c.step(in.next() / 32768.0, t) * normGain : 0.0;
                    double o = lim.step(y);
                    if (t >= LOOK) {
                        int v = (int) Math.round(Math.max(-1.0, Math.min(1.0, o)) * 32767.0);
                        if (Math.abs(v) > outPeak) outPeak = Math.abs(v);
                        buf[bn++] = (short) v;
                        if (bn == buf.length) { out.feed(buf, bn); bn = 0; }
                    }
                }
            }
            if (bn > 0) out.feed(buf, bn);
            File done = out.finish();
            lastInfo = String.format(java.util.Locale.US, "szum %.0f dB · mowa od %.0f dB · +%.0f dB · szczyt %.1f dBFS",
                    noise, speech, 20 * Math.log10(normGain), toDb(outPeak / 32768.0));
            return done != null;
        } catch (Exception e) {
            lastInfo = "obróbka: " + e.getMessage();
            return false;
        }
    }

    // Szybki odczyt surowego PCM 16-bit little-endian (blokami po 64 KB)
    static final class Pcm implements AutoCloseable {
        final FileInputStream in;
        final byte[] b = new byte[1 << 16];
        int len = 0, pos = 0;
        Pcm(File f) throws Exception { in = new FileInputStream(f); }
        int next() throws Exception {
            if (pos + 1 >= len) {
                int keep = len - pos;
                if (keep > 0) b[0] = b[pos];
                int r = in.read(b, keep, b.length - keep);
                len = keep + Math.max(0, r); pos = 0;
                if (len < 2) return 0;   // koniec pliku — cisza
            }
            int v = (short) ((b[pos + 1] << 8) | (b[pos] & 0xff));
            pos += 2;
            return v;
        }
        public void close() { try { in.close(); } catch (Exception e) { } }
    }

    static float toDb(double v) { return (float) (20 * Math.log10(Math.max(v, 1e-9))); }

    // Filtr gornoprzepustowy Butterwortha 2. rzedu, 80 Hz
    static final class Hpf {
        final double b0, b1, b2, a1, a2;
        double x1, x2, y1, y2;
        Hpf() {
            double w = 2 * Math.PI * 80 / SR, cs = Math.cos(w), al = Math.sin(w) / (2 * Math.sqrt(0.5));
            double a0 = 1 + al;
            b0 = (1 + cs) / 2 / a0; b1 = -(1 + cs) / a0; b2 = (1 + cs) / 2 / a0;
            a1 = -2 * cs / a0; a2 = (1 - al) / a0;
        }
        double f(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x; y2 = y1; y1 = y;
            return y;
        }
    }

    // Filtr + wyrownanie + kompresor (deterministyczne — przejscia 2 i 3 licza to samo)
    static final class Chain {
        final Params P; final float[] g;
        final Hpf hpf = new Hpf();
        // kompresor blokowy 5 ms
        final double[] blk = new double[B];
        int bi = 0;
        double env = -90, grPrev = 0, grCur = 0;
        final double relC = 1 - Math.exp(-B / (0.12 * SR));
        final double[] pend = new double[B]; // probki czekajace na wzmocnienie bloku
        Chain(Params p, float[] g) { P = p; this.g = g; }

        double levelGain(long t) {
            if (!P.level || g.length == 0) return 1.0;
            double fpos = (double) t / F - 0.5;
            int i0 = (int) Math.floor(fpos);
            double fr = fpos - i0;
            int a = Math.max(0, Math.min(g.length - 1, i0)), b = Math.max(0, Math.min(g.length - 1, i0 + 1));
            double gd = g[a] + (g[b] - g[a]) * fr;
            return Math.pow(10, gd / 20.0);
        }

        double lgA = 1, lgB = 1;
        double grPrevLin = 1, grCurLin = 1;

        // Zwraca probke z opoznieniem 0 (kompresor liczy wzmocnienie z poprzedniego bloku,
        // przejscie miedzy blokami liniowe — bez trzaskow). Wzmocnienia liczone co 64 probki
        // i interpolowane (szybko — bez potegowania na kazdej probce).
        double step(double x, long t) {
            int k = (int) (t & 63);
            if (k == 0) { lgA = levelGain(t); lgB = levelGain(t + 64); }
            double y = hpf.f(x) * (lgA + (lgB - lgA) * k / 64.0);
            if (!P.level) return y;
            blk[bi] = y;
            double out = y * (grPrevLin + (grCurLin - grPrevLin) * bi / B);
            if (++bi == B) {
                double s = 0;
                for (double v : blk) s += v * v;
                double r = toDb(Math.sqrt(s / B));
                env = r > env ? r : env + (r - env) * relC;
                double over = env - P.thr, knee = 6, red;
                if (over <= -knee / 2) red = 0;
                else if (over < knee / 2) red = (1 / P.ratio - 1) * (over + knee / 2) * (over + knee / 2) / (2 * knee);
                else red = (1 / P.ratio - 1) * over;
                grPrev = grCur; grCur = red; bi = 0;
                grPrevLin = grCurLin; grCurLin = Math.pow(10, red / 20.0);
            }
            return out;
        }
    }

    // Limiter z wyprzedzeniem: sciszanie narasta plynnie i siega celu, zanim szczyt wyjdzie
    // z opoznienia — wynik nigdy nie przekracza -1 dBFS
    static final class Limiter {
        final double ceil = Math.pow(10, -1 / 20.0);
        final double rel = 1 - Math.exp(-1.0 / (0.08 * SR));
        final double[] delay = new double[LOOK + 1];
        final long[] dqI = new long[LOOK + 2];
        final double[] dqV = new double[LOOK + 2];
        int h = 0, tl = 0;
        final double[] hold = new double[LOOK];
        double holdSum = LOOK, env = 1;
        long t = 0;
        Limiter() { java.util.Arrays.fill(hold, 1.0); }

        double step(double x) {
            int slot = (int) (t % (LOOK + 1));
            double ax = Math.abs(x), rg = ax > ceil ? ceil / ax : 1.0;
            delay[slot] = x;
            int cap = LOOK + 2;
            while (tl != h && dqV[(tl - 1 + cap) % cap] >= rg) tl = (tl - 1 + cap) % cap;
            dqI[tl] = t; dqV[tl] = rg; tl = (tl + 1) % cap;
            while (dqI[h] < t - LOOK) h = (h + 1) % cap;
            double need = dqV[h];
            int hs = (int) (t % LOOK);
            holdSum += need - hold[hs]; hold[hs] = need;
            double avg = holdSum / LOOK;
            env = avg < env ? avg : env + (avg - env) * rel;
            double o = t >= LOOK ? delay[(int) ((t - LOOK) % (LOOK + 1))] * env : 0.0;
            t++;
            return o;
        }
    }
}
