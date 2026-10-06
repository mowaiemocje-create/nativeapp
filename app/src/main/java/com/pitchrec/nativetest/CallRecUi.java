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
    // ── KREATOR "SPRAWDŹ TELEFON": lista wszystkiego, czego potrzebuje nagrywanie rozmow,
    // ze stanem ✅/⚠️. Co sie da — apka ustawia sama (zgody: mikrofon, powiadomienia, bateria);
    // reszte otwiera dokladnie na wlasciwym ekranie ustawien (Android nie pozwala apce zmieniac
    // ustawien systemu bez Ciebie). Po powrocie do apki lista sama sie odswieza.
    private static java.lang.ref.WeakReference<LinearLayout> openBox = null;
    private static java.lang.ref.WeakReference<Activity> openAct = null;
    static final int REQ_PERMS = 7302;

    public static void setup(Activity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) Ui.dp(a, 18);
        box.setPadding(p, (int) Ui.dp(a, 8), p, 0);
        openBox = new java.lang.ref.WeakReference<>(box);
        openAct = new java.lang.ref.WeakReference<>(a);
        fillWizard(a, box);
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle("📞 " + L.t("Nagrywanie rozmów — sprawdź telefon"))
                .setView(sv)
                .setPositiveButton("OK", null)
                .show();
        dlg.setOnDismissListener(x -> { openBox = null; });
    }

    // Wolane z MainActivity.onResume / po zgodach — odswieza stany ✅/⚠️ w otwartym kreatorze
    public static void refreshOpenSetup() {
        LinearLayout box = openBox == null ? null : openBox.get();
        Activity a = openAct == null ? null : openAct.get();
        if (box != null && a != null && !a.isFinishing()) fillWizard(a, box);
    }

    static boolean has(Context c, String perm) {
        return androidx.core.content.ContextCompat.checkSelfPermission(c, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    static boolean notifOk(Context c) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return has(c, "android.permission.POST_NOTIFICATIONS");
    }

    private static void fillWizard(Activity a, LinearLayout box) {
        box.removeAllViews();
        boolean mic = has(a, android.Manifest.permission.RECORD_AUDIO);
        boolean notif = notifOk(a);
        boolean accOn = CallRecService.enabled(a);
        boolean acc = CallRecService.alive(a);          // wlaczona I naprawde dziala
        boolean phone = CallRecService.phoneStateOk(a);
        boolean bat = batteryFree(a);
        int ok = (mic ? 1 : 0) + (notif ? 1 : 0) + (phone ? 1 : 0) + (acc ? 1 : 0) + (bat ? 1 : 0);
        TextView sum = Ui.text(a, ok == 5 ? "🎉 " + L.t("Wszystko gotowe — rozmowy będą się nagrywać.") : L.f("Gotowe {0} z {1}. Dotknij przycisków poniżej.", ok, 5), 14f, R.color.pr_text);
        sum.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        if (ok == 5) sum.setTextColor(0xFF00E676);
        box.addView(sum);
        if (ok < 5) {
            Button all = Ui.button(a, "⚡ " + L.t("Ustaw automatycznie, co się da"), R.color.pr_accent, true);
            all.setOnClickListener(v -> autoFix(a));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) Ui.dp(a, 8);
            box.addView(all, lp);
        }
        box.addView(Ui.spacer(a, 8));
        row(a, box, mic, "🎤 " + L.t("Mikrofon"), L.t("Zgoda na nagrywanie dźwięku."), L.t("Zezwól"), () -> askPerms(a));
        row(a, box, notif, "🔔 " + L.t("Powiadomienia"), L.t("Żeby widzieć „Nagrywam rozmowę” i móc od razu opisać nagranie."), L.t("Zezwól"), () -> askPerms(a));
        row(a, box, phone, "📞 " + L.t("Wykrywanie rozmowy"), L.t("Zgoda „Telefon” — apka widzi tylko, że rozmowa trwa (bez numerów). Bez tego na części telefonów (np. Xiaomi) rozmowa nie zostanie wykryta."), L.t("Zezwól"), () -> askPerms(a));
        row(a, box, bat, "🔋 " + L.t("Bateria bez ograniczeń"), L.t("Bez tego telefon usypia apkę — wyłącza usługę albo urywa nagranie po wygaszeniu ekranu."), L.t("Ustaw"), () -> askBattery(a));
        row(a, box, acc, "♿ " + L.t("Usługa nagrywania rozmów"), accOn
                ? L.t("Usługa jest zaznaczona, ale telefon ją zatrzymał i NIE działa. Wyłącz ją i włącz ponownie w Ułatwieniach dostępu, a potem ustaw poniżej ustawienia producenta.")
                : L.t("Ułatwienia dostępu → Zainstalowane / Pobrane aplikacje → „New Speech — nagrywanie rozmów” → Włącz."), L.t("Otwórz"), () -> openAccessibility(a));
        if (!accOn && Build.VERSION.SDK_INT >= 33)
            hint(a, box, L.t("Przełącznik jest szary („Ustawienie ograniczone”)? Informacje o aplikacji → ⋮ w prawym górnym rogu → „Zezwól na ustawienia z ograniczeniami”, potem wróć tutaj."), L.t("Informacje o aplikacji"), () -> openAppInfo(a));
        // producent: dodatkowe usypianie aplikacji (nie da sie sprawdzic stanu — tylko otworzyc)
        Intent vendor = vendorIntent(a);
        if (vendor != null)
            hint(a, box, "📱 " + vendorText(), L.t("Otwórz"), () -> { try { a.startActivity(vendor); } catch (Exception e) { openAppInfo(a); } });
        if (isXiaomi()) {
            Intent miBat = miuiBatteryIntent(a);
            hint(a, box, "🔋 " + L.t("Xiaomi: Oszczędzanie baterii dla New Speech → „Bez ograniczeń” (to inne ustawienie niż bateria Androida powyżej)."), L.t("Otwórz"), () -> {
                try { if (miBat != null) a.startActivity(miBat); else openAppInfo(a); } catch (Exception e) { openAppInfo(a); } });
            hint(a, box, "🔒 " + L.t("Xiaomi: w ostatnich aplikacjach przytrzymaj New Speech i zablokuj kłódką — wtedy MIUI jej nie zamknie."), null, null);
        }
        hint(a, box, "💡 " + L.t("Wygaszanie ekranu przy uchu jest normalne — nagrywanie trwa dalej. Nie używaj słuchawek Bluetooth (nie nagrywają się)."), null, null);
    }

    private static void row(Activity a, LinearLayout box, boolean ok, String title, String sub, String btn, Runnable fix) {
        LinearLayout r = Ui.row(a);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pd = (int) Ui.dp(a, 8);
        r.setPadding(pd, pd, pd, pd);
        r.setBackground(Ui.rounded(ok ? 0x2200E676 : 0x22FFB300, ok ? 0x6600E676 : 0x66FFB300, Ui.dp(a, 1), Ui.dp(a, 10)));
        LinearLayout tb = new LinearLayout(a);
        tb.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(a, (ok ? "✅ " : "⚠️ ") + title, 14f, R.color.pr_text);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tb.addView(t);
        if (!ok) tb.addView(Ui.text(a, sub, 11f, R.color.pr_muted));
        r.addView(tb, Ui.weight(1f, Ui.dp(a, 6)));
        if (!ok) {
            Button b = Ui.button(a, btn, R.color.pr_accent, true);
            b.setOnClickListener(v -> fix.run());
            r.addView(b);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) Ui.dp(a, 6);
        box.addView(r, lp);
    }

    private static void hint(Activity a, LinearLayout box, String text, String btn, Runnable go) {
        TextView t = Ui.text(a, text, 11f, R.color.pr_muted);
        t.setPadding(0, (int) Ui.dp(a, 4), 0, (int) Ui.dp(a, 2));
        box.addView(t);
        if (btn != null) {
            Button b = Ui.button(a, btn, R.color.pr_muted, false);
            b.setOnClickListener(v -> go.run());
            box.addView(b);
        }
    }

    // Kolejno: zgody (mikrofon + powiadomienia jednym okienkiem) -> bateria -> ulatwienia dostepu
    static void autoFix(Activity a) {
        if (!has(a, android.Manifest.permission.RECORD_AUDIO) || !notifOk(a)) { askPerms(a); return; }
        if (!batteryFree(a)) { askBattery(a); return; }
        if (!CallRecService.phoneStateOk(a)) { askPerms(a); return; }
        if (!CallRecService.alive(a)) { openAccessibility(a); return; }
        refreshOpenSetup();
    }

    static void askPerms(Activity a) {
        java.util.List<String> need = new java.util.ArrayList<>();
        if (!has(a, android.Manifest.permission.RECORD_AUDIO)) need.add(android.Manifest.permission.RECORD_AUDIO);
        if (!notifOk(a)) need.add("android.permission.POST_NOTIFICATIONS");
        if (!CallRecService.phoneStateOk(a)) need.add("android.permission.READ_PHONE_STATE");
        if (need.isEmpty()) { refreshOpenSetup(); return; }
        androidx.core.app.ActivityCompat.requestPermissions(a, need.toArray(new String[0]), REQ_PERMS);
    }

    // Ekran usypiania aplikacji u producenta (Samsung, Xiaomi, Huawei, Oppo/Realme, Vivo)
    static Intent vendorIntent(Context c) {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(java.util.Locale.US);
        String[][] cand;
        if (m.contains("samsung")) cand = new String[][]{
                {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
                {"com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"}};
        else if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) cand = new String[][]{
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"}};
        else if (m.contains("huawei") || m.contains("honor")) cand = new String[][]{
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"}};
        else if (m.contains("oppo") || m.contains("realme") || m.contains("oneplus")) cand = new String[][]{
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"}};
        else if (m.contains("vivo")) cand = new String[][]{
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"}};
        else return null;
        for (String[] cn : cand) {
            Intent i = new Intent().setComponent(new android.content.ComponentName(cn[0], cn[1])).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { if (c.getPackageManager().resolveActivity(i, 0) != null) return i; } catch (Exception e) { }
        }
        return null;
    }

    static boolean isXiaomi() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(java.util.Locale.US);
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco");
    }

    // MIUI: osobne "Oszczedzanie baterii" dla aplikacji (Bez ograniczen)
    static Intent miuiBatteryIntent(Context c) {
        try {
            Intent i = new Intent().setComponent(new android.content.ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                    .putExtra("package_name", c.getPackageName()).putExtra("package_label", "New Speech").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (c.getPackageManager().resolveActivity(i, 0) != null) return i;
        } catch (Exception e) { }
        return null;
    }

    static String vendorText() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(java.util.Locale.US);
        if (m.contains("samsung")) return L.t("Samsung: Bateria → Limity użycia w tle → „Aplikacje nigdy nieusypiane” → dodaj New Speech.");
        if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) return L.t("Xiaomi: włącz „Autostart” dla New Speech.");
        if (m.contains("huawei") || m.contains("honor")) return L.t("Huawei: Uruchamianie aplikacji → New Speech → zarządzaj ręcznie (wszystko włączone).");
        return L.t("Zezwól apce New Speech na działanie w tle / autostart.");
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

    // zaznaczona w ustawieniach, ale system jej nie uruchomil (Xiaomi i in.) — sprawdzane w apce
    static boolean stoppedService(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(CallRecService.PREFS, Context.MODE_PRIVATE);
        return p.getBoolean("callrec_wanted", false) && CallRecService.enabled(c) && !CallRecService.alive(c);
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
        boolean stopped = stoppedService(a);
        if ((!lostService(a) && !stopped) || System.currentTimeMillis() - lastAsk < 10 * 60_000L) return;
        lastAsk = System.currentTimeMillis();
        new AlertDialog.Builder(a)
                .setTitle("⚠️ " + L.t("Nagrywanie rozmów wyłączone"))
                .setMessage(stopped
                        ? L.t("Usługa nagrywania rozmów jest zaznaczona, ale telefon ją zatrzymał — rozmowy się NIE nagrywają. Wyłącz ją i włącz ponownie w Ułatwieniach dostępu, a w kreatorze ustaw baterię i ustawienia producenta.")
                        : L.t("Telefon sam wyłączył usługę nagrywania rozmów. Włącz ją ponownie i ustaw baterię apki „Bez ograniczeń” — wtedy przestanie się wyłączać."))
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
        boolean alive = CallRecService.alive(a);
        TextView st = Ui.text(a, alive ? "✅ " + L.t("Usługa włączona — rozmowy z ☎ w apce nagrywają się same.")
                : on ? "⚠️ " + L.t("Usługa zaznaczona, ale zatrzymana przez telefon — rozmowy się NIE nagrywają. Otwórz kreator.")
                : "⚪ " + L.t("Usługa wyłączona — rozmowy się nie nagrywają."), 13f, R.color.pr_text);
        st.setTypeface(Typeface.DEFAULT_BOLD);
        if (alive) st.setTextColor(0xFF00E676);
        else if (on) { st.setTextColor(0xFFFFB300); st.setOnClickListener(v -> setup(a)); }
        s.addView(st);
        if (on && !batteryFree(a)) {
            TextView bw = Ui.text(a, "⚠️ " + L.t("Bateria apki ma ograniczenia — telefon może sam wyłączać nagrywanie. Dotknij, żeby zmienić."), 12f, R.color.pr_warn);
            bw.setPadding(0, (int) Ui.dp(a, 4), 0, 0);
            bw.setOnClickListener(v -> askBattery(a));
            s.addView(bw);
        }
        Button en = Ui.button(a, on ? "🔧 " + L.t("Sprawdź telefon (kreator ustawień)") : "📞 " + L.t("Włącz nagrywanie rozmów"), on ? R.color.pr_muted : R.color.pr_accent, !on);
        en.setOnClickListener(v -> setup(a));
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
        TextView bh = Ui.text(a, L.t("Głośność nagrania rozmowy:"), 12f, R.color.pr_muted);
        bh.setPadding(0, (int) Ui.dp(a, 10), 0, (int) Ui.dp(a, 4));
        s.addView(bh);
        String bc = p.getString("callrec_boost", "auto");
        String[][] bo = {{"auto", L.t("Auto")}, {"strong", L.t("Mocno")}, {"off", L.t("Bez wyrównania")}};
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
        TextView bi = Ui.text(a, L.t("Po rozłączeniu apka wyrównuje głośność: cichy rozmówca jest podgłaśniany, kompresja wyrównuje sylaby, a limiter nie dopuszcza do przesteru. Treść i brzmienie głosu się nie zmieniają."), 11f, R.color.pr_muted);
        bi.setPadding(0, (int) Ui.dp(a, 4), 0, 0);
        s.addView(bi);
        String post = p.getString("callrec_post", "");
        if (!post.isEmpty()) s.addView(Ui.text(a, "🔊 " + post, 11f, R.color.pr_muted));
        String last = p.getString("callrec_last", "");
        if (!last.isEmpty()) {
            TextView lt = Ui.text(a, L.t("Ostatnie nagranie rozmowy:") + " " + last, 11f, R.color.pr_muted);
            lt.setPadding(0, (int) Ui.dp(a, 8), 0, 0);
            s.addView(lt);
        }
        // diagnostyka (do zgloszenia problemu): czy usluga dziala, ostatni stan telefonu, czym wykryto rozmowe
        String diag = L.t("Diagnostyka") + ": " + (alive ? "usługa ✓" : on ? "usługa zatrzymana" : "usługa wył.")
                + " · tel: " + (CallRecService.phoneStateOk(a) ? p.getString("callrec_state", "—") : L.t("brak zgody"))
                + (p.getString("callrec_start", "").isEmpty() ? "" : " · start: " + p.getString("callrec_start", ""))
                + " · " + Build.MANUFACTURER + " " + Build.MODEL + " A" + Build.VERSION.SDK_INT;
        TextView dg = Ui.text(a, diag, 10f, R.color.pr_muted);
        dg.setPadding(0, (int) Ui.dp(a, 6), 0, 0);
        s.addView(dg);
        String err = p.getString("callrec_last_err", "");
        if (!err.isEmpty()) s.addView(Ui.text(a, "⚠️ " + err, 11f, R.color.pr_warn));
        TextView h = Ui.text(a, all
                ? L.t("Nagrywana jest każda rozmowa. Po rozłączeniu wybierasz: zapisz jako ćwiczenie albo usuń. Rozmowy z ☎ w apce od razu mają wybraną osobę. Nie działa przez słuchawki Bluetooth.")
                : L.t("Nagrywane są tylko rozmowy rozpoczęte przyciskiem ☎ w apce albo po „Nagraj następną rozmowę”. Nie działa przez słuchawki Bluetooth. Poinformuj rozmówcę, że rozmowa jest nagrywana."), 11f, R.color.pr_muted);
        h.setPadding(0, (int) Ui.dp(a, 8), 0, 0);
        s.addView(h);
    }
}
