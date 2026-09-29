package com.pitchrec.nativetest;

// BRAMKA SZUMOW z automatycznym rozpoznaniem szumu tla.
//
// Stara bramka zerowala caly bufor, gdy byl cichszy niz staly prog 0,02 — ucinala ciche
// poczatki/konce slow i rwala mowe (za czula), a prog nie zalezal od otoczenia.
//
// Teraz:
//  1. Przez pierwsze ~0,5 s nagrania bramka NIC nie wycisza, tylko mierzy szum tla (30. centyl
//     glosnosci okienek 10 ms — dziala, nawet gdy ktos od razu zaczyna mowic).
//  2. Potem otwiera sie, gdy dzwiek jest o X dB glosniejszy od TYPOWEGO szumu (X = sila 1..5),
//     a zamyka po 250 ms bez glosnego dzwieku (nie ucina koncowek slow).
//  3. Nie zeruje, tylko sciszana o 24 dB z plynnym wejsciem (3 ms) i wyjsciem (120 ms) —
//     brzmi naturalnie, bez "dziur" i trzaskow.
//  4. Szum tla jest sledzony dalej (np. wejscie do glosniejszego miejsca) — prog sam sie dopasowuje.
//  5. Decyzja dla okienka zapada PRZED jego przetworzeniem (bez opoznienia na poczatku slowa).
public final class NoiseGate {

    // Sila 1..5 = o ile dB dzwiek musi przewyzszac TYPOWY szum tla, zeby bramka sie otworzyla
    public static final float[] MARGIN_DB = {5f, 7f, 9f, 12f, 15f};
    public static volatile int strength = 2;          // domyslnie lekka (1..5)
    public static volatile float noiseRms = 0f;       // typowy szum tla (0..1), 0 = jeszcze nie
    public static volatile boolean learning = true;

    private static final float FLOOR = 0.063f;        // -24 dB
    private static final int LEARN_FRAMES = 40;       // 0,4 s "prawdziwych" okienek 10 ms
    private static final int SKIP_FRAMES = 10;        // pierwsze 100 ms pomijamy (mikrofon sie "budzi")
    private static final int HOLD_FRAMES = 25;        // 250 ms po ostatnim glosnym okienku

    private final int frameLen;
    private final float attackC, releaseC;
    private final float[] learn = new float[LEARN_FRAMES];
    private int learnN = 0, skipped = 0;
    private final float[] ring = new float[100];      // ostatnia 1 s (w dB) — wykrycie glosniejszego otoczenia
    private int ringN = 0, ringPos = 0, sinceCheck = 0;
    private float noiseDb = -90f;                     // TYPOWY poziom szumu (jak mediana), w dB
    private boolean open = true;
    private int hold = 0;
    private float gain = 1f;

    public NoiseGate(int sampleRate) {
        frameLen = Math.max(64, sampleRate / 100);
        attackC = (float) (1 - Math.exp(-1.0 / (0.003 * sampleRate)));
        releaseC = (float) (1 - Math.exp(-1.0 / (0.100 * sampleRate)));
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
            decide(dbOf((float) Math.sqrt(s / len)));
            float target = open ? 1f : FLOOR;
            for (int i = off; i < off + len; i++) {
                gain += (target - gain) * (target > gain ? attackC : releaseC);
                buf[i] = (short) Math.round(buf[i] * gain);
            }
        }
    }

    // Wszystko w dB. Szum = TYPOWY poziom (sledzony jak mediana), nie najcichsze momenty —
    // wczesniej prog liczony od najcichszych okienek byl ponizej zwyklych wahan szumu, bramka
    // "wisiala" otwarta i zamykala sie dopiero po kilku sekundach (tez po kazdej pauzie).
    private void decide(float db) {
        if (learning) {
            open = true;
            if ((skipped < SKIP_FRAMES || db < -90f) && skipped < 200) { skipped++; return; }
            learn[learnN++] = db;
            if (learnN >= LEARN_FRAMES) {
                float[] c = learn.clone();
                java.util.Arrays.sort(c);
                noiseDb = c[(int) (LEARN_FRAMES * 0.3f)]; // 30. centyl — odporny na mowe od pierwszej chwili
                learning = false;
                noiseRms = (float) Math.pow(10, noiseDb / 20);
            }
            return;
        }
        int st = Math.max(1, Math.min(5, strength));
        float thrOpen = noiseDb + MARGIN_DB[st - 1];
        if (db > thrOpen) { open = true; hold = HOLD_FRAMES; }
        else if (open && --hold <= 0) open = false;   // cichsze niz prog przez 250 ms -> zamknij

        // Sledzenie typowego szumu (tylko okienka ponizej progu = nie mowa) — biezacy ~60. centyl:
        // krok w gore 0,15 dB, w dol 0,1 dB (duzo cichsze okienko: 1 dB — szybka korekta, gdy
        // pomiar startowy trafil na mowe).
        if (db < thrOpen) {
            if (db < noiseDb - 6f) noiseDb -= 1f;
            else if (db < noiseDb) noiseDb -= 0.1f;
            else noiseDb += 0.15f;
        }
        // Otoczenie wyraznie glosniejsze na dluzej (nawet najcichsze okienko z 1 s jest 4 dB ponad
        // szumem) — przeskok od razu, bez czekania.
        ring[ringPos] = db; ringPos = (ringPos + 1) % ring.length; if (ringN < ring.length) ringN++;
        if (++sinceCheck >= 10 && ringN == ring.length) {
            sinceCheck = 0;
            float mn = Float.MAX_VALUE;
            for (float v : ring) if (v < mn) mn = v;
            if (mn > noiseDb + 4f) noiseDb = mn + 1.5f;
        }
        noiseRms = (float) Math.pow(10, noiseDb / 20);
    }
}
