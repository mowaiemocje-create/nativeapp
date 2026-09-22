package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// Odpowiednik .st/.sb/.sf/.sth z CSS PitchRec — cienki pasek tła, kolorowe wypełnienie,
// okrągła pomarańczowa gałka. Używane dla suwaków MIC (gain) i ZOOM.
public class SliderView extends View {

    public interface OnValueChangeListener {
        void onValueChange(float value); // 0.0 - 1.0
    }

    private float value = 0f; // 0.0 - 1.0
    private OnValueChangeListener listener;

    private final Paint trackPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint thumbPaint = new Paint();
    private final Paint thumbBorderPaint = new Paint();

    private static final float THUMB_RADIUS_DP = 18f;
    private static final float TRACK_HEIGHT_DP = 4f;

    public SliderView(Context context) {
        super(context);
        init();
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setColor(Color.parseColor("#33333C"));
        trackPaint.setAntiAlias(true);
        fillPaint.setColor(Color.parseColor("#F0973A"));
        fillPaint.setAntiAlias(true);
        thumbPaint.setColor(Color.parseColor("#FF9500"));
        thumbPaint.setAntiAlias(true);
        thumbBorderPaint.setColor(Color.parseColor("#40FF9500"));
        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setStrokeWidth(dp(3));
        thumbBorderPaint.setAntiAlias(true);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setValue(float v) {
        value = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    public float getValue() {
        return value;
    }

    public void setOnValueChangeListener(OnValueChangeListener l) {
        listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float midY = h / 2f;
        float trackHeight = dp(TRACK_HEIGHT_DP);
        float thumbRadius = dp(THUMB_RADIUS_DP);
        float usableWidth = w - thumbRadius * 2;
        float thumbX = thumbRadius + value * usableWidth;

        canvas.drawRoundRect(thumbRadius, midY - trackHeight / 2, w - thumbRadius, midY + trackHeight / 2,
                trackHeight / 2, trackHeight / 2, trackPaint);
        canvas.drawRoundRect(thumbRadius, midY - trackHeight / 2, thumbX, midY + trackHeight / 2,
                trackHeight / 2, trackHeight / 2, fillPaint);
        canvas.drawCircle(thumbX, midY, thumbRadius, thumbBorderPaint);
        canvas.drawCircle(thumbX, midY, thumbRadius, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                float thumbRadius = dp(THUMB_RADIUS_DP);
                float usableWidth = getWidth() - thumbRadius * 2;
                float newValue = (event.getX() - thumbRadius) / Math.max(1, usableWidth);
                setValue(newValue);
                if (listener != null) listener.onValueChange(value);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}
