package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// Przycisk 3D z prawdziwym neonowym blaskiem (BlurMaskFilter) — tylko symbol (bez tekstu),
// gradient sugerujacy fizyczna glebie (jak realny przycisk), i intensywna poswiata przy
// wcisnieciu.
public class NeonButton extends View {

    private String symbol = "";
    private int neonColor = Color.WHITE;
    private boolean pressed = false;
    private boolean enabled = true;
    private OnClickListener clickListener;

    private final Paint borderPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint symbolPaint = new Paint();
    private final Paint bgPaint = new Paint();

    // WAZNE: duzy inset (margines) daje miejsce na rozmycie WEWNATRZ granic widoku — bez
    // tego, BlurMaskFilter byl obcinany na krawedziach View (Android domyslnie przycina
    // rysowanie do wlasnych granic komponentu), przez co poswiata w ogole nie byla widoczna.
    private static final float INSET_DP = 6f;
    private static final float BLUR_RADIUS_DP = 10f;

    public NeonButton(Context context) {
        super(context);
        init();
    }

    public NeonButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2.5f));
        borderPaint.setAntiAlias(true);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAntiAlias(true);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(BLUR_RADIUS_DP), BlurMaskFilter.Blur.NORMAL));

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);

        symbolPaint.setTextAlign(Paint.Align.CENTER);
        symbolPaint.setAntiAlias(true);
        symbolPaint.setTextSize(dp(18));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setNeonColor(int color) {
        neonColor = color;
        invalidate();
    }

    public void setButtonText(String t) {
        // Tylko symbol — pierwszy niealfanumeryczny znak (▶ ● ■ itp.), bez podpisu tekstowego.
        symbol = t.replaceAll("[A-Za-zĄĘŁŃÓŚŹŻąęłńóśźż ]", "").trim();
        if (symbol.isEmpty()) symbol = t;
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
        float inset = dp(INSET_DP);

        int mutedColor = Color.parseColor("#4A4A54");
        int activeColor = enabled ? neonColor : mutedColor;

        float left = inset, top = inset, right = w - inset, bottom = h - inset;

        if (pressed && enabled) {
            // PRAWDZIWA poswiata — wypelniony, rozmyty prostokat, WIEKSZY od samego
            // przycisku (dodatkowy margines), zeby swiatlo "wyciekalo" na boki jak
            // prawdziwy neon.
            float glowExtra = dp(6);
            glowPaint.setColor(neonColor);
            canvas.drawRoundRect(left - glowExtra, top - glowExtra, right + glowExtra, bottom + glowExtra,
                    radius, radius, glowPaint);
        }

        // Gradient 3D — jasniej u gory, ciemniej u dolu (normalnie), odwrotnie gdy
        // wcisniety (wrazenie "wgniecenia" w dol, jak prawdziwy przycisk fizyczny).
        int topShade, bottomShade;
        if (pressed && enabled) {
            topShade = darken(neonColor, 0.55f);
            bottomShade = lighten(neonColor, 0.15f);
        } else {
            topShade = Color.parseColor("#2E2E36");
            bottomShade = Color.parseColor("#18181C");
        }
        bgPaint.setShader(new LinearGradient(0, top, 0, bottom, topShade, bottomShade, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, bgPaint);

        borderPaint.setColor(activeColor);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint);

        symbolPaint.setColor(pressed && enabled ? Color.WHITE : activeColor);
        float textY = (top + bottom) / 2f - (symbolPaint.descent() + symbolPaint.ascent()) / 2f;
        canvas.drawText(symbol, (left + right) / 2f, textY, symbolPaint);
    }

    private int darken(int color, float factor) {
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.rgb(r, g, b);
    }

    private int lighten(int color, float factor) {
        int r = (int) Math.min(255, Color.red(color) + (255 - Color.red(color)) * factor);
        int g = (int) Math.min(255, Color.green(color) + (255 - Color.green(color)) * factor);
        int b = (int) Math.min(255, Color.blue(color) + (255 - Color.blue(color)) * factor);
        return Color.rgb(r, g, b);
    }
}
