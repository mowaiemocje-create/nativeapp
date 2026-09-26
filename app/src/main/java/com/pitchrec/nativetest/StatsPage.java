package com.pitchrec.nativetest;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// STATYSTYKI — jak zakladka STATS w PitchRec: podsumowanie tygodnia (nagrania, seria dni,
// lacznie), aktywnosc w ostatnich 5 tygodniach, ocena trenera, nagrania wg kategorii i
// osiagniecia. Sumy per kategoria pochodza z pelnej historii w NS (szybkie zrodlo, jak w PWA),
// a aktywnosc dzienna z nagran na telefonie.
public class StatsPage {

    public interface Host { String token(); String email(); boolean isCurrent(); }

    private final Activity a;
    private final LinearLayout root;
    private final Host host;
    private final float d;

    public StatsPage(Activity a, LinearLayout root, Host host) {
        this.a = a;
        this.root = root;
        this.host = host;
        this.d = a.getResources().getDisplayMetrics().density;
    }

    private static class Rec { long time; String cat; }

    public void render() {
        root.removeAllViews();
        TextView t = Ui.text(a, "STATYSTYKI", 18f, R.color.pr_muted);
        t.setLetterSpacing(0.3f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, (int) (12 * d));
        root.addView(t);

        List<Rec> local = new ArrayList<>();
        File[] files = a.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files != null) for (File f : files) { Rec r = new Rec(); r.time = f.lastModified(); r.cat = RecMeta.load(a, f.getName()).cat; local.add(r); }

        LinearLayout harm = Ui.card(a);
        root.addView(harm);
        loadHarmonogram(harm);
        LinearLayout summary = Ui.card(a);
        root.addView(summary);
        LinearLayout activity = Ui.card(a);
        root.addView(activity);
        LinearLayout review = Ui.card(a);
        review.setVisibility(View.GONE);
        root.addView(review);
        LinearLayout cats = Ui.card(a);
        root.addView(cats);
        LinearLayout miles = Ui.card(a);
        root.addView(miles);

        // Najpierw dane lokalne (od razu), potem pelne sumy z NS (gdy przyjda)
        Map<String, Integer> localCounts = new LinkedHashMap<>();
        for (Rec r : local) if (r.cat != null && !r.cat.isEmpty()) localCounts.put(r.cat, localCounts.getOrDefault(r.cat, 0) + 1);
        fillSummary(summary, local, -1);
        fillActivity(activity, local);
        fillCats(cats, localCounts, null, "z nagrań na tym telefonie");
        fillMilestones(miles, localCounts, local.size());

        String q = "/harmonogram-stats/from-ns?ns_token=" + NsClient.enc(host.token()) + "&ns_email=" + NsClient.enc(host.email())
                + "&ns_server=new&date_from=1970-01-01&date_to=2999-12-31";
        NsClient.backend("GET", q, null, r -> {
            if (!host.isCurrent() || !r.ok) return;
            try {
                JSONObject dj = new JSONObject(r.body);
                if (!dj.optBoolean("ok", false)) return;
                JSONObject by = dj.optJSONObject("byCategory");
                if (by == null) return;
                Map<String, Integer> sent = new LinkedHashMap<>(), correct = new LinkedHashMap<>();
                int total = 0;
                java.util.Iterator<String> it = by.keys();
                while (it.hasNext()) {
                    JSONObject c = by.optJSONObject(it.next());
                    if (c == null) continue;
                    String nm = c.optString("category_name", "");
                    if (nm.isEmpty()) continue;
                    int s = c.optInt("sent", 0);
                    sent.put(nm, sent.getOrDefault(nm, 0) + s);
                    correct.put(nm, correct.getOrDefault(nm, 0) + c.optInt("correct", 0));
                    total += s;
                }
                fillSummary(summary, local, total);
                fillCats(cats, sent, correct, "pełna historia z NewSpeech");
                fillMilestones(miles, sent, total);
                JSONObject ov = dj.optJSONObject("overall");
                if (ov != null) fillReview(review, ov.optInt("reviewed", 0), ov.optInt("correct", 0));
            } catch (Exception e) { /* zostaja dane lokalne */ }
        });
    }

    // ── HARMONOGRAM (jak panel wizyty w PitchRec): najblizsza wizyta z wymaganiami, postep per
    // kategoria w biezacym okresie (od poprzedniej wizyty do terminu), tempo realizacji. ──
    private void loadHarmonogram(LinearLayout card) {
        card.addView(Ui.label(a, "📅 HARMONOGRAM"));
        TextView loading = Ui.text(a, "⏳ Wczytywanie harmonogramu…", 12f, R.color.pr_muted);
        card.addView(loading);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=asc&date_from=" + today, host.token(), host.email(), null, null, r1 -> {
            JSONObject next = firstWithReqs(r1);
            if (next != null) { loadPrev(card, loading, next); return; }
            NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=desc", host.token(), host.email(), null, null, r2 -> {
                JSONObject last = firstWithReqs(r2);
                if (last == null) { loading.setText(r1.ok ? "Brak harmonogramu — trener nie ustalił jeszcze wymagań." : "Nie udało się wczytać harmonogramu."); return; }
                loadPrev(card, loading, last);
            });
        });
    }

    private static org.json.JSONArray coll(NsClient.Result r) {
        if (!r.ok) return null;
        try {
            String b = r.body.trim();
            if (b.startsWith("[")) return new org.json.JSONArray(b);
            return new JSONObject(b).optJSONArray("collection");
        } catch (Exception e) { return null; }
    }

    private static JSONObject firstWithReqs(NsClient.Result r) {
        org.json.JSONArray arr = coll(r);
        if (arr == null) return null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject v = arr.optJSONObject(i);
            if (v == null) continue;
            org.json.JSONArray reqs = v.optJSONArray("visit_record_requirements");
            if (reqs != null && reqs.length() > 0) return v;
        }
        return null;
    }

    private void loadPrev(LinearLayout card, TextView loading, JSONObject visit) {
        String vDate = visit.optString("date", "");
        String vId = visit.optString("id", "");
        NsClient.request("GET", "/visit_dates?page=1&page_size=10&sort_by=date&sort_order=desc", host.token(), host.email(), null, null, r -> {
            String start = null;
            org.json.JSONArray arr = coll(r);
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject v = arr.optJSONObject(i);
                    if (v == null || vId.equals(v.optString("id", ""))) continue;
                    String dd = v.optString("date", "");
                    if (!dd.isEmpty() && dd.compareTo(vDate) < 0) { start = dd; break; }
                }
            }
            if (start == null) start = visit.optString("created_at", "1970-01-01");
            String from = start.length() >= 10 ? start.substring(0, 10) : start;
            String to = vDate.length() >= 10 ? vDate.substring(0, 10) : vDate;
            String q = "/harmonogram-stats/from-ns?ns_token=" + NsClient.enc(host.token()) + "&ns_email=" + NsClient.enc(host.email())
                    + "&ns_server=new&date_from=" + from + "&date_to=" + to;
            final String fromF = from;
            NsClient.backend("GET", q, null, rs -> {
                if (!host.isCurrent()) return;
                JSONObject by = null;
                try { JSONObject dj = new JSONObject(rs.body); if (dj.optBoolean("ok", false)) by = dj.optJSONObject("byCategory"); } catch (Exception e) { }
                fillHarmonogram(card, visit, by, fromF, to);
            });
        });
    }

    private void fillHarmonogram(LinearLayout card, JSONObject visit, JSONObject by, String from, String to) {
        card.removeAllViews();
        card.addView(Ui.label(a, "📅 HARMONOGRAM"));
        long deadline = parseDay(to);
        long now = System.currentTimeMillis();
        int daysLeft = (int) Math.max(0, Math.ceil((deadline + 86400000L - now) / 86400000.0));
        String dl = to.length() >= 10 ? to.substring(8, 10) + "." + to.substring(5, 7) + "." + to.substring(0, 4) : to;
        TextView head = Ui.text(a, (deadline + 86400000L > now ? "Do wizyty " + dl + " — zostało " + daysLeft + " dni" : "Termin wizyty minął (" + dl + ")"), 13f, R.color.pr_text);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(head);
        org.json.JSONArray reqs = visit.optJSONArray("visit_record_requirements");
        int totReq = 0, totCorrect = 0, totSent = 0;
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; reqs != null && i < reqs.length(); i++) {
            JSONObject rq = reqs.optJSONObject(i);
            if (rq == null) continue;
            JSONObject cat = rq.optJSONObject("record_category");
            String cid = cat != null ? cat.optString("id", "") : "";
            String cname = cat != null ? (cat.optString("name_pl", "").isEmpty() ? cat.optString("name", "") : cat.optString("name_pl", "")) : "?";
            int need = rq.optInt("records_count", 0);
            int sent = 0, correct = 0, reviewed = 0;
            if (by != null && by.optJSONObject(cid) != null) {
                JSONObject b = by.optJSONObject(cid);
                sent = b.optInt("sent", 0); correct = b.optInt("correct", 0); reviewed = b.optInt("reviewed", 0);
            }
            totReq += need; totCorrect += Math.min(correct, need); totSent += sent;
            rows.add(new Object[]{cname, need, sent, correct, reviewed});
        }
        int pct = totReq > 0 ? Math.min(100, Math.round(totCorrect * 100f / totReq)) : 0;
        TextView sum = Ui.text(a, "Zaliczone: " + totCorrect + " z " + totReq + " (" + pct + "%) · wysłane w okresie: " + totSent, 12f, R.color.pr_muted);
        sum.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
        card.addView(sum);
        // Tempo (czysto motywacyjne — jak w PWA)
        if (totReq > 0 && deadline + 86400000L > now) {
            long start = parseDay(from);
            int elapsed = (int) Math.max(1, Math.ceil((now - start) / 86400000.0));
            int remaining = Math.max(0, totReq - totSent);
            double needed = daysLeft > 0 ? remaining / (double) daysLeft : remaining;
            double actual = totSent / (double) elapsed;
            boolean onTrack = actual >= needed - 0.01;
            TextView pace = Ui.text(a, onTrack ? "⚡ Świetnie! W tym tempie zdążysz przed końcem terminu (" + daysLeft + " dni zostało)."
                    : "⚡ Aby zdążyć na czas, potrzebujesz ok. " + (int) Math.ceil(needed) + " nagrań dziennie.", 12f, onTrack ? R.color.pr_accent : R.color.pr_warn);
            pace.setTypeface(Typeface.DEFAULT_BOLD);
            pace.setPadding(0, 0, 0, (int) (8 * d));
            card.addView(pace);
        }
        for (Object[] rw : rows) {
            int need = (Integer) rw[1], sent = (Integer) rw[2], correct = (Integer) rw[3];
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (3 * d), 0, (int) (3 * d));
            row.addView(Ui.text(a, (String) rw[0], 12f, R.color.pr_text), new LinearLayout.LayoutParams((int) (120 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout track = new LinearLayout(a);
            track.setBackground(Ui.rounded(Ui.col(a, R.color.pr_border), 0, 0, 4 * d));
            float w = need > 0 ? Math.min(1f, Math.max(0.03f, correct / (float) need)) : 0.03f;
            View fill = new View(a);
            fill.setBackground(Ui.rounded(correct >= need ? 0xFF00C853 : 0xFFE8820C, 0, 0, 4 * d));
            track.addView(fill, new LinearLayout.LayoutParams(0, (int) (9 * d), w));
            track.addView(new View(a), new LinearLayout.LayoutParams(0, (int) (9 * d), 1f - w + 0.0001f));
            row.addView(track, Ui.weight(1f, 8 * d));
            row.addView(Ui.text(a, "✓" + correct + "/" + need + (sent > correct ? " (wysł. " + sent + ")" : ""), 11f, correct >= need ? R.color.pr_accent : R.color.pr_muted));
            card.addView(row);
        }
    }

    private static long parseDay(String s) {
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(s.substring(0, 10)).getTime(); }
        catch (Exception e) { return System.currentTimeMillis(); }
    }

    private static String dayKey(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    private static int streak(List<Rec> recs) {
        Set<String> days = new HashSet<>();
        for (Rec r : recs) days.add(dayKey(r.time));
        int s = 0;
        Calendar c = Calendar.getInstance();
        while (days.contains(dayKey(c.getTimeInMillis()))) { s++; c.add(Calendar.DAY_OF_YEAR, -1); }
        return s;
    }

    private void fillSummary(LinearLayout card, List<Rec> local, int nsTotal) {
        card.removeAllViews();
        long weekAgo = System.currentTimeMillis() - 7L * 86400000L;
        int week = 0;
        Map<String, Integer> weekCats = new LinkedHashMap<>();
        for (Rec r : local) if (r.time >= weekAgo) { week++; if (r.cat != null && !r.cat.isEmpty()) weekCats.put(r.cat, weekCats.getOrDefault(r.cat, 0) + 1); }
        int st = streak(local);
        LinearLayout row = Ui.row(a);
        row.addView(bigStat(String.valueOf(week), "w tym tygodniu", R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(st > 0 ? "🔥" + st : "—", "dni z rzędu", R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(String.valueOf(nsTotal >= 0 ? nsTotal : local.size()), nsTotal >= 0 ? "łącznie w NS" : "łącznie", R.color.pr_purple), Ui.weight(1f, 0));
        card.addView(row);
        String top = null; int topN = 0;
        for (Map.Entry<String, Integer> e : weekCats.entrySet()) if (e.getValue() > topN) { topN = e.getValue(); top = e.getKey(); }
        if (top != null) {
            TextView tt = Ui.text(a, "Najczęściej w tym tygodniu: " + top + " (" + topN + "x)", 11f, R.color.pr_muted);
            tt.setGravity(Gravity.CENTER);
            tt.setPadding(0, (int) (10 * d), 0, 0);
            card.addView(tt);
        }
    }

    private View bigStat(String val, String label, int colorRes) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        TextView v = Ui.text(a, val, 26f, colorRes);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        l.addView(v);
        TextView lb = Ui.text(a, label, 10f, R.color.pr_muted);
        lb.setGravity(Gravity.CENTER);
        l.addView(lb);
        return l;
    }

    private void fillActivity(LinearLayout card, List<Rec> local) {
        card.removeAllViews();
        card.addView(Ui.label(a, "📅 AKTYWNOŚĆ — OSTATNIE 5 TYGODNI"));
        Map<String, Integer> perDay = new LinkedHashMap<>();
        for (Rec r : local) perDay.put(dayKey(r.time), perDay.getOrDefault(dayKey(r.time), 0) + 1);
        int[] weeks = new int[5];
        Calendar now = Calendar.getInstance();
        for (int w = 4; w >= 0; w--) {
            int tot = 0;
            for (int dd = 0; dd < 7; dd++) {
                Calendar c = (Calendar) now.clone();
                c.add(Calendar.DAY_OF_YEAR, -(w * 7 + dd));
                tot += perDay.getOrDefault(dayKey(c.getTimeInMillis()), 0);
            }
            weeks[4 - w] = tot;
        }
        int st = streak(local);
        String motiv;
        if (st >= 3) motiv = "🔥 " + st + " dni z rzędu — tak trzymaj!";
        else if (weeks[4] > weeks[3] && weeks[3] > 0) motiv = "📈 Rośniesz! Ten tydzień: " + weeks[4] + ", poprzedni: " + weeks[3];
        else if (weeks[4] == 0) motiv = "💪 Wróć do treningu — jedno nagranie dziś to już sukces!";
        else motiv = "🎯 Utrzymaj tempo — w tym tygodniu: " + weeks[4];
        TextView m = Ui.text(a, motiv, 13f, R.color.pr_text);
        m.setTypeface(Typeface.DEFAULT_BOLD);
        m.setPadding(0, 0, 0, (int) (10 * d));
        card.addView(m);

        int max = 1;
        for (int v : weeks) max = Math.max(max, v);
        LinearLayout bars = Ui.row(a);
        bars.setGravity(Gravity.BOTTOM);
        LinearLayout labels = Ui.row(a);
        String[] lbl = {"-4 tyg.", "-3 tyg.", "-2 tyg.", "-1 tydz.", "ten tydz."};
        for (int i = 0; i < 5; i++) {
            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            boolean cur = i == 4;
            int color = cur ? 0xFFE8820C : (weeks[i] == 0 ? Ui.col(a, R.color.pr_border) : 0xFFFFB74D);
            TextView n = Ui.text(a, weeks[i] > 0 ? String.valueOf(weeks[i]) : "", 10f, R.color.pr_text);
            n.setGravity(Gravity.CENTER);
            n.setTextColor(color);
            col.addView(n);
            View bar = new View(a);
            bar.setBackground(Ui.rounded(color, 0, 0, 5 * d));
            int h = (int) (Math.max(6, weeks[i] * 80.0 / max) * d);
            col.addView(bar, new LinearLayout.LayoutParams((int) (34 * d), h));
            bars.addView(col, new LinearLayout.LayoutParams(0, (int) (100 * d), 1f));
            TextView l = Ui.text(a, lbl[i], 9f, cur ? R.color.pr_accent : R.color.pr_muted);
            l.setGravity(Gravity.CENTER);
            labels.addView(l, Ui.weight(1f, 0));
        }
        card.addView(bars);
        card.addView(labels);
    }

    private void fillReview(LinearLayout card, int reviewed, int correct) {
        card.removeAllViews();
        card.setVisibility(View.VISIBLE);
        card.addView(Ui.label(a, "✅ OCENA TRENERA"));
        int pct = reviewed > 0 ? Math.round(correct * 100f / reviewed) : 0;
        LinearLayout row = Ui.row(a);
        row.addView(bigStat(String.valueOf(reviewed), "ocenionych", R.color.pr_purple), Ui.weight(1f, 0));
        row.addView(bigStat(String.valueOf(correct), "zaliczonych", R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(pct + "%", "skuteczność", R.color.pr_accent), Ui.weight(1f, 0));
        card.addView(row);
    }

    private void fillCats(LinearLayout card, Map<String, Integer> counts, Map<String, Integer> correct, String source) {
        card.removeAllViews();
        card.addView(Ui.label(a, "📊 NAGRANIA WG KATEGORII"));
        List<Map.Entry<String, Integer>> l = new ArrayList<>(counts.entrySet());
        java.util.Collections.sort(l, (x, y) -> y.getValue() - x.getValue());
        if (l.isEmpty()) { card.addView(Ui.text(a, "Brak nagrań z kategorią.", 12f, R.color.pr_muted)); return; }
        int max = l.get(0).getValue();
        for (int i = 0; i < Math.min(8, l.size()); i++) {
            Map.Entry<String, Integer> e = l.get(i);
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (3 * d), 0, (int) (3 * d));
            TextView name = Ui.text(a, e.getKey(), 12f, R.color.pr_text);
            row.addView(name, new LinearLayout.LayoutParams((int) (120 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout track = new LinearLayout(a);
            track.setBackground(Ui.rounded(Ui.col(a, R.color.pr_border), 0, 0, 4 * d));
            View fill = new View(a);
            fill.setBackground(Ui.rounded(0xFFE8820C, 0, 0, 4 * d));
            float w = Math.max(0.03f, e.getValue() / (float) max);
            track.addView(fill, new LinearLayout.LayoutParams(0, (int) (9 * d), w));
            View rest = new View(a);
            track.addView(rest, new LinearLayout.LayoutParams(0, (int) (9 * d), 1f - w + 0.0001f));
            row.addView(track, Ui.weight(1f, 8 * d));
            String val = String.valueOf(e.getValue());
            if (correct != null && correct.containsKey(e.getKey())) val += " (✓" + correct.get(e.getKey()) + ")";
            row.addView(Ui.text(a, val, 11f, R.color.pr_muted));
            card.addView(row);
        }
        TextView src = Ui.text(a, "Źródło: " + source, 9f, R.color.pr_muted);
        src.setPadding(0, (int) (6 * d), 0, 0);
        card.addView(src);
    }

    // Osiagniecia — progi jak w PitchRec
    private void fillMilestones(LinearLayout card, Map<String, Integer> counts, int total) {
        card.removeAllViews();
        card.addView(Ui.label(a, "🏆 OSIĄGNIĘCIA"));
        Object[][] ms = {
                {"🌱", "Pierwsze 10 nagrań!", null, 10}, {"⭐", "50 nagrań łącznie", null, 50}, {"🌟", "100 nagrań łącznie", null, 100},
                {"💫", "200 nagrań łącznie", null, 200}, {"🏆", "500 nagrań łącznie", null, 500}, {"👑", "1000 nagrań łącznie", null, 1000},
                {"🏪", "10 nagrań w Sklepach", new String[]{"Sklepy"}, 10}, {"🛒", "50 nagrań w Sklepach", new String[]{"Sklepy"}, 50},
                {"🚶", "10 Przechodniów", new String[]{"Przechodzień"}, 10}, {"🏃", "50 Przechodniów", new String[]{"Przechodzień"}, 50},
                {"📱", "10 rozmów telefonicznych", new String[]{"Telefon do miasta", "Telefon rodzina/znajomi", "Phone do Kursanta/Trenera"}, 10},
                {"📞", "50 rozmów telefonicznych", new String[]{"Telefon do miasta", "Telefon rodzina/znajomi", "Phone do Kursanta/Trenera"}, 50},
                {"🎤", "Pierwsze Wystąpienie!", new String[]{"Wystąpienie"}, 1}, {"🎭", "5 Wystąpień", new String[]{"Wystąpienie"}, 5}
        };
        for (Object[] m : ms) {
            int need = (Integer) m[3];
            int cur = 0;
            if (m[2] == null) cur = total;
            else for (String c : (String[]) m[2]) for (Map.Entry<String, Integer> e : counts.entrySet()) if (norm(e.getKey()).equals(norm(c))) cur += e.getValue();
            boolean done = cur >= need;
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
            TextView ic = Ui.text(a, (String) m[0], 20f, R.color.pr_text);
            ic.setAlpha(done ? 1f : 0.4f);
            row.addView(ic, new LinearLayout.LayoutParams((int) (34 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout mid = new LinearLayout(a);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.addView(Ui.text(a, (String) m[1], 11f, done ? R.color.pr_accent : R.color.pr_text));
            LinearLayout track = new LinearLayout(a);
            track.setBackground(Ui.rounded(Ui.col(a, R.color.pr_border), 0, 0, 4 * d));
            float w = Math.min(1f, Math.max(done ? 1f : 0.04f, cur / (float) need));
            View fill = new View(a);
            fill.setBackground(Ui.rounded(done ? 0xFF00C853 : 0xFFE8820C, 0, 0, 4 * d));
            track.addView(fill, new LinearLayout.LayoutParams(0, (int) (7 * d), w));
            track.addView(new View(a), new LinearLayout.LayoutParams(0, (int) (7 * d), 1f - w + 0.0001f));
            mid.addView(track);
            row.addView(mid, Ui.weight(1f, 8 * d));
            row.addView(Ui.text(a, Math.min(cur, need) + "/" + need, 10f, R.color.pr_muted));
            card.addView(row);
        }
    }

    private static String norm(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('ł', 'l').toLowerCase(java.util.Locale.ROOT).trim();
    }
}
