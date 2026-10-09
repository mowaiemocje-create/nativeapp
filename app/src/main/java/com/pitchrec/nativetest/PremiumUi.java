package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// PELNA WERSJA — okienko z oferta i karta w Ustawieniach (tylko wersja Google Play).
public final class PremiumUi {

    private PremiumUi() { }

    static String features() {
        return "📂 " + L.t("Kategorie rozmów i gwiazdki — jak się czułeś w każdej sytuacji") + "\n"
                + "📈 " + L.t("Statystyki i postęp — zobaczysz, jak spada stres") + "\n"
                + "🏆 " + L.t("40 odznak za kolejne nagrania") + "\n"
                + "🎬 " + L.t("Zadania specjalne — nowe wyzwanie co 10 nagrań");
    }

    static String buyLabel(Activity a) {
        String p = Premium.price(a);
        return "⭐ " + L.t("Kup pełną wersję") + (p.isEmpty() ? "" : " · " + p);
    }

    // Okienko z oferta; why = co uzytkownik probowal otworzyc (moze byc null)
    public static void offer(Activity a, String why, Runnable onChange) {
        if (a.isFinishing()) return;
        String msg = (why == null ? "" : why + "\n\n") + features() + "\n\n"
                + L.t("Jednorazowa opłata, bez abonamentu.") + "\n"
                + L.t("Kursanci Nowej Mowy: zaloguj się — masz wszystko bez kupowania.");
        new AlertDialog.Builder(a)
                .setTitle("⭐ " + L.t("Pełna wersja"))
                .setMessage(msg)
                .setPositiveButton(buyLabel(a), (d, w) -> Premium.buy(a, onChange))
                .setNeutralButton(L.t("Przywróć zakup"), (d, w) -> Premium.restore(a, onChange))
                .setNegativeButton(L.t("Nie teraz"), null)
                .show();
    }

    // Karta w Ustawieniach
    public static LinearLayout card(Activity a, Runnable onChange) {
        LinearLayout card = Ui.card(a);
        TextView t = Ui.text(a, "⭐ " + L.t("PEŁNA WERSJA"), 14f, R.color.pr_text);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);
        if (Access.loggedIn(a)) {
            card.addView(sub(a, "🎓 " + L.t("Konto Nowej Mowy — wszystkie funkcje są odblokowane.")));
            return card;
        }
        if (Premium.owned(a)) {
            TextView ok = sub(a, "✓ " + L.t("Pełna wersja aktywna. Dziękujemy!"));
            ok.setTextColor(0xFF00E676);
            card.addView(ok);
            return card;
        }
        card.addView(sub(a, features()));
        card.addView(sub(a, L.t("Jednorazowa opłata, bez abonamentu. Zakup zostaje na Twoim koncie Google — także po zmianie telefonu.")));
        Button buy = Ui.button(a, buyLabel(a), R.color.pr_accent, true);
        buy.setOnClickListener(v -> Premium.buy(a, onChange));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) Ui.dp(a, 8);
        card.addView(buy, lp);
        Button rest = Ui.button(a, L.t("Przywróć zakup"), R.color.pr_muted, false);
        rest.setOnClickListener(v -> Premium.restore(a, onChange));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = (int) Ui.dp(a, 6);
        card.addView(rest, lp2);
        card.addView(sub(a, "🎓 " + L.t("Kursanci Nowej Mowy: zaloguj się powyżej — masz wszystko bez kupowania.")));
        return card;
    }

    private static TextView sub(Activity a, String s) {
        TextView v = Ui.text(a, s, 12f, R.color.pr_muted);
        v.setPadding(0, (int) Ui.dp(a, 6), 0, 0);
        return v;
    }
}
