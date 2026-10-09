package com.pitchrec.nativetest;

import android.content.Context;

// Wersja Google Play: bez powiadomien push (na razie) — puste wywolania.
public final class Push {
    private Push() { }
    public static void register(Context c) { }
    public static void unregister(Context c) { }
}
