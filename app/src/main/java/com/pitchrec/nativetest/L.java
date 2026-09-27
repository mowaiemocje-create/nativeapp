package com.pitchrec.nativetest;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Tlumaczenia interfejsu (PL, EN, CS, SK, DE, ES).
// Kluczem jest polski tekst — L.t("Zapisz") zwraca tekst w jezyku wybranym w Ustawieniach.
// Tabela: assets/i18n.tsv (kolumny: pl, en, cs, sk, de, es). Emoji / symbole na poczatku i
// spacje na koncach sa zachowywane automatycznie ("⏳ Wczytywanie…" -> "⏳ Loading…").
// Brak tlumaczenia -> SK bierze CS, reszta EN, a na koncu oryginalny polski tekst.
public final class L {

    private static Context app;
    private static final Map<String, String[]> TABLE = new HashMap<>();
    private static boolean loaded = false;

    private L() { }

    public static void init(Context c) {
        if (c == null) return;
        app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        load();
    }

    private static synchronized void load() {
        if (loaded || app == null) return;
        loaded = true;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(app.getAssets().open("i18n.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\t", -1);
                if (p.length < 2) continue;
                String[] v = new String[6];
                for (int i = 0; i < 6; i++) v[i] = i < p.length ? p[i].replace("\\n", "\n") : "";
                TABLE.put(v[0].trim(), v);
            }
        } catch (Exception e) { /* brak tabeli -> polski */ }
    }

    public static String lang() {
        if (app == null) return "pl";
        return app.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getString("language", "en");
    }

    private static int col(String l) {
        switch (l) {
            case "en": return 1;
            case "cs": return 2;
            case "sk": return 3;
            case "de": return 4;
            case "es": return 5;
            default: return 0;
        }
    }

    private static String lookup(String core, int c) {
        String[] v = TABLE.get(core);
        if (v == null) return null;
        if (!v[c].isEmpty()) return v[c];
        if (c == 3 && !v[2].isEmpty()) return v[2];
        if (!v[1].isEmpty()) return v[1];
        return null;
    }

    // Tlumaczy tekst (klucz = polski). Zachowuje prefiks z emoji/symboli i spacje na brzegach.
    public static String t(String pl) {
        if (pl == null || pl.isEmpty()) return pl;
        int c = col(lang());
        if (c == 0) return pl;
        if (!loaded) load();
        String exact = lookup(pl.trim(), c);
        if (exact != null) return keepEdges(pl, pl.trim(), exact);
        // prefiks z emoji / znakow nie-literowych (np. "⏳ ", "☁✓ ", "• ")
        int i = 0;
        while (i < pl.length()) {
            int cp = pl.codePointAt(i);
            if (Character.isLetter(cp)) break;
            i += Character.charCount(cp);
        }
        if (i > 0 && i < pl.length()) {
            String core = pl.substring(i).trim();
            String tr = lookup(core, c);
            if (tr != null) return pl.substring(0, i) + keepEdges(pl.substring(i), core, tr);
        }
        return pl;
    }

    private static String keepEdges(String orig, String core, String tr) {
        int s = orig.indexOf(core);
        if (s < 0) return tr;
        return orig.substring(0, s) + tr + orig.substring(s + core.length());
    }

    // Tlumaczy i wstawia wartosci w miejsca {0}, {1}, …
    public static String f(String pl, Object... args) {
        String s = t(pl);
        for (int i = 0; i < args.length; i++) s = s.replace("{" + i + "}", String.valueOf(args[i]));
        return s;
    }

    // Nazwa kategorii nagrania do WYSWIETLENIA (wartosc wysylana do NS zostaje po polsku)
    public static String cat(String name) {
        return t(name);
    }
}
