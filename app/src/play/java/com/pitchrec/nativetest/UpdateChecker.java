package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.Context;

// WERSJA GOOGLE PLAY: aplikacje aktualizuje Sklep Play — bez samoaktualizacji (Google nie
// pozwala aplikacjom ze sklepu instalowac APK z innych zrodel). Zaslepka z tymi samymi
// metodami co w wersji kursantow (app/src/students/...).
public final class UpdateChecker {

    private UpdateChecker() { }

    public static boolean enabled(Context c) { return false; }

    public static long currentCode(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) { return 0L; }
    }

    public static String currentName(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
    }

    public static void check(Activity a, boolean manual) { }
}
