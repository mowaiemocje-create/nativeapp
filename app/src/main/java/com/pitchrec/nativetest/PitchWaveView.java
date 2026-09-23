package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
import java.util.Locale;

// Rysuje falę (obwiednia amplitudy) + żółtą linię pitch + podziałkę sekundową na górze.
// WYDAJNOŚĆ: linia pitch rysowana jako Path (jedno wywołanie drawPath na segment, gładkie
// połączenia) zamiast wielu pojedynczych drawLine — to była przyczyna zarówno lagów, jak i
// "poszarpanego" wyglądu linii pitch. Fala rysowana przez drawLines (wsadowo, jedno
// wywołanie), nie po jednej kolumnie na wywołanie.
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

    public PitchWaveView(Context context) {
        super(context);
        init();
    }

    public PitchWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint.setColor(Color.parseColor("#050510"));
        envelopePaint.setColor(Color.parseColor("#8B7EFF"));
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
        rulerTickPaint.setColor(Color.parseColor("#FFFFFF"));
        rulerTickPaint.setStrokeWidth(2f);
        rulerTextPaint.setColor(Color.parseColor("#FFFFFF"));
        rulerTextPaint.setTextSize(dp(12));
        rulerTextPaint.setAntiAlias(true);
        rulerTextPaint.setFakeBoldText(true);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float zoomSeconds = 0f;

    public void setZoomSeconds(float seconds) {
        zoomSeconds = seconds;
        invalidate();
    }

    private float freqToY(float freq, int height) {
        double logF = Math.log(freq) / Math.log(2);
        double logMin = Math.log(PMIN) / Math.log(2);
        double logMax = Math.log(PMAX) / Math.log(2);
        return (float) (height * (1 - (logF - logMin) / (logMax - logMin)));
    }

    // Reużywalne bufory — unikamy alokacji nowych tablic przy każdej klatce (dodatkowe
    // usprawnienie wydajności, oprócz Path/drawLines).
    private float[] envelopeLinePts = new float[0];
    private final Path pitchPath = new Path();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int fullH = getHeight();
        float rulerHeight = dp(RULER_HEIGHT_DP);
        int h = (int) (fullH - rulerHeight);
        int mid = h / 2;

        canvas.drawRect(0, 0, w, fullH, bgPaint);
        canvas.drawLine(0, rulerHeight + mid, w, rulerHeight + mid, midlinePaint);

        int totalEnvelopeCount = LiveAudioData.getEnvelopeSize();
        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : w;
        float[] envelope = LiveAudioData.snapshotEnvelopeTail(tailCount);

        if (envelope.length > 0) {
            int startIdx = totalEnvelopeCount - envelope.length;
            float xStep = (float) w / Math.max(1, envelope.length);

            // Fala — wsadowe rysowanie przez drawLines (jedno wywolanie, nie N wywolan).
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

            long visibleStartSample = (long) startIdx * 256;
            long totalSamples = LiveAudioData.getTotalSamplesWritten();
            float visibleSampleRange = Math.max(1, totalSamples - visibleStartSample);

            // Linia pitch — Path (gladkie, polaczone segmenty), jedno drawPath na segment.
            List<LiveAudioData.PitchPoint> pitchPts = LiveAudioData.snapshotPitchPointsFrom(visibleStartSample);
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

            drawRuler(canvas, w, rulerHeight, visibleStartSample, totalSamples);
        } else {
            canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        }
    }

    // Podziałka sekundowa (inspirowana RecForge) — znaczniki i etykiety co 1s (albo więcej,
    // gdy widoczny zakres jest długi — dostrajamy odstęp, żeby etykiety się nie zlewały).
    private void drawRuler(Canvas canvas, int w, float rulerHeight, long visibleStartSample, long totalSamples) {
        canvas.drawRect(0, 0, w, rulerHeight, rulerBgPaint);
        float visibleSeconds = Math.max(0.001f, (totalSamples - visibleStartSample) / (float) LiveAudioData.SAMPLE_RATE);
        float pxPerSecond = w / visibleSeconds;

        float tickIntervalSec = 1f;
        while (tickIntervalSec * pxPerSecond < dp(40)) tickIntervalSec *= 2; // nie za gesto

        float startSecond = visibleStartSample / (float) LiveAudioData.SAMPLE_RATE;
        float firstTickSecond = (float) (Math.ceil(startSecond / tickIntervalSec) * tickIntervalSec);

        for (float sec = firstTickSecond; ; sec += tickIntervalSec) {
            float x = (sec - startSecond) * pxPerSecond;
            if (x > w) break;
            canvas.drawLine(x, rulerHeight - dp(6), x, rulerHeight, rulerTickPaint);
            String label = String.format(Locale.getDefault(), "%.0fs", sec);
            canvas.drawText(label, x + dp(2), rulerHeight - dp(7), rulerTextPaint);
        }
    }
}
