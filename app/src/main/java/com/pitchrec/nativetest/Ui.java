package com.pitchrec.nativetest;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Wspolne "klocki" wygladu odwzorowane z CSS PitchRec (.scard, .ri-btn, .field-label itd.)
// — dzieki temu wszystkie ekrany wygladaja spojnie, jak w PWA.
public class Ui {

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    public static int col(Context c, int res) {
        return c.getResources().getColor(res);
    }

    public static GradientDrawable rounded(int fill, int stroke, float strokePx, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke((int) Math.max(1, strokePx), stroke);
        return g;
    }

    // .scard — biala (w ciemnym motywie grafitowa) karta z zaokragleniem i obramowaniem
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = (int) dp(c, 12);
        l.setPadding((int) dp(c, 14), p, (int) dp(c, 14), p);
        l.setBackground(rounded(col(c, R.color.pr_card), col(c, R.color.pr_border), dp(c, 1), dp(c, 14)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) dp(c, 8);
        l.setLayoutParams(lp);
        return l;
    }

    // .field-label / tytul karty — maly, pogrubiony, z rozstrzelonymi literami
    public static TextView label(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(10f);
        t.setLetterSpacing(0.2f);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setTextColor(col(c, R.color.pr_muted));
        t.setPadding(0, 0, 0, (int) dp(c, 6));
        return t;
    }

    public static TextView text(Context c, String s, float sp, int colorRes) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(col(c, colorRes));
        return t;
    }

    // .ri-btn — przycisk z obramowaniem w kolorze, przezroczyste tlo; filled = wypelniony
    public static Button button(Context c, String text, int colorRes, boolean filled) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setTextSize(12f);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        int pad = (int) dp(c, 10);
        b.setPadding(pad, pad, pad, pad);
        int color = col(c, colorRes);
        b.setTextColor(filled ? 0xFFFFFFFF : color);
        float r = dp(c, 9);
        GradientDrawable normal = rounded(filled ? color : 0x00000000, color, dp(c, 1), r);
        GradientDrawable pressed = rounded(filled ? color : ((color & 0x00FFFFFF) | 0x30000000), color, dp(c, 1), r);
        pressed.setAlpha(filled ? 170 : 255);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        s.addState(new int[]{}, normal);
        b.setBackground(s);
        b.setStateListAnimator(null);
        return b;
    }

    public static LinearLayout.LayoutParams weight(float w, float marginEndPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
        lp.rightMargin = (int) marginEndPx;
        return lp;
    }

    public static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    public static View spacer(Context c, float heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, (int) dp(c, heightDp)));
        return v;
    }
}
