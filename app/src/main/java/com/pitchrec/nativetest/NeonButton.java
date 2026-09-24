package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// Przycisk z PRAWDZIWYM neonowym blaskiem (BlurMaskFilter), nie plaskim wypelnieniem
// kolorem. BlurMaskFilter wymaga wylaczenia akceleracji sprzetowej (setLayerType
// SOFTWARE) — bez tego rozmycie w ogole nie dziala na wiekszosci urzadzen.
public class NeonButton extends View {

    private String text = "";
    private int neonColor = Color.WHITE;
    private boolean pressed = false;
    private boolean enabled = true;
    private OnClickListener clickListener;

    private final Paint borderPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint fillPaint = new Paint();

    public NeonButton(Context context) {
        super(context);
        init();
    }

    public NeonButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // KLUCZOWE: BlurMaskFilter dziala tylko na warstwie programowej (software), nie
        // sprzetowej — bez tej linii caly efekt poswiaty bylby niewidoczny.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2));
        borderPaint.setAntiAlias(true);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(2));
        glowPaint.setAntiAlias(true);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(12), BlurMaskFilter.Blur.NORMAL));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(dp(13));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setNeonColor(int color) {
        neonColor = color;
        invalidate();
    }

    public void setButtonText(String t) {
        text = t;
        invalidate();
    }

    public void setButtonEnabled(boolean e) {
        enabled = e;
        invalidate();
    }

    public void setOnButtonClickListener(OnClickListener l) {
        clickListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                pressed = false;
                invalidate();
                if (clickListener != null) clickListener.onClick(this);
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float radius = dp(8);
        float inset = dp(4); // margines na poswiate, zeby nie byla obcinana na krawedziach

        int mutedColor = Color.parseColor("#4A4A54");
        int borderColor = enabled ? (pressed ? neonColor : neonColor) : mutedColor;
        int textColor = enabled ? neonColor : mutedColor;

        if (pressed && enabled) {
            // Wypelnienie tlem podczas wcisniecia — subtelne, kolor przycisku z niska
            // przezroczystoscia.
            fillPaint.setColor((neonColor & 0x00FFFFFF) | 0x30000000);
            canvas.drawRoundRect(inset, inset, w - inset, h - inset, radius, radius, fillPaint);

            // PRAWDZIWA poswiata — rysowana PRZED obwiednia, wieksza/rozmyta.
            glowPaint.setColor(neonColor);
            canvas.drawRoundRect(inset, inset, w - inset, h - inset, radius, radius, glowPaint);
        }

        borderPaint.setColor(borderColor);
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, radius, radius, borderPaint);

        textPaint.setColor(textColor);
        float textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, w / 2f, textY, textPaint);
    }
}
