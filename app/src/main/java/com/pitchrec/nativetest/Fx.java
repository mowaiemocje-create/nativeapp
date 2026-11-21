package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 🪞 SAMOOCENA TECHNIKI (worker /self-assess/*) i 🗺 MAPA ODWAGI (worker /showcase/*).
//  • samoocena: trener włącza ją w panelu; po wysłaniu pojedynczego nagrania (lub poprawki) pytamy
//    „Jak oceniasz swoją technikę?” (dobrze / częściowo / do poprawy + co nie wyszło); w statystykach
//    zgodność z trenerem i lista porównań.
//  • mapa odwagi: zgoda autora na pokazanie nagrania (karta przy starcie), „Moje nagrania na mapie”
//    w ustawieniach, sama mapa w MapActivity (osobny proces — WebView nie blokuje mikrofonu).
// Bez słów „terapia / terapeutyczny / terapeuta”. Teksty pl / en / cs / sk.
public final class Fx {

    private Fx() { }

    public static final String WORKER = "https://newspeech-backend.mowaiemocje.workers.dev";

    // ── teksty ──
    private static final Map<String, String[]> TR = new HashMap<>(); // klucz -> {pl, en, cs, sk}
    private static void t(String k, String pl, String en, String cs, String sk) { TR.put(k, new String[]{pl, en, cs, sk}); }
    static {
        t("saQ", "Jak oceniasz swoją technikę w tym nagraniu?", "How do you rate your technique in this recording?", "Jak hodnotíš svou techniku v této nahrávce?", "Ako hodnotíš svoju techniku v tejto nahrávke?");
        t("ok", "✅ Dobrze", "✅ Good", "✅ Dobře", "✅ Dobre");
        t("part", "⚠️ Częściowo", "⚠️ Partly", "⚠️ Částečně", "⚠️ Čiastočne");
        t("bad", "❌ Do poprawy", "❌ Needs work", "❌ K opravě", "❌ Na opravu");
        t("what", "Co nie wyszło? (opcjonalnie)", "What went wrong? (optional)", "Co nevyšlo? (volitelné)", "Čo nevyšlo? (voliteľné)");
        t("skip", "Pomiń", "Skip", "Přeskočit", "Preskočiť");
        t("save", "Zapisz", "Save", "Uložit", "Uložiť");
        t("saved", "Zapisano Twoją ocenę ✓", "Your rating is saved ✓", "Tvé hodnocení je uloženo ✓", "Tvoje hodnotenie je uložené ✓");
        t("el1", "intonacja", "intonation", "intonace", "intonácia");
        t("el2", "przedłużenia", "prolongations", "prodlužování", "predlžovanie");
        t("el3", "tempo", "tempo", "tempo", "tempo");
        t("el4", "oddech", "breathing", "dech", "dych");
        t("el5", "start głosu", "voice onset", "nasazení hlasu", "nasadenie hlasu");
        t("saCard", "🪞 Samoocena techniki", "🪞 Technique self-assessment", "🪞 Sebehodnocení techniky", "🪞 Sebahodnotenie techniky");
        t("agree", "Zgodność z trenerem", "Agreement with trainer", "Shoda s trenérem", "Zhoda s trénerom");
        t("last14", "ostatnie 14 dni", "last 14 days", "posledních 14 dní", "posledných 14 dní");
        t("compared", "porównane nagrania", "compared recordings", "porovnané nahrávky", "porovnané nahrávky");
        t("praise", "Coraz lepiej słyszysz swoją technikę 👏", "You hear your technique better and better 👏", "Svou techniku slyšíš čím dál lépe 👏", "Svoju techniku počuješ čoraz lepšie 👏");
        t("you", "Ty", "You", "Ty", "Ty");
        t("trainer", "Trener", "Trainer", "Trenér", "Tréner");
        t("waiting", "czeka na ocenę trenera", "waiting for the trainer", "čeká na hodnocení trenéra", "čaká na hodnotenie trénera");
        t("over", "Tu jeszcze nie słyszysz błędu — posłuchaj nagrania jeszcze raz i zwróć uwagę na: {0}", "You don't hear the mistake here yet — listen again and pay attention to: {0}", "Tady chybu ještě neslyšíš — poslechni si nahrávku znovu a zaměř se na: {0}", "Tu chybu ešte nepočuješ — vypočuj si nahrávku znova a zameraj sa na: {0}");
        t("overNo", "Tu jeszcze nie słyszysz błędu — posłuchaj nagrania jeszcze raz.", "You don't hear the mistake here yet — listen to the recording again.", "Tady chybu ještě neslyšíš — poslechni si nahrávku znovu.", "Tu chybu ešte nepočuješ — vypočuj si nahrávku znova.");
        t("r_ok", "dobrze", "good", "dobře", "dobre");
        t("r_part", "częściowo", "partly", "částečně", "čiastočne");
        t("r_bad", "do poprawy", "needs work", "k opravě", "na opravu");
        t("weeks", "Tygodnie", "Weeks", "Týdny", "Týždne");
        t("noData", "Jeszcze nie ma porównań — oceniaj swoje nagrania po wysłaniu.", "No comparisons yet — rate your recordings after sending.", "Zatím žádná porovnání — hodnoť své nahrávky po odeslání.", "Zatiaľ žiadne porovnania — hodnoť svoje nahrávky po odoslaní.");
        t("close", "Zamknij", "Close", "Zavřít", "Zavrieť");
        t("loading", "Wczytywanie…", "Loading…", "Načítám…", "Načítavam…");
        t("loadErr", "Nie udało się wczytać — sprawdź internet.", "Could not load — check your internet.", "Nepodařilo se načíst — zkontroluj internet.", "Nepodarilo sa načítať — skontroluj internet.");
        t("scTitle", "🗺 Mapa odwagi", "🗺 Courage map", "🗺 Mapa odvahy", "🗺 Mapa odvahy");
        t("slogan", "Inni już to zrobili, Ty też dasz radę", "Others have done it — you can do it too", "Jiní už to dokázali, ty to zvládneš taky", "Iní to už dokázali, ty to zvládneš tiež");
        t("consentQ", "🗺 Trener chce pokazać Twoje nagranie innym kursantom jako przykład: {0}{1}. Podpis: {2}. Zgadzasz się?", "🗺 Your trainer would like to show your recording to other students as an example: {0}{1}. Signed as: {2}. Do you agree?", "🗺 Trenér chce ukázat tvou nahrávku ostatním kurzistům jako příklad: {0}{1}. Podpis: {2}. Souhlasíš?", "🗺 Tréner chce ukázať tvoju nahrávku ostatným kurzistom ako príklad: {0}{1}. Podpis: {2}. Súhlasíš?");
        t("consentName", "Jak Cię podpisać", "How to sign you", "Jak tě podepsat", "Ako ťa podpísať");
        t("yes", "Tak, pokaż", "Yes, show it", "Ano, ukázat", "Áno, ukázať");
        t("no", "Nie", "No", "Ne", "Nie");
        t("later", "Później", "Later", "Později", "Neskôr");
        t("listen", "▶ Posłuchaj", "▶ Listen", "▶ Poslechnout", "▶ Vypočuť");
        t("stop", "■ Stop", "■ Stop", "■ Stop", "■ Stop");
        t("mineTitle", "🗺 MOJE NAGRANIA NA MAPIE", "🗺 MY RECORDINGS ON THE MAP", "🗺 MOJE NAHRÁVKY NA MAPĚ", "🗺 MOJE NAHRÁVKY NA MAPE");
        t("st_yes", "widoczne dla innych", "visible to others", "viditelné pro ostatní", "viditeľné pre ostatných");
        t("st_no", "ukryte", "hidden", "skryté", "skryté");
        t("st_pending", "czeka na Twoją zgodę", "waiting for your consent", "čeká na tvůj souhlas", "čaká na tvoj súhlas");
        t("st_off", "wyłączone przez trenera", "turned off by the trainer", "vypnuto trenérem", "vypnuté trénerom");
        t("hide", "Ukryj", "Hide", "Skrýt", "Skryť");
        t("show", "Pokaż", "Show", "Ukázat", "Ukázať");
        t("recHint", "Nagraj swoje — kategoria {0} wybierze się sama przy zapisie. Naciśnij REC.", "Record yours — category {0} will be selected when you save. Press REC.", "Nahraj svoje — kategorie {0} se vybere sama při uložení. Stiskni REC.", "Nahraj svoje — kategória {0} sa vyberie sama pri uložení. Stlač REC.");
    }

    public static String lang() {
        String l = L.lang();
        if ("pl".equals(l) || "en".equals(l) || "cs".equals(l) || "sk".equals(l)) return l;
        return "en";
    }

    public static String tr(String k, String... a) {
        String[] v = TR.get(k);
        if (v == null) return k;
        String l = lang();
        String s = "en".equals(l) ? v[1] : "cs".equals(l) ? v[2] : "sk".equals(l) ? v[3] : v[0];
        for (int i = 0; i < a.length; i++) s = s.replace("{" + i + "}", a[i] == null ? "" : a[i]);
        return s;
    }

    private static SharedPreferences p(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    static String auth(Context c) {
        String tok = p(c).getString("ns_token", null);
        if (tok == null) return null;
        return "ns_token=" + NsClient.enc(tok) + "&ns_email=" + NsClient.enc(p(c).getString("ns_email", "")) + "&ns_server=new";
    }

    private static JSONObject authBody(Context c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("ns_token", p(c).getString("ns_token", ""));
        o.put("ns_email", p(c).getString("ns_email", ""));
        o.put("ns_server", "new");
        return o;
    }

    private static JSONObject parse(NsClient.Result r) {
        try { return r != null && r.body != null ? new JSONObject(r.body) : null; } catch (Exception e) { return null; }
    }

    // ══════════════ 🪞 SAMOOCENA ══════════════

    public static boolean saOn(Context c) { return p(c).getBoolean("sa_on", false) && auth(c) != null; }

    /** przy starcie, po zalogowaniu i raz dziennie (force = zawsze) */
    public static void saCheck(Context c, boolean force, Runnable done) {
        String a = auth(c);
        if (a == null) { if (done != null) done.run(); return; }
        String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        if (!force && day.equals(p(c).getString("sa_check_day", ""))) { if (done != null) done.run(); return; }
        final Context ac = c.getApplicationContext();
        NsClient.backend("GET", "/self-assess/me?" + a, null, r -> {
            JSONObject o = parse(r);
            if (r.ok && o != null && o.optBoolean("ok", false)) {
                p(ac).edit().putBoolean("sa_on", o.optBoolean("enabled", false)).putString("sa_check_day", day).apply();
            }
            if (done != null) done.run();
        });
    }

    /** pytanie po wysłaniu nagrania (recordId = id w NS) */
    public static void saAsk(Activity a, String recordId) {
        if (a == null || a.isFinishing() || recordId == null || recordId.isEmpty() || !saOn(a)) return;
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (18 * d), (int) (12 * d), (int) (18 * d), 0);
        TextView q = Ui.text(a, "🪞 " + tr("saQ"), 15f, R.color.pr_text);
        q.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(q);
        LinearLayout row = Ui.row(a);
        final String[] rating = {null};
        final List<Button> rbs = new ArrayList<>();
        final List<String> els = new ArrayList<>();
        FlowLayout chips = new FlowLayout(a);
        TextView whatLbl = Ui.text(a, tr("what"), 12f, R.color.pr_text);
        whatLbl.setVisibility(View.GONE);
        chips.setVisibility(View.GONE);
        final AlertDialog[] dlg = {null};
        for (String k : new String[]{"ok", "part", "bad"}) {
            Button b = Ui.button(a, tr(k), R.color.pr_muted, false);
            b.setAllCaps(false);
            b.setTextSize(13f);
            b.setOnClickListener(v -> {
                rating[0] = k;
                for (Button x : rbs) styleSel(a, x, x == b);
                boolean showEl = !"ok".equals(k);
                whatLbl.setVisibility(showEl ? View.VISIBLE : View.GONE);
                chips.setVisibility(showEl ? View.VISIBLE : View.GONE);
                if (dlg[0] != null) dlg[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
            rbs.add(b);
            row.addView(b, Ui.weight(1f, "bad".equals(k) ? 0 : 6 * d));
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = (int) (10 * d);
        box.addView(row, rlp);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wl.topMargin = (int) (12 * d);
        box.addView(whatLbl, wl);
        for (String k : new String[]{"el1", "el2", "el3", "el4", "el5"}) {
            String label = tr(k);
            Button c = Ui.button(a, label, R.color.pr_muted, false);
            c.setAllCaps(false);
            c.setTextSize(12f);
            c.setOnClickListener(v -> {
                if (els.contains(label)) els.remove(label); else els.add(label);
                styleSel(a, c, els.contains(label));
            });
            chips.addView(c);
        }
        box.addView(chips);
        dlg[0] = new AlertDialog.Builder(a)
                .setView(box)
                .setNegativeButton(tr("skip"), null)
                .setPositiveButton(tr("save"), (di, w) -> {
                    if (rating[0] == null) return;
                    try {
                        JSONObject o = authBody(a);
                        o.put("ns_record_id", recordId);
                        o.put("rating", rating[0]);
                        o.put("element", "ok".equals(rating[0]) ? "" : android.text.TextUtils.join(", ", els));
                        final Context ac = a.getApplicationContext();
                        NsClient.backend("POST", "/self-assess/rate", o.toString(), r -> {
                            if (r.ok) Toast.makeText(ac, tr("saved"), Toast.LENGTH_SHORT).show();
                            else if (r.status == 403) p(ac).edit().putBoolean("sa_on", false).apply(); // trener wyłączył — chowamy pytanie
                        });
                    } catch (Exception e) { }
                })
                .create();
        dlg[0].show();
        dlg[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private static void styleSel(Context c, Button b, boolean on) {
        int acc = Ui.col(c, R.color.pr_accent);
        b.setBackground(Ui.rounded(on ? ((acc & 0x00FFFFFF) | 0x33000000) : 0x14000000, on ? acc : 0x33000000, Ui.dp(c, on ? 2 : 1), Ui.dp(c, 10)));
    }

    /** karta w statystykach (tylko gdy samoocena włączona) */
    public static void saFillCard(Activity a, LinearLayout card, java.util.function.BooleanSupplier isCurrent) {
        card.removeAllViews();
        if (!saOn(a)) { card.setVisibility(View.GONE); return; }
        card.setVisibility(View.VISIBLE);
        TextView h = Ui.text(a, tr("saCard"), 14f, R.color.pr_text);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(h);
        TextView sub = Ui.text(a, tr("loading"), 12f, R.color.pr_text);
        card.addView(sub);
        card.setOnClickListener(v -> saShowCompare(a));
        String au = auth(a);
        if (au == null) return;
        NsClient.backend("GET", "/self-assess/compare?" + au, null, r -> {
            if (!isCurrent.getAsBoolean()) return;
            JSONObject o = parse(r);
            if (o == null || !r.ok) { sub.setText(""); return; }
            Object p14 = o.opt("agree_14d"), pa = o.opt("agree_pct");
            if (p14 instanceof Number) sub.setText(tr("agree") + ": " + ((Number) p14).intValue() + "% (" + tr("last14") + ")  ›");
            else if (pa instanceof Number) sub.setText(tr("agree") + ": " + ((Number) pa).intValue() + "%  ›");
            else sub.setText(tr("noData"));
        });
    }

    private static String rl(String r) {
        if ("ok".equals(r)) return "✅ " + tr("r_ok");
        if ("part".equals(r)) return "⚠️ " + tr("r_part");
        if ("bad".equals(r)) return "❌ " + tr("r_bad");
        return "—";
    }

    public static void saShowCompare(Activity a) {
        String au = auth(a);
        if (au == null) return;
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (18 * d), (int) (10 * d), (int) (18 * d), (int) (10 * d));
        TextView wait = Ui.text(a, tr("loading"), 13f, R.color.pr_text);
        box.addView(wait);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        AlertDialog dlg = new AlertDialog.Builder(a).setTitle(tr("saCard")).setView(sv).setPositiveButton(tr("close"), null).show();
        NsClient.backend("GET", "/self-assess/compare?" + au, null, r -> {
            if (!dlg.isShowing()) return;
            box.removeAllViews();
            JSONObject o = parse(r);
            if (o == null || !r.ok) { box.addView(Ui.text(a, tr("loadErr"), 13f, R.color.pr_text)); return; }
            Object p14 = o.opt("agree_14d");
            if (!(p14 instanceof Number)) p14 = o.opt("agree_pct");
            JSONArray items = o.optJSONArray("items");
            if (!(p14 instanceof Number) && (items == null || items.length() == 0)) { box.addView(Ui.text(a, tr("noData"), 13f, R.color.pr_text)); return; }
            int pct = p14 instanceof Number ? ((Number) p14).intValue() : -1;
            TextView l1 = Ui.text(a, tr("agree") + " (" + tr("last14") + ")", 12f, R.color.pr_text);
            l1.setGravity(Gravity.CENTER);
            box.addView(l1);
            TextView big = Ui.text(a, pct < 0 ? "—" : pct + "%", 38f, R.color.pr_text);
            big.setGravity(Gravity.CENTER);
            big.setTypeface(Typeface.DEFAULT_BOLD);
            big.setTextColor(pct >= 85 ? 0xFF2E7D32 : Ui.col(a, R.color.pr_accent));
            box.addView(big);
            if (o.has("compared")) { TextView cmp = Ui.text(a, o.optInt("compared") + " " + tr("compared"), 11f, R.color.pr_text); cmp.setGravity(Gravity.CENTER); box.addView(cmp); }
            if (pct >= 85) {
                TextView pr = Ui.text(a, tr("praise"), 13f, R.color.pr_text);
                pr.setTypeface(Typeface.DEFAULT_BOLD);
                pr.setTextColor(0xFF2E7D32);
                pr.setGravity(Gravity.CENTER);
                pr.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
                box.addView(pr);
            }
            JSONArray weeks = o.optJSONArray("weeks");
            if (weeks != null && weeks.length() > 0) {
                box.addView(Ui.text(a, tr("weeks"), 11f, R.color.pr_text));
                LinearLayout bars = new LinearLayout(a);
                bars.setOrientation(LinearLayout.HORIZONTAL);
                bars.setGravity(Gravity.BOTTOM);
                int from = Math.max(0, weeks.length() - 10);
                for (int i = from; i < weeks.length(); i++) {
                    JSONObject w = weeks.optJSONObject(i);
                    if (w == null) continue;
                    boolean has = w.opt("agree_pct") instanceof Number;
                    int v = has ? w.optInt("agree_pct") : 0;
                    LinearLayout col = new LinearLayout(a);
                    col.setOrientation(LinearLayout.VERTICAL);
                    col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                    TextView vt = Ui.text(a, has ? v + "%" : "", 9f, R.color.pr_text);
                    vt.setGravity(Gravity.CENTER);
                    col.addView(vt);
                    View bar = new View(a);
                    bar.setBackground(Ui.rounded(v >= 85 ? 0xFF66BB6A : 0xFFFFB74D, 0, 0, Ui.dp(a, 3)));
                    col.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (Math.max(2, v * 0.7f) * d)));
                    String wk = w.optString("week", "");
                    TextView wt = Ui.text(a, wk.length() > 3 ? wk.substring(wk.length() - 3) : wk, 8f, R.color.pr_text);
                    wt.setGravity(Gravity.CENTER);
                    col.addView(wt);
                    LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, (int) (100 * d), 1f);
                    cl.rightMargin = (int) (3 * d);
                    bars.addView(col, cl);
                }
                box.addView(bars);
            }
            if (items != null) {
                for (int i = 0; i < Math.min(40, items.length()); i++) {
                    JSONObject it = items.optJSONObject(i);
                    if (it == null) continue;
                    View sep = new View(a);
                    sep.setBackgroundColor(0x22000000);
                    LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    sl.topMargin = (int) (8 * d);
                    sl.bottomMargin = (int) (6 * d);
                    box.addView(sep, sl);
                    String cat = it.optString("category", "");
                    box.addView(Ui.text(a, it.optString("date", "") + (cat.isEmpty() ? "" : " · " + cat), 11f, R.color.pr_text));
                    String se = it.optString("self_element", "");
                    String trn = it.isNull("trainer") ? tr("waiting") : rl(it.optString("trainer")) + (it.optString("trainer_weak", "").isEmpty() || it.isNull("trainer_weak") ? "" : " (" + it.optString("trainer_weak") + ")");
                    box.addView(Ui.text(a, tr("you") + ": " + rl(it.optString("self")) + (se.isEmpty() ? "" : " (" + se + ")") + " · " + tr("trainer") + ": " + trn, 13f, R.color.pr_text));
                    if ("overrated".equals(it.optString("kind"))) {
                        String weak = it.isNull("trainer_weak") ? "" : it.optString("trainer_weak", "");
                        TextView tip = Ui.text(a, "💡 " + (weak.isEmpty() ? tr("overNo") : tr("over", weak)), 12f, R.color.pr_text);
                        tip.setPadding((int) (8 * d), (int) (6 * d), (int) (8 * d), (int) (6 * d));
                        tip.setBackground(Ui.rounded(0x22E8820C, 0, 0, Ui.dp(a, 8)));
                        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        tl.topMargin = (int) (4 * d);
                        box.addView(tip, tl);
                    }
                }
            }
        });
    }

    // ══════════════ 🗺 MAPA ODWAGI ══════════════

    private static final java.util.Set<Integer> asked = new java.util.HashSet<>();
    private static long mineAt = 0;
    private static JSONArray mine = null;

    /** zgoda autora: sprawdzane przy starcie, po zalogowaniu i przy powrocie (co 10 min) */
    public static void scCheckMine(Activity a, boolean force, Runnable onList) {
        String au = auth(a);
        if (au == null) return;
        if (!force && System.currentTimeMillis() - mineAt < 600000L) { if (onList != null) onList.run(); return; }
        mineAt = System.currentTimeMillis();
        NsClient.backend("GET", "/showcase/mine?" + au, null, r -> {
            JSONObject o = parse(r);
            if (o == null || !r.ok) return;
            mine = o.optJSONArray("items");
            if (onList != null) onList.run();
            if (mine == null || a.isFinishing() || StudentLock.locked) return;
            for (int i = 0; i < mine.length(); i++) {
                JSONObject it = mine.optJSONObject(i);
                if (it != null && "pending".equals(it.optString("consent")) && !asked.contains(it.optInt("id"))) { consentDialog(a, it); break; }
            }
        });
    }

    private static MediaPlayer player;

    private static void stopPlayer() {
        if (player != null) { try { player.stop(); } catch (Exception e) { } try { player.release(); } catch (Exception e) { } player = null; }
    }

    static String audioUrl(Context c, String path) {
        String au = auth(c);
        if (path == null || path.isEmpty() || au == null) return null;
        return WORKER + path + (path.contains("?") ? "&" : "?") + au;
    }

    private static void consentDialog(Activity a, JSONObject it) {
        asked.add(it.optInt("id"));
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (18 * d), (int) (14 * d), (int) (18 * d), 0);
        String place = it.optString("place", "");
        box.addView(Ui.text(a, tr("consentQ", it.optString("title", ""), place.isEmpty() ? "" : " (" + place + ")", it.optString("display_name", "")), 14f, R.color.pr_text));
        String note = it.optString("note", "");
        if (!note.isEmpty()) box.addView(Ui.text(a, "💬 " + note, 12f, R.color.pr_text));
        final String url = audioUrl(a, it.optString("audio", ""));
        if (url != null) {
            Button play = Ui.button(a, tr("listen"), R.color.pr_accent, false);
            play.setAllCaps(false);
            play.setOnClickListener(v -> {
                if (player != null) { stopPlayer(); play.setText(tr("listen")); return; }
                try {
                    player = new MediaPlayer();
                    player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                    player.setDataSource(url);
                    player.setOnPreparedListener(MediaPlayer::start);
                    player.setOnCompletionListener(mp -> { stopPlayer(); play.setText(tr("listen")); });
                    player.prepareAsync();
                    play.setText(tr("stop"));
                } catch (Exception e) { stopPlayer(); }
            });
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pl.topMargin = (int) (10 * d);
            box.addView(play, pl);
        }
        TextView nl = Ui.text(a, tr("consentName"), 11f, R.color.pr_text);
        nl.setPadding(0, (int) (10 * d), 0, 0);
        box.addView(nl);
        EditText name = new EditText(a);
        name.setSingleLine(true);
        name.setText(it.optString("display_name", ""));
        box.addView(name);
        new AlertDialog.Builder(a)
                .setView(box)
                .setCancelable(false)
                .setPositiveButton(tr("yes"), (di, w) -> { stopPlayer(); sendConsent(a, it.optInt("id"), true, name.getText().toString().trim(), null); })
                .setNegativeButton(tr("no"), (di, w) -> { stopPlayer(); sendConsent(a, it.optInt("id"), false, null, null); })
                .setNeutralButton(tr("later"), (di, w) -> stopPlayer())
                .show();
    }

    static void sendConsent(Activity a, int id, boolean yes, String name, Runnable done) {
        try {
            JSONObject o = authBody(a);
            o.put("id", id);
            o.put("yes", yes);
            if (yes && name != null && !name.isEmpty()) o.put("display_name", name);
            NsClient.backend("POST", "/showcase/consent", o.toString(), r -> { mineAt = 0; if (done != null) done.run(); });
        } catch (Exception e) { }
    }

    /** ustawienia: „Moje nagrania na mapie” — karta pokazuje się tylko, gdy są wpisy */
    public static void scFillMine(Activity a, LinearLayout card, java.util.function.BooleanSupplier isCurrent) {
        card.setVisibility(View.GONE);
        scCheckMine(a, true, () -> { if (isCurrent.getAsBoolean()) renderMine(a, card, isCurrent); });
    }

    private static void renderMine(Activity a, LinearLayout card, java.util.function.BooleanSupplier isCurrent) {
        card.removeAllViews();
        if (mine == null || mine.length() == 0) { card.setVisibility(View.GONE); return; }
        card.setVisibility(View.VISIBLE);
        float d = a.getResources().getDisplayMetrics().density;
        card.addView(Ui.label(a, tr("mineTitle")));
        for (int i = 0; i < mine.length(); i++) {
            JSONObject it = mine.optJSONObject(i);
            if (it == null) continue;
            String c = it.optString("consent");
            String st = !it.optBoolean("active", true) ? tr("st_off") : "yes".equals(c) ? tr("st_yes") : "no".equals(c) ? tr("st_no") : tr("st_pending");
            LinearLayout row = Ui.row(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout tb = new LinearLayout(a);
            tb.setOrientation(LinearLayout.VERTICAL);
            TextView t1 = Ui.text(a, it.optString("title", ""), 13f, R.color.pr_text);
            t1.setTypeface(Typeface.DEFAULT_BOLD);
            tb.addView(t1);
            String place = it.optString("place", "");
            tb.addView(Ui.text(a, it.optString("display_name", "") + (place.isEmpty() ? "" : " · " + place) + " · " + st, 11f, R.color.pr_text));
            row.addView(tb, Ui.weight(1f, 8 * d));
            boolean yes = "yes".equals(c);
            Button b = Ui.button(a, yes ? tr("hide") : tr("show"), yes ? R.color.pr_muted : R.color.pr_accent, !yes);
            b.setAllCaps(false);
            final int id = it.optInt("id");
            b.setOnClickListener(v -> { b.setEnabled(false); sendConsent(a, id, !yes, null, () -> scCheckMine(a, true, () -> { if (isCurrent.getAsBoolean()) renderMine(a, card, isCurrent); })); });
            row.addView(b, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rl.topMargin = (int) (8 * d);
            card.addView(row, rl);
        }
        Button open = Ui.button(a, tr("scTitle"), R.color.pr_accent, false);
        open.setAllCaps(false);
        open.setOnClickListener(v -> openCourageMap(a));
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ol.topMargin = (int) (10 * d);
        card.addView(open, ol);
    }

    public static final int REQ_COURAGE = 4402;

    /** sama mapa — w MapActivity (osobny proces), wynik „Nagraj swoje” wraca przez onActivityResult */
    public static void openCourageMap(Activity a) {
        if (auth(a) == null) return;
        android.content.Intent i = new android.content.Intent(a, MapActivity.class);
        i.putExtra("courage", true);
        i.putExtra("token", p(a).getString("ns_token", ""));
        i.putExtra("email", p(a).getString("ns_email", ""));
        i.putExtra("lang", lang());
        a.startActivityForResult(i, REQ_COURAGE);
    }

    /** karta w statystykach: wejście na mapę odwagi */
    public static LinearLayout mapCard(Activity a) {
        LinearLayout card = Ui.card(a);
        TextView h = Ui.text(a, tr("scTitle") + "  ›", 14f, R.color.pr_text);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(h);
        TextView s = Ui.text(a, tr("slogan"), 12f, R.color.pr_accent);
        card.addView(s);
        card.setOnClickListener(v -> openCourageMap(a));
        return card;
    }
}
