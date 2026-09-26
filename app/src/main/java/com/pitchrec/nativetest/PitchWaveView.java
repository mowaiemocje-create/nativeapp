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

    public void setPlayheadSample(long sample) {
        playheadSample = sample;
        // Auto-przewijanie do przodu podczas odtwarzania wczytanego pliku (tryb
        // statyczny) — bez tego, widok zostawal zablokowany na stalym oknie (np. 8s) i
        // playhead po prostu "wychodzil" poza widoczny zakres podczas dluzszego
        // odtwarzania, zamiast plynnie przewijac sie razem z odtwarzaniem.
        if (!isLiveMode && lastVisibleSampleRange > 0) {
            long visibleEnd = lastVisibleStartSample + (long) lastVisibleSampleRange;
            if (sample > visibleEnd - lastVisibleSampleRange * 0.1f) {
                // Playhead blisko prawej krawedzi — przesuwamy okno tak, zeby playhead
                // byl blisko lewej krawedzi nowego okna (efekt plynnego przewijania).
                panOffsetSample = Math.max(0, sample - (long) (lastVisibleSampleRange * 0.1f));
            }
        }
        invalidate();
    }

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

        if (playheadSample >= 0 && lastVisibleSampleRange > 0) {
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
        }
    }
}
