package com.pitchrec.nativetest;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// TLUMACZENIE nazw z oceny trenera (kryteria typu "Tempo", "Rezonans" i poziomy typu "słabo",
// "losowo"). Szybka lista "do poprawy" z serwera podaje je tylko po polsku, wiec:
//  1) uczymy sie tlumaczen z pelnych rekordow NS (tam sa name_en / name_cz) i zapamietujemy je,
//  2) dla najczestszych nazw jest wbudowany slownik (EN, CS, SK, DE, ES, HU).
public final class RateNames {

    private static final Map<String, String[]> LEARNED = new HashMap<>(); // pl(lower) -> {en, cz}
    private static SharedPreferences sp;

    // pl -> {en, cs, sk, de, es, hu}
    private static final Map<String, String[]> DICT = new HashMap<>();
    private static void d(String pl, String en, String cs, String sk, String de, String es, String hu) {
        DICT.put(pl.toLowerCase(Locale.ROOT), new String[]{en, cs, sk, de, es, hu});
    }
    static {
        // kryteria
        d("Tempo", "Tempo", "Tempo", "Tempo", "Tempo", "Ritmo", "Tempó");
        d("Rezonans", "Resonance", "Rezonance", "Rezonancia", "Resonanz", "Resonancia", "Rezonancia");
        d("Intonacja", "Intonation", "Intonace", "Intonácia", "Intonation", "Entonación", "Intonáció");
        d("Melodia", "Melody", "Melodie", "Melódia", "Melodie", "Melodía", "Dallam");
        d("Pauzy", "Pauses", "Pauzy", "Pauzy", "Pausen", "Pausas", "Szünetek");
        d("Pauza", "Pause", "Pauza", "Pauza", "Pause", "Pausa", "Szünet");
        d("Oddech", "Breathing", "Dech", "Dych", "Atmung", "Respiración", "Légzés");
        d("Artykulacja", "Articulation", "Artikulace", "Artikulácia", "Artikulation", "Articulación", "Artikuláció");
        d("Emisja", "Voice emission", "Emise", "Emisia", "Stimmgebung", "Emisión", "Hangképzés");
        d("Emisja głosu", "Voice emission", "Emise hlasu", "Emisia hlasu", "Stimmgebung", "Emisión de voz", "Hangképzés");
        d("Płynność", "Fluency", "Plynulost", "Plynulosť", "Flüssigkeit", "Fluidez", "Folyékonyság");
        d("Głośność", "Volume", "Hlasitost", "Hlasitosť", "Lautstärke", "Volumen", "Hangerő");
        d("Akcent", "Stress", "Přízvuk", "Prízvuk", "Betonung", "Acento", "Hangsúly");
        d("Luz", "Relaxation", "Uvolnění", "Uvoľnenie", "Lockerheit", "Relajación", "Lazaság");
        d("Napięcie", "Tension", "Napětí", "Napätie", "Spannung", "Tensión", "Feszültség");
        d("Postawa", "Posture", "Postoj", "Postoj", "Haltung", "Postura", "Testtartás");
        d("Kontakt wzrokowy", "Eye contact", "Oční kontakt", "Očný kontakt", "Blickkontakt", "Contacto visual", "Szemkontaktus");
        d("Mimika", "Facial expression", "Mimika", "Mimika", "Mimik", "Expresión facial", "Mimika");
        d("Sylaba 4-fazowa", "4-phase syllable", "4fázová slabika", "4-fázová slabika", "4-Phasen-Silbe", "Sílaba de 4 fases", "4 fázisú szótag");
        d("Faza leniwa", "Lazy phase", "Líná fáze", "Lenivá fáza", "Träge Phase", "Fase perezosa", "Lusta fázis");
        d("Technika", "Technique", "Technika", "Technika", "Technik", "Técnica", "Technika");
        d("Jąkanie", "Stuttering", "Koktání", "Zajakávanie", "Stottern", "Tartamudeo", "Dadogás");
        d("Blok", "Block", "Blok", "Blok", "Block", "Bloqueo", "Blokk");
        d("Kamuflaż", "Camouflage", "Kamufláž", "Kamufláž", "Tarnung", "Camuflaje", "Álcázás");
        d("Ogólne wrażenie", "Overall impression", "Celkový dojem", "Celkový dojem", "Gesamteindruck", "Impresión general", "Összbenyomás");
        // poziomy
        d("słabo", "weak", "slabě", "slabo", "schwach", "débil", "gyenge");
        d("losowo", "random", "nahodile", "náhodne", "zufällig", "aleatorio", "véletlenszerű");
        d("dobrze", "good", "dobře", "dobre", "gut", "bien", "jó");
        d("bardzo dobrze", "very good", "velmi dobře", "veľmi dobre", "sehr gut", "muy bien", "nagyon jó");
        d("średnio", "average", "průměrně", "priemerne", "mittel", "regular", "közepes");
        d("dostatecznie", "sufficient", "dostatečně", "dostatočne", "ausreichend", "suficiente", "elégséges");
        d("niedostatecznie", "insufficient", "nedostatečně", "nedostatočne", "ungenügend", "insuficiente", "elégtelen");
        d("brak", "none", "žádné", "žiadne", "keine", "ninguno", "nincs");
        d("zawsze", "always", "vždy", "vždy", "immer", "siempre", "mindig");
        d("często", "often", "často", "často", "oft", "a menudo", "gyakran");
        d("czasami", "sometimes", "občas", "občas", "manchmal", "a veces", "néha");
        d("rzadko", "rarely", "zřídka", "zriedka", "selten", "raramente", "ritkán");
        d("nigdy", "never", "nikdy", "nikdy", "nie", "nunca", "soha");
        d("częściowo", "partly", "částečně", "čiastočne", "teilweise", "parcialmente", "részben");
        d("poprawnie", "correct", "správně", "správne", "korrekt", "correcto", "helyes");
        d("niepoprawnie", "incorrect", "nesprávně", "nesprávne", "falsch", "incorrecto", "helytelen");
        d("za szybko", "too fast", "příliš rychle", "príliš rýchlo", "zu schnell", "demasiado rápido", "túl gyors");
        d("za wolno", "too slow", "příliš pomalu", "príliš pomaly", "zu langsam", "demasiado lento", "túl lassú");
        d("za głośno", "too loud", "příliš hlasitě", "príliš hlasno", "zu laut", "demasiado alto", "túl hangos");
        d("za cicho", "too quiet", "příliš potichu", "príliš potichu", "zu leise", "demasiado bajo", "túl halk");
        d("za krótko", "too short", "příliš krátce", "príliš krátko", "zu kurz", "demasiado corto", "túl rövid");
        d("za długo", "too long", "příliš dlouho", "príliš dlho", "zu lang", "demasiado largo", "túl hosszú");
        d("nierówno", "uneven", "nerovnoměrně", "nerovnomerne", "ungleichmäßig", "irregular", "egyenetlen");
        d("równo", "even", "rovnoměrně", "rovnomerne", "gleichmäßig", "regular", "egyenletes");
    }

    private RateNames() { }

    public static synchronized void init(Context c) {
        if (sp != null) return;
        sp = c.getApplicationContext().getSharedPreferences("rate_names", Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            String v = String.valueOf(e.getValue());
            int k = v.indexOf('\u0001');
            if (k >= 0) LEARNED.put(e.getKey(), new String[]{v.substring(0, k), v.substring(k + 1)});
        }
    }

    // obiekt z NS z polami name / name_pl / name_en / name_cz
    public static synchronized void learn(Context c, JSONObject o) {
        if (o == null) return;
        init(c);
        String pl = o.optString("name_pl", "");
        if (pl.isEmpty() || "null".equals(pl)) pl = o.optString("name", "");
        if (pl.isEmpty() || "null".equals(pl)) return;
        String en = clean(o.optString("name_en", "")), cz = clean(o.optString("name_cz", ""));
        if (en.isEmpty() && cz.isEmpty()) return;
        String key = pl.trim().toLowerCase(Locale.ROOT);
        String[] old = LEARNED.get(key);
        if (old != null && old[0].equals(en) && old[1].equals(cz)) return;
        LEARNED.put(key, new String[]{en, cz});
        sp.edit().putString(key, en + '\u0001' + cz).apply();
    }

    private static String clean(String s) { return s == null || "null".equals(s) ? "" : s.trim(); }

    // Nazwa po polsku -> w jezyku aplikacji (gdy nie ma tlumaczenia — bez zmian)
    public static synchronized String tr(String pl) {
        if (pl == null) return "";
        String s = pl.trim();
        String l = L.lang();
        if (s.isEmpty() || "pl".equals(l)) return s;
        String key = s.toLowerCase(Locale.ROOT);
        String[] le = LEARNED.get(key);
        if (le != null) {
            String v = "cs".equals(l) || "sk".equals(l) ? le[1] : le[0];
            if (!v.isEmpty() && ("en".equals(l) || "cs".equals(l) || "sk".equals(l) || DICT.get(key) == null)) return v;
        }
        String[] dv = DICT.get(key);
        if (dv != null) {
            int i = "en".equals(l) ? 0 : "cs".equals(l) ? 1 : "sk".equals(l) ? 2 : "de".equals(l) ? 3 : "es".equals(l) ? 4 : "hu".equals(l) ? 5 : 0;
            String v = dv[i];
            return Character.isUpperCase(s.charAt(0)) ? Character.toUpperCase(v.charAt(0)) + v.substring(1) : v;
        }
        return le != null && !le[0].isEmpty() ? le[0] : s;
    }
}
