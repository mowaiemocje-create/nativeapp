package com.pitchrec.nativetest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

// GPS jak w PitchRec: pozycja pobierana na STARCIE nagrywania (recordingGPS) i
// zapamietywana jako ostatnia znana (lastGPS). Bez Google Play Services — zwykly
// LocationManager (GPS + siec), dziala na kazdym Androidzie.
public class GpsHelper {

    public static volatile Location lastFix = null;       // ostatnia znana pozycja
    public static volatile Location recordingFix = null;  // pozycja z czasu nagrywania

    public interface Listener { void onFix(Location loc); }

    // Pozycja przybliżona (z adresu IP — tablet bez GPS, gdy Wi-Fi/sieć nie podały pozycji)
    public static boolean isApprox(Location l) { return l != null && "ip".equals(l.getProvider()); }

    // Wszyscy dostawcy: GPS, sieć (Wi-Fi / maszty), „fused” (Android 12+, Google), pasywny
    private static String[] providers(LocationManager lm, boolean enabledOnly) {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
        s.add(LocationManager.GPS_PROVIDER);
        s.add(LocationManager.NETWORK_PROVIDER);
        if (android.os.Build.VERSION.SDK_INT >= 31) s.add(LocationManager.FUSED_PROVIDER);
        try { s.addAll(enabledOnly ? lm.getProviders(true) : lm.getAllProviders()); } catch (Exception e) { }
        if (enabledOnly) s.remove(LocationManager.PASSIVE_PROVIDER); else s.add(LocationManager.PASSIVE_PROVIDER);
        return s.toArray(new String[0]);
    }

    // Czy urządzenie ma w ogóle GPS (np. Galaxy Tab bez LTE nie ma)
    public static boolean hasGpsHardware(Context c) {
        try { return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS); } catch (Exception e) { return true; }
    }

    // Ostatnia deska ratunku: przybliżona pozycja z adresu internetowego (worker /geo, Cloudflare) — zwykle miasto
    private static void ipFix(Listener l) {
        new Thread(() -> {
            Location out = null;
            try {
                java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL("https://newspeech-backend.mowaiemocje.workers.dev/geo").openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                org.json.JSONObject o = new org.json.JSONObject(sb.toString());
                if (o.optBoolean("ok", false)) {
                    out = new Location("ip");
                    out.setLatitude(o.optDouble("lat", 0));
                    out.setLongitude(o.optDouble("lon", 0));
                    out.setAccuracy(5000f);
                    out.setTime(System.currentTimeMillis());
                }
            } catch (Exception e) { }
            final Location fo = out;
            new Handler(Looper.getMainLooper()).post(() -> { if (fo != null && lastFix == null) lastFix = fo; if (l != null) l.onFix(fo); });
        }, "ip-geo").start();
    }

    public static boolean hasPermission(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Jednorazowe pobranie pozycji (maks. ~15 s). Zwraca od razu ostatnia znana, jesli
    // jest swieza (< 2 min), a i tak probuje dostac dokladniejsza.
    public static void requestFix(Context c, Listener l) {
        if (!hasPermission(c)) { if (l != null) l.onFix(null); return; }
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { if (l != null) l.onFix(null); return; }
        final boolean[] delivered = {false};
        Handler h = new Handler(Looper.getMainLooper());
        try {
            Location best = null;
            for (String p : providers(lm, false)) {
                try {
                    Location loc = lm.getLastKnownLocation(p);
                    if (loc != null && (best == null || loc.getTime() > best.getTime())) best = loc;
                } catch (SecurityException e) { /* brak zgody */ }
            }
            if (best != null && System.currentTimeMillis() - best.getTime() < 120000) {
                lastFix = best;
                delivered[0] = true;
                if (l != null) l.onFix(best);
            }
            LocationListener ll = new LocationListener() {
                @Override public void onLocationChanged(Location loc) {
                    lastFix = loc;
                    try { lm.removeUpdates(this); } catch (Exception e) { }
                    if (!delivered[0]) { delivered[0] = true; if (l != null) l.onFix(loc); }
                }
                @Override public void onStatusChanged(String p, int s, Bundle b) { }
                @Override public void onProviderEnabled(String p) { }
                @Override public void onProviderDisabled(String p) { }
            };
            boolean any = false;
            for (String p : providers(lm, true)) {
                try {
                    if (lm.isProviderEnabled(p)) { lm.requestLocationUpdates(p, 0L, 0f, ll, Looper.getMainLooper()); any = true; }
                } catch (Exception e) { /* dostawca niedostepny */ }
            }
            final Location fallback = best;
            h.postDelayed(() -> {
                try { lm.removeUpdates(ll); } catch (Exception e) { }
                if (delivered[0]) return;
                delivered[0] = true;
                if (fallback != null) { if (l != null) l.onFix(fallback); }
                else ipFix(l); // nic z GPS / Wi-Fi / sieci — przybliżona pozycja z adresu internetowego
            }, any ? 15000 : 10);
        } catch (Exception e) {
            if (!delivered[0] && l != null) { delivered[0] = true; l.onFix(null); }
        }
    }
}
