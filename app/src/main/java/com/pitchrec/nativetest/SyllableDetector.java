package com.pitchrec.nativetest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// LICZENIE SYLAB w porcji mowy (metoda "jader sylab" — de Jong & Wempe 2009, uproszczona):
//  • obwiednia glosnosci (co ~5,8 ms) wygladzona ~50 ms i zamieniona na dB,
//  • kandydat na sylabe = lokalne maksimum ponizej ktorego jest wyrazny "dolek" (spolgloska)
//    — sasiednie maksima bez dolka >= 2 dB sa laczone w jedna sylabe,
//  • maksimum musi byc glosne (nie dalej niz 20 dB od najglosniejszego fragmentu porcji)
//    i DZWIECZNE (w poblizu jest ton krtaniowy z linii pitch) — szum i stuki odpadaja,
//  • granice sylab = najcichsze miejsca miedzy jadrami.
// Wynik: lista sylab {start, jadro, koniec} [s]. Tempo = sylaby / czas porcji * 60 [syl/min].
public class SyllableDetector {

    public static class Syl {
        public final double start, nucleus, end;
        public Syl(double s, double n, double e) { start = s; nucleus = n; end = e; }
    }

    private static final int CHUNK = 256;
    public static double MIN_DIP_DB = 2.0;
    public static double MIN_GAP_S = 0.075;
    public static double RANGE_DB = 20.0;
    public static int HALF = 4;

    // env: obwiednia (max |probka| w oknach 256 probek), env[0] = probka envStartSample.
    // voiced: czasy [s] punktow z tonem krtaniowym (posortowane); null = bez sprawdzania.
    public static List<Syl> detect(float[] env, long envStartSample, int sr, double segStart, double segEnd, double[] voiced) {
        List<Syl> out = new ArrayList<>();
        double hop = CHUNK / (double) sr;
        int a = (int) Math.max(0, Math.floor((segStart * sr - envStartSample) / CHUNK));
        int b = (int) Math.min(env.length - 1, Math.ceil((segEnd * sr - envStartSample) / CHUNK));
        if (b - a < 10) return out;
        int n = b - a + 1;
        // wygladzenie ~50 ms (srednia ruchoma) + dB
        int half = HALF;
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0; int c = 0;
            for (int j = -half; j <= half; j++) {
                int k = a + i + j;
                if (k >= 0 && k < env.length) { sum += env[k]; c++; }
            }
            s[i] = 20 * Math.log10(Math.max(1e-5, sum / Math.max(1, c)));
        }
        double[] sorted = s.clone();
        Arrays.sort(sorted);
        double top = sorted[(int) Math.min(n - 1, Math.floor(n * 0.95))];
        double thr = top - RANGE_DB;

        // lokalne maksima
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < n - 1; i++) {
            if (s[i] > thr && s[i] >= s[i - 1] && s[i] > s[i + 1]) peaks.add(i);
        }
        // laczenie maksimow bez wyraznego dolka / zbyt bliskich
        List<Integer> kept = new ArrayList<>();
        for (int p : peaks) {
            if (kept.isEmpty()) { kept.add(p); continue; }
            int q = kept.get(kept.size() - 1);
            double dip = Double.MAX_VALUE;
            for (int i = q; i <= p; i++) dip = Math.min(dip, s[i]);
            boolean separate = (Math.min(s[p], s[q]) - dip) >= MIN_DIP_DB && (p - q) * hop >= MIN_GAP_S;
            if (separate) kept.add(p);
            else if (s[p] > s[q]) kept.set(kept.size() - 1, p);
        }
        // tylko dzwieczne jadra
        List<Integer> nuc = new ArrayList<>();
        for (int p : kept) {
            double t = (envStartSample + (long) (a + p) * CHUNK + CHUNK / 2.0) / sr;
            if (voiced == null || nearVoiced(voiced, t, 0.07)) nuc.add(p);
        }
        // granice = najcichsze miejsca miedzy jadrami
        double prev = segStart;
        for (int k = 0; k < nuc.size(); k++) {
            int p = nuc.get(k);
            double end;
            if (k + 1 < nuc.size()) {
                int q = nuc.get(k + 1), mi = p;
                for (int i = p; i <= q; i++) if (s[i] < s[mi]) mi = i;
                end = (envStartSample + (long) (a + mi) * CHUNK) / (double) sr;
            } else end = segEnd;
            double tn = (envStartSample + (long) (a + p) * CHUNK + CHUNK / 2.0) / sr;
            out.add(new Syl(prev, tn, end));
            prev = end;
        }
        return out;
    }

    private static boolean nearVoiced(double[] v, double t, double tol) {
        int lo = 0, hi = v.length - 1;
        while (lo <= hi) {
            int m = (lo + hi) >>> 1;
            if (v[m] < t - tol) lo = m + 1;
            else if (v[m] > t + tol) hi = m - 1;
            else return true;
        }
        return false;
    }
}
