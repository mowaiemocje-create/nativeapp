package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.Context;

// WERSJA KURSANTOW: nie ma zakupow — pelna wersja jest zawsze. Zaslepka z tymi samymi
// metodami co w wersji Google Play (app/src/play/.../Premium.java).
public final class Premium {

    private Premium() { }

    public static boolean owned(Context c) { return true; }
    public static String price(Context c) { return ""; }
    public static void refresh(Activity a, Runnable onChange) { }
    public static void buy(Activity a, Runnable onChange) { }
    public static void restore(Activity a, Runnable onChange) { }
}
