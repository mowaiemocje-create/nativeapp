package com.pitchrec.backgroundrecorder;

import java.io.File;
import java.io.RandomAccessFile;

// GLOSNOSC GOTOWEGO MP3 BEZ PONOWNEGO KODOWANIA (zasada jak w mp3gain): kazda ramka MP3
// ma pole "global_gain" — zwiekszenie go o 1 = +1,5 dB. Zmieniamy tylko te bajty, wiec
// podglosnienie calego nagrania trwa ulamek sekundy i nie traci jakosci.
public final class Mp3Gain {

    private static final int[] BR_MPEG1_L3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    private static final int[] SR_MPEG1 = {44100, 48000, 32000, 0};

    private Mp3Gain() { }

    // steps: ile krokow po 1,5 dB (np. 4 = +6 dB). Zwraca liczbe zmienionych ramek.
    public static int apply(File f, int steps) {
        if (steps == 0) return 0;
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            long len = raf.length();
            if (len < 8 || len > Integer.MAX_VALUE) return 0;
            byte[] b = new byte[(int) len];
            raf.readFully(b);
            int n = b.length, i = 0, frames = 0;
            if (n > 10 && b[0] == 'I' && b[1] == 'D' && b[2] == '3') {
                int sz = ((b[6] & 0x7f) << 21) | ((b[7] & 0x7f) << 14) | ((b[8] & 0x7f) << 7) | (b[9] & 0x7f);
                i = 10 + sz;
            }
            while (i + 4 <= n) {
                if ((b[i] & 0xff) != 0xff || (b[i + 1] & 0xe0) != 0xe0) { i++; continue; }
                int ver = (b[i + 1] >> 3) & 3, layer = (b[i + 1] >> 1) & 3, prot = b[i + 1] & 1;
                int brI = (b[i + 2] >> 4) & 15, srI = (b[i + 2] >> 2) & 3, pad = (b[i + 2] >> 1) & 1;
                int mode = (b[i + 3] >> 6) & 3;
                if (ver != 3 || layer != 1 || BR_MPEG1_L3[brI] == 0 || SR_MPEG1[srI] == 0) { i++; continue; }
                int flen = 144 * BR_MPEG1_L3[brI] * 1000 / SR_MPEG1[srI] + pad;
                if (flen < 21 || i + flen > n) break;
                boolean mono = mode == 3;
                int side = i + 4 + (prot == 0 ? 2 : 0);
                int sideLen = mono ? 17 : 32;
                boolean infoTag = side + sideLen + 4 <= n && isTag(b, side + sideLen);
                if (prot == 1 && !infoTag) { // z suma CRC nie ruszamy (musialaby byc przeliczona)
                    int base = mono ? 18 : 20, ch = mono ? 1 : 2;
                    for (int gr = 0; gr < 2; gr++) {
                        for (int c = 0; c < ch; c++) {
                            int bit = side * 8 + base + (gr * ch + c) * 59 + 21;
                            int v = getBits(b, bit, 8) + steps;
                            putBits(b, bit, 8, Math.max(0, Math.min(255, v)));
                        }
                    }
                    frames++;
                }
                i += flen;
            }
            if (frames > 0) { raf.seek(0); raf.write(b); }
            return frames;
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean isTag(byte[] b, int p) {
        return (b[p] == 'X' && b[p + 1] == 'i' && b[p + 2] == 'n' && b[p + 3] == 'g')
                || (b[p] == 'I' && b[p + 1] == 'n' && b[p + 2] == 'f' && b[p + 3] == 'o');
    }

    private static int getBits(byte[] b, int bit, int cnt) {
        int v = 0;
        for (int k = 0; k < cnt; k++) {
            int p = bit + k;
            v = (v << 1) | ((b[p >> 3] >> (7 - (p & 7))) & 1);
        }
        return v;
    }

    private static void putBits(byte[] b, int bit, int cnt, int v) {
        for (int k = 0; k < cnt; k++) {
            int p = bit + k;
            int bitv = (v >> (cnt - 1 - k)) & 1;
            if (bitv == 1) b[p >> 3] |= (byte) (1 << (7 - (p & 7)));
            else b[p >> 3] &= (byte) ~(1 << (7 - (p & 7)));
        }
    }
}
