package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

// Rysuje falę (obwiednia amplitudy) + żółtą linię pitch, odpowiednik drawStatic() z PitchRec
// (JS) — skala Y dla pitch jest logarytmiczna (fY), tak samo jak w oryginale.
public class PitchWaveView extends View {

    private static final float PMIN = 55f;
    private static final float PMAX = 1050f;

    private final Paint bgPaint = new Paint();
    private final Paint envelopePaint = new Paint();
    private final Paint midlinePaint = new Paint();
    private final Paint pitchPaint = new Paint();

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
        pitchPaint.setStyle(Paint.Style.STROKE);
        pitchPaint.setAntiAlias(true);
    }

    private float zoomSeconds = 0f; // 0 = pokaz tyle ile sie zmiesci (domyslnie), >0 = ograniczony zakres czasu

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int mid = h / 2;

        canvas.drawRect(0, 0, w, h, bgPaint);
        canvas.drawLine(0, mid, w, mid, midlinePaint);

        int totalEnvelopeCount = LiveAudioData.getEnvelopeSize();
        // Gdy zoomSeconds>0, ograniczamy widoczny zakres do tylu sekund (ile chunkow
        // obwiedni odpowiada tej liczbie sekund) — inaczej pokazujemy tyle punktow ile
        // pikseli szerokosci ma widok (czyli "ALL", zawsze dopasowane do ekranu).
        int envChunksPerSecond = LiveAudioData.SAMPLE_RATE / 256;
        int tailCount = (zoomSeconds > 0) ? (int) (zoomSeconds * envChunksPerSecond) : w;
        float[] envelope = LiveAudioData.snapshotEnvelopeTail(tailCount);
        if (envelope.length > 0) {
            int startIdx = totalEnvelopeCount - envelope.length; // pozycja pierwszego punktu w PELNEJ historii
            float xStep = (float) w / Math.max(1, envelope.length);

            for (int i = 0; i < envelope.length; i++) {
                float amp = envelope[i];
                float barHeight = amp * mid;
                float x = i * xStep;
                canvas.drawLine(x, mid - barHeight, x, mid + barHeight, envelopePaint);
            }

            long visibleStartSample = (long) startIdx * 256;
            long totalSamples = LiveAudioData.getTotalSamplesWritten();
            float visibleSampleRange = Math.max(1, totalSamples - visibleStartSample);

            List<LiveAudioData.PitchPoint> pitchPts = LiveAudioData.snapshotPitchPointsFrom(visibleStartSample);

            float[] pathX = new float[pitchPts.size()];
            float[] pathY = new float[pathX.length];
            int segLen = 0;

            for (LiveAudioData.PitchPoint p : pitchPts) {
                if (p.freq <= 0 || p.freq < PMIN || p.freq > PMAX) {
                    drawSegment(canvas, pathX, pathY, segLen);
                    segLen = 0;
                    continue;
                }
                float x = ((p.sampleIndex - visibleStartSample) / visibleSampleRange) * w;
                if (x < 0 || x > w) continue;
                pathX[segLen] = x;
                pathY[segLen] = freqToY(p.freq, h);
                segLen++;
            }
            drawSegment(canvas, pathX, pathY, segLen);
        }
    }

    private void drawSegment(Canvas canvas, float[] xs, float[] ys, int len) {
        if (len < 2) return;
        for (int i = 0; i < len - 1; i++) {
            canvas.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1], pitchPaint);
        }
    }
}
