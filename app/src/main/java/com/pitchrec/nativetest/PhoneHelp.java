package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

// Wspolne pomocniki telefonu (obie wersje: kursanci i Google Play): zgody, powiadomienia,
// bateria, ustawienia producenta. Wczesniej byly w CallRecUi (nagrywanie rozmow), ale
// potrzebuje ich tez wersja Play, w ktorej nagrywania rozmow nie ma.
public final class PhoneHelp {

    private PhoneHelp() { }

    static boolean has(Context c, String perm) {
        return androidx.core.content.ContextCompat.checkSelfPermission(c, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // Zgoda (Android 13+) ORAZ powiadomienia niezablokowane w ustawieniach telefonu (np. MIUI
    // potrafi je wylaczyc aplikacjom spoza sklepu)
    static boolean notifOk(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && !has(c, "android.permission.POST_NOTIFICATIONS")) return false;
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.areNotificationsEnabled()) return false;
        } catch (Exception e) { }
        return true;
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

    static boolean batteryFree(Context c) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
        } catch (Exception e) { return false; }
    }

    // Kursanci: systemowe okienko "Zezwolić na dzialanie w tle bez ograniczen?".
    // Play: Google ogranicza zgode REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, wiec otwieramy
    // liste "Optymalizacja baterii" (nie wymaga zadnej zgody), a gdy niedostepna — Informacje o aplikacji.
    static void askBattery(Activity a) {
        if (!batteryFree(a)) {
            try {
                if (BuildConfig.PLAY)
                    a.startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                else
                    a.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + a.getPackageName())));
                return;
            } catch (Exception e) { }
        }
        openAppInfo(a);
    }

    static void openAppInfo(Activity a) {
        try {
            a.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName())));
        } catch (Exception e) { Toast.makeText(a, L.t("Nie udało się otworzyć ustawień"), Toast.LENGTH_SHORT).show(); }
    }
}
