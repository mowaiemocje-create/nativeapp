package com.pitchrec.nativetest;

import android.content.Context;

// POZIOMY DOSTEPU (wersja Google Play):
//  • PODSTAWOWA (za darmo): DAW, wykres wysokosci glosu, zapis nagran do plikow.
//  • PELNA (jednorazowy zakup w Google Play): + kategorie, gwiazdki, statystyki, odznaki,
//    zadania specjalne — wszystko liczone na telefonie, bez trenera.
//  • ZALOGOWANY (kursant Nowej Mowy, login i haslo NewSpeech): wszystko, takze normy, strzalki
//    intonacji, trener, dziennik, korekta, mapa — bez kupowania.
// Wersja kursantow (APK) zawsze ma pelna wersje — tam nic sie nie zmienia.
public final class Access {

    private Access() { }

    public static boolean loggedIn(Context c) {
        String t = c.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getString("ns_token", null);
        return t != null && !t.trim().isEmpty();
    }

    // Pelna wersja: kategorie, gwiazdki, statystyki, odznaki, zadania specjalne
    public static boolean full(Context c) {
        return !BuildConfig.PLAY || loggedIn(c) || Premium.owned(c);
    }

    // Tryb "sam na telefonie": wersja Play z pelna wersja, ale bez konta NewSpeech —
    // statystyki i zadania specjalne liczone z nagran na telefonie (bez trenera)
    public static boolean localMode(Context c) {
        return BuildConfig.PLAY && !loggedIn(c);
    }
}
