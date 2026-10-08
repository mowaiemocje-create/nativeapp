package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;

// WERSJA GOOGLE PLAY: bez nagrywania rozmow — zaslepka z tymi samymi metodami co w wersji
// kursantow (app/src/students/...). Przycisk ☎ po prostu otwiera telefon z numerem.
public final class CallRecUi {

    static final int REQ_PERMS = 7302;

    private CallRecUi() { }

    public static void dial(Activity a, String phone, String label) { ContactList.dial(a, phone); }
    public static void armNext(Activity a) { }
    public static void setup(Activity a) { }
    public static void refreshOpenSetup() { }
    public static void watch(Context c) { }
    public static void checkOnResume(Activity a) { }
    public static void addArmRow(Activity a, LinearLayout card, Runnable refresh) { }
    public static void fillSettings(Activity a, LinearLayout s, Runnable rerender) { }
}
