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
        midlinePaint.setColor(Color.parseColor("#40FFFFFF"));
        midlinePaint.setStrokeWidth(1f);
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
        invalidate();
    }

    public void pauseKeepingPosition() {
        long totalSamples = LiveAudioData.getTotalSamplesWritten();
        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : 1000;
        long tailSamples = (long) tailCount * 256;
        panOffsetSample = Math.max(0, totalSamples - tailSamples);
        isLiveMode = false;
        invalidate();
    }

    public void setOnSeekListener(OnSeekListener l) {
        seekListener = l;
    }

    public void setPlayheadSample(long sample) {
        playheadSample = sample;
        invalidate();
    }

    public void resetPan() {
        panOffsetSample = 0L;
        playheadSample = -1L;
        cacheValid = false;
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
                panOffsetSample = Math.max(0, panOffsetSample + sampleDelta);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!touchMoved) {
                    float pxPerSecond = getWidth() / Math.max(0.001f, lastVisibleSeconds);
                    long tappedSample = panOffsetSample + (long) (event.getX() / pxPerSecond * LiveAudioData.SAMPLE_RATE);
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
        boolean needsRebuild = !cacheValid
                || (dataChanged && (now - lastRebuildTimeMs >= MIN_REBUILD_INTERVAL_MS));

        if (needsRebuild) {
            rebuildCache(cacheCanvas, w, fullH);
            lastCachedTotalSamples = totalSamples;
            lastCachedPanOffset = panOffsetSample;
            lastCachedZoomSeconds = zoomSeconds;
            lastCachedLiveMode = isLiveMode;
            lastRebuildTimeMs = now;
            cacheValid = true;
        }

        canvas.drawBitmap(cacheBitmap, 0, 0, null);

        if (playheadSample >= 0 && lastVisibleSampleRange > 0) {
            float rulerHeight = dp(RULER_HEIGHT_DP);
            float px = ((playheadSample - lastVisibleStartSample) / lastVisibleSampleRange) * w;
            if (px >= 0 && px <= w) {
                canvas.drawLine(px, rulerHeight, px, fullH, playheadPaint);
            }
        }
    }

    private void rebuildCache(Canvas canvas, int w, int fullH) {
        float rulerHeight = dp(RULER_HEIGHT_DP);
        int h = (int) (fullH - rulerHeight);
        if (h <= 0) return; // zabezpieczenie — widok za maly, nic sensownego do rysowania
        int mid = h / 2;

        canvas.drawRect(0, 0, w, fullH, bgPaint);

        int horizontalLines = 8;
        for (int i = 1; i < horizontalLines; i++) {
            float y = rulerHeight + (h * i / (float) horizontalLines);
            canvas.drawLine(0, y, w, y, gridLinePaint);
        }

        canvas.drawLine(0, rulerHeight + mid, w, rulerHeight + mid, midlinePaint);

        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : w;
        if (tailCount <= 0) tailCount = 1; // zabezpieczenie

        LiveAudioData.FrameSnapshot snap = isLiveMode
                ? LiveAudioData.snapshotForDrawing(tailCount)
                : LiveAudioData.snapshotForDrawingAtSample(panOffsetSample, tailCount);
        float[] envelope = snap.envelope;

        if (envelope.length > 0) {
            float xStep = (float) w / Math.max(1, envelope.length);

            int neededSize = envelope.length * 4;
            if (envelopeLinePts.length != neededSize) envelopeLinePts = new float[neededSize];
            for (int i = 0; i < envelope.length; i++) {
                float barHeight = envelope[i] * mid;
                float x = i * xStep;
                int base = i * 4;
                envelopeLinePts[base] = x;
                envelopeLinePts[base + 1] = rulerHeight + mid - barHeight;
                envelopeLinePts[base + 2] = x;
                envelopeLinePts[base + 3] = rulerHeight + mid + barHeight;
            }
            canvas.drawLines(envelopeLinePts, 0, neededSize, envelopePaint);

            long visibleStartSample = snap.visibleStartSample;
            long totalSamples = snap.totalSamples;
            float visibleSampleRange = Math.max(1, isLiveMode
                    ? (totalSamples - visibleStartSample)
                    : ((long) tailCount * 256));
            lastVisibleSeconds = visibleSampleRange / (float) LiveAudioData.SAMPLE_RATE;
            lastVisibleStartSample = visibleStartSample;
            lastVisibleSampleRange = visibleSampleRange;

            List<LiveAudioData.PitchPoint> pitchPts = snap.pitchPoints;
            pitchPath.reset();
            boolean penDown = false;

            for (LiveAudioData.PitchPoint p : pitchPts) {
                if (p.freq <= 0 || p.freq < PMIN || p.freq > PMAX) {
                    penDown = false;
                    continue;
                }
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

            drawRuler(canvas, w, fullH, rulerHeight, visibleStartSample, visibleSampleRange);
        } else {
            canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        }
    }

    private void drawRuler(Canvas canvas, int w, int fullH, float rulerHeight, long visibleStartSample, float visibleSampleRange) {
        canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        float visibleSeconds = Math.max(0.001f, visibleSampleRange / (float) LiveAudioData.SAMPLE_RATE);
        float pxPerSecond = w / visibleSeconds;

        // ZABEZPIECZENIE: to byl glowny podejrzany poprzedniej regresji — zdegenerowana
        // wartosc pxPerSecond (zero/NaN/nieskonczonosc) moglaby uwiezic petle nizej w
        // nieskonczonosci, blokujac caly watek UI.
        if (pxPerSecond <= 0f || !Float.isFinite(pxPerSecond)) return;

        float tickIntervalSec = 1f;
        int safety = 0;
        while (tickIntervalSec * pxPerSecond < dp(40) && safety < 30) { tickIntervalSec *= 2; safety++; }
        if (tickIntervalSec <= 0f || !Float.isFinite(tickIntervalSec)) return;

        float startSecond = visibleStartSample / (float) LiveAudioData.SAMPLE_RATE;
        float firstTickSecond = (float) (Math.ceil(startSecond / tickIntervalSec) * tickIntervalSec);

        int tickSafety = 0;
        for (float sec = firstTickSecond; tickSafety < 200; sec += tickIntervalSec, tickSafety++) {
            float x = (sec - startSecond) * pxPerSecond;
            if (x > w) break;
            canvas.drawLine(x, rulerHeight, x, fullH, gridLinePaint);
            canvas.drawLine(x, rulerHeight - dp(6), x, rulerHeight, rulerTickPaint);
            String label = String.format(Locale.getDefault(), "%.0fs", sec);
            canvas.drawText(label, x + dp(2), rulerHeight - dp(7), rulerTextPaint);
        }
    }
}
