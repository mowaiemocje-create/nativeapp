package com.pitchrec.nativetest;

// Ciagla linia intonacji (pitch). Surowy YIN co ~46 ms czesto "gubi" pojedyncze okna
// (spolgloski, szept, chwilowy szum), a czasem myli oktawe — przez to linia sie rwala i
// skakala, a strzalki (liczone z tej linii) wychodzily przypadkowe. Tu:
//  • krotkie dziury w mowie (do ~0,3 s) sa zalatane interpolacja, dluzsze = przerwa,
//  • bledy oktawy (×2 / ×½) sa korygowane do biezacego poziomu glosu,
//  • pojedyncze "strzaly" (skok > 45%) sa ignorowane, dopoki sie nie potwierdza,
//  • mediana z 3 okien + lagodne wygladzanie (mniejsze opoznienie niz dawne 0,88/0,12).
public class PitchTracker {

    private static final int MAX_GAP_FRAMES = 6;      // ~0,28 s przy oknie 2048 probek
    private static final float MIN_RMS = 0.004f;

    private float smoothed = 0f;
    private float lastF = 0f;                 // ostatnia zaakceptowana wartosc (wygladzona)
    private long lastSample = -1;
    private final float[] med = new float[3];
    private int medN = 0;
    private final long[] gap = new long[MAX_GAP_FRAMES + 1];
    private int gapN = 0;
    private int outliers = 0;
    private float pendF = 0f;       // pierwsze okno nowego odcinka — czeka na potwierdzenie
    private long pendSample = -1;

    public interface Sink { void point(long sample, float freq); }

    public synchronized void reset() {
        smoothed = 0f; lastF = 0f; lastSample = -1; medN = 0; gapN = 0; outliers = 0; pendF = 0f; pendSample = -1;
    }

    // strict = wynik YIN z progiem 0,20; relaxed = wariant z globalnym minimum (lub -1)
    public synchronized void frame(long sample, float rms, float strict, float relaxed, Sink out) {
        float f = strict > 0 ? strict : relaxed;
        boolean valid = f > 70 && f < 1000 && rms > MIN_RMS;
        // Luzniejszy wynik przyjmujemy tylko w trakcie trwajacej wypowiedzi (nie startujemy od niego)
        if (valid && strict <= 0 && lastF <= 0) valid = false;

        // START ODCINKA dopiero po 2 zgodnych oknach (±20%) — pojedynczy blad oktawy na poczatku
        // wypowiedzi rysowal pionowa "szpilke" od gory wykresu w dol.
        if (lastF <= 0) {
            if (!valid) { pendF = 0f; return; }
            if (pendF > 0 && Math.abs(f - pendF) / pendF < 0.2f) {
                smoothed = pendF; lastF = pendF; medN = 0; med[medN++] = pendF; gapN = 0; outliers = 0;
                out.point(pendSample, pendF);
                pendF = 0f;
            } else { pendF = f; pendSample = sample; return; }
        }
        if (valid && lastF > 0) {
            float r = f / lastF;
            if (r > 1.8f && r < 2.25f) f /= 2f;          // blad oktawy w gore
            else if (r > 0.44f && r < 0.56f) f *= 2f;    // blad oktawy w dol
            r = f / lastF;
            if (r > 1.45f || r < 1f / 1.45f) {           // pojedynczy strzal
                outliers++;
                if (outliers < 3) valid = false;
                else {
                    // potwierdzony nowy poziom glosu — przerwa w linii zamiast pionowej kreski
                    out.point(sample, -1);
                    medN = 0; smoothed = 0f; gapN = 0; outliers = 0;
                }
            } else outliers = 0;
        }

        if (!valid) {
            if (lastF > 0) {
                if (gapN < gap.length) gap[gapN++] = sample;
                if (gapN > MAX_GAP_FRAMES) endRun(out);
            }
            return;
        }

        // mediana z 3
        if (medN < 3) med[medN++] = f;
        else { med[0] = med[1]; med[1] = med[2]; med[2] = f; }
        float m = f;
        if (medN == 3) {
            float a = med[0], b = med[1], c = med[2];
            m = Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
        }
        smoothed = smoothed > 0 ? smoothed * 0.6f + m * 0.4f : m;

        // zalatanie krotkiej dziury: liniowo od ostatniej wartosci do obecnej
        if (gapN > 0 && lastF > 0) {
            for (int i = 0; i < gapN; i++) {
                float k = (i + 1f) / (gapN + 1f);
                out.point(gap[i], lastF + (smoothed - lastF) * k);
            }
        }
        gapN = 0;
        lastF = smoothed;
        lastSample = sample;
        out.point(sample, smoothed);
    }

    private void endRun(Sink out) {
        out.point(gap[0], -1);
        gapN = 0; lastF = 0f; smoothed = 0f; medN = 0; outliers = 0; pendF = 0f;
    }

    // Koniec nagrania / pliku — domkniecie otwartej przerwy
    public synchronized void flush(Sink out) {
        if (gapN > 0) endRun(out);
    }
}
