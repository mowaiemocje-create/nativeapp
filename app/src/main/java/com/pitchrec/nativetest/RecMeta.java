package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

// Opis nagrania (jak rekord w IndexedDB w PitchRec): kategoria, system, emocje, notatka,
// imie kursanta, GPS, status wysylki do NS. Trzymany w SharedPreferences, kluczem jest
// nazwa pliku (przy zmianie nazwy opis jest przenoszony).
public class RecMeta {

    public String cat = "";     // kategoria (pusta = "bez opisu")
    public String sys = "";     // System Nowej Mowy (Basic/U1/...)
    public int emotion = 0;     // 1-5 gwiazdek, 0 = brak
    public String note = "";
    public String name = "";    // imie kursanta
    public double lat = Double.NaN, lon = Double.NaN;
    public String ns = "";      // "" | "sent" | "error"
    public String nsRecordId = "";

    public static final String[] EMOTION_LABELS = {"", "luz", "komfortowo", "neutralnie", "niekomfortowo", "stres"};
    public static final String[] SYS_LEVELS = {"Basic", "U1", "U1K", "FIX", "K1", "K2", "Full"};

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("rec_meta", Context.MODE_PRIVATE);
    }

    public boolean hasGps() { return !Double.isNaN(lat) && !Double.isNaN(lon); }

    public static RecMeta load(Context c, String fileName) {
        RecMeta m = new RecMeta();
        String js = sp(c).getString(fileName, null);
        if (js != null) {
            try {
                JSONObject o = new JSONObject(js);
                m.cat = o.optString("cat", "");
                m.sys = o.optString("sys", "");
                m.emotion = o.optInt("emo", 0);
                m.note = o.optString("note", "");
                m.name = o.optString("name", "");
                m.lat = o.optDouble("lat", Double.NaN);
                m.lon = o.optDouble("lon", Double.NaN);
                m.ns = o.optString("ns", "");
                m.nsRecordId = o.optString("nsId", "");
            } catch (Exception e) { /* uszkodzony wpis — traktujemy jak brak opisu */ }
        }
        // Zgodnosc: nagrania wyslane przed wprowadzeniem opisow (lista "ns_sent_files")
        if (m.ns.isEmpty()) {
            java.util.Set<String> old = c.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .getStringSet("ns_sent_files", null);
            if (old != null && old.contains(fileName)) m.ns = "sent";
        }
        return m;
    }

    public void save(Context c, String fileName) {
        try {
            JSONObject o = new JSONObject();
            o.put("cat", cat);
            o.put("sys", sys);
            o.put("emo", emotion);
            o.put("note", note);
            o.put("name", name);
            if (hasGps()) { o.put("lat", lat); o.put("lon", lon); }
            o.put("ns", ns);
            o.put("nsId", nsRecordId);
            sp(c).edit().putString(fileName, o.toString()).apply();
        } catch (Exception e) { /* ignorowane */ }
    }

    public static void delete(Context c, String fileName) {
        sp(c).edit().remove(fileName).apply();
    }

    // Nazwa pliku DOKLADNIE jak w PitchRec:
    // Kategoria_26-wrzesien-2026-(21h-3m-5s)-U1-komfortowo_GPS52.12345_21.00012.wav
    private static final String[] MONTHS = {"styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
            "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień"};

    public String buildFileName(long timeMs, String ext) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMs);
        String catPart = (cat != null && !cat.isEmpty()) ? cat : "bez-kategorii";
        catPart = catPart.replaceAll("[^a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\-]", "-");
        if (catPart.length() > 20) catPart = catPart.substring(0, 20);
        String datePart = cal.get(Calendar.DAY_OF_MONTH) + "-" + MONTHS[cal.get(Calendar.MONTH)] + "-" + cal.get(Calendar.YEAR)
                + "-(" + cal.get(Calendar.HOUR_OF_DAY) + "h-" + cal.get(Calendar.MINUTE) + "m-" + cal.get(Calendar.SECOND) + "s)";
        String sysWord = sys == null ? "" : sys.replaceAll("[^a-zA-Z0-9]", "");
        String emoWord = (emotion > 0 && emotion < EMOTION_LABELS.length) ? EMOTION_LABELS[emotion] : "";
        String gpsWord = hasGps() ? String.format(Locale.US, "_GPS%.5f_%.5f", lat, lon) : "";
        return catPart + "_" + datePart + (sysWord.isEmpty() ? "" : "-" + sysWord)
                + (emoWord.isEmpty() ? "" : "-" + emoWord) + gpsWord + ext;
    }

    // Zmienia nazwe pliku na nowa (wg opisu) i przenosi opis. Zwraca nowy plik (albo stary,
    // jesli zmiana nazwy sie nie udala).
    public static File renameWithMeta(Context c, File f, RecMeta m) {
        String ext = f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") ? ".mp3" : ".wav";
        String base = m.buildFileName(f.lastModified(), "");
        File dest = new File(f.getParentFile(), base + ext);
        int n = 1;
        while (dest.exists() && !dest.equals(f)) {
            dest = new File(f.getParentFile(), base + "_" + n + ext);
            n++;
        }
        if (!dest.equals(f) && f.renameTo(dest)) {
            delete(c, f.getName());
            m.save(c, dest.getName());
            return dest;
        }
        m.save(c, f.getName());
        return f;
    }
}
