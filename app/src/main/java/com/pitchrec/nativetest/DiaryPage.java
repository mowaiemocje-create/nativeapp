package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// DZIENNIK — odwzorowanie zakladki DZIENNIK z PitchRec: cwiczenia relaksacyjne, sukcesy dnia,
// pola badawcze, z kim sukcesy, rozmowy/telefony bez nagrania, utrata kontroli nad mowa,
// samopoczucie. Zapis: POST /diaries (nowy wpis) albo PUT /diaries/{id} (poprawa dzisiejszego),
// plus kopia do nowego backendu i do Google Forms (jak w PWA). Wpis sprawdzony przez trenera
// jest zablokowany do edycji.
public class DiaryPage {

    public interface Host {
        String token();
        String email();
        String studentName();
        void onAuthExpired();
        boolean isCurrent();
    }

    static class Draft {
        String date;
        int jacobson = 0, scan = 0;
        final java.util.Map<String, Boolean> success = new java.util.HashMap<>();
        String extraNote = "", diffNote = "";
        final List<String> successWho = new ArrayList<>(), talkNoRec = new ArrayList<>(), phoneNoRec = new ArrayList<>();
        Boolean controlLost = null;
        final List<String> controlDetails = new ArrayList<>();
        String feelingId = null;
        final List<String> feelingContext = new ArrayList<>();
        String existingId = null;   // null = nie sprawdzono, "" = nie ma dzisiejszego wpisu
        boolean locked = false;
        String city = "";
        final List<String> hard = new ArrayList<>(), easy = new ArrayList<>(), alcohol = new ArrayList<>();
        int tiredness = 5, motivation = 5, coffee = 0;
        double sleep = 7;
        String activity = null;     // "tak" / "nie"
    }

    private static Draft draft;
    private static JSONArray feelings, talkPeople, successPeople;

    private static final String[][] SUCCESS_Q = {
            {"spoke", "Powiedziałeś coś na głos poza domem?", "Powiedziałem coś na głos poza domem."},
            {"initiated", "Zacząłeś rozmowę jako pierwszy?", "Zacząłem rozmowę jako pierwszy."},
            {"shop", "Zamówiłeś coś / zapytałeś w sklepie?", "Zamówiłem coś / zapytałem w sklepie."},
            {"phone", "Odebrałeś / wykonałeś telefon?", "Odebrałem / wykonałem telefon."}
    };
    private static final String[][] CONTROL_OPT = {
            {"esbs_free", "Mowa spontaniczna, brak ESBS"}, {"stutter", "Jąkanie"}, {"block", "Blok"}, {"camouflage", "Kamuflaż"}
    };
    private static final String[][] CTX_NEG = {{"health", "Zdrowie / fizyczne"}, {"unrelated", "Sprawy niezw. z mową"}, {"speech", "Trudność w mowie"}};
    private static final String[][] CTX_POS = {{"speech_success", "Sukces związany z mową"}, {"unrelated_emotion", "Emocje nie związane z mową"}};
    private static final String[] SITUATIONS = {"Sklepy", "Przechodzień", "Telefon do miasta", "Telefon rodzina/znajomi", "Restauracja Kelner", "Wystąpienie", "Praca / Szkoła", "Rodzina", "Przyjaciele", "W grupie"};
    private static final String[] ALCOHOL = {"Nie", "Piwo", "Wino", "Wódka", "Koniak", "Inne"};

    private final Activity a;
    private final LinearLayout root;
    private final Host host;
    private final float d;

    public DiaryPage(Activity a, LinearLayout root, Host host) {
        this.a = a;
        this.root = root;
        this.host = host;
        this.d = a.getResources().getDisplayMetrics().density;
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // ── wczytanie ──
    public void render() {
        if (draft == null || !today().equals(draft.date)) { draft = new Draft(); draft.date = today(); }
        root.removeAllViews();
        root.addView(header());
        TextView loading = Ui.text(a, L.t("⏳ Wczytywanie dziennika…"), 14f, R.color.pr_muted);
        root.addView(loading);
        loadDicts(ok -> {
            if (!host.isCurrent()) return;
            if (!ok) {
                loading.setText(L.t("Nie udało się wczytać dziennika. Sprawdź połączenie z internetem."));
                loading.setTextColor(Ui.col(a, R.color.pr_warn));
                Button retry = Ui.button(a, L.t("Spróbuj ponownie"), R.color.pr_accent, false);
                retry.setOnClickListener(v -> render());
                root.addView(retry);
                return;
            }
            checkToday(() -> {
                if (!host.isCurrent()) return;
                root.removeAllViews();
                root.addView(header());
                buildForm();
            });
        });
    }

    interface Done { void run(boolean ok); }

    private void loadDicts(Done done) {
        if (feelings != null && feelings.length() > 0 && successPeople != null && successPeople.length() > 0 && talkPeople != null) { done.run(true); return; }
        NsClient.request("GET", "/talk_feelings", host.token(), host.email(), null, null, r1 -> {
            if (r1.isAuthError()) { host.onAuthExpired(); return; }
            JSONArray f = collection(r1);
            NsClient.request("GET", "/talk_participants", host.token(), host.email(), null, null, r2 -> {
                JSONArray tp = collection(r2);
                NsClient.request("GET", "/diary_success_participants", host.token(), host.email(), null, null, r3 -> {
                    JSONArray sp = collection(r3);
                    if (f == null || sp == null) { done.run(false); return; }
                    feelings = sortFeelings(f);
                    talkPeople = tp != null ? tp : new JSONArray();
                    successPeople = sp;
                    done.run(true);
                });
            });
        });
    }

    private static JSONArray collection(NsClient.Result r) {
        if (!r.ok) return null;
        try {
            String b = r.body.trim();
            if (b.startsWith("[")) return new JSONArray(b);
            JSONObject o = new JSONObject(b);
            JSONArray arr = o.optJSONArray("collection");
            return arr != null ? arr : new JSONArray();
        } catch (Exception e) { return null; }
    }

    private static JSONArray sortFeelings(JSONArray in) {
        String[] order = {"wyśmienite", "wysmienite", "bardzo dobre", "dobre", "słabe", "slabe", "smutek"};
        List<JSONObject> l = new ArrayList<>();
        for (int i = 0; i < in.length(); i++) { JSONObject o = in.optJSONObject(i); if (o != null) l.add(o); }
        java.util.Collections.sort(l, (x, y) -> Integer.compare(idx(order, name(x).toLowerCase(Locale.ROOT).trim()), idx(order, name(y).toLowerCase(Locale.ROOT).trim())));
        JSONArray out = new JSONArray();
        for (JSONObject o : l) out.put(o);
        return out;
    }

    private static int idx(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 999;
    }

    private static String name(JSONObject o) {
        String n = o.optString("name_pl", "");
        return n.isEmpty() ? o.optString("name", "") : n;
    }

    // Czy dzisiejszy wpis juz istnieje? Jesli tak — wczytujemy go (zapis = PUT, a nie nowy wpis).
    private void checkToday(Runnable then) {
        if (draft.existingId != null) { then.run(); return; }
        NsClient.request("GET", "/diaries?sort_by=date&sort_order=desc&page_size=5", host.token(), host.email(), null, null, r -> {
            JSONArray list = collection(r);
            draft.existingId = "";
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject e = list.optJSONObject(i);
                    if (e == null || !draft.date.equals(e.optString("date", ""))) continue;
                    draft.existingId = e.optString("id", "");
                    ReminderReceiver.markDiary(a, draft.date);
                    draft.locked = !e.isNull("reviewed_at") && !e.optString("reviewed_at", "").isEmpty();
                    draft.jacobson = e.optInt("meditation_count", 0);
                    draft.scan = 0;
                    if (!e.isNull("old_speech_usage")) draft.controlLost = e.optBoolean("old_speech_usage", false);
                    String fid = e.optString("talk_feeling_id", "");
                    if (fid.isEmpty() && e.optJSONObject("talk_feeling") != null) fid = e.optJSONObject("talk_feeling").optString("id", "");
                    draft.feelingId = fid.isEmpty() ? null : fid;
                    ids(e.optJSONArray("diary_success_participants"), draft.successWho);
                    ids(e.optJSONArray("talk_participants_without_record"), draft.talkNoRec);
                    ids(e.optJSONArray("phone_call_participants_without_record"), draft.phoneNoRec);
                    draft.extraNote = e.isNull("notes") ? "" : e.optString("notes", "");
                    break;
                }
            }
            then.run();
        });
    }

    private static void ids(JSONArray arr, List<String> out) {
        out.clear();
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); if (o != null) out.add(o.optString("id", "")); }
    }

    // ── UI ──
    private View header() {
        LinearLayout h = Ui.row(a);
        TextView t = Ui.text(a, L.t("DZIENNIK"), 18f, R.color.pr_muted);
        t.setLetterSpacing(0.3f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        h.addView(Ui.text(a, draft != null ? draft.date + "  " : "", 10f, R.color.pr_muted));
        Button hist = Ui.button(a, L.t("📅 HISTORIA"), R.color.pr_accent, false);
        hist.setTextSize(10f);
        hist.setOnClickListener(v -> showHistory());
        h.addView(hist);
        h.setPadding(0, 0, 0, (int) (12 * d));
        return h;
    }

    private LinearLayout section(String label) {
        LinearLayout c = Ui.card(a);
        c.addView(sectionHead(label));
        root.addView(c);
        return c;
    }

    // Wyrazny naglowek sekcji: pomaranczowy pasek z lewej + pogrubiony, wiekszy tekst
    private View sectionHead(String label) {
        LinearLayout r = Ui.row(a);
        r.setPadding(0, 0, 0, (int) (10 * d));
        View bar = new View(a);
        bar.setBackground(Ui.rounded(Ui.col(a, R.color.pr_accent), 0, 0, 2 * d));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams((int) (4 * d), (int) (20 * d));
        bl.rightMargin = (int) (8 * d);
        r.addView(bar, bl);
        TextView t = Ui.text(a, label, 15f, R.color.pr_text);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.04f);
        r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    // Pytanie wewnatrz sekcji — pogrubione, w kolorze akcentu, z odstepem nad
    private TextView question(String label) {
        TextView t = Ui.text(a, label, 13f, R.color.pr_accent);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, (int) (12 * d), 0, (int) (6 * d));
        return t;
    }

    private void buildForm() {
        if (draft.locked) {
            TextView lock = Ui.text(a, L.t("🔒 Trener już sprawdził ten wpis — edycja zablokowana"), 12f, R.color.pr_pause);
            lock.setTypeface(Typeface.DEFAULT_BOLD);
            lock.setPadding((int) (10 * d), (int) (10 * d), (int) (10 * d), (int) (10 * d));
            lock.setBackground(Ui.rounded((Ui.col(a, R.color.pr_pause) & 0x00FFFFFF) | 0x1A000000, Ui.col(a, R.color.pr_pause), d, 8 * d));
            root.addView(lock);
            root.addView(Ui.spacer(a, 8));
        } else if (draft.existingId != null && !draft.existingId.isEmpty()) {
            TextView info = Ui.text(a, L.t("Dzisiejszy wpis już istnieje — możesz go poprawić i zapisać ponownie."), 12f, R.color.pr_muted);
            info.setPadding(0, 0, 0, (int) (8 * d));
            root.addView(info);
        }

        // Cwiczenia relaksacyjne
        LinearLayout relax = section(L.t("ĆWICZENIA RELAKSACYJNE DZISIAJ"));
        relax.addView(counter(L.t("Trening Jacobsona"), () -> draft.jacobson, v -> draft.jacobson = v, 0, 20, 1));
        relax.addView(counter(L.t("Skanowanie ciała"), () -> draft.scan, v -> draft.scan = v, 0, 20, 1));

        // Sukcesy
        LinearLayout succ = section(L.t("✨ SUKCESY DZISIAJ"));
        succ.addView(Ui.text(a, L.t("Każde \"tak\" się liczy"), 11f, R.color.pr_muted));
        for (String[] q : SUCCESS_Q) succ.addView(yesNoRow(q[1], () -> draft.success.get(q[0]), v -> draft.success.put(q[0], v)));
        succ.addView(question(L.t("CO BYŁO INNE DZIŚ NIŻ WCZORAJ? (opcjonalnie)")));
        succ.addView(textInput(draft.diffNote, s -> draft.diffNote = s));
        succ.addView(Ui.spacer(a, 8));
        succ.addView(question(L.t("COŚ JESZCZE CHCESZ DOPISAĆ? (opcjonalnie)")));
        succ.addView(textInput(draft.extraNote, s -> draft.extraNote = s));

        // Dodatkowe informacje (pola badawcze)
        LinearLayout extra = section(L.t("📊 DODATKOWE INFORMACJE"));
        extra.addView(question(L.t("MIEJSCOWOŚĆ")));
        extra.addView(textInput(draft.city, s -> draft.city = s));
        extra.addView(Ui.spacer(a, 8));
        extra.addView(question(L.t("SYTUACJE DLA MNIE TRUDNE (można kilka)")));
        extra.addView(simpleChips(SITUATIONS, draft.hard, null));
        extra.addView(Ui.spacer(a, 8));
        extra.addView(question(L.t("SYTUACJE DLA MNIE ŁATWE (można kilka)")));
        extra.addView(simpleChips(SITUATIONS, draft.easy, null));
        extra.addView(Ui.spacer(a, 8));
        extra.addView(counter(L.t("Poziom zmęczenia (0-10)"), () -> draft.tiredness, v -> draft.tiredness = v, 0, 10, 1));
        extra.addView(decimalCounter(L.t("Ilość snu (godz.)")));
        extra.addView(counter(L.t("Ilość wypitych kaw"), () -> draft.coffee, v -> draft.coffee = v, 0, 15, 1));
        extra.addView(question(L.t("CZY BYŁ SPOŻYWANY ALKOHOL? (można kilka)")));
        extra.addView(simpleChips(ALCOHOL, draft.alcohol, "Nie"));
        extra.addView(Ui.spacer(a, 8));
        extra.addView(yesNoRow(L.t("Czy była aktywność fizyczna?"), () -> draft.activity == null ? null : "tak".equals(draft.activity), v -> draft.activity = v ? "tak" : "nie"));
        extra.addView(counter(L.t("Poziom motywacji do ćwiczeń (0-10)"), () -> draft.motivation, v -> draft.motivation = v, 0, 10, 1));

        // Z kim sukcesy / rozmowy / telefony
        LinearLayout who = section(L.t("Z KIM DZIŚ SUKCESY"));
        autoFillSuccessWho();
        who.addView(dictChips(successPeople, draft.successWho));
        LinearLayout talk = section(L.t("ROZMOWY BEZ NAGRANIA"));
        talk.addView(dictChips(talkPeople, draft.talkNoRec));
        LinearLayout phone = section(L.t("TELEFONY BEZ NAGRANIA"));
        phone.addView(dictChips(talkPeople, draft.phoneNoRec));

        // Utrata kontroli
        LinearLayout ctrl = section(L.t("CZY DZIŚ STRACIŁEŚ KONTROLĘ NAD NOWĄ MOWĄ?"));
        FlowLayout details = new FlowLayout(a);
        String[] ctrlLabels = new String[CONTROL_OPT.length];
        for (int i = 0; i < CONTROL_OPT.length; i++) ctrlLabels[i] = CONTROL_OPT[i][1];
        List<String> detailLabels = new ArrayList<>();
        for (String k : draft.controlDetails) for (String[] o : CONTROL_OPT) if (o[0].equals(k)) detailLabels.add(o[1]);
        LinearLayout detailWrap = new LinearLayout(a);
        detailWrap.setOrientation(LinearLayout.VERTICAL);
        detailWrap.addView(simpleChipsInto(details, ctrlLabels, detailLabels, null, () -> {
            draft.controlDetails.clear();
            for (String l : detailLabels) for (String[] o : CONTROL_OPT) if (o[1].equals(l)) draft.controlDetails.add(o[0]);
        }));
        ctrl.addView(yesNoRow("", () -> draft.controlLost, v -> {
            draft.controlLost = v;
            detailWrap.setVisibility(v ? View.VISIBLE : View.GONE);
        }));
        detailWrap.setVisibility(Boolean.TRUE.equals(draft.controlLost) ? View.VISIBLE : View.GONE);
        ctrl.addView(detailWrap);

        // Samopoczucie
        LinearLayout feel = section(L.t("SAMOPOCZUCIE DZISIAJ"));
        LinearLayout ctxWrap = new LinearLayout(a);
        ctxWrap.setOrientation(LinearLayout.VERTICAL);
        feel.addView(feelingChips(ctxWrap));
        feel.addView(Ui.spacer(a, 8));
        feel.addView(ctxWrap);
        renderFeelingContext(ctxWrap);

        // Zapis
        Button save = Ui.button(a, draft.existingId != null && !draft.existingId.isEmpty() ? L.t("ZAPISZ ZMIANY W DZIENNIKU") : L.t("ZAPISZ WPIS DZIENNIKA"), R.color.pr_accent, true);
        save.setPadding((int) (12 * d), (int) (16 * d), (int) (12 * d), (int) (16 * d));
        save.setTextSize(14f);
        if (draft.locked) { save.setEnabled(false); save.setAlpha(0.5f); }
        save.setOnClickListener(v -> submit(save));
        root.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(Ui.spacer(a, 30));
    }

    interface IntGet { int get(); }
    interface IntSet { void set(int v); }
    interface BoolGet { Boolean get(); }
    interface BoolSet { void set(boolean v); }
    interface StrSet { void set(String s); }

    private View counter(String label, IntGet g, IntSet st, int min, int max, int step) {
        LinearLayout r = Ui.row(a);
        r.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
        r.addView(Ui.text(a, L.t(label), 13f, R.color.pr_text), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button minus = Ui.button(a, "−", R.color.pr_muted, false);
        TextView val = Ui.text(a, String.valueOf(g.get()), 17f, R.color.pr_accent);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.CENTER);
        Button plus = Ui.button(a, "+", R.color.pr_accent, false);
        View.OnClickListener l = v -> {
            if (draft.locked) return;
            int nv = Math.max(min, Math.min(max, g.get() + (v == plus ? step : -step)));
            st.set(nv);
            val.setText(String.valueOf(nv));
        };
        minus.setOnClickListener(l);
        plus.setOnClickListener(l);
        int bs = (int) (38 * d);
        r.addView(minus, new LinearLayout.LayoutParams(bs, bs));
        r.addView(val, new LinearLayout.LayoutParams((int) (40 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(plus, new LinearLayout.LayoutParams(bs, bs));
        return r;
    }

    private View decimalCounter(String label) {
        LinearLayout r = Ui.row(a);
        r.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
        r.addView(Ui.text(a, L.t(label), 13f, R.color.pr_text), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button minus = Ui.button(a, "−", R.color.pr_muted, false);
        TextView val = Ui.text(a, fmtSleep(draft.sleep), 17f, R.color.pr_accent);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setGravity(Gravity.CENTER);
        Button plus = Ui.button(a, "+", R.color.pr_accent, false);
        View.OnClickListener l = v -> {
            if (draft.locked) return;
            draft.sleep = Math.max(0, Math.min(16, Math.round((draft.sleep + (v == plus ? 0.5 : -0.5)) * 10) / 10.0));
            val.setText(fmtSleep(draft.sleep));
        };
        minus.setOnClickListener(l);
        plus.setOnClickListener(l);
        int bs = (int) (38 * d);
        r.addView(minus, new LinearLayout.LayoutParams(bs, bs));
        r.addView(val, new LinearLayout.LayoutParams((int) (40 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(plus, new LinearLayout.LayoutParams(bs, bs));
        return r;
    }

    private static String fmtSleep(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private View yesNoRow(String label, BoolGet g, BoolSet st) {
        LinearLayout r = Ui.row(a);
        r.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
        if (!label.isEmpty()) r.addView(Ui.text(a, L.t(label), 13f, R.color.pr_text), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button no = Ui.button(a, L.t("Nie"), R.color.pr_muted, false);
        Button yes = Ui.button(a, L.t("Tak"), R.color.pr_muted, false);
        Runnable paint = () -> {
            Boolean v = g.get();
            styleToggle(no, Boolean.FALSE.equals(v));
            styleToggle(yes, Boolean.TRUE.equals(v));
        };
        no.setOnClickListener(v -> { if (draft.locked) return; st.set(false); paint.run(); });
        yes.setOnClickListener(v -> { if (draft.locked) return; st.set(true); paint.run(); });
        paint.run();
        LinearLayout.LayoutParams lp = label.isEmpty() ? Ui.weight(1f, 8 * d) : new LinearLayout.LayoutParams((int) (64 * d), LinearLayout.LayoutParams.WRAP_CONTENT);
        if (!label.isEmpty()) lp.rightMargin = (int) (6 * d);
        r.addView(no, lp);
        r.addView(yes, label.isEmpty() ? Ui.weight(1f, 0) : new LinearLayout.LayoutParams((int) (64 * d), LinearLayout.LayoutParams.WRAP_CONTENT));
        return r;
    }

    private void styleToggle(Button b, boolean on) {
        int acc = Ui.col(a, R.color.pr_accent);
        b.setBackground(Ui.rounded(on ? (acc & 0x00FFFFFF) | 0x26000000 : Ui.col(a, R.color.pr_card), on ? acc : Ui.col(a, R.color.pr_border), d, 8 * d));
        b.setTextColor(on ? acc : Ui.col(a, R.color.pr_muted));
    }

    private EditText textInput(String initial, StrSet st) {
        EditText e = new EditText(a);
        e.setText(initial);
        e.setTextSize(14f);
        e.setTextColor(Ui.col(a, R.color.pr_text));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int p = (int) (10 * d);
        e.setPadding((int) (12 * d), p, (int) (12 * d), p);
        e.setBackground(Ui.rounded(Ui.col(a, R.color.pr_bg), Ui.col(a, R.color.pr_border), d, 8 * d));
        e.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st2, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st2, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) { st.set(s.toString()); }
        });
        if (draft.locked) e.setEnabled(false);
        return e;
    }

    private TextView chip(String text) {
        TextView c = Ui.text(a, L.t(text), 12f, R.color.pr_text);
        c.setTag(text);
        c.setPadding((int) (12 * d), (int) (7 * d), (int) (12 * d), (int) (7 * d));
        return c;
    }

    private void styleChip(TextView c, boolean on) {
        int acc = Ui.col(a, R.color.pr_accent);
        c.setBackground(Ui.rounded(on ? (acc & 0x00FFFFFF) | 0x26000000 : Ui.col(a, R.color.pr_card), on ? acc : Ui.col(a, R.color.pr_border), d, 8 * d));
        c.setTextColor(on ? acc : Ui.col(a, R.color.pr_text));
    }

    // Chipy z listy tekstow (wielokrotny wybor). exclusive = opcja wykluczajaca inne (np. "Nie").
    private View simpleChips(String[] items, List<String> selected, String exclusive) {
        return simpleChipsInto(new FlowLayout(a), items, selected, exclusive, null);
    }

    private View simpleChipsInto(FlowLayout fl, String[] items, List<String> selected, String exclusive, Runnable onChange) {
        List<TextView> all = new ArrayList<>();
        for (String it : items) {
            TextView c = chip(it);
            all.add(c);
            c.setOnClickListener(v -> {
                if (draft.locked) return;
                if (selected.contains(it)) selected.remove(it);
                else {
                    if (it.equals(exclusive)) selected.clear();
                    else if (exclusive != null) selected.remove(exclusive);
                    selected.add(it);
                }
                for (TextView x : all) styleChip(x, selected.contains(String.valueOf(x.getTag())));
                if (onChange != null) onChange.run();
            });
            styleChip(c, selected.contains(it));
            fl.addView(c);
        }
        return fl;
    }

    // Chipy ze slownika NS (id + nazwa). "Nikt" wyklucza pozostale — jak w PWA.
    private View dictChips(JSONArray items, List<String> selectedIds) {
        FlowLayout fl = new FlowLayout(a);
        List<TextView> all = new ArrayList<>();
        List<String> allIds = new ArrayList<>();
        if (items == null) return fl;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            String nm = name(o);
            boolean nikt = nm.toLowerCase(Locale.ROOT).trim().equals("nikt");
            TextView c = chip(nm);
            all.add(c);
            allIds.add(id);
            c.setOnClickListener(v -> {
                if (draft.locked) return;
                if (nikt) {
                    boolean had = selectedIds.contains(id);
                    selectedIds.clear();
                    if (!had) selectedIds.add(id);
                } else {
                    if (selectedIds.contains(id)) selectedIds.remove(id);
                    else {
                        // wybor kogokolwiek zdejmuje "Nikt"
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject oj = items.optJSONObject(j);
                            if (oj != null && name(oj).toLowerCase(Locale.ROOT).trim().equals("nikt")) selectedIds.remove(oj.optString("id", ""));
                        }
                        selectedIds.add(id);
                    }
                }
                for (int k = 0; k < all.size(); k++) styleChip(all.get(k), selectedIds.contains(allIds.get(k)));
            });
            styleChip(c, selectedIds.contains(id));
            fl.addView(c);
        }
        return fl;
    }

    private View feelingChips(LinearLayout ctxWrap) {
        FlowLayout fl = new FlowLayout(a);
        List<TextView> all = new ArrayList<>();
        List<String> allIds = new ArrayList<>();
        for (int i = 0; i < feelings.length(); i++) {
            JSONObject o = feelings.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            TextView c = chip(name(o));
            all.add(c);
            allIds.add(id);
            c.setOnClickListener(v -> {
                if (draft.locked) return;
                draft.feelingId = id;
                draft.feelingContext.clear();
                for (int k = 0; k < all.size(); k++) styleChip(all.get(k), allIds.get(k).equals(draft.feelingId));
                renderFeelingContext(ctxWrap);
            });
            styleChip(c, id.equals(draft.feelingId));
            fl.addView(c);
        }
        return fl;
    }

    private boolean feelingNegative() {
        if (draft.feelingId == null) return false;
        for (int i = 0; i < feelings.length(); i++) {
            JSONObject o = feelings.optJSONObject(i);
            if (o != null && draft.feelingId.equals(o.optString("id", ""))) {
                String n = name(o).toLowerCase(Locale.ROOT).trim();
                return n.equals("słabe") || n.equals("slabe") || n.equals("smutek");
            }
        }
        return false;
    }

    private void renderFeelingContext(LinearLayout wrap) {
        wrap.removeAllViews();
        if (draft.feelingId == null) return;
        wrap.addView(question(L.t("CO WPŁYNĘŁO NA SAMOPOCZUCIE?")));
        String[][] opts = feelingNegative() ? CTX_NEG : CTX_POS;
        FlowLayout fl = new FlowLayout(a);
        List<TextView> all = new ArrayList<>();
        for (String[] o : opts) {
            TextView c = chip(o[1]);
            all.add(c);
            c.setOnClickListener(v -> {
                if (draft.locked) return;
                if (draft.feelingContext.contains(o[0])) draft.feelingContext.remove(o[0]); else draft.feelingContext.add(o[0]);
                for (int k = 0; k < all.size(); k++) styleChip(all.get(k), draft.feelingContext.contains(opts[k][0]));
            });
            styleChip(c, draft.feelingContext.contains(o[0]));
            fl.addView(c);
        }
        wrap.addView(fl);
    }

    // Jak w PWA: jesli dzis sa nagrania, "Z kim sukcesy" podpowiada Rodzine / Obcych.
    private void autoFillSuccessWho() {
        if (!draft.successWho.isEmpty() || successPeople == null) return;
        java.io.File[] files = a.getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files == null) return;
        String t = today();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        boolean fam = false, other = false;
        for (java.io.File f : files) {
            if (!t.equals(df.format(new Date(f.lastModified())))) continue;
            String cat = RecMeta.load(a, f.getName()).cat;
            if ("Rodzina".equals(cat)) fam = true; else if (!cat.isEmpty()) other = true;
        }
        for (int i = 0; i < successPeople.length(); i++) {
            JSONObject o = successPeople.optJSONObject(i);
            if (o == null) continue;
            String n = name(o).toLowerCase(Locale.ROOT);
            if (fam && n.contains("rodzin")) draft.successWho.add(o.optString("id", ""));
            if (other && n.contains("obc")) draft.successWho.add(o.optString("id", ""));
        }
    }

    // ── zapis ──
    private String buildNotes() {
        StringBuilder sb = new StringBuilder();
        for (String[] q : SUCCESS_Q) if (Boolean.TRUE.equals(draft.success.get(q[0]))) { if (sb.length() > 0) sb.append(' '); sb.append(q[2]); }
        if (!draft.diffNote.trim().isEmpty()) { if (sb.length() > 0) sb.append(' '); sb.append(draft.diffNote.trim()); }
        if (!draft.extraNote.trim().isEmpty()) { if (sb.length() > 0) sb.append(' '); sb.append(draft.extraNote.trim()); }
        return sb.toString();
    }

    private String controlComment() {
        if (draft.controlDetails.isEmpty()) return null;
        List<String> l = new ArrayList<>();
        for (String k : draft.controlDetails) for (String[] o : CONTROL_OPT) if (o[0].equals(k)) l.add(o[1]);
        return android.text.TextUtils.join(", ", l);
    }

    private String feelingSubst() {
        if (draft.feelingContext.isEmpty()) return null;
        List<String> l = new ArrayList<>();
        for (String k : draft.feelingContext) {
            for (String[] o : CTX_NEG) if (o[0].equals(k)) l.add(o[1]);
            for (String[] o : CTX_POS) if (o[0].equals(k)) l.add(o[1]);
        }
        return android.text.TextUtils.join(", ", l);
    }

    private void submit(Button btn) {
        if (draft.locked) {
            new AlertDialog.Builder(a).setMessage(L.t("Nie możesz edytować tego wpisu, trener już go sprawdził.")).setPositiveButton("OK", null).show();
            return;
        }
        List<String> missing = new ArrayList<>();
        if (draft.successWho.isEmpty()) missing.add(L.t("• Z kim dziś sukcesy"));
        if (draft.talkNoRec.isEmpty()) missing.add(L.t("• Rozmowy bez nagrania"));
        if (draft.phoneNoRec.isEmpty()) missing.add(L.t("• Telefony bez nagrania"));
        if (draft.controlLost == null) missing.add(L.t("• Czy straciłeś kontrolę nad nową mową"));
        if (draft.feelingId == null) missing.add(L.t("• Samopoczucie dzisiaj"));
        if (!missing.isEmpty()) {
            new AlertDialog.Builder(a).setTitle(L.t("Uzupełnij brakujące pola:"))
                    .setMessage(android.text.TextUtils.join("\n", missing)).setPositiveButton("OK", null).show();
            return;
        }
        String notes = buildNotes();
        String comment = controlComment();
        String subst = feelingSubst();
        int meditation = draft.jacobson + draft.scan;
        boolean isUpdate = draft.existingId != null && !draft.existingId.isEmpty();
        btn.setEnabled(false);
        btn.setText(L.t("Zapisywanie…"));
        NsClient.Callback cb = r -> {
            btn.setEnabled(true);
            btn.setText(isUpdate || (draft.existingId != null && !draft.existingId.isEmpty()) ? L.t("ZAPISZ ZMIANY W DZIENNIKU") : L.t("ZAPISZ WPIS DZIENNIKA"));
            if (r.ok) {
                if (!isUpdate) {
                    try { draft.existingId = new JSONObject(r.body).optString("id", ""); } catch (Exception e) { }
                }
                ReminderReceiver.markDiary(a, draft.date);
                sendCopies(notes, comment, subst, meditation);
                new AlertDialog.Builder(a).setTitle(isUpdate ? L.t("Wpis dziennika zaktualizowany!") : L.t("Zapisano ✓"))
                        .setMessage(L.t("Dziękujemy — wpis z dnia ") + draft.date + L.t(" jest zapisany w NewSpeech."))
                        .setPositiveButton("OK", null).show();
            } else if (r.status == 403) {
                draft.locked = true;
                new AlertDialog.Builder(a).setMessage(L.t("Nie możesz edytować tego wpisu, trener już go sprawdził.")).setPositiveButton("OK", null).show();
                render();
            } else if (r.isAuthError()) {
                host.onAuthExpired();
            } else {
                new AlertDialog.Builder(a).setTitle(L.t("Błąd zapisu dziennika")).setMessage(r.err).setPositiveButton("OK", null).show();
            }
        };
        try {
            if (!isUpdate) {
                JSONObject p = new JSONObject();
                p.put("date", draft.date);
                p.put("meditation_count", meditation);
                p.put("visualisation_count", 0);
                p.put("anchor_count", 0);
                p.put("old_speech_usage", Boolean.TRUE.equals(draft.controlLost));
                p.put("old_speech_usage_comment", comment == null ? JSONObject.NULL : comment);
                p.put("notes", notes);
                p.put("talk_feeling_id", draft.feelingId);
                p.put("talk_feelings_substantiation", subst == null ? JSONObject.NULL : subst);
                p.put("diary_success_participant_ids", new JSONArray(draft.successWho));
                p.put("talk_participants_without_record_ids", new JSONArray(draft.talkNoRec));
                p.put("phone_call_participants_without_record_ids", new JSONArray(draft.phoneNoRec));
                NsClient.request("POST", "/diaries", host.token(), host.email(), "application/json",
                        p.toString().getBytes(StandardCharsets.UTF_8), cb);
            } else {
                // Potwierdzony format PUT /diaries/{id}: formularz (nie JSON), listy jako pole[]=wartosc
                StringBuilder f = new StringBuilder();
                form(f, "id", draft.existingId);
                form(f, "date", draft.date);
                form(f, "meditation_count", String.valueOf(meditation));
                form(f, "visualisation_count", "0");
                form(f, "anchor_count", "0");
                form(f, "old_speech_usage", String.valueOf(Boolean.TRUE.equals(draft.controlLost)));
                form(f, "old_speech_usage_comment", comment == null ? "" : comment);
                form(f, "notes", notes);
                form(f, "talk_feeling_id", draft.feelingId);
                form(f, "talk_feelings_substantiation", subst == null ? "" : subst);
                for (String id : draft.successWho) form(f, "diary_success_participant_ids[]", id);
                for (String id : draft.talkNoRec) form(f, "talk_participants_without_record_ids[]", id);
                for (String id : draft.phoneNoRec) form(f, "phone_call_participants_without_record_ids[]", id);
                NsClient.request("PUT", "/diaries/" + draft.existingId, host.token(), host.email(), "application/x-www-form-urlencoded",
                        f.toString().getBytes(StandardCharsets.UTF_8), cb);
            }
        } catch (Exception e) {
            btn.setEnabled(true);
            new AlertDialog.Builder(a).setTitle("Błąd").setMessage(String.valueOf(e.getMessage())).setPositiveButton("OK", null).show();
        }
    }

    private static void form(StringBuilder f, String k, String v) {
        if (f.length() > 0) f.append('&');
        f.append(NsClient.enc(k)).append('=').append(NsClient.enc(v));
    }

    // Kopie wpisu (nieblokujace, jak w PWA): nowy backend (pelen zestaw pol badawczych) + Google Forms
    private void sendCopies(String notes, String comment, String subst, int meditation) {
        try {
            JSONObject b = new JSONObject();
            b.put("ns_token", host.token());
            b.put("ns_email", host.email());
            b.put("ns_server", "new");
            b.put("entry_date", draft.date);
            b.put("city", draft.city.isEmpty() ? JSONObject.NULL : draft.city);
            b.put("success_who", new JSONArray(draft.successWho));
            b.put("talk_no_record", new JSONArray(draft.talkNoRec));
            b.put("phone_no_record", new JSONArray(draft.phoneNoRec));
            b.put("control_lost", Boolean.TRUE.equals(draft.controlLost));
            b.put("control_details", new JSONArray(draft.controlDetails));
            b.put("feeling", draft.feelingId);
            b.put("feeling_context", new JSONArray(draft.feelingContext));
            b.put("hard_situations", new JSONArray(draft.hard));
            b.put("easy_situations", new JSONArray(draft.easy));
            b.put("tiredness", draft.tiredness);
            b.put("sleep_hours", draft.sleep);
            b.put("coffee_count", draft.coffee);
            b.put("alcohol_types", new JSONArray(draft.alcohol));
            b.put("physical_activity", draft.activity == null ? JSONObject.NULL : draft.activity);
            b.put("motivation", draft.motivation);
            b.put("meditation_count", meditation);
            b.put("notes", notes);
            JSONObject cf = new JSONObject();
            cf.put("extraNote", draft.extraNote);
            cf.put("diffNote", draft.diffNote);
            b.put("custom_fields", cf);
            NsClient.backend("POST", "/diary/from-ns", b.toString(), r -> { });
        } catch (Exception e) { /* nieblokujace */ }
        try {
            StringBuilder f = new StringBuilder();
            form(f, "entry.1244485226", host.studentName());
            form(f, "entry.860777657", draft.city);
            form(f, "entry.1023150112", draft.date);
            form(f, "entry.969083146", labels(successPeople, draft.successWho));
            form(f, "entry.43664871", labels(talkPeople, draft.talkNoRec));
            form(f, "entry.2029413692", labels(talkPeople, draft.phoneNoRec));
            form(f, "entry.187721614", Boolean.TRUE.equals(draft.controlLost) ? "Tak" : "Nie");
            form(f, "entry.373933262", comment == null ? "" : comment);
            form(f, "entry.1593170214", comment == null ? "" : comment);
            form(f, "entry.1508972403", labels(feelings, java.util.Collections.singletonList(draft.feelingId)));
            form(f, "entry.1469449892", subst == null ? "" : subst);
            form(f, "entry.1406507271", String.valueOf(meditation));
            form(f, "entry.651616882", notes);
            form(f, "entry.1912168290", android.text.TextUtils.join(", ", draft.hard));
            form(f, "entry.1504433012", android.text.TextUtils.join(", ", draft.easy));
            form(f, "entry.1980284306", String.valueOf(draft.tiredness));
            form(f, "entry.935007890", fmtSleep(draft.sleep));
            form(f, "entry.928142028", String.valueOf(draft.coffee));
            form(f, "entry.410088544", draft.alcohol.isEmpty() ? "Nie" : android.text.TextUtils.join(", ", draft.alcohol));
            form(f, "entry.205996520", "tak".equals(draft.activity) ? "Tak" : "nie".equals(draft.activity) ? "Nie" : "");
            form(f, "entry.417360315", String.valueOf(draft.motivation));
            NsClient.postForm("https://docs.google.com/forms/d/e/1FAIpQLSePaDEaFxt7XRA1-gnvxqkBq_Pl1gneVzMWL3j_ro8535EYcQ/formResponse", f.toString());
        } catch (Exception e) { /* nieblokujace */ }
    }

    private static String labels(JSONArray dict, List<String> ids) {
        if (dict == null) return "";
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (id == null) continue;
            String lbl = id;
            for (int i = 0; i < dict.length(); i++) {
                JSONObject o = dict.optJSONObject(i);
                if (o != null && id.equals(o.optString("id", ""))) { lbl = name(o); break; }
            }
            out.add(lbl);
        }
        return android.text.TextUtils.join(", ", out);
    }

    // ── historia ──
    private void showHistory() {
        NsClient.request("GET", "/diaries?sort_by=date&sort_order=desc&page_size=30", host.token(), host.email(), null, null, r -> {
            if (r.isAuthError()) { host.onAuthExpired(); return; }
            JSONArray list = collection(r);
            LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            int p = (int) (16 * d);
            box.setPadding(p, p / 2, p, p);
            if (list == null || list.length() == 0) {
                box.addView(Ui.text(a, list == null ? L.t("Nie udało się wczytać historii.") : L.t("Brak wcześniejszych wpisów dziennika."), 13f, R.color.pr_muted));
            } else {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject e = list.optJSONObject(i);
                    if (e == null) continue;
                    boolean reviewed = !e.isNull("reviewed_at") && !e.optString("reviewed_at", "").isEmpty();
                    String feel = "";
                    JSONObject tf = e.optJSONObject("talk_feeling");
                    if (tf != null) feel = name(tf);
                    TextView t = Ui.text(a, e.optString("date", "") + (feel.isEmpty() ? "" : " · " + feel)
                            + (reviewed ? L.t("  ✓ sprawdzone") : L.t("  ⏳ czeka na ocenę")), 13f, R.color.pr_text);
                    t.setTypeface(Typeface.DEFAULT_BOLD);
                    box.addView(t);
                    String n = e.isNull("notes") ? "" : e.optString("notes", "");
                    if (!n.isEmpty()) box.addView(Ui.text(a, n, 12f, R.color.pr_muted));
                    box.addView(Ui.spacer(a, 10));
                }
            }
            android.widget.ScrollView sv = new android.widget.ScrollView(a);
            sv.addView(box);
            new AlertDialog.Builder(a).setTitle(L.t("HISTORIA DZIENNIKA")).setView(sv).setPositiveButton(L.t("Zamknij"), null).show();
        });
    }
}
