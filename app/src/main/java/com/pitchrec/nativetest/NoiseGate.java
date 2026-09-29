package com.pitchrec.nativetest;

// BRAMKA SZUMOW z automatycznym rozpoznaniem szumu tla.
//
// Stara bramka zerowala caly bufor, gdy byl cichszy niz staly prog 0,02 — ucinala ciche
// poczatki/konce slow i rwala mowe (za czula), a prog nie zalezal od otoczenia.
//
// Teraz:
//  1. Przez pierwsze ~0,75 s nagrania bramka NIC nie wycisza, tylko mierzy szum tla (20. centyl
//     glosnosci okienek 10 ms — dziala, nawet gdy ktos od razu zaczyna mowic).
//  2. Potem otwiera sie, gdy dzwiek jest X razy glosniejszy od szumu (X = sila bramki 1..5),
//     a zamyka dopiero po 250 ms ciszy (nie ucina koncowek slow). Histereza: zamyka ponizej 70% progu.
//  3. Nie zeruje, tylko sciszana o 24 dB z plynnym wejsciem (3 ms) i wyjsciem (120 ms) —
//     brzmi naturalnie, bez "dziur" i trzaskow.
//  4. Szum tla jest sledzony dalej (np. wejscie do glosniejszego miejsca) — prog sam sie dopasowuje.
//  5. Decyzja dla okienka zapada PRZED jego przetworzeniem (bez opoznienia na poczatku slowa).
public final class NoiseGate {

    // 1 = delikatna … 5 = mocna: ile razy mowa musi byc glosniejsza od szumu tla
    public static final float[] FACTOR = {1.6f, 2.2f, 3.0f, 4.2f, 6.0f};
    public static volatile int strength = 2;          // domyslnie delikatnie (1..5)
    public static volatile float noiseRms = 0f;       // zmierzony szum tla (0..1), 0 = jeszcze nie
    public static volatile boolean learning = true;

    private static final float FLOOR = 0.063f;        // -24 dB
    private static final int LEARN_FRAMES = 60;       // 0,6 s "prawdziwych" okienek 10 ms
    private static final int SKIP_FRAMES = 15;        // pierwsze 150 ms pomijamy (mikrofon sie "budzi")
    private static final int HOLD_FRAMES = 25;        // 250 ms

    private final int frameLen;
    private final float attackC, releaseC;
    private final float[] learn = new float[LEARN_FRAMES];
    private int learnN = 0;
    private final float[] ring = new float[80];       // ostatnie 0,8 s (do wykrycia glosniejszego otoczenia)
    private int skipped = 0;
    private int ringN = 0, ringPos = 0, sinceCheck = 0;
    private boolean open = true;
    private int hold = 0;
    private float gain = 1f;

    public NoiseGate(int sampleRate) {
        frameLen = Math.max(64, sampleRate / 100);
        attackC = (float) (1 - Math.exp(-1.0 / (0.003 * sampleRate)));
        releaseC = (float) (1 - Math.exp(-1.0 / (0.120 * sampleRate)));
        noiseRms = 0f;
        learning = true;
    }

    public static float dbOf(float rms) {
        return rms <= 1e-6f ? -120f : (float) (20 * Math.log10(rms));
    }

    // Przetwarza bufor w miejscu (probki po wzmocnieniu MIC)
    public void process(short[] buf, int n) {
        for (int off = 0; off < n; off += frameLen) {
            int len = Math.min(frameLen, n - off);
            double s = 0;
            for (int i = off; i < off + len; i++) { double v = buf[i] / 32768.0; s += v * v; }
            float rms = (float) Math.sqrt(s / len);
            decide(rms);
            float target = open ? 1f : FLOOR;
            for (int i = off; i < off + len; i++) {
                gain += (target - gain) * (target > gain ? attackC : releaseC);
                buf[i] = (short) Math.round(buf[i] * gain);
            }
        }
    }

    private void decide(float rms) {
        if (learning) {
            open = true;
            // start mikrofonu: pierwsze okienka bywaja cyfrowa cisza/rozbiegiem — zanizalyby szum
            // (wtedy bramka nic nie tlumila i dopiero po ~10 s "doganiala" prawdziwy szum)
            if (skipped < SKIP_FRAMES || rms < 3e-5f) { skipped++; if (skipped < 200) return; }
            learn[learnN++] = rms;
            if (learnN >= LEARN_FRAMES) {
                float[] c = learn.clone();
                java.util.Arrays.sort(c);
                noiseRms = Math.max(1e-4f, c[LEARN_FRAMES / 5]);
                learning = false;
            }
            return;
        }
        float noise = noiseRms;
        int st = Math.max(1, Math.min(5, strength));
        float thrOpen = noise * FACTOR[st - 1], thrClose = thrOpen * 0.7f;
        if (rms > thrOpen) { open = true; hold = HOLD_FRAMES; }
        else if (open) {
            if (rms < thrClose) { if (--hold <= 0) open = false; }
            else hold = HOLD_FRAMES;
        }
        // sledzenie szumu: w ciszy powoli w strone biezacego poziomu (szybciej w dol)
        if (!open && rms < thrOpen) noise += (rms - noise) * (rms < noise ? 0.10f : 0.02f);
        // otoczenie stalo sie glosniejsze na dluzej (minimum z 3 s duzo ponad szumem) — podnies szum
        ring[ringPos] = rms; ringPos = (ringPos + 1) % ring.length; if (ringN < ring.length) ringN++;
        if (++sinceCheck >= 10 && ringN == ring.length) {
            sinceCheck = 0;
            float mn = Float.MAX_VALUE;
            for (float v : ring) if (v < mn) mn = v;
            // szum wyraznie wzrosl (np. mikrofon sam podkrecil czulosc) — od razu do nowego poziomu
            if (mn > noise * 1.5f) noise = mn * 0.9f;
        }
        noiseRms = Math.max(1e-4f, noise);
    }
}
