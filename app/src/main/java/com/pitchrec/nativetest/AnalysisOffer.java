package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// OFERTA: ANALIZA MOWY Z TRENEREM — dla niezalogowanych i dla kursantow, ktorym skonczyl sie
// harmonogram. Link otwiera od razu formularz zamowienia w sklepie nowamowa.com (analiza jest
// juz w koszyku — wystarczy wpisac dane i zaplacic).
public final class AnalysisOffer {

    public static final String URL_60 = "https://nowamowa.com/sklep/?kup=analiza-60";
    public static final String URL_120 = "https://nowamowa.com/sklep/?kup=analiza-120";

    private AnalysisOffer() { }

    // Karta z dwoma przyciskami; ended = kursant po zakonczonym harmonogramie (inny tekst)
    public static LinearLayout card(Activity a, boolean ended) {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.rounded(0x1A00E676, 0xFF00E676, Ui.dp(a, 1.5f), Ui.dp(a, 12)));
        TextView t = Ui.text(a, "🎯 " + L.t("ANALIZA MOWY Z TRENEREM"), 14f, R.color.pr_text);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF00E676);
        card.addView(t);
        TextView sub = Ui.text(a, ended
                ? L.t("Twój harmonogram się zakończył. Zamów analizę mowy — trener oceni postępy i przygotuje plan na kolejny etap.")
                : L.t("Sprawdź, jak mówisz. Trener przeanalizuje Twoją mowę i przygotuje plan ćwiczeń dopasowany do Ciebie."), 13f, R.color.pr_text);
        sub.setPadding(0, (int) Ui.dp(a, 4), 0, (int) Ui.dp(a, 8));
        card.addView(sub);
        card.addView(buyButton(a, "⏱ " + L.t("Analiza 60 min") + " · 250 zł", URL_60, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) Ui.dp(a, 6);
        card.addView(buyButton(a, "⏱ " + L.t("Analiza 120 min") + " · 500 zł", URL_120, false), lp);
        TextView n = Ui.text(a, L.t("Otworzy się formularz zamówienia na nowamowa.com — wpisz dane i zapłać."), 11f, R.color.pr_muted);
        n.setPadding(0, (int) Ui.dp(a, 6), 0, 0);
        card.addView(n);
        return card;
    }

    private static Button buyButton(Activity a, String text, String url, boolean filled) {
        Button b = Ui.button(a, text, R.color.pr_accent, filled);
        b.setOnClickListener(v -> open(a, url));
        return b;
    }

    public static void open(Activity a, String url) {
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) { Toast.makeText(a, url, Toast.LENGTH_LONG).show(); }
    }

    // Okienko z oferta (np. z okna "Wymagane logowanie")
    public static void dialog(Activity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) Ui.dp(a, 14);
        box.setPadding(p, (int) Ui.dp(a, 6), p, 0);
        box.addView(card(a, false));
        new AlertDialog.Builder(a).setView(box).setPositiveButton(L.t("Zamknij"), null).show();
    }
}
