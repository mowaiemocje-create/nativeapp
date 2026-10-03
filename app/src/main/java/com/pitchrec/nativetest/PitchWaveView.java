package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Locale;

// Podwojne buforowanie (technika potwierdzona w RecForge — Bitmap L rysowana od nowa TYLKO
// gdy dane sie zmienia, nie przy kazdej klatce). Wersja v2, znacznie bardziej defensywna niz
// poprzednia proba (ktora spowodowala zawieszenie) — kazdy krok zabezpieczony przed
// znanymi przyczynami zawieszen (zerowe wymiary, wyjatki, potencjalnie zdegenerowane
// wartosci w petlach).
public class PitchWaveView extends View {

    private static final float PMIN = 55f;
    private static final float PMAX = 1050f;
    private static final int RULER_HEIGHT_DP = 26;

    private final Paint bgPaint = new Paint();
    private final Paint envelopePaint = new Paint();
    private final Paint midlinePaint = new Paint();
    private final Paint pitchPaint = new Paint();
    private final Paint rulerBgPaint = new Paint();
    private final Paint rulerTickPaint = new Paint();
    private final Paint rulerTextPaint = new Paint();
    private final Paint playheadPaint = new Paint();
    private final Paint gridLinePaint = new Paint();
    private final Paint nowLinePaint = new Paint();
    private final Paint pauseFillPaint = new Paint();
    private final Paint pauseBorderPaint = new Paint();
    private final Paint pauseTextPaint = new Paint();
    private final Paint pauseLabelBgPaint = new Paint();
    private static final float PITCH_GAP_S = 0.2f;

    public interface OnSeekListener {
        void onSeek(long sampleIndex);
    }

    private OnSeekListener seekListener;

    private Bitmap cacheBitmap;
    private Canvas cacheCanvas;
    private long lastCachedTotalSamples = -1L;
    private long lastCachedPanOffset = -1L;
    private float lastCachedZoomSeconds = -999f;
    private boolean lastCachedLiveMode = true;
    private boolean cacheValid = false;
    private boolean forceNextRebuild = false;

    public PitchWaveView(Context context) {
        super(context);
        init();
    }

    public PitchWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        bgPaint.setColor(Color.parseColor("#1a1a1a"));
        envelopePaint.setColor(Color.parseColor("#00E000"));
        envelopePaint.setStrokeWidth(2f);
        envelopePaint.setAntiAlias(false);
        midlinePaint.setColor(Color.parseColor("#40FFFFFF"));
        midlinePaint.setStrokeWidth(1f);
        midlinePaint.setAntiAlias(false);
        pitchPaint.setColor(Color.parseColor("#FF3B30"));
        pitchPaint.setStrokeWidth(4f);
        pitchPaint.setStrokeJoin(Paint.Join.ROUND);
        pitchPaint.setStrokeCap(Paint.Cap.ROUND);
        pitchPaint.setStyle(Paint.Style.STROKE);
        pitchPaint.setAntiAlias(true);

        rulerBgPaint.setColor(Color.parseColor("#08081a"));
        rulerTickPaint.setColor(Color.parseColor("#7EC8E3"));
        rulerTickPaint.setStrokeWidth(2f);
        rulerTextPaint.setColor(Color.parseColor("#7EC8E3"));
        rulerTextPaint.setTextSize(dp(12));
        rulerTextPaint.setAntiAlias(true);
        rulerTextPaint.setFakeBoldText(true);

        gridLinePaint.setColor(Color.parseColor("#20FFFFFF"));
        gridLinePaint.setStrokeWidth(1f);
        gridLinePaint.setAntiAlias(false);

        pauseBorderPaint.setStyle(Paint.Style.STROKE);
        pauseBorderPaint.setStrokeWidth(dp(1.5f));
        pauseTextPaint.setTextSize(dp(12));
        pauseTextPaint.setFakeBoldText(true);
        pauseTextPaint.setAntiAlias(true);
        pauseLabelBgPaint.setColor(0x99000000);

        nowLinePaint.setColor(Color.parseColor("#66FFFFFF"));
        nowLinePaint.setStrokeWidth(dp(1.5f));
        nowLinePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(5), dp(4)}, 0));

        playheadPaint.setColor(Color.parseColor("#FFFFFF"));
        playheadPaint.setStrokeWidth(dp(2));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float zoomSeconds = 0f;
    private boolean isLiveMode = true;
    private long panOffsetSample = 0L;
    private long playheadSample = -1L;

    public void setZoomSeconds(float seconds) {
        zoomSeconds = seconds;
        invalidate();
    }

    public void setLiveMode(boolean live) {
        isLiveMode = live;
        if (live) { playheadSample = -1L; lastFollowSample = -1L; }
        forceNextRebuild = true; // wymuszamy natychmiastowa przebudowe (nie throttled) —
                                  // bez tego, lastVisibleSeconds/Start/Range mogly zostac
                                  // NIEAKTUALNE (z trybu live), psujac liczenie pozycji
                                  // dotyku zaraz po przelaczeniu na tryb statyczny.
        invalidate();
    }

    public void pauseKeepingPosition() {
        long totalSamples = LiveAudioData.getTotalSamplesWritten();
        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : 1000;
        long tailSamples = (long) tailCount * 256;
        // Jak w RecForge: biezaca chwila zostaje na SRODKU ekranu (tak samo jak podczas
        // nagrywania) — bez skoku widoku w momencie pauzy. Moze byc ujemne na poczatku.
        panOffsetSample = totalSamples - tailSamples / 2;
        isLiveMode = false;
        forceNextRebuild = true;
        invalidate();
    }

    // Wymusza natychmiastowe przerysowanie (np. po zmianie kolorow/grubosci w Ustawieniach,
    // gdy nic sie nie nagrywa i petla odswiezania nie dziala).
    public void refreshStyle() {
        forceNextRebuild = true;
        invalidate();
    }

    public void setOnSeekListener(OnSeekListener l) {
        seekListener = l;
    }

    private long lastTouchMs = 0L;
    private long lastFollowSample = -1L;

    public void setPlayheadSample(long sample) {
        if (isLiveMode) return; // podczas nagrywania nie ma odtwarzania
        // Podazanie za odtwarzaniem TYLKO gdy linia sie przesuwa (trwa odtwarzanie) i palec
        // nie przewijal przed chwila — wczesniej petla odtwarzacza (dzialajaca tez w pauzie)
        // co 50 ms "sciagala" widok z powrotem i nie dalo sie przewinac do poczatku.
        boolean moving = sample != lastFollowSample;
        lastFollowSample = sample;
        playheadSample = sample;
        if (!moving || System.currentTimeMillis() - lastTouchMs < 2500) { invalidate(); return; }
        // Auto-przewijanie do przodu podczas odtwarzania wczytanego pliku (tryb
        // statyczny) — bez tego, widok zostawal zablokowany na stalym oknie (np. 8s) i
        // playhead po prostu "wychodzil" poza widoczny zakres podczas dluzszego
        // odtwarzania, zamiast plynnie przewijac sie razem z odtwarzaniem.
        if (!isLiveMode && lastVisibleSampleRange > 0) {
            // Jak podczas nagrywania: suwak dochodzi do SRODKA ekranu, a potem wykres, siatka
            // i sekundy przesuwaja sie plynnie pod nim (wczesniej okno skakalo co cala strone).
            long half = (long) (lastVisibleSampleRange / 2f);
            if (sample > panOffsetSample + half) panOffsetSample = sample - half;
            else if (sample < panOffsetSample) panOffsetSample = Math.max(0, sample - half);
        }
        invalidate();
    }

    // start odtwarzania: widok od razu podaza za biala linia (bez czekania po dotknieciu)
    public void clearTouchHold() { lastTouchMs = 0L; }

    public void resetPan() {
        panOffsetSample = 0L;
        playheadSample = -1L;
        cacheValid = false;
    }

    // JEDNO wspolne okno widoku dla fali, pitch, siatki i podzialki — dzieki temu wszystko
    // zawsze pokrywa sie z sekundami (wczesniej podzialka w trybie live startowala od 0 i
    // "rosla" przez pierwsze sekundy, a fala miala stale okno — rozjezdzaly sie).
    // Tryb live: biezaca chwila na SRODKU ekranu (jak RecForge).
    private long windowStartSample(long totalSamples, long tailSamples) {
        return isLiveMode ? totalSamples - tailSamples / 2 : panOffsetSample;
    }

    private int tailCountFor(int w) {
        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : w;
        return tailCount <= 0 ? 1 : tailCount;
    }

    private float freqToY(float freq, int height) {
        double logF = Math.log(freq) / Math.log(2);
        double logMin = Math.log(PMIN) / Math.log(2);
        double logMax = Math.log(PMAX) / Math.log(2);
        return (float) (height * (1 - (logF - logMin) / (logMax - logMin)));
    }

    private float[] envelopeLinePts = new float[0];
    private final Path pitchPath = new Path();

    private float touchStartX = 0f;
    private float touchLastX = 0f;
    private boolean touchMoved = false;
    private static final float TAP_THRESHOLD_PX = 12f;
    private float lastVisibleSeconds = 1f;
    private long lastVisibleStartSample = 0L;
    private float lastVisibleSampleRange = 1f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLiveMode) return false;
        lastTouchMs = System.currentTimeMillis();
        // przewijanie w poziomie nie moze byc przejmowane przez przewijana strone
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(event.getAction() != MotionEvent.ACTION_UP && event.getAction() != MotionEvent.ACTION_CANCEL);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchLastX = touchStartX;
                touchMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - touchLastX;
                touchLastX = event.getX();
                if (Math.abs(event.getX() - touchStartX) > TAP_THRESHOLD_PX) touchMoved = true;

                float pxPerSecond = getWidth() / Math.max(0.001f, lastVisibleSeconds);
                long sampleDelta = (long) (-dx / pxPerSecond * LiveAudioData.SAMPLE_RATE);
                long newPan = panOffsetSample + sampleDelta;
                // OGRANICZENIE: nie pozwalamy przewijac dalej niz do konca faktycznie
                // nagranych/wczytanych danych — bez tego, mozna bylo przewijac w
                // nieskonczonosc nawet przy krotkim nagraniu.
                long totalSamples = LiveAudioData.getTotalSamplesWritten();
                long maxPan = Math.max(0, totalSamples - (long) (lastVisibleSeconds * 0.2f * LiveAudioData.SAMPLE_RATE));
                long minPan = -(long) (lastVisibleSampleRange / 2f);
                panOffsetSample = Math.max(minPan, Math.min(newPan, maxPan));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!touchMoved) {
                    float pxPerSecond = getWidth() / Math.max(0.001f, lastVisibleSeconds);
                    long tappedSample = Math.max(0, panOffsetSample + (long) (event.getX() / pxPerSecond * LiveAudioData.SAMPLE_RATE));
                    playheadSample = tappedSample;
                    if (seekListener != null) seekListener.onSeek(tappedSample);
                    invalidate();
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int fullH = getHeight();

        // ZABEZPIECZENIE: bez tego, Bitmap.createBitmap ponizej moglby otrzymac 0 albo
        // ujemny wymiar (np. przy pierwszym layout-pass, zanim widok ma realny rozmiar) —
        // co jest znanym powodem wyjatkow/zawieszen na niektorych urzadzeniach.
        if (w <= 0 || fullH <= 0) return;

        try {
            drawWithCache(canvas, w, fullH);
        } catch (Exception e) {
            // ZABEZPIECZENIE: jakikolwiek nieoczekiwany wyjatek w rysowaniu NIE MOZE
            // zablokowac calego wątku UI — lepiej pokazac jedna pusta klatke niz zawiesic
            // caly interfejs (to bylo prawdopodobne zrodlo poprzedniej regresji).
            cacheValid = false;
        }
    }

    private long lastRebuildTimeMs = 0L;
    private static final long MIN_REBUILD_INTERVAL_MS = 40; // ~25 odswiezen/sek dla KOSZTOWNEJ przebudowy

    private void drawWithCache(Canvas canvas, int w, int fullH) {
        if (cacheBitmap == null || cacheBitmap.getWidth() != w || cacheBitmap.getHeight() != fullH) {
            if (cacheBitmap != null) {
                try { cacheBitmap.recycle(); } catch (Exception e) { }
            }
            cacheBitmap = Bitmap.createBitmap(w, fullH, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas(cacheBitmap);
            cacheValid = false;
        }

        long totalSamples = LiveAudioData.getTotalSamplesWritten();
        long now = System.currentTimeMillis();
        boolean dataChanged = totalSamples != lastCachedTotalSamples
                || panOffsetSample != lastCachedPanOffset
                || zoomSeconds != lastCachedZoomSeconds
                || isLiveMode != lastCachedLiveMode;
        // KLUCZOWA POPRAWKA: nawet gdy dane sie zmienily, nie przebudowuj czesciej niz co
        // MIN_REBUILD_INTERVAL_MS — audio dopisuje nowe probki czesciej niz warto
        // przerysowywac caly wykres, wiec bez tego ograniczenia cache przebudowywal sie
        // praktycznie przy kazdej klatce, negujac wiekszosc korzysci buforowania.
        boolean needsRebuild = !cacheValid || forceNextRebuild
                || (dataChanged && (now - lastRebuildTimeMs >= MIN_REBUILD_INTERVAL_MS));

        if (needsRebuild) {
            rebuildCache(cacheCanvas, w, fullH);
            lastCachedTotalSamples = totalSamples;
            lastCachedPanOffset = panOffsetSample;
            lastCachedZoomSeconds = zoomSeconds;
            lastCachedLiveMode = isLiveMode;
            lastRebuildTimeMs = now;
            cacheValid = true;
            forceNextRebuild = false;
        }

        // KLUCZOWA POPRAWKA (na podstawie analizy nagrania): siatka/podzialka porusza sie
        // plynnie (co klatke), ale cachowana bitmapa fali stoi w miejscu miedzy
        // przebudowami (throttled, co 40ms) — to powodowalo, ze fala i siatka
        // "rozjezdzaly sie" wzgledem siebie, wygladajac jak wibrowanie. Przesuwamy teraz
        // cala bitmape o maly, plynny offset odpowiadajacy uplynetemu czasowi od
        // ostatniej przebudowy — bitmapa "jedzie" razem z siatka, nie stoi w miejscu.
        float offsetX = 0f;
        if (isLiveMode && lastVisibleSeconds > 0) {
            long elapsedMs = now - lastRebuildTimeMs;
            if (elapsedMs > 0 && elapsedMs < 500) {
                float pxPerMs = w / (lastVisibleSeconds * 1000f);
                offsetX = -(elapsedMs * pxPerMs);
            }
        }
        canvas.drawBitmap(cacheBitmap, offsetX, 0, null);

        // Siatka i podziałka rysowane co klatke (NIE throttled jak reszta wykresu) —
        // to lekkie do narysowania (kilka linii + kilka etykiet), a inaczej "zamierałyby"
        // na czas throttlingu (40ms) i skakały przy kazdej przebudowie cache, co bylo
        // duzo bardziej widoczne niz dla fali (prosta, precyzyjna linia/tekst reaguje
        // znacznie gorzej na dyskretne skoki niz organicznie "rosnaca" fala).
        drawLiveRulerAndGrid(canvas, w, fullH);
        drawLiveHints(canvas, w, dp(RULER_HEIGHT_DP));

        // Biala linia odtwarzania NIGDY podczas nagrywania na zywo (zostawala stara pozycja
        // z poprzedniego odsluchu/dotkniecia i przesuwala sie po ekranie razem z wykresem)
        if (!isLiveMode && playheadSample >= 0 && lastVisibleSampleRange > 0) {
            float rulerHeight = dp(RULER_HEIGHT_DP);
            float px = ((playheadSample - lastVisibleStartSample) / lastVisibleSampleRange) * w;
            if (px >= 0 && px <= w) {
                canvas.drawLine(px, rulerHeight, px, fullH, playheadPaint);
            }
        }
    }

    // Liczy AKTUALNY (nie throttled/cachowany) widoczny zakres i rysuje siatke+podzialke
    // NA WIERZCHU cachowanej bitmapy — dzieki temu porusza sie plynnie, niezaleznie od
    // throttlingu przebudowy samej fali/pitch.
    private void drawLiveRulerAndGrid(Canvas canvas, int w, int fullH) {
        float rulerHeight = dp(RULER_HEIGHT_DP);
        int h = (int) (fullH - rulerHeight);
        if (h <= 0) return;

        int tailCount = tailCountFor(w);
        long tailSamples = (long) tailCount * 256;
        long totalSamples = LiveAudioData.getExtrapolatedTotalSamples();
        long visibleStartSample = windowStartSample(totalSamples, tailSamples);
        float visibleSampleRange = tailSamples;

        // Zamalowujemy pas podzialki na wierzchu cache (tlo), zeby stare etykiety/linie z
        // poprzedniej klatki nie przebijaly przez nowe.
        canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);

        float visibleSeconds = Math.max(0.001f, visibleSampleRange / (float) LiveAudioData.SAMPLE_RATE);
        float pxPerSecond = w / visibleSeconds;
        if (pxPerSecond <= 0f || !Float.isFinite(pxPerSecond)) return;

        gridLinePaint.setColor(LiveAudioData.gridLineColor);
        gridLinePaint.setStrokeWidth(dp(LiveAudioData.gridLineWidthDp));

        // PIONOWE linie co 1 s. Przy duzym oddaleniu (linie gestsze niz ~6dp) rzadsze kroki,
        // zeby siatka nie zlala sie w jedna plame.
        float[] steps = {1f, 5f, 10f, 30f, 60f, 300f, 600f};
        float lineStep = 600f;
        for (float st : steps) { if (st * pxPerSecond >= dp(6)) { lineStep = st; break; } }
        // Opis sekund (na gorze) co tyle linii, zeby napisy sie nie nakladaly.
        int labelEvery = Math.max(1, (int) Math.ceil(dp(34) / (lineStep * pxPerSecond)));

        float startSecond = visibleStartSample / (float) LiveAudioData.SAMPLE_RATE;
        long firstIdx = (long) Math.ceil(startSecond / lineStep);
        int tickSafety = 0;
        for (long idx = firstIdx; tickSafety < 400; idx++, tickSafety++) {
            float sec = idx * lineStep;
            float x = (sec - startSecond) * pxPerSecond;
            if (x > w) break;
            canvas.drawLine(x, rulerHeight, x, fullH, gridLinePaint);
            if (sec >= 0 && idx % labelEvery == 0) {
                canvas.drawLine(x, rulerHeight - dp(6), x, rulerHeight, rulerTickPaint);
                String label = String.format(Locale.getDefault(), "%.0fs", sec);
                canvas.drawText(label, x + dp(2), rulerHeight - dp(7), rulerTextPaint);
            }
        }

        // Linia "teraz" na srodku ekranu podczas nagrywania (jak w RecForge)
        if (isLiveMode && LiveAudioData.isRecordingActive) {
            float nowX = ((totalSamples - visibleStartSample) / visibleSampleRange) * w;
            if (nowX >= 0 && nowX <= w) canvas.drawLine(nowX, rulerHeight, nowX, fullH, nowLinePaint);
        }
    }

    private void rebuildCache(Canvas canvas, int w, int fullH) {
        // Zastosuj ustawienia grubosci/koloru linii pitch (z ekranu Ustawien) — odczytywane
        // przy kazdej przebudowie cache, wiec zmiana w ustawieniach dziala automatycznie.
        pitchPaint.setColor(LiveAudioData.pitchLineColor);
        pitchPaint.setStrokeWidth(dp(LiveAudioData.pitchLineWidthDp));
        gridLinePaint.setColor(LiveAudioData.gridLineColor);
        gridLinePaint.setStrokeWidth(dp(LiveAudioData.gridLineWidthDp));

        float rulerHeight = dp(RULER_HEIGHT_DP);
        int h = (int) (fullH - rulerHeight);
        if (h <= 0) return; // zabezpieczenie — widok za maly, nic sensownego do rysowania
        int mid = h / 2;

        bgPaint.setColor(LiveAudioData.dawBackgroundColor);
        envelopePaint.setColor(LiveAudioData.waveColor);
        canvas.drawRect(0, 0, w, fullH, bgPaint);

        // POZIOME linie: glosnosc — 5 w gore i 5 w dol od srodka, rowne odstepy.
        // Ta sama grubosc i kolor co linie pionowe (jedna wspolna regulacja w Ustawieniach).
        float vGap = (mid * 0.92f) / 5f;
        for (int k = 1; k <= 5; k++) {
            float yu = rulerHeight + mid - k * vGap;
            float yd = rulerHeight + mid + k * vGap;
            canvas.drawLine(0, yu, w, yu, gridLinePaint);
            canvas.drawLine(0, yd, w, yd, gridLinePaint);
        }

        midlinePaint.setStrokeWidth(dp(LiveAudioData.gridLineWidthDp));
        canvas.drawLine(0, rulerHeight + mid, w, rulerHeight + mid, midlinePaint);

        int tailCount = tailCountFor(w);
        long tailSamplesW = (long) tailCount * 256;
        long totalNow = isLiveMode ? LiveAudioData.getExtrapolatedTotalSamples() : LiveAudioData.getTotalSamplesWritten();
        long windowStart = windowStartSample(totalNow, tailSamplesW);

        // Dane od poczatku okna (moze byc "przed" 0 s — wtedy od 0).
        LiveAudioData.FrameSnapshot snap = LiveAudioData.snapshotForDrawingAtSample(Math.max(0, windowStart), tailCount);
        float[] envelope = snap.envelope;

        if (envelope.length > 0) {
            long tailSamples = tailSamplesW;
            // WAZNE: okno docelowe (visibleStartSample/Range) jest ZAWSZE STALEJ
            // wielkosci (tailSamples), niezaleznie od tego, ile danych faktycznie juz
            // jest nagranych — visibleStartSample MOZE wyjsc "przed" poczatek nagrania
            // (ujemny wzgledem 0), co jest OK koncepcyjnie (po prostu jeszcze tam nie
            // ma zadnych probek). Bez tego, przy krotszym nagraniu niz cale okno, zakres
            // "rosl" przy kazdej przebudowie, przeliczajac WSZYSTKIE pozycje na nowo —
            // to bylo prawdziwe zrodlo skakania fali (nie tylko na starcie nagrania, ale
            // przy KAZDYM nagraniu krotszym niz pelne okno zoom).
            long visibleStartSample = windowStart;
            float visibleSampleRange = tailSamples;
            lastVisibleSeconds = visibleSampleRange / (float) LiveAudioData.SAMPLE_RATE;
            lastVisibleStartSample = visibleStartSample;
            lastVisibleSampleRange = visibleSampleRange;

            // Slupki fali — pozycjonowane wg RZECZYWISTEJ pozycji probki (tak jak linia
            // pitch), nie wg indeksu w tablicy — dzieki temu sa zawsze konsystentne z
            // pitch/podzialka, i nie "sciskaja/rozciagaja sie" gdy przybywa danych.
            int neededSize = envelope.length * 4;
            if (envelopeLinePts.length != neededSize) envelopeLinePts = new float[neededSize];
            int visiblePointCount = 0;
            for (int i = 0; i < envelope.length; i++) {
                long samplePos = snap.visibleStartSample + (long) i * 256;
                float x = ((samplePos - visibleStartSample) / visibleSampleRange) * w;
                if (x < -10 || x > w + 10) continue; // poza widocznym zakresem, pomijamy
                float barHeight = envelope[i] * mid;
                int base = visiblePointCount * 4;
                envelopeLinePts[base] = x;
                envelopeLinePts[base + 1] = rulerHeight + mid - barHeight;
                envelopeLinePts[base + 2] = x;
                envelopeLinePts[base + 3] = rulerHeight + mid + barHeight;
                visiblePointCount++;
            }
            canvas.drawLines(envelopeLinePts, 0, visiblePointCount * 4, envelopePaint);

            // PAUZY (jak w PitchRec): zielone = w prawidlowym zakresie, czerwone = za krotkie/za dlugie,
            // z dlugoscia pauzy na srodku. Granice liczone dokladnie przez VadDetector.
            List<LiveAudioData.Pause> pz = new java.util.ArrayList<>();
            double visA = visibleStartSample / (double) LiveAudioData.SAMPLE_RATE;
            double visB = (visibleStartSample + visibleSampleRange) / (double) LiveAudioData.SAMPLE_RATE;
            for (LiveAudioData.Pause pa : LiveAudioData.pausesSnapshot()) { if (pa.end >= visA && pa.start <= visB) pz.add(pa); }
            for (LiveAudioData.Pause pa : LiveAudioData.showPauses ? pz : new java.util.ArrayList<LiveAudioData.Pause>()) {
                float x0 = (float) ((pa.start * LiveAudioData.SAMPLE_RATE - visibleStartSample) / visibleSampleRange * w);
                float x1 = (float) ((pa.end * LiveAudioData.SAMPLE_RATE - visibleStartSample) / visibleSampleRange * w);
                if (x1 < 0 || x0 > w) continue;
                boolean ok = pa.ok();
                pauseFillPaint.setColor(ok ? 0x7300C86E : 0x73C83232);
                pauseBorderPaint.setColor(ok ? 0xB300FF8C : 0xB3FF5050);
                canvas.drawRect(x0, rulerHeight, x1, rulerHeight + h, pauseFillPaint);
                canvas.drawRect(x0, rulerHeight, x1, rulerHeight + h, pauseBorderPaint);
                String lbl = String.format(Locale.US, "%.2fs", pa.dur());
                float tw = pauseTextPaint.measureText(lbl);
                if (x1 - x0 > tw + dp(6)) {
                    float cx = (x0 + x1) / 2f, cy = rulerHeight + mid;
                    canvas.drawRect(cx - tw / 2 - dp(4), cy - dp(11), cx + tw / 2 + dp(4), cy + dp(6), pauseLabelBgPaint);
                    pauseTextPaint.setColor(ok ? 0xFF00FF8A : 0xFFFF6464);
                    canvas.drawText(lbl, cx - tw / 2, cy + dp(2), pauseTextPaint);
                }
            }

            List<LiveAudioData.PitchPoint> pitchPts = snap.pitchPoints;
            pitchPath.reset();
            boolean penDown = false;
            long lastPitchSample = Long.MIN_VALUE;
            long maxGap = (long) (PITCH_GAP_S * LiveAudioData.SAMPLE_RATE);

            for (LiveAudioData.PitchPoint p : pitchPts) {
                if (p.freq <= 0 || p.freq < PMIN || p.freq > PMAX) {
                    penDown = false;
                    continue;
                }
                // Przerwa w danych dluzsza niz 0,2 s = pauza — NIE laczymy linii przez nia.
                if (lastPitchSample != Long.MIN_VALUE && p.sampleIndex - lastPitchSample > maxGap) penDown = false;
                // W wykrytej pauzie nie rysujemy pitch (jak w PitchRec)
                double pt = p.sampleIndex / (double) LiveAudioData.SAMPLE_RATE;
                boolean inPause = false;
                for (LiveAudioData.Pause pa : pz) { if (pt >= pa.start && pt <= pa.end) { inPause = true; break; } }
                if (inPause) { penDown = false; lastPitchSample = p.sampleIndex; continue; }
                lastPitchSample = p.sampleIndex;
                float x = ((p.sampleIndex - visibleStartSample) / visibleSampleRange) * w;
                if (x < 0 || x > w) { penDown = false; continue; }
                float y = rulerHeight + freqToY(p.freq, h);
                if (!penDown) {
                    pitchPath.moveTo(x, y);
                    penDown = true;
                } else {
                    pitchPath.lineTo(x, y);
                }
            }
            canvas.drawPath(pitchPath, pitchPaint);

            if (LiveAudioData.normsOn() || LiveAudioData.showTempo) drawNormLabels(canvas, w, rulerHeight, visibleStartSample, visibleSampleRange);
            if (LiveAudioData.showTempo) drawSyllableTicks(canvas, w, h, rulerHeight, visibleStartSample, visibleSampleRange);
            if (LiveAudioData.arrowsOn()) drawArrows(canvas, w, h, rulerHeight, pitchPts, pz, visibleStartSample, visibleSampleRange);
        }
    }

    // ── STRZALKI TRENDU INTONACJI (port z PitchRec PWA) ──
    // Ciagle fragmenty linii pitch dzielone na okna ~3,5 s; w kazdym oknie regresja liniowa
    // f(t): wzrost > 3%/s = strzalka w gore (zielona), spadek = w dol (niebieska), inaczej plaska.
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // STRZALKI INTONACJI: po jednej na sylabe, zgodnie z linia pitch (zmiana tonu w POLTONACH,
    // regresja na skali logarytmicznej).
    //  • Sylaba 4-fazowa = JEDNA strzalka przez cala jej dlugosc (dwie 4-fazowe pod rzad = dwie).
    //  • Pozostale sylaby: w gore / w dol, gdy ton zmienia sie o wiecej niz prog z Ustawien
    //    (domyslnie 1,5 poltonu), inaczej strzalka PLASKA; sasiednie w tym samym kierunku sie lacza.
    //  • Im wieksza zmiana, tym bardziej stroma strzalka.
    private void drawArrows(Canvas canvas, int w, int h, float top, List<LiveAudioData.PitchPoint> pts,
                            List<LiveAudioData.Pause> pz, long visStart, float visRange) {
        double visA = visStart / (double) LiveAudioData.SAMPLE_RATE;
        double visB = (visStart + visRange) / (double) LiveAudioData.SAMPLE_RATE;
        List<SyllableDetector.Syl> syl = new java.util.ArrayList<>();
        for (SyllableDetector.Syl sy : LiveAudioData.syllablesSnapshot()) if (sy.end >= visA && sy.start <= visB) syl.add(sy);
        if (syl.isEmpty()) { drawArrowsWindows(canvas, w, h, top, pts, pz, visStart, visRange); return; }
        float thr = LiveAudioData.arrowThresholdSt;
        int n = syl.size();
        double[] ch = new double[n];
        boolean[] ok = new boolean[n];
        for (int i = 0; i < n; i++) {
            SyllableDetector.Syl sy = syl.get(i);
            List<LiveAudioData.PitchPoint> in = new java.util.ArrayList<>();
            for (LiveAudioData.PitchPoint p : pts) {
                if (p.freq <= 0) continue;
                double t = (p.sampleIndex + 1024) / (double) LiveAudioData.SAMPLE_RATE;
                if (t >= sy.start && t <= sy.end) in.add(p);
            }
            ok[i] = in.size() >= 2;
            ch[i] = ok[i] ? semitoneChange(in) : 0;
        }
        float arrowY = top + h * 0.82f;
        float y = arrowY;
        int i = 0;
        double prevEnd = -1;
        while (i < n) {
            if (!ok[i]) { i++; continue; }
            int d = ch[i] >= thr ? 1 : ch[i] <= -thr ? -1 : 0;
            int j = i;
            double total = ch[i];
            if (!syl.get(i).four) {
                while (j + 1 < n && ok[j + 1] && !syl.get(j + 1).four && syl.get(j + 1).start - syl.get(j).end < 0.05) {
                    double c = ch[j + 1];
                    int d2 = c >= thr ? 1 : c <= -thr ? -1 : 0;
                    if (d2 != d) break;
                    j++; total += c;
                }
            }
            double t0 = syl.get(i).start, t1 = syl.get(j).end;
            if (prevEnd < 0 || t0 - prevEnd > 0.3) y = arrowY; // nowa porcja mowy
            float x0 = (float) ((t0 * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w) + dp(2);
            float x1 = (float) ((t1 * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w) - dp(2);
            if (x1 - x0 >= dp(6) && x1 > 0 && x0 < w) {
                // stromosc = wielkosc zmiany (ok. 7 dp na poltonu, maks. 34 dp); plaska = pozioma
                float dy = d == 0 ? 0f : (float) Math.max(dp(6), Math.min(dp(34), Math.abs(total) * dp(7)));
                float ye = y + (d > 0 ? -dy : dy);
                drawOneArrow(canvas, x0, y, x1, ye, d, true);
                y = (ye < arrowY - dp(34) || ye > arrowY + dp(34)) ? arrowY : ye;
            }
            prevEnd = t1;
            i = j + 1;
        }
    }

    // Zmiana tonu w poltonach od poczatku do konca fragmentu (regresja na 12·log2 f)
    private static double semitoneChange(List<LiveAudioData.PitchPoint> pts) {
        int n = pts.size();
        double sx = 0, sy = 0, sxy = 0, sx2 = 0;
        for (LiveAudioData.PitchPoint p : pts) {
            double x = p.sampleIndex / (double) LiveAudioData.SAMPLE_RATE;
            double yv = 12 * Math.log(p.freq) / Math.log(2);
            sx += x; sy += yv; sxy += x * yv; sx2 += x * x;
        }
        double den = n * sx2 - sx * sx;
        double sl = den > 0 ? (n * sxy - sx * sy) / den : 0;
        double dur = (pts.get(n - 1).sampleIndex - pts.get(0).sampleIndex) / (double) LiveAudioData.SAMPLE_RATE;
        return sl * dur;
    }

    private void drawOneArrow(Canvas canvas, float x0, float y0, float x1, float y1, int dir, boolean dot) {
        int col = dir > 0 ? 0xFF00E5A0 : dir < 0 ? 0xFF00CFFF : 0xFF8888BB;
        float dx = x1 - x0;
        float thick = Math.max(dp(2.5f), Math.min(dp(5), dx / 30f));
        float head = Math.max(dp(7), Math.min(dp(14), dx / 4f));
        arrowPaint.setColor(col);
        arrowPaint.setAlpha(230);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(thick);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x0, y0, x1, y1, arrowPaint);
        double ga = Math.atan2(y1 - y0, x1 - x0), ha = Math.PI / 5;
        canvas.drawLine(x1, y1, (float) (x1 - Math.cos(ga - ha) * head), (float) (y1 - Math.sin(ga - ha) * head), arrowPaint);
        canvas.drawLine(x1, y1, (float) (x1 - Math.cos(ga + ha) * head), (float) (y1 - Math.sin(ga + ha) * head), arrowPaint);
        if (dot) {
            arrowPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x0, y0, thick * 0.9f, arrowPaint);
        }
    }

    // Zapasowo (gdy sylaby nie sa jeszcze policzone): okna ~3,5 s jak w PitchRec PWA
    private void drawArrowsWindows(Canvas canvas, int w, int h, float top, List<LiveAudioData.PitchPoint> pts,
                            List<LiveAudioData.Pause> pz, long visStart, float visRange) {
        List<List<LiveAudioData.PitchPoint>> segs = new java.util.ArrayList<>();
        List<LiveAudioData.PitchPoint> cur = new java.util.ArrayList<>();
        long last = Long.MIN_VALUE;
        long maxGap = (long) (PITCH_GAP_S * LiveAudioData.SAMPLE_RATE);
        for (LiveAudioData.PitchPoint p : pts) {
            double t = p.sampleIndex / (double) LiveAudioData.SAMPLE_RATE;
            boolean inPause = false;
            for (LiveAudioData.Pause pa : pz) if (t >= pa.start && t <= pa.end) { inPause = true; break; }
            boolean brk = p.freq <= 0 || p.freq < PMIN || p.freq > PMAX || inPause || (last != Long.MIN_VALUE && p.sampleIndex - last > maxGap);
            if (p.freq > 0) last = p.sampleIndex;
            if (brk) {
                if (cur.size() > 5) segs.add(cur);
                cur = new java.util.ArrayList<>();
                if (p.freq <= 0 || inPause) continue;
            }
            cur.add(p);
        }
        if (cur.size() > 5) segs.add(cur);

        float arrowY = top + h * 0.82f;
        double angle = Math.PI / 7;
        float secPx = w / (visRange / LiveAudioData.SAMPLE_RATE);
        for (List<LiveAudioData.PitchPoint> seg : segs) {
            List<LiveAudioData.PitchPoint> vis = new java.util.ArrayList<>();
            for (LiveAudioData.PitchPoint p : seg) {
                float x = ((p.sampleIndex - visStart) / visRange) * w;
                if (x >= 0 && x <= w) vis.add(p);
            }
            if (vis.size() < 3) continue;
            float xs = ((vis.get(0).sampleIndex - visStart) / visRange) * w;
            float xe = ((vis.get(vis.size() - 1).sampleIndex - visStart) / visRange) * w;
            if (xe - xs < dp(8)) continue;
            int chunks = Math.max(1, (int) Math.ceil((xe - xs) / (1.0f * secPx)));
            int per = Math.max(1, vis.size() / chunks);
            float[] cx = new float[chunks + 1];
            cx[0] = xs + dp(4);
            for (int c = 0; c < chunks; c++) cx[c + 1] = Math.min(xe - dp(4), xs + dp(4) + (c + 1) * (xe - xs - dp(8)) / chunks);
            float y = arrowY;
            // okna ~1 s (bez policzonych sylab) — ta sama zasada progu w poltonach
            for (int c = 0; c < chunks; c++) {
                int i0 = c * per, i1 = Math.min(vis.size() - 1, (c + 1) * per);
                if (i1 - i0 < 1) continue;
                double chg = semitoneChange(vis.subList(i0, i1 + 1));
                int dir = chg >= LiveAudioData.arrowThresholdSt ? 1 : chg <= -LiveAudioData.arrowThresholdSt ? -1 : 0;
                float dy = dir == 0 ? 0f : (float) Math.max(dp(6), Math.min(dp(34), Math.abs(chg) * dp(7)));
                float ye = y + (dir > 0 ? -dy : dy);
                drawOneArrow(canvas, cx[c], y, cx[c + 1], ye, dir, true);
                y = (ye < arrowY - dp(34) || ye > arrowY + dp(34)) ? arrowY : ye;
            }
        }
    }

    private static int chunkDir(List<LiveAudioData.PitchPoint> pts) {
        int n = pts.size();
        if (n < 2) return 0;
        double sx = 0, sy = 0, sxy = 0, sx2 = 0;
        for (LiveAudioData.PitchPoint p : pts) {
            double x = p.sampleIndex / (double) LiveAudioData.SAMPLE_RATE;
            sx += x; sy += p.freq; sxy += x * p.freq; sx2 += x * x;
        }
        double den = n * sx2 - sx * sx;
        double sl = den > 0 ? (n * sxy - sx * sy) / den : 0;
        double mean = sy / n;
        double sp = Math.abs(sl) / mean * 100;
        return sp < 3 ? 0 : sl > 0 ? 1 : -1;
    }

    // ── OCENA EMISJI NA WYKRESIE: nad kazda porcja mowy pastylka z wynikiem (jak w PitchRec) ──
    private final Paint normPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint normTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static int scoreColor(int score) {
        return score >= 100 ? 0xFF00F5A8 : score >= 70 ? 0xFFE8820C : score >= 45 ? 0xFFFFC107 : 0xFFFF5050;
    }

    private void drawNormLabels(Canvas canvas, int w, float top, long visStart, float visRange) {
        normTextPaint.setTextSize(dp(11));
        normTextPaint.setFakeBoldText(true);
        for (Norms.Segment sg : LiveAudioData.norms.segmentsSnapshot()) {
            float x0 = (float) ((sg.start * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w);
            float x1 = (float) ((sg.end * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w);
            if (x1 < 0 || x0 > w) continue;
            boolean scored = LiveAudioData.normsOn() && sg.result != null;
            int col = scored ? scoreColor(sg.result.score) : 0xFFB388FF;
            normPaint.setStyle(Paint.Style.FILL);
            // pasek pod podzialka: sylaba 4-fazowa w kolorze oceny, dalsze sylaby fioletowe
            float xu = sg.unitEnd > 0 ? (float) ((sg.unitEnd * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w) : x1;
            if (scored) {
                normPaint.setColor((col & 0x00FFFFFF) | 0xCC000000);
                canvas.drawRect(Math.max(0, x0), top + dp(2), Math.min(w, xu), top + dp(5), normPaint);
            }
            if (!scored || xu < x1) {
                normPaint.setColor(0x99B388FF);
                canvas.drawRect(Math.max(0, scored ? xu : x0), top + dp(3), Math.min(w, x1), top + dp(4.5f), normPaint);
            }
            String tempo = LiveAudioData.showTempo && sg.syllables >= 0 ? sg.syllables + " " + L.t("syl") + " · " + Math.round(sg.rate) + "/min" : "";
            String lbl = scored ? (sg.fourPhase ? "4F " : "") + sg.result.score + "%" + (tempo.isEmpty() ? "" : "  " + tempo) : tempo;
            if (lbl.isEmpty()) continue;
            float tw = normTextPaint.measureText(lbl);
            float cx = Math.max(tw / 2 + dp(6), Math.min(w - tw / 2 - dp(6), (x0 + x1) / 2f));
            normPaint.setColor(0xB0000000);
            canvas.drawRoundRect(new android.graphics.RectF(cx - tw / 2 - dp(5), top + dp(7), cx + tw / 2 + dp(5), top + dp(23)), dp(8), dp(8), normPaint);
            normTextPaint.setColor(col);
            canvas.drawText(lbl, cx - tw / 2, top + dp(19), normTextPaint);
        }
    }

    // Kreseczki = wykryte sylaby (jadra), u dolu wykresu — widac, co zostalo policzone
    private void drawSyllableTicks(Canvas canvas, int w, int h, float top, long visStart, float visRange) {
        normPaint.setColor(0xCCB388FF);
        normPaint.setStyle(Paint.Style.FILL);
        float yb = top + h - dp(3);
        for (SyllableDetector.Syl sy : LiveAudioData.syllablesSnapshot()) {
            float x = (float) ((sy.nucleus * LiveAudioData.SAMPLE_RATE - visStart) / visRange * w);
            if (x < 0 || x > w) continue;
            canvas.drawRect(x - dp(1.5f), yb - dp(9), x + dp(1.5f), yb, normPaint);
        }
    }

    // ── PODPOWIEDZI W CZASIE NAGRYWANIA (rysowane co klatke, na wierzchu) ──
    private final Paint hintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void drawLiveHints(Canvas canvas, int w, float top) {
        if (!LiveAudioData.isRecordingActive) return;
        float y = top + dp(28);
        float pad = dp(8);
        hintText.setFakeBoldText(true);
        Norms.Result r = LiveAudioData.norms.live;
        if (LiveAudioData.normsOn() && r != null && System.currentTimeMillis() - LiveAudioData.norms.liveAtMs < 2500) {
            int col = scoreColor(r.score);
            String l1 = L.t("SYLABA 4-FAZOWA") + " " + r.score + "%  ·  " + L.f(r.detail, r.args);
            String q = r.hasQuiet == null ? L.t("WEJŚCIE") + ": …" : r.hasQuiet ? L.t("WEJŚCIE") + ": " + L.t("łagodne ✓") : L.t("WEJŚCIE") + ": " + L.t("za głośno ✗");
            String l2 = q + (r.shapeSimilarity != null ? "  ·  " + L.t("KSZTAŁT") + ": " + r.shapeSimilarity + "%" : "");
            y = hintBox(canvas, w, y, pad, l1, col, l2, 0xFFDDDDDD);
        }
        if (LiveAudioData.showTempo && LiveAudioData.liveRate > 0) {
            float avg = LiveAudioData.averageRate();
            String l = "🗣 " + L.t("TEMPO") + " " + Math.round(LiveAudioData.liveRate) + " " + L.t("sylab/min")
                    + (avg > 0 ? "  ·  " + L.t("średnio") + " " + Math.round(avg) : "");
            y = hintBox(canvas, w, y, pad, l, 0xFFB388FF, null, 0);
        }
        if (LiveAudioData.showPauses) {
            List<LiveAudioData.Pause> pz = LiveAudioData.pausesSnapshot();
            if (!pz.isEmpty()) {
                LiveAudioData.Pause pa = pz.get(pz.size() - 1);
                double nowS = LiveAudioData.getTotalSamplesWritten() / (double) LiveAudioData.SAMPLE_RATE;
                if (nowS - pa.end < 3.0) {
                    boolean ok = pa.ok();
                    String l = String.format(Locale.US, "⏸ %s %.2f s ", L.t("PAUZA"), pa.dur()) + (ok ? "✓" : pa.dur() < LiveAudioData.pauseMinS ? L.t("za krótka") : L.t("za długa"))
                            + String.format(Locale.US, "  (%.1f–%.1f s)", LiveAudioData.pauseMinS, LiveAudioData.pauseMaxS);
                    hintBox(canvas, w, y, pad, l, ok ? 0xFF00FF8A : 0xFFFF6464, null, 0);
                }
            }
        }
    }

    private float hintBox(Canvas canvas, int w, float y, float pad, String l1, int c1, String l2, int c2) {
        hintText.setTextSize(dp(13));
        float w1 = hintText.measureText(l1);
        float w2 = 0;
        if (l2 != null) { hintText.setTextSize(dp(11)); w2 = hintText.measureText(l2); }
        float bw = Math.min(w - dp(16), Math.max(w1, w2) + pad * 2);
        float bh = dp(l2 != null ? 40 : 24);
        hintBg.setColor(0xC0000000);
        canvas.drawRoundRect(new android.graphics.RectF(dp(8), y, dp(8) + bw, y + bh), dp(10), dp(10), hintBg);
        hintBg.setColor(c1);
        canvas.drawRect(dp(8), y + dp(6), dp(11), y + bh - dp(6), hintBg);
        hintText.setTextSize(dp(13));
        hintText.setColor(c1);
        canvas.drawText(ellipsize(l1, bw - pad * 2), dp(8) + pad, y + dp(17), hintText);
        if (l2 != null) {
            hintText.setTextSize(dp(11));
            hintText.setColor(c2);
            canvas.drawText(ellipsize(l2, bw - pad * 2), dp(8) + pad, y + dp(33), hintText);
        }
        return y + bh + dp(6);
    }

    private String ellipsize(String s, float max) {
        if (hintText.measureText(s) <= max) return s;
        while (s.length() > 3 && hintText.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
