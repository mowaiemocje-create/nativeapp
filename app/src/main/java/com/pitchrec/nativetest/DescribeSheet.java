package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Ekran "ZAPISZ NAGRANIE" — odwzorowanie #catModal z PitchRec: imie kursanta, kategoria
// (siatka 2 kolumny, pomaranczowe przyciski), System Nowej Mowy, lokalizacja GPS, emocje
// (gwiazdki 1-5), notatka + ZAPISZ ✓. Pelnoekranowa nakladka na android.R.id.content.
public class DescribeSheet {

    public interface OnSave { void onSave(RecMeta meta); }

    private static final int CAT_BG = 0xFFFF9500, CAT_BORDER = 0xFFE07800;
    private static final int[] STAR_COLORS = {0, 0xFF00E5A0, 0xFF88CC44, 0xFFE5D200, 0xFFFF8C00, 0xFFFF4444};

    // newRecording = true: pola wymagane jak w PitchRec (imie, kategoria, emocje, GPS).
    // false (opisywanie istniejacego pliku): wymagana tylko kategoria.
    public static void show(Activity a, RecMeta initial, boolean newRecording, String title, OnSave onSave) {
        show(a, initial, newRecording, title, null, onSave, null);
    }

    // banner = np. "Nagrywasz poprawkę dla: Sklepy" (tryb poprawki); onClose = "Zamknij" bez opisu
    public static void show(Activity a, RecMeta initial, boolean newRecording, String title, String banner, OnSave onSave, Runnable onClose) {
        final RecMeta m = initial != null ? initial : new RecMeta();
        final ViewGroup root = a.findViewById(android.R.id.content);

        FrameLayout overlay = new FrameLayout(a);
        overlay.setBackgroundColor(Ui.col(a, R.color.pr_bg));
        overlay.setClickable(true); // nie przepuszcza dotyku do ekranu pod spodem
        overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        // odstep od belek systemowych (overlay jest na calym ekranie)
        int top = statusBarHeight(a), bottom = navBarHeight(a);
        col.setPadding(0, top, 0, bottom);
        overlay.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // naglowek
        LinearLayout head = Ui.row(a);
        head.setBackgroundColor(Ui.col(a, R.color.pr_card));
        int hp = (int) Ui.dp(a, 14);
        head.setPadding(hp, (int) Ui.dp(a, 10), hp, (int) Ui.dp(a, 10));
        TextView t = Ui.text(a, title, 12f, R.color.pr_accent);
        t.setLetterSpacing(0.2f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.text(a, "✕", 22f, R.color.pr_muted);
        close.setPadding((int) Ui.dp(a, 8), 0, (int) Ui.dp(a, 4), 0);
        head.addView(close);
        col.addView(head);

        ScrollView sv = new ScrollView(a);
        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(hp, hp, hp, hp);
        sv.addView(body);
        col.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        android.content.SharedPreferences prefs = a.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // ZAMKNIJ bez opisu — NA SAMEJ GORZE (od razu widoczny). Nagranie zostaje w NAGRANIACH
        // jako "bez opisu"; opisac i wyslac mozna je pozniej ("📝 Opisz i wyślij").
        Button closeBtn = Ui.button(a, newRecording ? L.t("✕ ZAMKNIJ — zapisz bez opisu") : L.t("✕ ZAMKNIJ"), R.color.pr_warn, false);
        body.addView(closeBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(Ui.spacer(a, 12));

        if (banner != null) {
            TextView bn = Ui.text(a, banner, 13f, R.color.pr_accent);
            bn.setTypeface(Typeface.DEFAULT_BOLD);
            int bp = (int) Ui.dp(a, 10);
            bn.setPadding(bp, bp, bp, bp);
            bn.setBackground(Ui.rounded((Ui.col(a, R.color.pr_accent) & 0x00FFFFFF) | 0x1A000000, Ui.col(a, R.color.pr_accent), Ui.dp(a, 1), Ui.dp(a, 8)));
            body.addView(bn);
            body.addView(Ui.spacer(a, 12));
        }

        // IMIE KURSANTA (zapamietywane jak w PWA)
        body.addView(Ui.label(a, L.t("IMIĘ KURSANTA")));
        EditText nameInput = input(a, L.t("np. Anna K."));
        String savedName = m.name != null && !m.name.isEmpty() ? m.name : prefs.getString("student_name", "");
        nameInput.setText(savedName);
        body.addView(nameInput);
        body.addView(Ui.spacer(a, 12));

        // KATEGORIA
        body.addView(Ui.label(a, L.t("KATEGORIA")));
        final String[] selCat = {m.cat == null ? "" : m.cat};
        LinearLayout catGrid = new LinearLayout(a);
        catGrid.setOrientation(LinearLayout.VERTICAL);
        List<Button> catBtns = new ArrayList<>();
        body.addView(catGrid);

        // ZADANIE SPECJALNE — tylko dla kategorii Special: kursant wybiera, ktore zadanie wykonal
        final String[] selSpecial = {m.special == null ? "" : m.special};
        LinearLayout specialBox = new LinearLayout(a);
        specialBox.setOrientation(LinearLayout.VERTICAL);
        body.addView(specialBox);
        final Runnable[] buildSpecial = new Runnable[1];
        buildSpecial[0] = () -> {
            specialBox.removeAllViews();
            if (!"Special".equals(selCat[0])) return;
            specialBox.addView(Ui.spacer(a, 8));
            specialBox.addView(Ui.label(a, "🎬 " + L.t("ZADANIE SPECJALNE — które wykonujesz?")));
            TextView hintSp = Ui.text(a, L.t("Dotknij zadania, żeby zobaczyć jego opis."), 11f, R.color.pr_muted);
            hintSp.setPadding(0, 0, 0, (int) Ui.dp(a, 6));
            specialBox.addView(hintSp);
            String mySys = prefs.getString("last_sys", "");
            // zadania zaliczone przez trenera znikaja z listy (sa juz na stale w Statystykach)
            List<SpecialTasks.Task> order = new ArrayList<>();
            int okN = 0;
            for (SpecialTasks.Task tk : SpecialTasks.ALL) { if ("ok".equals(SpecialTasks.state(a, tk))) { okN++; continue; } if (tk.forSystem(mySys)) order.add(tk); }
            for (SpecialTasks.Task tk : SpecialTasks.ALL) if (!"ok".equals(SpecialTasks.state(a, tk)) && !tk.forSystem(mySys)) order.add(tk);
            if (okN > 0) {
                TextView okInfo = Ui.text(a, "✓ " + L.f("{0} zadań zaliczonych — nie ma ich już na liście", okN), 11f, R.color.pr_accent);
                okInfo.setPadding(0, 0, 0, (int) Ui.dp(a, 6));
                specialBox.addView(okInfo);
            }
            for (SpecialTasks.Task tk : order) {
                boolean on = tk.name.equals(selSpecial[0]);
                String stt = SpecialTasks.state(a, tk);
                LinearLayout row = Ui.row(a);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                int pd = (int) Ui.dp(a, 8);
                row.setPadding(pd, pd, pd, pd);
                row.setBackground(Ui.rounded(on ? 0x33E8820C : 0x00000000, on ? 0xFFE8820C : 0x33FFFFFF, Ui.dp(a, on ? 2 : 1), Ui.dp(a, 8)));
                TextView ic = Ui.text(a, tk.icon, 18f, R.color.pr_text);
                ic.setGravity(android.view.Gravity.CENTER);
                ic.setBackground(Ui.rounded(tk.color, 0, 0, Ui.dp(a, 8)));
                int icS = (int) Ui.dp(a, 36);
                row.addView(ic, new LinearLayout.LayoutParams(icS, icS));
                String sub = "pending".equals(stt) ? "  ⏳ " + L.t("czeka na ocenę")
                        : "rejected".equals(stt) ? "  ↺ " + L.t("do poprawy")
                        : (tk.forSystem(mySys) ? "" : "  · " + android.text.TextUtils.join("/", tk.systems));
                LinearLayout txBox = new LinearLayout(a);
                txBox.setOrientation(LinearLayout.VERTICAL);
                txBox.setPadding((int) Ui.dp(a, 10), 0, 0, 0);
                TextView tx = Ui.text(a, tk.shortName() + sub, 12f, R.color.pr_text);
                if (on) tx.setTypeface(Typeface.DEFAULT_BOLD);
                tx.setAlpha(tk.forSystem(mySys) || on ? 1f : 0.55f);
                txBox.addView(tx);
                // Po dotknieciu: pelny opis zadania (co dokladnie nagrac) + dla jakich systemow
                if (on) {
                    String desc = tk.description();
                    if (!desc.isEmpty()) {
                        TextView ds = Ui.text(a, desc, 12f, R.color.pr_text);
                        ds.setPadding(0, (int) Ui.dp(a, 4), 0, 0);
                        txBox.addView(ds);
                    }
                    TextView sy = Ui.text(a, tk.systems.length == 0 ? L.t("Dla każdego systemu") : L.t("Systemy: ") + android.text.TextUtils.join(", ", tk.systems), 10f, R.color.pr_muted);
                    sy.setPadding(0, (int) Ui.dp(a, 3), 0, 0);
                    txBox.addView(sy);
                }
                row.addView(txBox, Ui.weight(1f, 0));
                row.setOnClickListener(v -> { selSpecial[0] = tk.name.equals(selSpecial[0]) ? "" : tk.name; buildSpecial[0].run(); });
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = (int) Ui.dp(a, 5);
                specialBox.addView(row, rlp);
            }
        };
        final boolean[] nsAsked = {false};
        Runnable onCat = () -> {
            buildSpecial[0].run();
            if ("Special".equals(selCat[0]) && !nsAsked[0]) {
                nsAsked[0] = true;
                SpecialTasks.refreshFromNs(a, () -> buildSpecial[0].run()); // swiezy stan z NewSpeech
            }
        };
        buildGrid(a, catGrid, NsClient.CATEGORIES, catBtns, selCat, onCat);
        onCat.run();
        body.addView(Ui.spacer(a, 12));

        // SYSTEM NOWEJ MOWY (ostatni wybor zapamietany)
        body.addView(Ui.label(a, L.t("SYSTEM NOWEJ MOWY")));
        final String lockedSys = m.fixSys != null ? m.fixSys : "";
        final String[] selSys = {!lockedSys.isEmpty() ? lockedSys : m.sys != null && !m.sys.isEmpty() ? m.sys : prefs.getString("last_sys", "")};
        if (!m.fixRecordId.isEmpty()) {
            // POPRAWKA: system mowy musi byc taki sam jak w nagraniu, w ktorym byl blad
            TextView lockInfo = Ui.text(a, lockedSys.isEmpty()
                    ? "⚠ " + L.t("Poprawkę nagraj w TYM SAMYM systemie mowy, co nagranie z błędem — wybierz go poniżej.")
                    : "🔒 " + L.f("Poprawka musi być w tym samym systemie mowy co nagranie z błędem: {0}", lockedSys), 12f, R.color.pr_text);
            int wc = Ui.col(a, R.color.pr_warn);
            lockInfo.setPadding((int) Ui.dp(a, 10), (int) Ui.dp(a, 8), (int) Ui.dp(a, 10), (int) Ui.dp(a, 8));
            lockInfo.setBackground(Ui.rounded((wc & 0x00FFFFFF) | 0x22000000, wc, Ui.dp(a, 1), Ui.dp(a, 8)));
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.bottomMargin = (int) Ui.dp(a, 8);
            body.addView(lockInfo, llp);
        }
        LinearLayout sysGrid = new LinearLayout(a);
        sysGrid.setOrientation(LinearLayout.VERTICAL);
        List<Button> sysBtns = new ArrayList<>();
        buildGrid(a, sysGrid, RecMeta.SYS_LEVELS, sysBtns, selSys);
        if (!lockedSys.isEmpty()) {
            for (Button sb : sysBtns) {
                boolean same = lockedSys.equals(String.valueOf(sb.getTag()));
                sb.setEnabled(same);
                sb.setAlpha(same ? 1f : 0.3f);
            }
        }
        body.addView(sysGrid);
        body.addView(Ui.spacer(a, 12));

        // LOKALIZACJA GPS
        body.addView(Ui.label(a, L.t("LOKALIZACJA GPS")));
        TextView gpsBox = Ui.text(a, "", 11f, R.color.pr_muted);
        gpsBox.setTypeface(Typeface.DEFAULT_BOLD);
        int gp = (int) Ui.dp(a, 10);
        gpsBox.setPadding((int) Ui.dp(a, 12), gp, (int) Ui.dp(a, 12), gp);
        body.addView(gpsBox);
        final Location[] gps = {null};
        if (m.hasGps()) {
            Location l = new Location("saved");
            l.setLatitude(m.lat);
            l.setLongitude(m.lon);
            gps[0] = l;
        } else if (GpsHelper.recordingFix != null && newRecording) {
            gps[0] = GpsHelper.recordingFix;
        } else if (GpsHelper.lastFix != null) {
            gps[0] = GpsHelper.lastFix;
        }
        Runnable refreshGps = () -> showGps(a, gpsBox, gps[0], null);
        refreshGps.run();
        View.OnClickListener fetchGps = v -> {
            if (!GpsHelper.hasPermission(a)) {
                showGps(a, gpsBox, null, L.t("❌ Brak zgody na lokalizację — kliknij, aby zezwolić"));
                MainActivity.requestLocationPermission(a);
                return;
            }
            showGps(a, gpsBox, null, L.t("⏳ Pobieranie GPS…"));
            GpsHelper.requestFix(a, loc -> {
                if (loc != null) gps[0] = loc;
                showGps(a, gpsBox, gps[0], loc == null && gps[0] == null ? L.t("❌ GPS niedostępny — kliknij, aby spróbować ponownie") : null);
            });
        };
        gpsBox.setOnClickListener(fetchGps);
        if (gps[0] == null) fetchGps.onClick(gpsBox);
        body.addView(Ui.spacer(a, 12));

        // EMOCJE W TEJ SYTUACJI
        body.addView(Ui.label(a, L.t("EMOCJE W TEJ SYTUACJI")));
        LinearLayout stars = Ui.row(a);
        final int[] emo = {m.emotion};
        TextView starLbl = Ui.text(a, "—", 11f, R.color.pr_muted);
        List<TextView> starViews = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int v = i;
            TextView s = Ui.text(a, "★", 32f, R.color.pr_muted);
            s.setPadding((int) Ui.dp(a, 4), 0, (int) Ui.dp(a, 4), 0);
            s.setOnClickListener(x -> { emo[0] = v; paintStars(a, starViews, starLbl, v); });
            starViews.add(s);
            stars.addView(s);
        }
        starLbl.setPadding((int) Ui.dp(a, 6), 0, 0, 0);
        stars.addView(starLbl);
        paintStars(a, starViews, starLbl, emo[0]);
        body.addView(stars);
        body.addView(Ui.spacer(a, 12));

        // NOTATKA + ZAPISZ
        body.addView(Ui.label(a, L.t("NOTATKA")));
        LinearLayout noteRow = Ui.row(a);
        EditText noteInput = input(a, L.t("opcjonalnie…"));
        noteInput.setText(m.note);
        noteRow.addView(noteInput, Ui.weight(1f, Ui.dp(a, 8)));
        Button save = Ui.button(a, L.t("ZAPISZ ✓"), R.color.pr_purple, true);
        save.setTextColor(Ui.col(a, R.color.pr_bg));
        save.setLetterSpacing(0.1f);
        noteRow.addView(save);
        body.addView(noteRow);
        body.addView(Ui.spacer(a, 16));

        // ZAMKNIJ bez opisu — nagranie zostaje w NAGRANIACH jako "bez opisu"; opisac i wyslac
        // mozna je pozniej przyciskiem "📝 Opisz i wyślij" (tak jak w PitchRec).
        body.addView(Ui.spacer(a, 24));

        Runnable dismiss = () -> { try { root.removeView(overlay); } catch (Exception e) { } };
        View.OnClickListener closeL = v -> { dismiss.run(); if (onClose != null) onClose.run(); };
        close.setOnClickListener(closeL);
        closeBtn.setOnClickListener(closeL);

        save.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            List<String> missing = new ArrayList<>();
            if (selCat[0].isEmpty()) missing.add(L.t("• Kategoria"));
            if (!m.fixRecordId.isEmpty() && lockedSys.isEmpty() && selSys[0].isEmpty()) missing.add(L.t("• System nowej mowy (taki sam jak w nagraniu z błędem)"));
            if (newRecording) {
                if (name.isEmpty()) missing.add(L.t("• Imię kursanta"));
                if (emo[0] == 0) missing.add(L.t("• Emocje (gwiazdki 1-5)"));
                // GPS wymagany jak w PWA — chyba ze uzytkownik nie dal zgody na lokalizacje
                if (gps[0] == null && GpsHelper.hasPermission(a)) missing.add(L.t("• GPS — poczekaj lub kliknij pole GPS"));
            }
            if (!missing.isEmpty()) {
                new AlertDialog.Builder(a).setTitle(L.t("Wymagane"))
                        .setMessage(android.text.TextUtils.join("\n", missing))
                        .setPositiveButton("OK", null).show();
                return;
            }
            m.name = name;
            m.cat = selCat[0];
            m.sys = !lockedSys.isEmpty() ? lockedSys : selSys[0];
            m.emotion = emo[0];
            m.note = noteInput.getText().toString().trim();
            m.special = "Special".equals(selCat[0]) ? selSpecial[0] : "";
            SpecialTasks.Task st = SpecialTasks.find(m.special);
            if (st != null && !m.note.contains(st.shortName())) m.note = "🎬 " + st.shortName() + (m.note.isEmpty() ? "" : " — " + m.note);
            if (gps[0] != null) { m.lat = gps[0].getLatitude(); m.lon = gps[0].getLongitude(); }
            prefs.edit().putString("student_name", name).putString("last_sys", selSys[0]).apply();
            dismiss.run();
            if (onSave != null) onSave.onSave(m);
        });

        root.addView(overlay);
    }

    private static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setTextSize(14f);
        e.setTextColor(Ui.col(c, R.color.pr_text));
        e.setHintTextColor(Ui.col(c, R.color.pr_muted));
        int p = (int) Ui.dp(c, 10);
        e.setPadding((int) Ui.dp(c, 12), p, (int) Ui.dp(c, 12), p);
        e.setBackground(Ui.rounded(Ui.col(c, R.color.pr_card), Ui.col(c, R.color.pr_border), Ui.dp(c, 1), Ui.dp(c, 8)));
        return e;
    }

    // Siatka 2 kolumny jak .cat-grid — pomaranczowe przyciski, wybrany = bialy z pomaranczowym napisem
    private static void buildGrid(Context c, LinearLayout grid, String[] items, List<Button> btns, String[] selected) {
        buildGrid(c, grid, items, btns, selected, null);
    }

    private static void buildGrid(Context c, LinearLayout grid, String[] items, List<Button> btns, String[] selected, Runnable onChange) {
        LinearLayout row = null;
        for (int i = 0; i < items.length; i++) {
            if (i % 2 == 0) {
                row = Ui.row(c);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = (int) Ui.dp(c, 6);
                grid.addView(row, rp);
            }
            final String item = items[i];
            Button b = new Button(c);
            b.setText(L.t(item));
            b.setTag(item);
            b.setAllCaps(false);
            b.setTextSize(12f);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setMinHeight(0);
            b.setMinimumHeight(0);
            b.setStateListAnimator(null);
            int p = (int) Ui.dp(c, 12);
            b.setPadding((int) Ui.dp(c, 6), p, (int) Ui.dp(c, 6), p);
            btns.add(b);
            b.setOnClickListener(v -> {
                selected[0] = item;
                for (Button x : btns) styleCat(c, x, String.valueOf(x.getTag()).equals(selected[0]));
                if (onChange != null) onChange.run();
            });
            styleCat(c, b, item.equals(selected[0]));
            row.addView(b, Ui.weight(1f, (i % 2 == 0) ? Ui.dp(c, 6) : 0));
        }
        if (items.length % 2 == 1 && row != null) {
            View filler = new View(c);
            row.addView(filler, Ui.weight(1f, 0));
        }
    }

    private static void styleCat(Context c, Button b, boolean on) {
        b.setBackground(Ui.rounded(on ? 0xFFFFFFFF : CAT_BG, CAT_BORDER, Ui.dp(c, 2), Ui.dp(c, 8)));
        b.setTextColor(on ? CAT_BORDER : 0xFFFFFFFF);
    }

    private static void paintStars(Context c, List<TextView> stars, TextView lbl, int v) {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setTextColor(i < v ? STAR_COLORS[v] : Ui.col(c, R.color.pr_muted));
        }
        lbl.setText(v > 0 ? RecMeta.EMOTION_LABELS[v] : "—");
    }

    private static void showGps(Context c, TextView box, Location loc, String msg) {
        boolean ok = msg == null && loc != null;
        if (msg != null) box.setText(msg);
        else if (loc != null) box.setText(String.format(Locale.US, L.t("📍 %.5f, %.5f (±%dm)"), loc.getLatitude(), loc.getLongitude(), Math.round(loc.getAccuracy())));
        else box.setText(L.t("❌ Brak GPS z czasu nagrywania — kliknij, aby pobrać"));
        box.setTextColor(Ui.col(c, ok ? R.color.pr_text : (msg != null && msg.startsWith("⏳") ? R.color.pr_muted : R.color.pr_warn)));
        box.setBackground(Ui.rounded(Ui.col(c, R.color.pr_card), Ui.col(c, ok ? R.color.pr_border : R.color.pr_warn), Ui.dp(c, 1), Ui.dp(c, 8)));
    }

    private static int statusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : (int) Ui.dp(c, 24);
    }

    private static int navBarHeight(Context c) {
        int id = c.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : 0;
    }
}
