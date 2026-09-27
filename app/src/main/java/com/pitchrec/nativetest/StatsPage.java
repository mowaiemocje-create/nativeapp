package com.pitchrec.nativetest;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// STATYSTYKI — jak zakladka STATS w PitchRec:
//  • HARMONOGRAM: ile brakuje do kiedy, w ktorych kategoriach, tempo, pomysl na nagranie,
//    motywacja wg pory dnia
//  • 🎯 CO POPRAWIC: elementy techniki z ostatniej oceny trenera
//  • 💡 INSPIRACJE NA DZIS: miejsca w poblizu (OpenStreetMap) z pomyslem na rozmowe
//  • podsumowanie tygodnia, aktywnosc, ocena trenera, nagrania wg kategorii
//  • 🏆 pelna lista osiagniec (40 odznak)
public class StatsPage {

    public interface Host { String token(); String email(); boolean isCurrent(); }

    private final Activity a;
    private final LinearLayout root;
    private final Host host;
    private final float d;
    private int todayCount = 0;

    public StatsPage(Activity a, LinearLayout root, Host host) {
        this.a = a;
        this.root = root;
        this.host = host;
        this.d = a.getResources().getDisplayMetrics().density;
    }

    private static class Rec { long time; String cat; }

    public void render() {
        root.removeAllViews();
        TextView t = Ui.text(a, L.t("STATYSTYKI"), 18f, R.color.pr_muted);
        t.setLetterSpacing(0.3f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, (int) (12 * d));
        root.addView(t);

        List<Rec> local = new ArrayList<>();
        File[] files = a.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        String todayKey = dayKey(System.currentTimeMillis());
        if (files != null) for (File f : files) {
            Rec r = new Rec(); r.time = f.lastModified(); r.cat = RecMeta.load(a, f.getName()).cat; local.add(r);
            if (dayKey(r.time).equals(todayKey)) todayCount++;
        }

        LinearLayout harm = Ui.card(a);
        root.addView(harm);
        loadHarmonogram(harm);
        LinearLayout improve = Ui.card(a);
        improve.setVisibility(View.GONE);
        root.addView(improve);
        loadImprove(improve);
        LinearLayout insp = Ui.card(a);
        root.addView(insp);
        buildInspirations(insp);
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
        root.addView(Ui.spacer(a, 20));

        Map<String, Integer> localCounts = new LinkedHashMap<>();
        for (Rec r : local) if (r.cat != null && !r.cat.isEmpty()) localCounts.put(r.cat, localCounts.getOrDefault(r.cat, 0) + 1);
        fillSummary(summary, local, -1);
        fillActivity(activity, local);
        fillCats(cats, localCounts, null, L.t("z nagrań na tym telefonie"));
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
                fillCats(cats, sent, correct, L.t("pełna historia z NewSpeech"));
                fillMilestones(miles, sent, total);
                JSONObject ov = dj.optJSONObject("overall");
                if (ov != null) fillReview(review, ov.optInt("reviewed", 0), ov.optInt("correct", 0));
            } catch (Exception e) { /* zostaja dane lokalne */ }
        });
    }

    // ═════════════ HARMONOGRAM ═════════════
    private void loadHarmonogram(LinearLayout card) {
        card.addView(Ui.label(a, "📅 " + L.t("HARMONOGRAM")));
        TextView loading = Ui.text(a, "⏳ " + L.t("Wczytywanie harmonogramu…"), 12f, R.color.pr_muted);
        card.addView(loading);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=asc&date_from=" + today, host.token(), host.email(), null, null, r1 -> {
            JSONObject next = firstWithReqs(r1);
            if (next != null) { loadPrev(card, next); return; }
            NsClient.request("GET", "/visit_dates?page=1&page_size=5&sort_by=date&sort_order=desc", host.token(), host.email(), null, null, r2 -> {
                JSONObject last = firstWithReqs(r2);
                if (last == null) { loading.setText(r1.ok ? L.t("Brak harmonogramu — trener nie ustalił jeszcze wymagań.") : L.t("Nie udało się wczytać harmonogramu.")); return; }
                loadPrev(card, last);
            });
        });
    }

    private static JSONArray coll(NsClient.Result r) {
        if (!r.ok) return null;
        try {
            String b = r.body.trim();
            if (b.startsWith("[")) return new JSONArray(b);
            return new JSONObject(b).optJSONArray("collection");
        } catch (Exception e) { return null; }
    }

    private static JSONObject firstWithReqs(NsClient.Result r) {
        JSONArray arr = coll(r);
        if (arr == null) return null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject v = arr.optJSONObject(i);
            if (v == null) continue;
            JSONArray reqs = v.optJSONArray("visit_record_requirements");
            if (reqs != null && reqs.length() > 0) return v;
        }
        return null;
    }

    private void loadPrev(LinearLayout card, JSONObject visit) {
        String vDate = visit.optString("date", "");
        String vId = visit.optString("id", "");
        NsClient.request("GET", "/visit_dates?page=1&page_size=10&sort_by=date&sort_order=desc", host.token(), host.email(), null, null, r -> {
            String start = null;
            JSONArray arr = coll(r);
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
            NsClient.backend("GET", q, null, rs -> {
                if (!host.isCurrent()) return;
                JSONObject by = null;
                try { JSONObject dj = new JSONObject(rs.body); if (dj.optBoolean("ok", false)) by = dj.optJSONObject("byCategory"); } catch (Exception e) { }
                fillHarmonogram(card, visit, by, from, to);
            });
        });
    }

    private static int miss(Object[] rw) {
        int need = (Integer) rw[1], correct = (Integer) rw[3];
        return Math.max(0, need - Math.min(correct, need));
    }

    private void fillHarmonogram(LinearLayout card, JSONObject visit, JSONObject by, String from, String to) {
        card.removeAllViews();
        card.addView(Ui.label(a, "📅 " + L.t("HARMONOGRAM")));
        long deadline = parseDay(to);
        long now = System.currentTimeMillis();
        boolean active = deadline + 86400000L > now;
        int daysLeft = (int) Math.max(0, Math.ceil((deadline + 86400000L - now) / 86400000.0));
        String dl = to.length() >= 10 ? to.substring(8, 10) + "." + to.substring(5, 7) + "." + to.substring(0, 4) : to;

        JSONArray reqs = visit.optJSONArray("visit_record_requirements");
        int totReq = 0, totCorrect = 0, totSent = 0;
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; reqs != null && i < reqs.length(); i++) {
            JSONObject rq = reqs.optJSONObject(i);
            if (rq == null) continue;
            JSONObject cat = rq.optJSONObject("record_category");
            String cid = cat != null ? cat.optString("id", "") : "";
            String cname = cat != null ? (cat.optString("name_pl", "").isEmpty() ? cat.optString("name", "") : cat.optString("name_pl", "")) : "?";
            int need = rq.optInt("records_count", 0);
            int sent = 0, correct = 0;
            if (by != null && by.optJSONObject(cid) != null) {
                JSONObject b = by.optJSONObject(cid);
                sent = b.optInt("sent", 0); correct = b.optInt("correct", 0);
            }
            totReq += need; totCorrect += Math.min(correct, need); totSent += sent;
            rows.add(new Object[]{cname, need, sent, correct});
        }
        int missing = Math.max(0, totReq - totCorrect);
        int pct = totReq > 0 ? Math.min(100, Math.round(totCorrect * 100f / totReq)) : 0;

        // Duzy, czytelny naglowek: procent + ile brakuje + do kiedy
        LinearLayout hero = Ui.row(a);
        hero.setPadding(0, (int) (4 * d), 0, (int) (10 * d));
        TextView big = Ui.text(a, pct + "%", 40f, R.color.pr_accent);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        hero.addView(big, new LinearLayout.LayoutParams((int) (110 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout heroTxt = new LinearLayout(a);
        heroTxt.setOrientation(LinearLayout.VERTICAL);
        TextView h1 = Ui.text(a, missing == 0 ? "🎉 " + L.t("Harmonogram wykonany!") : L.f("Brakuje {0} zaliczonych nagrań", missing), 15f, R.color.pr_text);
        h1.setTypeface(Typeface.DEFAULT_BOLD);
        heroTxt.addView(h1);
        heroTxt.addView(Ui.text(a, active ? L.f("do wizyty {0} — zostało {1} dni", dl, daysLeft) : L.f("termin wizyty minął ({0})", dl), 12f, active && daysLeft <= 3 && missing > 0 ? R.color.pr_warn : R.color.pr_muted));
        heroTxt.addView(Ui.text(a, L.f("zaliczone {0} z {1} · wysłane {2}", totCorrect, totReq, totSent), 11f, R.color.pr_muted));
        hero.addView(heroTxt, Ui.weight(1f, 0));
        card.addView(hero);
        card.addView(bar(pct / 100f, pct >= 100 ? 0xFF00C853 : 0xFFE8820C, 10));

        // Tempo + motywacja
        if (totReq > 0 && active && missing > 0) {
            long start = parseDay(from);
            int elapsed = (int) Math.max(1, Math.ceil((now - start) / 86400000.0));
            int remainingToSend = Math.max(0, totReq - totSent);
            double needed = daysLeft > 0 ? Math.max(missing, remainingToSend) / (double) daysLeft : missing;
            double actual = totSent / (double) elapsed;
            boolean onTrack = actual >= needed - 0.01;
            card.addView(tip(onTrack ? "⚡ " + L.f("Świetnie! W tym tempie zdążysz przed terminem ({0} dni zostało).", daysLeft)
                    : "⚡ " + L.f("Aby zdążyć, potrzebujesz ok. {0} nagrań dziennie.", (int) Math.ceil(needed)), onTrack ? R.color.pr_accent : R.color.pr_warn));
        }
        card.addView(tip(motivation(), R.color.pr_accent));

        // Pomysl na nagranie: kategoria z najwiekszym brakiem
        Object[] worst = null;
        for (Object[] rw : rows) if (miss(rw) > 0 && (worst == null || miss(rw) > miss(worst))) worst = rw;
        if (worst != null) {
            String[] ideas = {"Nagraj w kategorii {0} — brakuje jeszcze {1} zaliczonych.", "Kategoria {0} czeka — jeszcze {1} nagrań i będzie zaliczona!", "Mała sesja w kategorii {0}? Potrzebujesz {1} zaliczonych."};
            card.addView(tip("💡 " + L.f(ideas[Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % ideas.length], L.cat((String) worst[0]), miss(worst)), R.color.pr_pause));
        }

        // Per kategoria: co i ile brakuje (najwieksze braki na gorze)
        TextView sub = Ui.label(a, L.t("CO JESZCZE TRZEBA NAGRAĆ"));
        sub.setPadding(0, (int) (12 * d), 0, (int) (6 * d));
        card.addView(sub);
        java.util.Collections.sort(rows, (x, y) -> Integer.compare(miss(y), miss(x)));
        for (Object[] rw : rows) {
            int need = (Integer) rw[1], sent = (Integer) rw[2], correct = (Integer) rw[3];
            int m = miss(rw);
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, (int) (4 * d), 0, (int) (6 * d));
            LinearLayout top = Ui.row(a);
            TextView nm = Ui.text(a, (m == 0 ? "✅ " : "") + L.cat((String) rw[0]), 13f, R.color.pr_text);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            top.addView(nm, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(Ui.text(a, m == 0 ? L.t("zaliczone") : L.f("brakuje {0}", m), 12f, m == 0 ? R.color.pr_accent : R.color.pr_warn));
            row.addView(top);
            row.addView(bar(need > 0 ? Math.min(1f, correct / (float) need) : 0f, m == 0 ? 0xFF00C853 : 0xFFE8820C, 7));
            String detail = L.f("✓ {0} z {1}", Math.min(correct, need), need) + (sent > correct ? " · " + L.f("{0} czeka na ocenę / do poprawy", sent - correct) : "");
            row.addView(Ui.text(a, detail, 10f, R.color.pr_muted));
            card.addView(row);
        }
    }

    // Pora dnia + dzisiejsze nagrania -> komunikat motywacyjny (jak w PitchRec)
    private String motivation() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int n = todayCount;
        int pick = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 2;
        String[] m;
        if (h < 10) m = n > 0 ? new String[]{"Dobry początek dnia! Już {0} nagrań za Tobą 💪", "Świetnie, że zaczynasz dzień od nagrań!"} : new String[]{"Dzień dobry! Dobry moment na pierwsze nagranie dnia.", "Rano głos jest najczystszy — to dobry czas na nagrania."};
        else if (h < 14) m = n > 0 ? new String[]{"Już {0} nagrań dzisiaj — tak dalej!", "Dobre tempo jak na środek dnia 👍"} : new String[]{"Połowa dnia — może czas na chwilę nagrań?", "Krótka przerwa na nagranie dobrze zrobi."};
        else if (h < 19) m = n > 0 ? new String[]{"Popołudniowy postęp: {0} nagrań 🎯", "Niezły wynik na dziś, jeszcze chwila i będzie więcej."} : new String[]{"Popołudnie to dobry czas na nagranie przed wieczorem.", "Jeszcze nic dzisiaj? Mała sesja teraz się przyda."};
        else m = n > 0 ? new String[]{"Dzisiaj zrobione: {0} nagrań — gratulacje!", "Dobry dzień za Tobą, {0} nagrań w skrzynce."} : new String[]{"Wieczór to ostatnia szansa na dzisiejsze nagranie.", "Jeszcze jedno nagranie przed snem?"};
        return "✨ " + L.f(m[pick], n);
    }

    private View tip(String text, int colorRes) {
        TextView t = Ui.text(a, text, 12f, R.color.pr_text);
        int c = Ui.col(a, colorRes);
        t.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
        t.setBackground(Ui.rounded((c & 0x00FFFFFF) | 0x18000000, (c & 0x00FFFFFF) | 0x66000000, d, 10 * d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (8 * d);
        t.setLayoutParams(lp);
        return t;
    }

    private View bar(float frac, int color, int hDp) {
        LinearLayout track = new LinearLayout(a);
        track.setBackground(Ui.rounded(Ui.col(a, R.color.pr_border), 0, 0, hDp / 2f * d));
        float w = Math.max(frac > 0 ? 0.03f : 0f, Math.min(1f, frac));
        View fill = new View(a);
        fill.setBackground(Ui.rounded(color, 0, 0, hDp / 2f * d));
        track.addView(fill, new LinearLayout.LayoutParams(0, (int) (hDp * d), w));
        track.addView(new View(a), new LinearLayout.LayoutParams(0, (int) (hDp * d), 1f - w + 0.0001f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (3 * d);
        lp.bottomMargin = (int) (3 * d);
        track.setLayoutParams(lp);
        return track;
    }

    // ═════════════ CO POPRAWIC (ostatnia ocena trenera) ═════════════
    private void loadImprove(LinearLayout card) {
        NsClient.request("GET", "/records?page_size=10&sort_by=date&sort_order=desc", host.token(), host.email(), null, null, r -> {
            if (!host.isCurrent()) return;
            JSONArray arr = coll(r);
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject rec = arr.optJSONObject(i);
                if (rec == null || rec.isNull("reviewed_at")) continue;
                JSONArray rates = rec.optJSONArray("record_rates");
                if (rates == null || rates.length() == 0) continue;
                List<String[]> bad = new ArrayList<>();
                for (int j = 0; j < rates.length(); j++) {
                    JSONObject rt = rates.optJSONObject(j);
                    if (rt == null) continue;
                    JSONObject unit = rt.optJSONObject("record_rate_unit");
                    JSONObject cat = rt.optJSONObject("record_rate_category");
                    if (unit == null || unit.optInt("percent_mark", 100) >= 100) continue;
                    bad.add(new String[]{loc(cat), loc(unit), String.valueOf(unit.optInt("percent_mark", 0))});
                }
                if (bad.isEmpty()) return;
                card.setVisibility(View.VISIBLE);
                card.addView(Ui.label(a, "🎯 " + L.t("CO POPRAWIĆ W NASTĘPNYM NAGRANIU")));
                card.addView(Ui.text(a, L.t("Z ostatniej oceny trenera:"), 11f, R.color.pr_muted));
                for (String[] b : bad) {
                    int pct = Integer.parseInt(b[2]);
                    TextView t = Ui.text(a, "● " + b[0] + " — " + b[1], 13f, R.color.pr_text);
                    t.setTextColor(pct < 50 ? 0xFFFF5050 : 0xFFE8820C);
                    t.setPadding(0, (int) (4 * d), 0, 0);
                    card.addView(t);
                }
                return;
            }
        });
    }

    private static String loc(JSONObject o) {
        if (o == null) return "";
        String l = L.lang();
        String v = "cs".equals(l) || "sk".equals(l) ? o.optString("name_cz", "") : "pl".equals(l) ? o.optString("name_pl", "") : o.optString("name_en", "");
        if (v.isEmpty()) v = o.optString("name_pl", "");
        return v.isEmpty() ? o.optString("name", "") : v;
    }

    // ═════════════ INSPIRACJE NA DZIS ═════════════
    private void buildInspirations(LinearLayout card) {
        card.addView(Ui.label(a, "💡 " + L.t("INSPIRACJE NA DZIŚ")));
        TextView info = Ui.text(a, L.t("Miejsca w pobliżu i pomysły, co nagrać."), 12f, R.color.pr_muted);
        card.addView(info);
        Button go = Ui.button(a, "📍 " + L.t("Pokaż miejsca w pobliżu"), R.color.pr_accent, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (8 * d);
        card.addView(go, lp);
        go.setOnClickListener(v -> {
            go.setVisibility(View.GONE);
            info.setText("⏳ " + L.t("Pobieranie GPS…"));
            if (!GpsHelper.hasPermission(a)) { MainActivity.requestLocationPermission(a); info.setText("📍 " + L.t("Włącz lokalizację i spróbuj ponownie.")); go.setVisibility(View.VISIBLE); return; }
            GpsHelper.requestFix(a, loc -> {
                android.location.Location l = loc != null ? loc : GpsHelper.lastFix;
                if (l == null) { info.setText("📍 " + L.t("GPS niedostępny. Włącz lokalizację i spróbuj ponownie.")); go.setVisibility(View.VISIBLE); return; }
                info.setText("⏳ " + L.t("Szukam miejsc w pobliżu…"));
                fetchPlaces(card, info, l.getLatitude(), l.getLongitude());
            });
        });
    }

    private void fetchPlaces(LinearLayout card, TextView info, double lat, double lon) {
        String q = "[out:json][timeout:20];(node[\"amenity\"](around:3000," + lat + "," + lon + ");node[\"shop\"](around:3000," + lat + "," + lon + ");"
                + "node[\"leisure\"~\"park|garden|sports_centre\"][\"name\"](around:3000," + lat + "," + lon + ");"
                + "node[\"tourism\"~\"museum|attraction\"][\"name\"](around:3000," + lat + "," + lon + "););out center tags;";
        byte[] body = ("data=" + NsClient.enc(q)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        NsClient.request("POST", "/overpass", null, null, "application/x-www-form-urlencoded", body, r -> {
            if (!host.isCurrent()) return;
            List<Object[]> places = new ArrayList<>(); // nazwa, odleglosc [m], typ
            Set<String> seen = new HashSet<>();
            String[] skip = {"parking", "toilets", "bench", "waste_basket", "bicycle_parking", "atm", "charging_station", "vending_machine", "recycling"};
            try {
                JSONArray els = new JSONObject(r.body).optJSONArray("elements");
                for (int i = 0; els != null && i < els.length(); i++) {
                    JSONObject el = els.optJSONObject(i);
                    JSONObject tags = el == null ? null : el.optJSONObject("tags");
                    if (tags == null) continue;
                    String name = tags.optString("name", "");
                    if (name.isEmpty() || !seen.add(name.toLowerCase(Locale.ROOT))) continue;
                    JSONObject center = el.optJSONObject("center");
                    double la = el.optDouble("lat", center != null ? center.optDouble("lat", 0) : 0);
                    double lo = el.optDouble("lon", center != null ? center.optDouble("lon", 0) : 0);
                    if (la == 0) continue;
                    String type = tags.optString("amenity", tags.optString("shop", tags.optString("leisure", tags.optString("tourism", "_shop"))));
                    boolean sk = false;
                    for (String s : skip) if (s.equals(type)) sk = true;
                    if (sk) continue;
                    double dLat = (la - lat) * 111000, dLon = (lo - lon) * 111000 * Math.cos(Math.toRadians(lat));
                    places.add(new Object[]{name, (int) Math.round(Math.sqrt(dLat * dLat + dLon * dLon)), type});
                }
            } catch (Exception e) { }
            java.util.Collections.sort(places, (x, y) -> (Integer) x[1] - (Integer) y[1]);
            if (places.isEmpty()) { info.setText(r.ok ? L.t("Nie znaleziono miejsc w pobliżu.") : L.t("Nie udało się pobrać miejsc — sprawdź internet.")); return; }
            info.setText("📍 " + L.f("MIEJSCA W POBLIŻU ({0})", places.size()));
            showPlaces(card, places, 0);
        });
    }

    private void showPlaces(LinearLayout card, List<Object[]> places, int from) {
        int to = Math.min(places.size(), from + 10);
        for (int i = from; i < to; i++) {
            Object[] p = places.get(i);
            Places.P info = Places.get((String) p[2]);
            LinearLayout row = Ui.row(a);
            row.setGravity(Gravity.TOP);
            row.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
            row.setBackground(Ui.rounded(Ui.col(a, R.color.pr_bg), 0, 0, 8 * d));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.topMargin = (int) (6 * d);
            row.addView(Ui.text(a, info.icon, 20f, R.color.pr_text), new LinearLayout.LayoutParams((int) (34 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout txt = new LinearLayout(a);
            txt.setOrientation(LinearLayout.VERTICAL);
            TextView nm = Ui.text(a, p[0] + "  (" + p[1] + " m)", 13f, R.color.pr_text);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            txt.addView(nm);
            txt.addView(Ui.text(a, Places.tip((String) p[2], (String) p[0], ((String) p[0]).hashCode() + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)), 12f, R.color.pr_muted));
            txt.addView(Ui.text(a, "→ " + L.cat(info.cat), 10f, R.color.pr_accent));
            row.addView(txt, Ui.weight(1f, 0));
            card.addView(row, rp);
        }
        if (to < places.size()) {
            Button more = Ui.button(a, L.f("Pokaż kolejne {0} (zostało {1})", Math.min(10, places.size() - to), places.size() - to), R.color.pr_accent, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * d);
            card.addView(more, lp);
            more.setOnClickListener(v -> { card.removeView(more); showPlaces(card, places, to); });
        }
    }

    // ═════════════ PODSUMOWANIE / AKTYWNOSC / OCENY / KATEGORIE ═════════════
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

    private static long parseDay(String s) {
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s.substring(0, 10)).getTime(); }
        catch (Exception e) { return System.currentTimeMillis(); }
    }

    private void fillSummary(LinearLayout card, List<Rec> local, int nsTotal) {
        card.removeAllViews();
        long weekAgo = System.currentTimeMillis() - 7L * 86400000L;
        int week = 0;
        Map<String, Integer> weekCats = new LinkedHashMap<>();
        for (Rec r : local) if (r.time >= weekAgo) { week++; if (r.cat != null && !r.cat.isEmpty()) weekCats.put(r.cat, weekCats.getOrDefault(r.cat, 0) + 1); }
        int st = streak(local);
        LinearLayout row = Ui.row(a);
        row.addView(bigStat(String.valueOf(week), L.t("w tym tygodniu"), R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(st > 0 ? "🔥" + st : "—", L.t("dni z rzędu"), R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(String.valueOf(nsTotal >= 0 ? nsTotal : local.size()), nsTotal >= 0 ? L.t("łącznie w NS") : L.t("łącznie"), R.color.pr_purple), Ui.weight(1f, 0));
        card.addView(row);
        String top = null; int topN = 0;
        for (Map.Entry<String, Integer> e : weekCats.entrySet()) if (e.getValue() > topN) { topN = e.getValue(); top = e.getKey(); }
        if (top != null) {
            TextView tt = Ui.text(a, L.f("Najczęściej w tym tygodniu: {0} ({1}x)", L.cat(top), topN), 11f, R.color.pr_muted);
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
        card.addView(Ui.label(a, "📅 " + L.t("AKTYWNOŚĆ — OSTATNIE 5 TYGODNI")));
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
        if (st >= 3) motiv = "🔥 " + L.f("{0} dni z rzędu — tak trzymaj!", st);
        else if (weeks[4] > weeks[3] && weeks[3] > 0) motiv = "📈 " + L.f("Rośniesz! Ten tydzień: {0}, poprzedni: {1}", weeks[4], weeks[3]);
        else if (weeks[4] == 0) motiv = "💪 " + L.t("Wróć do treningu — jedno nagranie dziś to już sukces!");
        else motiv = "🎯 " + L.f("Utrzymaj tempo — w tym tygodniu: {0}", weeks[4]);
        TextView m = Ui.text(a, motiv, 13f, R.color.pr_text);
        m.setTypeface(Typeface.DEFAULT_BOLD);
        m.setPadding(0, 0, 0, (int) (10 * d));
        card.addView(m);

        int max = 1;
        for (int v : weeks) max = Math.max(max, v);
        LinearLayout bars = Ui.row(a);
        bars.setGravity(Gravity.BOTTOM);
        LinearLayout labels = Ui.row(a);
        String[] lbl = {L.t("-4 tyg."), L.t("-3 tyg."), L.t("-2 tyg."), L.t("-1 tydz."), L.t("ten tydz.")};
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
            View b = new View(a);
            b.setBackground(Ui.rounded(color, 0, 0, 5 * d));
            int h = (int) (Math.max(6, weeks[i] * 80.0 / max) * d);
            col.addView(b, new LinearLayout.LayoutParams((int) (34 * d), h));
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
        card.addView(Ui.label(a, "✅ " + L.t("OCENA TRENERA")));
        int pct = reviewed > 0 ? Math.round(correct * 100f / reviewed) : 0;
        LinearLayout row = Ui.row(a);
        row.addView(bigStat(String.valueOf(reviewed), L.t("ocenionych"), R.color.pr_purple), Ui.weight(1f, 0));
        row.addView(bigStat(String.valueOf(correct), L.t("zaliczonych"), R.color.pr_accent), Ui.weight(1f, 0));
        row.addView(bigStat(pct + "%", L.t("skuteczność"), R.color.pr_accent), Ui.weight(1f, 0));
        card.addView(row);
    }

    private void fillCats(LinearLayout card, Map<String, Integer> counts, Map<String, Integer> correct, String source) {
        card.removeAllViews();
        card.addView(Ui.label(a, "📊 " + L.t("NAGRANIA WG KATEGORII")));
        List<Map.Entry<String, Integer>> l = new ArrayList<>(counts.entrySet());
        java.util.Collections.sort(l, (x, y) -> y.getValue() - x.getValue());
        if (l.isEmpty()) { card.addView(Ui.text(a, L.t("Brak nagrań z kategorią."), 12f, R.color.pr_muted)); return; }
        int max = l.get(0).getValue();
        for (int i = 0; i < Math.min(10, l.size()); i++) {
            Map.Entry<String, Integer> e = l.get(i);
            LinearLayout row = Ui.row(a);
            row.setPadding(0, (int) (3 * d), 0, (int) (3 * d));
            row.addView(Ui.text(a, L.cat(e.getKey()), 12f, R.color.pr_text), new LinearLayout.LayoutParams((int) (120 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
            View b = bar(e.getValue() / (float) max, 0xFFE8820C, 9);
            b.setLayoutParams(Ui.weight(1f, 8 * d));
            row.addView(b);
            String val = String.valueOf(e.getValue());
            if (correct != null && correct.containsKey(e.getKey())) val += " (✓" + correct.get(e.getKey()) + ")";
            row.addView(Ui.text(a, val, 11f, R.color.pr_muted));
            card.addView(row);
        }
        TextView src = Ui.text(a, L.t("Źródło:") + " " + source, 9f, R.color.pr_muted);
        src.setPadding(0, (int) (6 * d), 0, 0);
        card.addView(src);
    }

    // ═════════════ OSIAGNIECIA (pelna lista, odznaki w siatce) ═════════════
    private void fillMilestones(LinearLayout card, Map<String, Integer> counts, int total) {
        card.removeAllViews();
        int done = 0;
        int[] prog = new int[Achievements.ALL.length];
        for (int i = 0; i < Achievements.ALL.length; i++) {
            Achievements.A m = Achievements.ALL[i];
            int cur = 0;
            if (m.cats == null) cur = total;
            else for (String c : m.cats) for (Map.Entry<String, Integer> e : counts.entrySet()) if (norm(e.getKey()).equals(norm(c))) cur += e.getValue();
            prog[i] = cur;
            if (cur >= m.count) done++;
        }
        card.addView(Ui.label(a, "🏆 " + L.t("OSIĄGNIĘCIA") + "  " + done + "/" + Achievements.ALL.length));
        String lastGroup = null;
        LinearLayout row = null;
        int inRow = 0;
        for (int i = 0; i < Achievements.ALL.length; i++) {
            Achievements.A m = Achievements.ALL[i];
            String g = Achievements.group(m);
            if (!g.equals(lastGroup)) {
                TextView gh = Ui.text(a, g, 11f, R.color.pr_muted);
                gh.setTypeface(Typeface.DEFAULT_BOLD);
                gh.setPadding(0, (int) (10 * d), 0, (int) (4 * d));
                card.addView(gh);
                lastGroup = g;
                row = null;
            }
            if (row == null || inRow == 3) {
                row = Ui.row(a);
                row.setGravity(Gravity.TOP);
                card.addView(row);
                inRow = 0;
            }
            row.addView(badge(m, prog[i]), Ui.weight(1f, inRow < 2 ? 6 * d : 0));
            inRow++;
            boolean groupEnds = i == Achievements.ALL.length - 1 || !Achievements.group(Achievements.ALL[i + 1]).equals(g);
            if (groupEnds) { while (inRow < 3) { row.addView(new View(a), Ui.weight(1f, inRow < 2 ? 6 * d : 0)); inRow++; } }
        }
    }

    // Odznaka: kolorowy "medal" z ikona, nazwa, pasek postepu. Zdobyta = zloty (Wystapienia: fioletowy).
    private View badge(Achievements.A m, int cur) {
        boolean done = cur >= m.count;
        LinearLayout b = new LinearLayout(a);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (8 * d);
        b.setPadding(pad / 2, pad, pad / 2, pad);
        int gold = m.special ? 0xFFB388FF : 0xFFFFC107;
        b.setBackground(Ui.rounded(done ? (gold & 0x00FFFFFF) | 0x2A000000 : Ui.col(a, R.color.pr_bg), done ? gold : Ui.col(a, R.color.pr_border), done ? 2 * d : d, 12 * d));
        TextView ic = Ui.text(a, m.icon, 26f, R.color.pr_text);
        ic.setGravity(Gravity.CENTER);
        int sz = (int) (50 * d);
        ic.setBackground(Ui.rounded(done ? gold : Ui.col(a, R.color.pr_border), 0, 0, sz / 2f));
        ic.setAlpha(done ? 1f : 0.45f);
        b.addView(ic, new LinearLayout.LayoutParams(sz, sz));
        TextView nm = Ui.text(a, m.label(), 10f, done ? R.color.pr_text : R.color.pr_muted);
        nm.setGravity(Gravity.CENTER);
        nm.setTypeface(done ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        nm.setMaxLines(3);
        nm.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
        b.addView(nm);
        b.addView(bar(Math.min(1f, cur / (float) m.count), done ? 0xFF00C853 : 0xFFE8820C, 5));
        TextView pr = Ui.text(a, done ? "✓" : Math.min(cur, m.count) + "/" + m.count, 9f, done ? R.color.pr_accent : R.color.pr_muted);
        pr.setGravity(Gravity.CENTER);
        b.addView(pr);
        if (done) b.setOnClickListener(v -> android.widget.Toast.makeText(a, m.icon + " " + m.label(), android.widget.Toast.LENGTH_SHORT).show());
        return b;
    }

    private static String norm(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('ł', 'l').toLowerCase(Locale.ROOT).trim();
    }
}
