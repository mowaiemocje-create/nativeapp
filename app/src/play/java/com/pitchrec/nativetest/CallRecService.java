package com.pitchrec.nativetest;

import android.content.Context;

// WERSJA GOOGLE PLAY: bez nagrywania rozmow (Google nie pozwala nagrywac rozmow przez
// Ulatwienia dostepu). Ta klasa to tylko zaslepka z tymi samymi metodami co w wersji
// kursantow (app/src/students/...), zeby wspolny kod dzialal bez zmian. To NIE jest usluga
// i nie ma jej w manifescie.
public final class CallRecService {

    static final String PREFS = "app_settings";

    private CallRecService() { }

    public static boolean enabled(Context c) { return false; }
    public static boolean allOn(Context c) { return false; }
    public static boolean autoOn(Context c) { return false; }
    public static void arm(Context c, String label, int minutes) { }
    public static void disarm(Context c) { }
    public static boolean armed(Context c) { return false; }
    public static boolean alive(Context c) { return false; }
    public static boolean isRecording() { return false; }
}
