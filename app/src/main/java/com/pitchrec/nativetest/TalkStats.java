package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ⏱ ILE JUŻ MÓWISZ — minuty i słowa kursanta z serwera (worker /talk-stats/mine).
// Kafelki Dziś / 7 dni / 30 dni / Razem, kategorie (wg minut), wykres 12 tygodni, komunikat motywujący.
// Ostatni wynik zapamiętany w telefonie — karta pokazuje się od razu, świeże dane dochodzą w tle.
public final class TalkStats {

    private TalkStats() { }

    public static void fill(Activity a, LinearLayout card, java.util.function.BooleanSupplier isCurrent) {
        SharedPreferences p = a.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        String token = p.getString("ns_token", null), email = p.getString("ns_email", "");
        if (token == null) { card.setVisibility(View.GONE); return; }
        String cached = p.getString("talk_stats_json", null);
        if (cached != null && email.equals(p.getString("talk_stats_email", ""))) {
            try { render(a, card, new JSONObject(cached)); } catch (Exception e) { card.setVisibility(View.GONE); }
        } else card.setVisibility(View.GONE);
        String q = "/talk-stats/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            if (!r.ok || r.body == null) return;
            try {
                JSONObject d = new JSONObject(r.body);
                if (!d.optBoolean("ok", false)) return;
                p.edit().putString("talk_stats_json", r.body).putString("talk_stats_email", email).apply();
                if (isCurrent == null || isCurrent.getAsBoolean()) render(a, card, d);
            } catch (Exception e) { }
        });
    }

    static String min(double m) {
        long v = Math.round(m);
        if (v < 60) return v + " min";
        return (v / 60) + " h " + (v % 60) + " min";
    }

    static String num(double n) {
        return String.format(Locale.getDefault(), "%,d", Math.round(n));
    }

    private static void render(Activity a, LinearLayout card, JSONObject d) {
        JSONObject T = d.optJSONObject("totals");
        if (T == null) { card.setVisibility(View.GONE); return; }
        float dp = a.getResources().getDisplayMetrics().density;
        card.removeAllViews();
        card.addView(Ui.label(a, "⏱ " + L.t("ILE JUŻ MÓWISZ")));

        LinearLayout tiles = Ui.row(a);
        String[][] def = {{"today", "Dziś"}, {"d7", "7 dni"}, {"d30", "30 dni"}, {"all", "Razem"}};
        for (String[] t : def) {
            JSONObject o = T.optJSONObject(t[0]);
            if (o == null) o = new JSONObject();
            LinearLayout tile = new LinearLayout(a);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setPadding((int) (2 * dp), (int) (8 * dp), (int) (2 * dp), (int) (8 * dp));
            tile.setBackground(Ui.rounded(Ui.col(a, R.color.pr_bg), 0, 0, 10 * dp));
            TextView lb = Ui.text(a, L.t(t[1]), 10f, R.color.pr_muted);
            lb.setGravity(Gravity.CENTER);
            tile.addView(lb);
            TextView big = Ui.text(a, min(o.optDouble("min", 0)), 15f, R.color.pr_accent);
            big.setTypeface(Typeface.DEFAULT_BOLD);
            big.setGravity(Gravity.CENTER);
            big.setMaxLines(1);
            tile.addView(big);
            TextView sm = Ui.text(a, num(o.optDouble("words", 0)) + " " + L.t("słów") + "\n" + num(o.optDouble("n", 0)) + " " + L.t("nagr."), 9f, R.color.pr_muted);
            sm.setGravity(Gravity.CENTER);
            tile.addView(sm);
            tiles.addView(tile, Ui.weight(1f, 3 * dp));
        }
        card.addView(tiles);

        // komunikat motywujący: dwa ostatnie tygodnie
        List<JSONObject> weeks = new ArrayList<>();
        JSONArray W = d.optJSONArray("weeks");
        for (int i = 0; W != null && i < W.length(); i++) if (W.optJSONObject(i) != null) weeks.add(W.optJSONObject(i));
        java.util.Collections.sort(weeks, (x, y) -> x.optString("week", "").compareTo(y.optString("week", "")));
        StringBuilder mot = new StringBuilder();
        JSONObject cur = weeks.isEmpty() ? null : weeks.get(weeks.size() - 1);
        JSONObject prev = weeks.size() < 2 ? null : weeks.get(weeks.size() - 2);
        if (cur != null && cur.optDouble("min", 0) > 0) mot.append(L.f("W tym tygodniu przeprowadziłeś już {0} rozmowy 💪", min(cur.optDouble("min", 0))));
        if (cur != null && prev != null && prev.optDouble("min", 0) > 0 && cur.optDouble("min", 0) > prev.optDouble("min", 0)) {
            if (mot.length() > 0) mot.append("\n");
            mot.append(L.f("+{0}% minut względem zeszłego tygodnia", Math.round((cur.optDouble("min", 0) - prev.optDouble("min", 0)) / prev.optDouble("min", 1) * 100)));
        }
        if (mot.length() > 0) {
            TextView m = Ui.text(a, mot.toString(), 12f, R.color.pr_accent);
            m.setTypeface(Typeface.DEFAULT_BOLD);
            m.setPadding(0, (int) (10 * dp), 0, 0);
            card.addView(m);
        }

        // kategorie wg minut
        List<JSONObject> cats = new ArrayList<>();
        JSONArray C = d.optJSONArray("categories");
        for (int i = 0; C != null && i < C.length(); i++) if (C.optJSONObject(i) != null) cats.add(C.optJSONObject(i));
        java.util.Collections.sort(cats, (x, y) -> Double.compare(y.optDouble("min", 0), x.optDouble("min", 0)));
        double mx = cats.isEmpty() ? 1 : Math.max(1, cats.get(0).optDouble("min", 0));
        for (JSONObject c : cats) {
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (8 * dp), 0, 0);
            String nm = c.optString("category", "");
            TextView n = Ui.text(a, icon(nm) + " " + L.cat(nm), 11f, R.color.pr_text);
            n.setMaxLines(1);
            row.addView(n, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Ui.text(a, min(c.optDouble("min", 0)) + " · " + num(c.optDouble("words", 0)) + " " + L.t("słów"), 11f, R.color.pr_muted));
            card.addView(row);
            card.addView(bar(a, c.optDouble("min", 0) / mx, 5));
        }

        // wykres minut w tygodniach (12)
        if (!weeks.isEmpty()) {
            List<JSONObject> w12 = weeks.subList(Math.max(0, weeks.size() - 12), weeks.size());
            double wm = 1;
            for (JSONObject w : w12) wm = Math.max(wm, w.optDouble("min", 0));
            TextView wl = Ui.text(a, L.t("Minuty w tygodniach"), 10f, R.color.pr_muted);
            wl.setPadding(0, (int) (12 * dp), 0, (int) (4 * dp));
            card.addView(wl);
            LinearLayout chart = new LinearLayout(a);
            chart.setOrientation(LinearLayout.HORIZONTAL);
            chart.setGravity(Gravity.BOTTOM);
            int H = (int) (56 * dp);
            for (int i = 0; i < w12.size(); i++) {
                View b = new View(a);
                int h = Math.max((int) (2 * dp), (int) (w12.get(i).optDouble("min", 0) / wm * H));
                int col = Ui.col(a, R.color.pr_accent);
                b.setBackground(Ui.rounded(i == w12.size() - 1 ? col : ((col & 0x00FFFFFF) | 0x73000000), 0, 0, 3 * dp));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, 1f);
                lp.setMargins((int) (1.5f * dp), 0, (int) (1.5f * dp), 0);
                chart.addView(b, lp);
            }
            card.addView(chart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, H));
            LinearLayout dates = Ui.row(a);
            dates.addView(Ui.text(a, ddmm(w12.get(0).optString("week", "")), 9f, R.color.pr_muted), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            dates.addView(Ui.text(a, ddmm(w12.get(w12.size() - 1).optString("week", "")), 9f, R.color.pr_muted));
            card.addView(dates);
        }
        if (d.optInt("without_text", 0) > 0) {
            TextView wt = Ui.text(a, "⏳ " + L.t("Część nagrań jest jeszcze przeliczana"), 10f, R.color.pr_muted);
            wt.setPadding(0, (int) (8 * dp), 0, 0);
            card.addView(wt);
        }
        card.setVisibility(View.VISIBLE);
    }

    private static String icon(String cat) {
        String k = cat.toLowerCase(Locale.ROOT);
        if (k.startsWith("sklep")) return "🛒";
        if (k.startsWith("rodzina")) return "🏠";
        if (k.startsWith("telefon rodzina")) return "📱";
        if (k.startsWith("telefon do miasta")) return "☎️";
        if (k.startsWith("phone")) return "📞";
        if (k.startsWith("wyst")) return "🎤";
        if (k.startsWith("praca")) return "💼";
        if (k.startsWith("monolog")) return "📖";
        if (k.startsWith("przech")) return "🚶";
        if (k.startsWith("miasto")) return "🏙️";
        if (k.startsWith("przyjac")) return "🤝";
        if (k.startsWith("w grupie")) return "👥";
        if (k.startsWith("restaur")) return "🍽️";
        if (k.startsWith("mcdrive")) return "🍟";
        if (k.startsWith("zer")) return "🌱";
        return "🎙️";
    }

    private static String ddmm(String iso) {
        return iso != null && iso.length() >= 10 ? iso.substring(8, 10) + "." + iso.substring(5, 7) : "";
    }

    private static View bar(Activity a, double frac, int hDp) {
        float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout track = new LinearLayout(a);
        track.setBackground(Ui.rounded(Ui.col(a, R.color.pr_border), 0, 0, 4 * dp));
        View fill = new View(a);
        fill.setBackground(Ui.rounded(Ui.col(a, R.color.pr_accent), 0, 0, 4 * dp));
        float f = (float) Math.max(0.03, Math.min(1, frac));
        track.addView(fill, new LinearLayout.LayoutParams(0, (int) (hDp * dp), f));
        track.addView(new View(a), new LinearLayout.LayoutParams(0, (int) (hDp * dp), 1f - f + 0.0001f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (hDp * dp));
        lp.topMargin = (int) (3 * dp);
        track.setLayoutParams(lp);
        return track;
    }
}
