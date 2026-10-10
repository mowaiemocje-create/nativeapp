package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

// DOM KURSANTA — zapamietywany SAM (bez przycisku): srednia pozycji z nagran "domowych"
// (Monolog / Czytanie, Rodzina, Zerowka). Nagranie "terenowe" (Sklepy, Przechodzien, Miasto…)
// zrobione blizej niz 250 m od domu dostaje podpowiedz, zeby wyjsc dalej do miasta.
// Tak samo jak w PWA. Pozycja domu zostaje tylko w telefonie.
public final class HomeGps {

    private HomeGps() { }

    static final int RADIUS_M = 250;
    private static final String[] HOME = {"Monolog / Czytanie", "Rodzina", "Zerówka"};
    private static final String[] OUTDOOR = {"Sklepy", "Przechodzień", "Miasto - inne", "Restauracja Kelner", "McDrive, DriveThru", "Telefon do miasta"};

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    private static boolean in(String[] a, String cat) { for (String s : a) if (s.equals(cat)) return true; return false; }

    // Zwraca odleglosc od domu w metrach, gdy nagranie terenowe jest "z domu"; inaczej -1.
    public static int onRecording(Context c, String cat, double lat, double lon) {
        if (cat == null || Double.isNaN(lat) || Double.isNaN(lon)) return -1;
        SharedPreferences p = prefs(c);
        if (in(HOME, cat)) {
            int n = Math.min(p.getInt("home_n", 0), 10);
            double hl = n == 0 ? lat : (Double.longBitsToDouble(p.getLong("home_lat", 0L)) * n + lat) / (n + 1);
            double ho = n == 0 ? lon : (Double.longBitsToDouble(p.getLong("home_lon", 0L)) * n + lon) / (n + 1);
            p.edit().putLong("home_lat", Double.doubleToLongBits(hl)).putLong("home_lon", Double.doubleToLongBits(ho)).putInt("home_n", n + 1).apply();
            return -1;
        }
        if (!in(OUTDOOR, cat) || p.getInt("home_n", 0) == 0) return -1;
        double hl = Double.longBitsToDouble(p.getLong("home_lat", 0L)), ho = Double.longBitsToDouble(p.getLong("home_lon", 0L));
        double dLat = (lat - hl) * 111000, dLon = (lon - ho) * 111000 * Math.cos(Math.toRadians(lat));
        int dist = (int) Math.round(Math.sqrt(dLat * dLat + dLon * dLon));
        return dist < RADIUS_M ? dist : -1;
    }
}
