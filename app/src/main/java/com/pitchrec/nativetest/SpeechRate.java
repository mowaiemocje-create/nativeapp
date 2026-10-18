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

// 🗣 TEMPO MOWY W MONOLOGACH — z serwera (worker /speech-rate/mine), liczone po przepisaniu CAŁEGO nagrania:
// sylaby i słowa z tekstu AI, czas mówienia z czasów słów. Tempo mowy = bez pauz, tempo ogólne = z pauzami.
// Tylko monologi / czytanie od 1 minuty. Ostatni wynik zapamiętany w telefonie.
public final class SpeechRate {

    private SpeechRate() { }

    public static void fill(Activity a, LinearLayout card, java.util.function.BooleanSupplier isCurrent) {
        SharedPreferences p = a.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        String token = p.getString("ns_token", null), email = p.getString("ns_email", "");
        if (token == null) { card.setVisibility(View.GONE); return; }
        String cached = p.getString("speech_rate_json", null);
        if (cached != null && email.equals(p.getString("speech_rate_email", ""))) {
            try { render(a, card, new JSONObject(cached)); } catch (Exception e) { card.setVisibility(View.GONE); }
        } else card.setVisibility(View.GONE);
        String q = "/speech-rate/mine?ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            if (!r.ok || r.body == null) return;
            try {
                JSONObject d = new JSONObject(r.body);
                if (!d.optBoolean("ok", false)) return;
                p.edit().putString("speech_rate_json", r.body).putString("speech_rate_email", email).apply();
                if (isCurrent == null || isCurrent.getAsBoolean()) render(a, card, d);
            } catch (Exception e) { }
        });
    }

    private static void render(Activity a, LinearLayout card, JSONObject d) {
        JSONArray items = d.optJSONArray("items");
        if (items == null || items.length() == 0) { card.setVisibility(View.GONE); return; }
        float dp = a.getResources().getDisplayMetrics().density;
        card.removeAllViews();
        card.addView(Ui.label(a, "🗣 " + L.t("TEMPO MOWY W MONOLOGACH")));

        // okres: ostatnie 7 dni, a gdy pusto — 30 dni, a gdy pusto — wszystko
        JSONObject per = d.optJSONObject("d7");
        String perName = L.t("ostatnie 7 dni");
        if (per == null || per.optInt("n", 0) == 0) { per = d.optJSONObject("d30"); perName = L.t("ostatnie 30 dni"); }
        if (per == null || per.optInt("n", 0) == 0) { per = d.optJSONObject("all"); perName = L.t("wszystkie monologi"); }
        if (per == null) per = new JSONObject();

        LinearLayout tiles = Ui.row(a);
        tiles.addView(tile(a, L.t("Tempo mowy"), per.optInt("syl_min_speech", 0) + "", L.t("sylab/min") + "\n" + per.optInt("wpm_speech", 0) + " " + L.t("słów/min")), Ui.weight(1f, 3 * dp));
        tiles.addView(tile(a, L.t("Z pauzami"), per.optInt("syl_min", 0) + "", L.t("sylab/min") + "\n" + per.optInt("wpm", 0) + " " + L.t("słów/min")), Ui.weight(1f, 3 * dp));
        tiles.addView(tile(a, L.t("Pauzy"), per.optInt("pause_pct", 0) + "%", L.t("nagrania")), Ui.weight(1f, 3 * dp));
        card.addView(tiles);
        TextView pn = Ui.text(a, L.f("Mediana: {0} · monologów: {1}", perName, per.optInt("n", 0)), 10f, R.color.pr_muted);
        pn.setPadding(0, (int) (6 * dp), 0, 0);
        card.addView(pn);

        // zmiana względem poprzedniego tygodnia
        JSONObject d7 = d.optJSONObject("d7"), p7 = d.optJSONObject("prev7");
        if (d7 != null && p7 != null && d7.optInt("n", 0) > 0 && p7.optInt("n", 0) > 0) {
            int diff = d7.optInt("syl_min_speech", 0) - p7.optInt("syl_min_speech", 0);
            String t = diff == 0 ? L.t("Tempo mowy takie samo jak tydzień wcześniej")
                    : L.f(diff > 0 ? "Tempo mowy +{0} sylab/min względem zeszłego tygodnia" : "Tempo mowy −{0} sylab/min względem zeszłego tygodnia", Math.abs(diff));
            TextView m = Ui.text(a, t, 12f, R.color.pr_accent);
            m.setTypeface(Typeface.DEFAULT_BOLD);
            m.setPadding(0, (int) (8 * dp), 0, 0);
            card.addView(m);
        }

        // ostatnie monologi
        TextView lh = Ui.text(a, L.t("Ostatnie monologi"), 10f, R.color.pr_muted);
        lh.setPadding(0, (int) (12 * dp), 0, (int) (2 * dp));
        card.addView(lh);
        for (int i = 0; i < Math.min(5, items.length()); i++) {
            JSONObject x = items.optJSONObject(i);
            if (x == null) continue;
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (4 * dp), 0, 0);
            String dt = x.optString("date", "");
            TextView n = Ui.text(a, (dt.length() >= 10 ? dt.substring(8, 10) + "." + dt.substring(5, 7) : dt) + "  " + Math.round(x.optDouble("dur", 0) / 60.0 * 10) / 10.0 + " min", 11f, R.color.pr_text);
            row.addView(n, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Ui.text(a, x.optInt("syl_min_speech", 0) + " " + L.t("syl/min") + " · " + x.optInt("wpm_speech", 0) + " " + L.t("sł/min")
                    + " · " + L.t("pauzy") + " " + x.optInt("pause_pct", 0) + "%", 11f, R.color.pr_muted));
            card.addView(row);
        }

        // tygodnie: tempo mowy (mediana)
        JSONArray W = d.optJSONArray("weeks");
        if (W != null && W.length() > 1) {
            double mx = 1;
            for (int i = 0; i < W.length(); i++) mx = Math.max(mx, W.optJSONObject(i) == null ? 0 : W.optJSONObject(i).optDouble("syl_min_speech", 0));
            TextView wl = Ui.text(a, L.t("Tempo mowy w tygodniach (sylab/min)"), 10f, R.color.pr_muted);
            wl.setPadding(0, (int) (12 * dp), 0, (int) (4 * dp));
            card.addView(wl);
            LinearLayout chart = new LinearLayout(a);
            chart.setOrientation(LinearLayout.HORIZONTAL);
            chart.setGravity(Gravity.BOTTOM);
            int H = (int) (56 * dp);
            for (int i = 0; i < W.length(); i++) {
                JSONObject w = W.optJSONObject(i);
                double v = w == null ? 0 : w.optDouble("syl_min_speech", 0);
                LinearLayout col = new LinearLayout(a);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                TextView val = Ui.text(a, String.valueOf(Math.round(v)), 8f, R.color.pr_muted);
                val.setGravity(Gravity.CENTER);
                col.addView(val);
                View b = new View(a);
                int c = Ui.col(a, R.color.pr_accent);
                b.setBackground(Ui.rounded(i == W.length() - 1 ? c : ((c & 0x00FFFFFF) | 0x73000000), 0, 0, 3 * dp));
                col.addView(b, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max((int) (2 * dp), (int) (v / mx * H))));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, H + (int) (14 * dp), 1f);
                lp.setMargins((int) (1.5f * dp), 0, (int) (1.5f * dp), 0);
                chart.addView(col, lp);
            }
            card.addView(chart);
        }
        TextView note = Ui.text(a, L.t("Liczone z całego nagrania po przepisaniu tekstu (monologi od 1 minuty). Tempo mowy — bez pauz."), 10f, R.color.pr_muted);
        note.setPadding(0, (int) (8 * dp), 0, 0);
        card.addView(note);
        if (d.optInt("without_text", 0) > 0) {
            TextView wt = Ui.text(a, "⏳ " + L.t("Część nagrań jest jeszcze przeliczana"), 10f, R.color.pr_muted);
            card.addView(wt);
        }
        card.setVisibility(View.VISIBLE);
    }

    // Szczegóły nagrania: tempo TEGO nagrania (worker /speech-rate/record) — dopisywane do okna, gdy przyjdzie
    public static void recordInto(Activity a, LinearLayout box, String recordId) {
        SharedPreferences p = a.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        String token = p.getString("ns_token", null), email = p.getString("ns_email", "");
        if (token == null || recordId == null || recordId.isEmpty()) return;
        float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout sec = new LinearLayout(a);
        sec.setOrientation(LinearLayout.VERTICAL);
        TextView l = Ui.label(a, "🗣 " + L.t("TEMPO MOWY"));
        l.setPadding(0, (int) (14 * dp), 0, (int) (2 * dp));
        sec.addView(l);
        TextView wait = Ui.text(a, "⏳ " + L.t("Liczę tempo…"), 12f, R.color.pr_muted);
        sec.addView(wait);
        box.addView(sec);
        String q = "/speech-rate/record?record_id=" + NsClient.enc(recordId) + "&ns_token=" + NsClient.enc(token) + "&ns_email=" + NsClient.enc(email) + "&ns_server=new";
        NsClient.backend("GET", q, null, r -> {
            sec.removeView(wait);
            JSONObject d = null;
            try { if (r.ok && r.body != null) d = new JSONObject(r.body); } catch (Exception e) { }
            if (d == null || !d.optBoolean("ok", false)) { sec.addView(Ui.text(a, L.t("Nie udało się policzyć tempa — spróbuj później."), 12f, R.color.pr_muted)); return; }
            if (d.optBoolean("pending", false)) { sec.addView(Ui.text(a, "⏳ " + L.t("Tekst nagrania jest jeszcze przepisywany — tempo pojawi się za kilka minut."), 12f, R.color.pr_muted)); return; }
            if (d.optBoolean("short", false) || !d.has("syl_min_speech")) { sec.addView(Ui.text(a, L.t("Nagranie za krótkie, by policzyć tempo."), 12f, R.color.pr_muted)); return; }
            if (!d.optBoolean("monolog", false)) {
                TextView w = Ui.text(a, "ℹ️ " + L.t("To rozmowa — w tekście są też słowa rozmówcy, więc tempo jest tylko orientacyjne. Dokładne tempo: monologi i czytanie."), 11f, R.color.pr_pause);
                w.setPadding(0, 0, 0, (int) (4 * dp));
                sec.addView(w);
            }
            LinearLayout tiles = Ui.row(a);
            tiles.addView(tile(a, L.t("Tempo mowy"), d.optInt("syl_min_speech", 0) + "", L.t("sylab/min") + "\n" + d.optInt("wpm_speech", 0) + " " + L.t("słów/min")), Ui.weight(1f, 3 * dp));
            tiles.addView(tile(a, L.t("Z pauzami"), d.optInt("syl_min", 0) + "", L.t("sylab/min") + "\n" + d.optInt("wpm", 0) + " " + L.t("słów/min")), Ui.weight(1f, 3 * dp));
            tiles.addView(tile(a, L.t("Pauzy"), d.optInt("pause_pct", 0) + "%", L.t("nagrania")), Ui.weight(1f, 3 * dp));
            sec.addView(tiles);
            TextView det = Ui.text(a, L.f("{0} sylab · {1} słów · mowa {2} s · pauzy {3} s (dłuższych niż 1 s: {4})",
                    d.optInt("syl", 0), d.optInt("words", 0), Math.round(d.optDouble("speech_sec", 0)), Math.round(d.optDouble("pause_sec", 0)), d.optInt("pauses_1s", 0)), 10f, R.color.pr_muted);
            det.setPadding(0, (int) (6 * dp), 0, 0);
            sec.addView(det);
        });
    }

    private static View tile(Activity a, String label, String big, String small) {
        float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout tile = new LinearLayout(a);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding((int) (2 * dp), (int) (8 * dp), (int) (2 * dp), (int) (8 * dp));
        tile.setBackground(Ui.rounded(Ui.col(a, R.color.pr_bg), 0, 0, 10 * dp));
        TextView lb = Ui.text(a, label, 10f, R.color.pr_muted);
        lb.setGravity(Gravity.CENTER);
        tile.addView(lb);
        TextView bg = Ui.text(a, big, 17f, R.color.pr_accent);
        bg.setTypeface(Typeface.DEFAULT_BOLD);
        bg.setGravity(Gravity.CENTER);
        tile.addView(bg);
        TextView sm = Ui.text(a, small, 9f, R.color.pr_muted);
        sm.setGravity(Gravity.CENTER);
        tile.addView(sm);
        return tile;
    }
}
