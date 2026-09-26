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
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
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
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try {
                    if (lm.isProviderEnabled(p)) { lm.requestLocationUpdates(p, 0L, 0f, ll, Looper.getMainLooper()); any = true; }
                } catch (Exception e) { /* dostawca niedostepny */ }
            }
            final Location fallback = best;
            h.postDelayed(() -> {
                try { lm.removeUpdates(ll); } catch (Exception e) { }
                if (!delivered[0]) { delivered[0] = true; if (l != null) l.onFix(fallback); }
            }, any ? 15000 : 10);
        } catch (Exception e) {
            if (!delivered[0] && l != null) { delivered[0] = true; l.onFix(null); }
        }
    }
}
