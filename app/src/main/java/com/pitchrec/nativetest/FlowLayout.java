package com.pitchrec.nativetest;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

// Ukladanie "chipow" w wierszach z zawijaniem (jak flex-wrap w PitchRec) — np. wybor
// "Z kim dzis sukcesy", sytuacji, samopoczucia.
public class FlowLayout extends ViewGroup {

    private final int gap;

    public FlowLayout(Context c) {
        super(c);
        gap = (int) Ui.dp(c, 6);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxW = MeasureSpec.getSize(widthMeasureSpec);
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            measureChild(ch, MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) { x = 0; y += rowH + gap; rowH = 0; }
            x += w + gap;
            rowH = Math.max(rowH, h);
        }
        setMeasuredDimension(maxW, y + rowH);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l;
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) { x = 0; y += rowH + gap; rowH = 0; }
            ch.layout(x, y, x + w, y + h);
            x += w + gap;
            rowH = Math.max(rowH, h);
        }
    }
}
