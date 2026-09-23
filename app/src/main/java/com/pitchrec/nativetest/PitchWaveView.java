package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;
import java.util.Locale;

// SurfaceView z dedykowanym watkiem rysujacym (nie zwykly View na glownym watku UI) — ta
// sama architektura co profesjonalne aplikacje do nagrywania (RecForge i podobne), unikajaca
// calkowicie rywalizacji o czas procesora z watkiem interfejsu. Rysuje falę (zielona
// obwiednia, jak RecForge) + zolta linie pitch + siatke (pionowa+pozioma) + podzialke
// sekundowa + suwak odtwarzania. Publiczne API (setZoomSeconds, setLiveMode,
// setOnSeekListener, setPlayheadSample, resetPan) identyczne jak poprzednio — MainActivity
// nie wymaga zmian poza deklaracja typu.
public class PitchWaveView extends SurfaceView implements SurfaceHolder.Callback {

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

    // ── Watek rysujacy ──
    private RenderThread renderThread;
    private volatile boolean surfaceReady = false;

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
        getHolder().addCallback(this);

        bgPaint.setColor(Color.parseColor("#1a1a1a"));
        envelopePaint.setColor(Color.parseColor("#00E000"));
        envelopePaint.setStrokeWidth(2f);
        midlinePaint.setColor(Color.parseColor("#40FFFFFF"));
        midlinePaint.setStrokeWidth(1f);
        pitchPaint.setColor(Color.parseColor("#FFE600"));
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

    // ── SurfaceHolder.Callback ──

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        renderThread = new RenderThread(holder);
        renderThread.setRunning(true);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        if (renderThread != null) {
            renderThread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    renderThread.join();
                    retry = false;
                } catch (InterruptedException e) { /* sprobuj znowu */ }
            }
        }
    }

    // Wywolywane z zewnatrz (MainActivity), zeby wymusic natychmiastowe przerysowanie —
    // odpowiednik invalidate() ze zwyklego View, tutaj po prostu budzi wątek rysujący,
    // ktory i tak rysuje w petli, wiec w praktyce nie jest to nawet niezbedne, ale zostaje
    // dla zgodnosci z poprzednim API.
    public void invalidate() {
        // no-op — RenderThread rysuje nieprzerwanie w wlasnej petli
    }

    private class RenderThread extends Thread {
        private final SurfaceHolder holder;
        private volatile boolean running = false;

        RenderThread(SurfaceHolder holder) {
            this.holder = holder;
        }

        void setRunning(boolean r) {
            running = r;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            drawFrame(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        try { holder.unlockCanvasAndPost(canvas); } catch (Exception e) { }
                    }
                }
                // Lekkie ograniczenie (~60fps) — lockCanvas() sam nie synchronizuje z
                // odswiezaniem ekranu, wiec bez tego watek probowalby rysowac tak szybko jak
                // mozliwe, niepotrzebnie obciazajac procesor.
                try { Thread.sleep(16); } catch (InterruptedException e) { }
            }
        }
    }

    private float zoomSeconds = 0f;
    private boolean isLiveMode = true;
    private long panOffsetSample = 0L;
    private long playheadSample = -1L;

    public void setZoomSeconds(float seconds) {
        zoomSeconds = seconds;
    }

    public void setLiveMode(boolean live) {
        isLiveMode = live;
    }

    public void setOnSeekListener(OnSeekListener l) {
        seekListener = l;
    }

    public void setPlayheadSample(long sample) {
        playheadSample = sample;
    }

    public void resetPan() {
        panOffsetSample = 0L;
        playheadSample = -1L;
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
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!touchMoved) {
                    float pxPerSecond = getWidth() / Math.max(0.001f, lastVisibleSeconds);
                    long tappedSample = panOffsetSample + (long) (event.getX() / pxPerSecond * LiveAudioData.SAMPLE_RATE);
                    playheadSample = tappedSample;
                    if (seekListener != null) seekListener.onSeek(tappedSample);
                }
                return true;
        }
        return false;
    }

    private void drawFrame(Canvas canvas) {
        int w = getWidth();
        int fullH = getHeight();
        if (w <= 0 || fullH <= 0) return;
        float rulerHeight = dp(RULER_HEIGHT_DP);
        int h = (int) (fullH - rulerHeight);
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

            if (playheadSample >= 0) {
                float px = ((playheadSample - visibleStartSample) / visibleSampleRange) * w;
                if (px >= 0 && px <= w) {
                    canvas.drawLine(px, rulerHeight, px, fullH, playheadPaint);
                }
            }

            drawRuler(canvas, w, rulerHeight, visibleStartSample, visibleSampleRange);
        } else {
            canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        }
    }

    private void drawRuler(Canvas canvas, int w, float rulerHeight, long visibleStartSample, float visibleSampleRange) {
        canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        float visibleSeconds = Math.max(0.001f, visibleSampleRange / (float) LiveAudioData.SAMPLE_RATE);
        float pxPerSecond = w / visibleSeconds;

        float tickIntervalSec = 1f;
        while (tickIntervalSec * pxPerSecond < dp(40)) tickIntervalSec *= 2;

        float startSecond = visibleStartSample / (float) LiveAudioData.SAMPLE_RATE;
        float firstTickSecond = (float) (Math.ceil(startSecond / tickIntervalSec) * tickIntervalSec);

        for (float sec = firstTickSecond; ; sec += tickIntervalSec) {
            float x = (sec - startSecond) * pxPerSecond;
            if (x > w) break;
            canvas.drawLine(x, rulerHeight, x, canvas.getHeight(), gridLinePaint);
            canvas.drawLine(x, rulerHeight - dp(6), x, rulerHeight, rulerTickPaint);
            String label = String.format(Locale.getDefault(), "%.0fs", sec);
            canvas.drawText(label, x + dp(2), rulerHeight - dp(7), rulerTextPaint);
        }
    }
}
