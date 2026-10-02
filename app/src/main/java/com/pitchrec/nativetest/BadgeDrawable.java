package com.pitchrec.nativetest;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

// Czerwona kropka z liczba na ikonie dolnego menu (rysowana w nakladce ikony — bez zmian w layoucie)
public final class BadgeDrawable extends Drawable {
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), fg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";
    private final float density;

    public BadgeDrawable(float density) {
        this.density = density;
        bg.setColor(0xFFE53935);
        fg.setColor(0xFFFFFFFF);
        fg.setTextAlign(Paint.Align.CENTER);
        fg.setFakeBoldText(true);
        fg.setTextSize(9 * density);
    }

    public void setCount(int n) { text = n <= 0 ? "" : n > 9 ? "9+" : String.valueOf(n); invalidateSelf(); }

    @Override public void draw(Canvas c) {
        if (text.isEmpty()) return;
        float r = 7.5f * density;
        float cx = getBounds().right - r * 0.6f, cy = getBounds().top + r * 0.6f;
        c.drawCircle(cx, cy, r, bg);
        c.drawText(text, cx, cy + 3.2f * density, fg);
    }
    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
