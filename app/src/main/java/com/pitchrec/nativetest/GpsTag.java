package com.pitchrec.nativetest;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// GPS W METADANYCH PLIKU MP3 (znacznik ID3v2.3, ramka TXXX "GPS" = "szer,dlug").
// Kazde nagranie MP3 zaczyna sie od tego znacznika z pustym miejscem na pozycje; pozycja z czasu
// nagrania jest wpisywana w to miejsce (bez przepisywania pliku). Pozycja jedzie razem z plikiem
// (wysylka do NS, udostepnienie, wczytanie na innym telefonie) — przy opisie odczytujemy ja stad,
// nawet gdy kursant opisuje nagranie z miasta bedac w domu.
public final class GpsTag {

    private GpsTag() { }

    private static final int VALUE_LEN = 40;                    // miejsce na "szer,dlug"
    private static final byte[] DESC = "GPS".getBytes(StandardCharsets.ISO_8859_1);

    // Znacznik z pustym miejscem — zapisywany na poczatku kazdego MP3 (Mp3Stream)
    public static byte[] placeholder() {
        int frameBody = 1 + DESC.length + 1 + VALUE_LEN;          // kodowanie + opis + 0 + wartosc
        int frames = 10 + frameBody;
        byte[] b = new byte[10 + frames];
        b[0] = 'I'; b[1] = 'D'; b[2] = '3'; b[3] = 3; b[4] = 0; b[5] = 0;
        // rozmiar (syncsafe, 4 x 7 bitow)
        b[6] = (byte) ((frames >> 21) & 0x7F); b[7] = (byte) ((frames >> 14) & 0x7F);
        b[8] = (byte) ((frames >> 7) & 0x7F); b[9] = (byte) (frames & 0x7F);
        int o = 10;
        b[o++] = 'T'; b[o++] = 'X'; b[o++] = 'X'; b[o++] = 'X';
        b[o++] = (byte) (frameBody >> 24); b[o++] = (byte) (frameBody >> 16); b[o++] = (byte) (frameBody >> 8); b[o++] = (byte) frameBody;
        b[o++] = 0; b[o++] = 0;                                  // flagi
        b[o++] = 0;                                              // ISO-8859-1
        System.arraycopy(DESC, 0, b, o, DESC.length); o += DESC.length;
        b[o++] = 0;
        for (int i = 0; i < VALUE_LEN; i++) b[o++] = 0;
        return b;
    }

    // pozycja wartosci "GPS" w pliku albo -1
    private static long valueOffset(RandomAccessFile r) throws Exception {
        byte[] h = new byte[10];
        r.seek(0);
        if (r.read(h) != 10 || h[0] != 'I' || h[1] != 'D' || h[2] != '3') return -1;
        int size = ((h[6] & 0x7F) << 21) | ((h[7] & 0x7F) << 14) | ((h[8] & 0x7F) << 7) | (h[9] & 0x7F);
        long pos = 10, end = 10L + size;
        byte[] fh = new byte[10];
        while (pos + 10 <= end) {
            r.seek(pos);
            if (r.read(fh) != 10 || fh[0] == 0) return -1;
            int fs = ((fh[4] & 0xFF) << 24) | ((fh[5] & 0xFF) << 16) | ((fh[6] & 0xFF) << 8) | (fh[7] & 0xFF);
            if (fs <= 0 || pos + 10 + fs > end) return -1;
            if (fh[0] == 'T' && fh[1] == 'X' && fh[2] == 'X' && fh[3] == 'X' && fs >= 1 + DESC.length + 1) {
                byte[] d = new byte[1 + DESC.length + 1];
                r.read(d);
                boolean gps = d[0] == 0 && d[d.length - 1] == 0;
                for (int i = 0; gps && i < DESC.length; i++) gps = d[1 + i] == DESC[i];
                if (gps) return pos + 10 + d.length;
            }
            pos += 10 + fs;
        }
        return -1;
    }

    // wpisuje pozycje w przygotowane miejsce (tylko pliki MP3 z naszym znacznikiem)
    public static boolean write(File f, double lat, double lon) {
        if (f == null || !f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") || Double.isNaN(lat) || Double.isNaN(lon)) return false;
        try (RandomAccessFile r = new RandomAccessFile(f, "rw")) {
            long off = valueOffset(r);
            if (off < 0) return false;
            String v = String.format(Locale.US, "%.5f,%.5f", lat, lon);
            byte[] val = new byte[VALUE_LEN];
            java.util.Arrays.fill(val, (byte) 0);
            byte[] vb = v.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(vb, 0, val, 0, Math.min(vb.length, VALUE_LEN));
            r.seek(off);
            r.write(val);
            return true;
        } catch (Exception e) { return false; }
    }

    // {szer, dlug} z metadanych pliku albo null
    public static double[] read(File f) {
        if (f == null || !f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3")) return null;
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            long off = valueOffset(r);
            if (off < 0) return null;
            byte[] val = new byte[VALUE_LEN];
            r.seek(off);
            r.read(val);
            return parse(new String(val, StandardCharsets.ISO_8859_1).trim());
        } catch (Exception e) { return null; }
    }

    private static final Pattern NAME_GPS = Pattern.compile("_GPS(-?\\d{1,3}\\.\\d+)_(-?\\d{1,3}\\.\\d+)");

    // {szer, dlug} z nazwy pliku ("..._GPS50.34812_18.91573...") albo null
    public static double[] fromName(String name) {
        if (name == null) return null;
        Matcher m = NAME_GPS.matcher(name);
        if (!m.find()) return null;
        try { return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))}; } catch (Exception e) { return null; }
    }

    // najpierw metadane, potem nazwa pliku
    public static double[] find(File f) {
        double[] g = read(f);
        return g != null ? g : (f == null ? null : fromName(f.getName()));
    }

    private static double[] parse(String v) {
        if (v == null || v.isEmpty()) return null;
        String[] p = v.split(",");
        if (p.length != 2) return null;
        try {
            double la = Double.parseDouble(p[0].trim()), lo = Double.parseDouble(p[1].trim());
            if (Math.abs(la) > 90 || Math.abs(lo) > 180) return null;
            return new double[]{la, lo};
        } catch (Exception e) { return null; }
    }
}
