package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Ekrany i przyciski NAGRYWANIA ROZMOW (CallRecService): wlaczanie uslugi, ☎ z nagraniem,
// "Nagraj nastepna rozmowe", sekcja w Ustawieniach.
public final class CallRecUi {

    private CallRecUi() { }

    // ☎ przy osobie z listy: rozmowa nagra sie sama (gdy usluga wlaczona)
    public static void dial(Activity a, String phone, String label) {
        if (!CallRecService.autoOn(a)) { ContactList.dial(a, phone); return; }
        if (CallRecService.enabled(a)) {
            CallRecService.arm(a, label, 3);
            Toast.makeText(a, "🔴 " + L.t("Ta rozmowa zostanie nagrana — po rozłączeniu zapiszesz ją w apce."), Toast.LENGTH_LONG).show();
            ContactList.dial(a, phone);
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("📞 " + L.t("Nagrać tę rozmowę?"))
                .setMessage(L.t("Apka może sama nagrać rozmowę (tak jak Cube ACR). Trzeba raz włączyć „New Speech — nagrywanie rozmów” w ułatwieniach dostępu."))
                .setPositiveButton(L.t("Włącz nagrywanie"), (d, w) -> setup(a))
                .setNegativeButton(L.t("Zadzwoń bez nagrania"), (d, w) -> ContactList.dial(a, phone))
                .setNeutralButton(L.t("Nie pytaj więcej"), (d, w) -> {
                    a.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE).edit().putBoolean("callrec_auto", false).apply();
                    ContactList.dial(a, phone);
                })
                .show();
    }

    // "Nagraj następną rozmowę" — np. gdy to inny kursant dzwoni do Ciebie
    public static void armNext(Activity a) {
        if (!CallRecService.enabled(a)) { setup(a); return; }
        CallRecService.arm(a, "", 30);
        Toast.makeText(a, "🔴 " + L.t("Następna rozmowa (w ciągu 30 min) zostanie nagrana."), Toast.LENGTH_LONG).show();
    }

    // Instrukcja wlaczenia uslugi ulatwien dostepu (z obejsciem "ustawien z ograniczeniami" Androida 13+)
    public static void setup(Activity a) {
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (18 * d);
        box.setPadding(p, (int) (8 * d), p, 0);
        box.addView(step(a, "1", L.t("Otwórz „Ułatwienia dostępu” i znajdź „New Speech — nagrywanie rozmów” (na Samsungu: Zainstalowane aplikacje; na innych: Pobrane aplikacje / Usługi). Włącz i potwierdź.")));
        Button acc = Ui.button(a, "♿ " + L.t("Otwórz ułatwienia dostępu"), R.color.pr_accent, true);
        acc.setOnClickListener(v -> openAccessibility(a));
        box.addView(acc);
        if (Build.VERSION.SDK_INT >= 33) {
            box.addView(Ui.spacer(a, 10));
            box.addView(step(a, "2", L.t("Przełącznik jest szary albo widzisz „Ustawienie ograniczone”? Otwórz Informacje o aplikacji → menu ⋮ w prawym górnym rogu → „Zezwól na ustawienia z ograniczeniami”. Potem wróć do kroku 1.")));
            Button info = Ui.button(a, "ⓘ " + L.t("Otwórz informacje o aplikacji"), R.color.pr_muted, false);
            info.setOnClickListener(v -> openAppInfo(a));
            box.addView(info);
        }
        // Samsung i inni wylaczaja usluge, gdy apka ma ograniczenia baterii
        box.addView(Ui.spacer(a, 10));
        box.addView(step(a, Build.VERSION.SDK_INT >= 33 ? "3" : "2", L.t("Żeby telefon sam nie wyłączał nagrywania: bateria apki „Bez ograniczeń”. Na Samsungu też: Ustawienia → Bateria → Limity użycia w tle → „Aplikacje nigdy nieusypiane” → dodaj New Speech.")
                + (batteryFree(a) ? "  ✅" : "")));
        Button bat = Ui.button(a, "🔋 " + L.t("Bateria: bez ograniczeń"), batteryFree(a) ? R.color.pr_muted : R.color.pr_accent, !batteryFree(a));
        bat.setOnClickListener(v -> askBattery(a));
        box.addView(bat);
        box.addView(Ui.spacer(a, 10));
        box.addView(step(a, Build.VERSION.SDK_INT >= 33 ? "4" : "3", L.t("Dzwoń przyciskiem ☎ w apce. Jeśli rozmówcę słabo słychać na nagraniu, włącz w czasie rozmowy głośnik.")));
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        new AlertDialog.Builder(a)
                .setTitle("📞 " + L.t("Włącz nagrywanie rozmów"))
                .setView(sv)
                .setPositiveButton("OK", null)
                .show();
    }

    private static TextView step(Activity a, String n, String s) {
        TextView t = Ui.text(a, n + ". " + s, 13f, R.color.pr_text);
        t.setPadding(0, 0, 0, (int) Ui.dp(a, 6));
        return t;
    }

    static boolean batteryFree(Context c) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
        } catch (Exception e) { return false; }
    }

    // Systemowe okienko "Zezwolić na dzialanie w tle bez ograniczen?"; gdy niedostepne — Informacje o aplikacji
    static void askBattery(Activity a) {
        if (!batteryFree(a)) {
            try {
                a.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + a.getPackageName())));
                return;
            } catch (Exception e) { }
        }
        openAppInfo(a);
    }

    // ── PILNOWANIE: telefon (np. Samsung) potrafi sam wylaczyc usluge ──
    // Zapamietujemy, ze kursant ja wlaczyl; gdy zniknie — przypominamy (okienko w apce + powiadomienie).
    static void noteState(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE);
        if (CallRecService.enabled(c)) p.edit().putBoolean("callrec_wanted", true).putBoolean("callrec_off_notified", false).apply();
    }

    static boolean lostService(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE);
        return p.getBoolean("callrec_wanted", false) && !CallRecService.enabled(c);
    }

    // W tle (alarm co ~15 min): powiadomienie raz na kazde wylaczenie
    public static void watch(Context c) {
        noteState(c);
        if (!lostService(c)) return;
        android.content.SharedPreferences p = c.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE);
        if (p.getBoolean("callrec_off_notified", false)) return;
        p.edit().putBoolean("callrec_off_notified", true).apply();
        android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.createNotificationChannel(new android.app.NotificationChannel("call_rec", L.t("Nagrywanie rozmów"), android.app.NotificationManager.IMPORTANCE_DEFAULT));
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("open_page", "callrec_setup");
        String text = L.t("Telefon wyłączył nagrywanie rozmów. Dotknij, żeby włączyć ponownie i ustawić baterię „Bez ograniczeń”.");
        android.app.Notification n = new android.app.Notification.Builder(c, "call_rec")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("⚠️ " + L.t("Nagrywanie rozmów wyłączone"))
                .setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                .setContentIntent(android.app.PendingIntent.getActivity(c, 4810, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true).build();
        try { nm.notify(4810, n); } catch (SecurityException e) { }
    }

    private static long lastAsk = 0L;

    // Przy otwarciu apki: okienko, gdy usluga zniknela (najwyzej co 10 min)
    public static void checkOnResume(Activity a) {
        noteState(a);
        if (!lostService(a) || System.currentTimeMillis() - lastAsk < 10 * 60_000L) return;
        lastAsk = System.currentTimeMillis();
        new AlertDialog.Builder(a)
                .setTitle("⚠️ " + L.t("Nagrywanie rozmów wyłączone"))
                .setMessage(L.t("Telefon sam wyłączył usługę nagrywania rozmów. Włącz ją ponownie i ustaw baterię apki „Bez ograniczeń” — wtedy przestanie się wyłączać."))
                .setPositiveButton(L.t("Włącz ponownie"), (d, w) -> setup(a))
                .setNeutralButton(L.t("Nie używam"), (d, w) -> a.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE).edit().putBoolean("callrec_wanted", false).apply())
                .setNegativeButton(L.t("Później"), null)
                .show();
    }

    static void openAccessibility(Activity a) {
        try { a.startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (Exception e) { Toast.makeText(a, L.t("Nie udało się otworzyć ustawień"), Toast.LENGTH_SHORT).show(); }
    }

    static void openAppInfo(Activity a) {
        try {
            a.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName())));
        } catch (Exception e) { Toast.makeText(a, L.t("Nie udało się otworzyć ustawień"), Toast.LENGTH_SHORT).show(); }
    }

    // Przycisk "Nagraj następną rozmowę" + stan (do karty telefonow w harmonogramie)
    public static void addArmRow(Activity a, LinearLayout card, Runnable refresh) {
        if (CallRecService.enabled(a) && CallRecService.allOn(a)) return; // i tak nagrywa kazda rozmowe
        boolean armed = CallRecService.armed(a);
        boolean on = CallRecService.enabled(a);
        Button b = Ui.button(a, armed ? "● " + L.t("Następna rozmowa zostanie nagrana — anuluj") : "🔴 " + L.t("Nagraj następną rozmowę (np. gdy ktoś dzwoni do Ciebie)"),
                armed ? R.color.pr_accent : R.color.pr_muted, armed);
        b.setOnClickListener(v -> {
            if (CallRecService.armed(a)) CallRecService.disarm(a); else armNext(a);
            if (refresh != null) refresh.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) Ui.dp(a, 6);
        card.addView(b, lp);
        if (!on) {
            TextView t = Ui.text(a, "ⓘ " + L.t("Nagrywanie rozmów jest wyłączone — dotknij, żeby włączyć."), 11f, R.color.pr_accent);
            t.setPadding(0, (int) Ui.dp(a, 4), 0, 0);
            t.setOnClickListener(v -> setup(a));
            card.addView(t);
        }
    }

    // Sekcja w Ustawieniach
    public static void fillSettings(Activity a, LinearLayout s, Runnable rerender) {
        android.content.SharedPreferences p = a.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE);
        boolean on = CallRecService.enabled(a);
        TextView st = Ui.text(a, on ? "✅ " + L.t("Usługa włączona — rozmowy z ☎ w apce nagrywają się same.")
                : "⚪ " + L.t("Usługa wyłączona — rozmowy się nie nagrywają."), 13f, R.color.pr_text);
        st.setTypeface(Typeface.DEFAULT_BOLD);
        if (on) st.setTextColor(0xFF00E676);
        s.addView(st);
        if (on && !batteryFree(a)) {
            TextView bw = Ui.text(a, "⚠️ " + L.t("Bateria apki ma ograniczenia — telefon może sam wyłączać nagrywanie. Dotknij, żeby zmienić."), 12f, R.color.pr_warn);
            bw.setPadding(0, (int) Ui.dp(a, 4), 0, 0);
            bw.setOnClickListener(v -> askBattery(a));
            s.addView(bw);
        }
        Button en = Ui.button(a, on ? L.t("Ustawienia ułatwień dostępu") : "📞 " + L.t("Włącz nagrywanie rozmów"), on ? R.color.pr_muted : R.color.pr_accent, !on);
        en.setOnClickListener(v -> { if (on) openAccessibility(a); else setup(a); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) Ui.dp(a, 6);
        s.addView(en, lp);
        boolean all = CallRecService.allOn(a);
        Button allB = Ui.button(a, "📞 " + L.t("Nagrywaj wszystkie rozmowy (też do miasta, rodziny)") + ": " + (all ? L.t("WŁ") : L.t("WYŁ")), all ? R.color.pr_accent : R.color.pr_muted, all);
        allB.setOnClickListener(v -> { p.edit().putBoolean("callrec_all", !all).apply(); CallRecService.arm(a, "", 0); rerender.run(); });
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpA.topMargin = (int) Ui.dp(a, 6);
        s.addView(allB, lpA);
        if (on && !all) {
            boolean armed = CallRecService.armed(a);
            Button nx = Ui.button(a, armed ? "● " + L.t("Następna rozmowa zostanie nagrana — anuluj") : "🔴 " + L.t("Nagraj następną rozmowę (np. gdy ktoś dzwoni do Ciebie)"), armed ? R.color.pr_accent : R.color.pr_muted, armed);
            nx.setOnClickListener(v -> { if (CallRecService.armed(a)) CallRecService.disarm(a); else armNext(a); rerender.run(); });
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp1.topMargin = (int) Ui.dp(a, 6);
            s.addView(nx, lp1);
        }
        boolean auto = CallRecService.autoOn(a);
        Button ab = Ui.button(a, "☎ " + L.t("Nagrywaj rozmowy z listy") + ": " + (auto ? L.t("WŁ") : L.t("WYŁ")), auto ? R.color.pr_accent : R.color.pr_muted, auto);
        ab.setOnClickListener(v -> { p.edit().putBoolean("callrec_auto", !auto).apply(); rerender.run(); });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = (int) Ui.dp(a, 6);
        s.addView(ab, lp2);
        // Zrodlo dzwieku — do testow: na roznych telefonach lepiej dziala inne
        TextView sh = Ui.text(a, L.t("Źródło dźwięku (zmień, jeśli rozmówcy nie słychać):"), 12f, R.color.pr_muted);
        sh.setPadding(0, (int) Ui.dp(a, 10), 0, (int) Ui.dp(a, 4));
        s.addView(sh);
        String cur = p.getString("callrec_src", "auto");
        String[][] src = {{"auto", L.t("Auto")}, {"rec", "Voice recognition"}, {"comm", "Voice communication"}, {"mic", "Mic"}, {"call", "Voice call"}};
        LinearLayout row = null;
        for (int i = 0; i < src.length; i++) {
            if (i % 2 == 0 || i == 0) { row = Ui.row(a); row.setPadding(0, i == 0 ? 0 : (int) Ui.dp(a, 4), 0, 0); s.addView(row); }
            String code = src[i][0];
            boolean sel = code.equals(cur);
            Button b = Ui.button(a, src[i][1], sel ? R.color.pr_accent : R.color.pr_muted, sel);
            b.setTextSize(11f);
            b.setOnClickListener(v -> { p.edit().putString("callrec_src", code).apply(); rerender.run(); });
            row.addView(b, Ui.weight(1f, i % 2 == 0 ? Ui.dp(a, 6) : 0));
        }
        // Glosnosc nagrania rozmowy
        TextView bh = Ui.text(a, L.t("Głośność nagrania — zawsze do 0 dB; Auto/Mocno dodatkowo wyrównuje mnie i rozmówcę:"), 12f, R.color.pr_muted);
        bh.setPadding(0, (int) Ui.dp(a, 10), 0, (int) Ui.dp(a, 4));
        s.addView(bh);
        String bc = p.getString("callrec_boost", "auto");
        String[][] bo = {{"auto", L.t("Auto")}, {"strong", L.t("Mocno")}, {"off", L.t("Tylko 0 dB")}};
        LinearLayout br = Ui.row(a);
        s.addView(br);
        for (int i = 0; i < bo.length; i++) {
            String code = bo[i][0];
            boolean sel = code.equals(bc);
            Button b = Ui.button(a, bo[i][1], sel ? R.color.pr_accent : R.color.pr_muted, sel);
            b.setTextSize(11f);
            b.setOnClickListener(v -> { p.edit().putString("callrec_boost", code).apply(); rerender.run(); });
            br.addView(b, Ui.weight(1f, i < bo.length - 1 ? Ui.dp(a, 6) : 0));
        }
        String last = p.getString("callrec_last", "");
        if (!last.isEmpty()) {
            TextView lt = Ui.text(a, L.t("Ostatnie nagranie rozmowy:") + " " + last, 11f, R.color.pr_muted);
            lt.setPadding(0, (int) Ui.dp(a, 8), 0, 0);
            s.addView(lt);
        }
        String err = p.getString("callrec_last_err", "");
        if (!err.isEmpty()) s.addView(Ui.text(a, "⚠️ " + err, 11f, R.color.pr_warn));
        TextView h = Ui.text(a, all
                ? L.t("Nagrywana jest każda rozmowa. Po rozłączeniu wybierasz: zapisz jako ćwiczenie albo usuń. Rozmowy z ☎ w apce od razu mają wybraną osobę. Nie działa przez słuchawki Bluetooth.")
                : L.t("Nagrywane są tylko rozmowy rozpoczęte przyciskiem ☎ w apce albo po „Nagraj następną rozmowę”. Nie działa przez słuchawki Bluetooth. Poinformuj rozmówcę, że rozmowa jest nagrywana."), 11f, R.color.pr_muted);
        h.setPadding(0, (int) Ui.dp(a, 8), 0, 0);
        s.addView(h);
    }
}
