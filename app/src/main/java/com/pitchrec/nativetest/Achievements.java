package com.pitchrec.nativetest;

// Pelna lista osiagniec z PitchRec (40 odznak) — progi, ikony i nazwy w 6 jezykach
// (PL, EN, CS, SK, DE, ES). Wygenerowane z listy MILESTONES w PWA.
public final class Achievements {
    public static final class A {
        public final String id, icon, msgPl;
        public final int count;
        public final String[] cats;   // null = wszystkie nagrania lacznie
        public final boolean special; // szczegolnie nagradzane (Wystapienia)
        private final String[] labels; // pl, en, cs, sk, de, es, hu
        A(String id, String icon, int count, String[] cats, boolean special, String[] labels, String msgPl) {
            this.id = id; this.icon = icon; this.count = count; this.cats = cats; this.special = special; this.labels = labels; this.msgPl = msgPl;
        }
        public String label() {
            String l = L.lang();
            int i = "en".equals(l) ? 1 : "cs".equals(l) ? 2 : "sk".equals(l) ? 3 : "de".equals(l) ? 4 : "es".equals(l) ? 5 : "hu".equals(l) ? 6 : 0;
            return labels[i];
        }
    }

    public static final A[] ALL = {
        new A("total10", "🌱", 10, null, false, new String[]{"Pierwsze 10 nagrań!","First 10 recordings!","Prvních 10 nahrávek!","Prvých 10 nahrávok!","Die ersten 10 Aufnahmen!","¡Las primeras 10 grabaciones!","Az első 10 felvétel!"}, "Zaczął się trening — pierwsze 10 nagrań! Tak trzymaj! 🌱"),
        new A("total50", "⭐", 50, null, false, new String[]{"50 nagrań łącznie","50 recordings total","50 nahrávek celkem","50 nahrávok celkom","50 Aufnahmen insgesamt","50 grabaciones en total","Összesen 50 felvétel"}, "50 nagrań! Wchodzisz w rytm! ⭐"),
        new A("total100", "🌟", 100, null, false, new String[]{"100 nagrań łącznie","100 recordings total","100 nahrávek celkem","100 nahrávok celkom","100 Aufnahmen insgesamt","100 grabaciones en total","Összesen 100 felvétel"}, "100 nagrań! Jesteś w TOP kursantów! 🌟"),
        new A("total200", "💫", 200, null, false, new String[]{"200 nagrań łącznie","200 recordings total","200 nahrávek celkem","200 nahrávok celkom","200 Aufnahmen insgesamt","200 grabaciones en total","Összesen 200 felvétel"}, "200 nagrań — prawdziwa determinacja! 💫"),
        new A("total500", "🏆", 500, null, false, new String[]{"500 nagrań łącznie","500 recordings total","500 nahrávek celkem","500 nahrávok celkom","500 Aufnahmen insgesamt","500 grabaciones en total","Összesen 500 felvétel"}, "500 nagrań! Mistrz Nowej Mowy! 🏆"),
        new A("total1000", "👑", 1000, null, false, new String[]{"1000 nagrań łącznie","1000 recordings total","1000 nahrávek celkem","1000 nahrávok celkom","1000 Aufnahmen insgesamt","1000 grabaciones en total","Összesen 1000 felvétel"}, "1000 nagrań — LEGENDA! 👑"),
        new A("total1500", "🔱", 1500, null, false, new String[]{"1500 nagrań — rok treningu!","1500 recordings — a year of training!","1500 nahrávek — rok tréninku!","1500 nahrávok — rok tréningu!","1500 Aufnahmen — ein Jahr Training!","1500 grabaciones — ¡un año de entrenamiento!","1500 felvétel — egy év edzés!"}, "Rok pełnego treningu! Niesamowita droga! 🔱"),
        new A("sklepy10", "🏪", 10, new String[]{"Sklepy"}, false, new String[]{"10 nagrań w Sklepach","10 Shop recordings","10 nahrávek v obchodech","10 nahrávok v obchodoch","10 Aufnahmen im Geschäft","10 grabaciones en tiendas","10 üzleti felvétel"}, "Sklepy już Cię nie straszą! 🏪"),
        new A("sklepy50", "🛒", 50, new String[]{"Sklepy"}, false, new String[]{"50 nagrań w Sklepach","50 Shop recordings","50 nahrávek v obchodech","50 nahrávok v obchodoch","50 Aufnahmen im Geschäft","50 grabaciones en tiendas","50 üzleti felvétel"}, "Ekspert sklepowych rozmów! 🛒"),
        new A("sklepy100", "🏬", 100, new String[]{"Sklepy"}, false, new String[]{"100 nagrań w Sklepach","100 Shop recordings","100 nahrávek v obchodech","100 nahrávok v obchodoch","100 Aufnahmen im Geschäft","100 grabaciones en tiendas","100 üzleti felvétel"}, "100 sklepów — jesteś wszędzie! 🏬"),
        new A("sklepy200", "💰", 200, new String[]{"Sklepy"}, false, new String[]{"200 nagrań w Sklepach","200 Shop recordings","200 nahrávek v obchodech","200 nahrávok v obchodoch","200 Aufnahmen im Geschäft","200 grabaciones en tiendas","200 üzleti felvétel"}, "Sklepy to Twój dom! 200 nagrań! 💰"),
        new A("sklepy500", "🌐", 500, new String[]{"Sklepy"}, false, new String[]{"500 nagrań w Sklepach","500 Shop recordings","500 nahrávek v obchodech","500 nahrávok v obchodoch","500 Aufnahmen im Geschäft","500 grabaciones en tiendas","500 üzleti felvétel"}, "Mistrz świata sklepowych rozmów! 🌐"),
        new A("przech10", "🚶", 10, new String[]{"Przechodzień"}, false, new String[]{"10 Przechodniów","10 Passersby","10 kolemjdoucích","10 okoloidúcich","10 Passanten","10 transeúntes","10 járókelő"}, "Odwaga na ulicy rośnie! 🚶"),
        new A("przech50", "🏃", 50, new String[]{"Przechodzień"}, false, new String[]{"50 Przechodniów","50 Passersby","50 kolemjdoucích","50 okoloidúcich","50 Passanten","50 transeúntes","50 járókelő"}, "50 nieznajomych zagadniętych! 🏃"),
        new A("przech100", "🌍", 100, new String[]{"Przechodzień"}, false, new String[]{"100 Przechodniów","100 Passersby","100 kolemjdoucích","100 okoloidúcich","100 Passanten","100 transeúntes","100 járókelő"}, "100 nowych kontaktów na ulicy! 🌍"),
        new A("przech200", "🦅", 200, new String[]{"Przechodzień"}, false, new String[]{"200 Przechodniów","200 Passersby","200 kolemjdoucích","200 okoloidúcich","200 Passanten","200 transeúntes","200 járókelő"}, "Ulica to Twój żywioł! 🦅"),
        new A("przech500", "⚡", 500, new String[]{"Przechodzień"}, false, new String[]{"500 Przechodniów","500 Passersby","500 kolemjdoucích","500 okoloidúcich","500 Passanten","500 transeúntes","500 járókelő"}, "Legendarny głos na ulicy! ⚡"),
        new A("tel10", "📱", 10, new String[]{"Telefon do miasta","Telefon rodzina/znajomi","Phone do Kursanta/Trenera"}, false, new String[]{"10 rozmów telefonicznych","10 phone conversations","10 telefonních rozhovorů","10 telefonických rozhovorov","10 Telefongespräche","10 conversaciones telefónicas","10 telefonbeszélgetés"}, "Telefon już nie jest straszny! 📱"),
        new A("tel50", "📞", 50, new String[]{"Telefon do miasta","Telefon rodzina/znajomi","Phone do Kursanta/Trenera"}, false, new String[]{"50 rozmów telefonicznych","50 phone conversations","50 telefonních rozhovorů","50 telefonických rozhovorov","50 Telefongespräche","50 conversaciones telefónicas","50 telefonbeszélgetés"}, "Telefoniczny ekspert! 📞"),
        new A("tel100", "☎", 100, new String[]{"Telefon do miasta","Telefon rodzina/znajomi","Phone do Kursanta/Trenera"}, false, new String[]{"100 rozmów telefonicznych","100 phone conversations","100 telefonních rozhovorů","100 telefonických rozhovorov","100 Telefongespräche","100 conversaciones telefónicas","100 telefonbeszélgetés"}, "100 telefonów — mistrz rozmów! ☎"),
        new A("tel200", "🎯", 200, new String[]{"Telefon do miasta","Telefon rodzina/znajomi","Phone do Kursanta/Trenera"}, false, new String[]{"200 rozmów telefonicznych","200 phone conversations","200 telefonních rozhovorů","200 telefonických rozhovorov","200 Telefongespräche","200 conversaciones telefónicas","200 telefonbeszélgetés"}, "Telefon to Twoja siła! 🎯"),
        new A("tel500", "🚀", 500, new String[]{"Telefon do miasta","Telefon rodzina/znajomi","Phone do Kursanta/Trenera"}, false, new String[]{"500 rozmów telefonicznych","500 phone conversations","500 telefonních rozhovorů","500 telefonických rozhovorov","500 Telefongespräche","500 conversaciones telefónicas","500 telefonbeszélgetés"}, "Niepokonany przez żaden telefon! 🚀"),
        new A("wyst1", "🎤", 1, new String[]{"Wystąpienie"}, true, new String[]{"Pierwsze Wystąpienie!","First Performance!","První vystoupení!","Prvé vystúpenie!","Erster Auftritt!","¡Primera actuación!","Első nyilvános beszéd!"}, "BRAWO! Pierwsze publiczne wystąpienie — najtrudniejszy krok! 🎤"),
        new A("wyst5", "🎭", 5, new String[]{"Wystąpienie"}, true, new String[]{"5 Wystąpień","5 Performances","5 vystoupení","5 vystúpení","5 Auftritte","5 actuaciones","5 nyilvános beszéd"}, "5 wystąpień — strach ustępuje miejsca sile! 🎭"),
        new A("wyst10", "🌟", 10, new String[]{"Wystąpienie"}, true, new String[]{"10 Wystąpień","10 Performances","10 vystoupení","10 vystúpení","10 Auftritte","10 actuaciones","10 nyilvános beszéd"}, "10 wystąpień — Twój głos brzmi pewnie! 🌟"),
        new A("wyst25", "🦁", 25, new String[]{"Wystąpienie"}, true, new String[]{"25 Wystąpień","25 Performances","25 vystoupení","25 vystúpení","25 Auftritte","25 actuaciones","25 nyilvános beszéd"}, "25 wystąpień — prawdziwy Lew Estrady! 🦁"),
        new A("wyst50", "👑", 50, new String[]{"Wystąpienie"}, true, new String[]{"50 Wystąpień — MISTRZ!","50 Performances — MASTER!","50 vystoupení — MISTR!","50 vystúpení — MAJSTER!","50 Auftritte — MEISTER!","50 actuaciones — ¡MAESTRO!","50 nyilvános beszéd — MESTER!"}, "50 wystąpień — jesteś Mistrzem Mowy Publicznej! 👑"),
        new A("wyst100", "🏆", 100, new String[]{"Wystąpienie"}, true, new String[]{"100 Wystąpień","100 Performances","100 vystoupení","100 vystúpení","100 Auftritte","100 actuaciones","100 nyilvános beszéd"}, "100 wystąpień — legenda sceny! 🏆"),
        new A("wyst200", "💎", 200, new String[]{"Wystąpienie"}, true, new String[]{"200 Wystąpień","200 Performances","200 vystoupení","200 vystúpení","200 Auftritte","200 actuaciones","200 nyilvános beszéd"}, "200 wystąpień — diament elokwencji! 💎"),
        new A("mono10", "📖", 10, new String[]{"Monolog / Czytanie"}, false, new String[]{"10 Monologów","10 Monologues","10 monologů","10 monológov","10 Monologe","10 monólogos","10 monológ"}, "10 monologów — głos nabiera barwy! 📖"),
        new A("mono50", "📚", 50, new String[]{"Monolog / Czytanie"}, false, new String[]{"50 Monologów","50 Monologues","50 monologů","50 monológov","50 Monologe","50 monólogos","50 monológ"}, "50 monologów — wytrenowany głos! 📚"),
        new A("mono100", "🎙", 100, new String[]{"Monolog / Czytanie"}, false, new String[]{"100 Monologów","100 Monologues","100 monologů","100 monológov","100 Monologe","100 monólogos","100 monológ"}, "100 monologów — Twój głos to instrument! 🎙"),
        new A("mono200", "🎵", 200, new String[]{"Monolog / Czytanie"}, false, new String[]{"200 Monologów","200 Monologues","200 monologů","200 monológov","200 Monologe","200 monólogos","200 monológ"}, "200 monologów — muzyk słowa! 🎵"),
        new A("mono500", "🌠", 500, new String[]{"Monolog / Czytanie"}, false, new String[]{"500 Monologów","500 Monologues","500 monologů","500 monológov","500 Monologe","500 monólogos","500 monológ"}, "500 monologów — mistrz narracji! 🌠"),
        new A("miasto10", "🏙", 10, new String[]{"Miasto - inne","Restauracja Kelner","McDrive, DriveThru","W grupie","Przyjaciele"}, false, new String[]{"10 nagrań w mieście","10 city recordings","10 nahrávek ve městě","10 nahrávok v meste","10 Aufnahmen in der Stadt","10 grabaciones en la ciudad","10 városi felvétel"}, "Miasto już zna Twój głos! 🏙"),
        new A("miasto50", "🌆", 50, new String[]{"Miasto - inne","Restauracja Kelner","McDrive, DriveThru","W grupie","Przyjaciele"}, false, new String[]{"50 nagrań w mieście","50 city recordings","50 nahrávek ve městě","50 nahrávok v meste","50 Aufnahmen in der Stadt","50 grabaciones en la ciudad","50 városi felvétel"}, "50 miejskich nagrań — jesteś wszędzie! 🌆"),
        new A("miasto100", "🗺", 100, new String[]{"Miasto - inne","Restauracja Kelner","McDrive, DriveThru","W grupie","Przyjaciele"}, false, new String[]{"100 nagrań w mieście","100 city recordings","100 nahrávek ve městě","100 nahrávok v meste","100 Aufnahmen in der Stadt","100 grabaciones en la ciudad","100 városi felvétel"}, "100 miejskich przygód! 🗺"),
        new A("mcdrive1", "🍔", 1, new String[]{"McDrive, DriveThru"}, false, new String[]{"Pierwsze zamówienie McDrive!","First McDrive order!","První objednávka McDrive!","Prvá objednávka McDrive!","Erste McDrive-Bestellung!","¡Primer pedido en McDrive!","Első McDrive rendelés!"}, "Pierwsze zamówienie przez okienko — najtrudniejszy krok za Tobą! 🍔"),
        new A("mcdrive10", "🍟", 10, new String[]{"McDrive, DriveThru"}, false, new String[]{"10 zamówień McDrive","10 McDrive orders","10 objednávek McDrive","10 objednávok McDrive","10 McDrive-Bestellungen","10 pedidos en McDrive","10 McDrive rendelés"}, "10 zamówień przez drive-thru — rutyna! 🍟"),
        new A("mcdrive20", "🥤", 20, new String[]{"McDrive, DriveThru"}, false, new String[]{"20 zamówień McDrive","20 McDrive orders","20 objednávek McDrive","20 objednávok McDrive","20 McDrive-Bestellungen","20 pedidos en McDrive","20 McDrive rendelés"}, "20 zamówień — mistrz okienka! 🥤"),
    };

    // Grupy do wyswietlania (naglowki sekcji)
    // Nowo zdobyte odznaki od ostatniego sprawdzenia (pierwsze sprawdzenie tylko zapamietuje stan)
    public static java.util.List<A> newlyEarned(android.content.Context c, java.util.Map<String, Integer> counts, int total) {
        android.content.SharedPreferences p = c.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        java.util.Set<String> seen = new java.util.HashSet<>(p.getStringSet("ach_seen", new java.util.HashSet<>()));
        boolean first = !p.getBoolean("ach_init", false);
        java.util.List<A> out = new java.util.ArrayList<>();
        for (A m : ALL) {
            int cur = 0;
            if (m.cats == null) cur = total;
            else for (String cat : m.cats) for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) if (norm(e.getKey()).equals(norm(cat))) cur += e.getValue();
            if (cur >= m.count && seen.add(m.id) && !first) out.add(m);
        }
        p.edit().putStringSet("ach_seen", seen).putBoolean("ach_init", true).apply();
        return out;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('ł', 'l').replace('Ł', 'L').toLowerCase(java.util.Locale.ROOT).trim();
    }

    // Okienko z gratulacjami za nowa odznake
    public static void celebrate(android.app.Activity a, java.util.List<A> list) {
        if (list == null || list.isEmpty() || a.isFinishing()) return;
        A m = list.get(0);
        float d = a.getResources().getDisplayMetrics().density;
        android.widget.LinearLayout box = new android.widget.LinearLayout(a);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        box.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (6 * d));
        android.widget.TextView ic = new android.widget.TextView(a);
        ic.setText(m.icon);
        ic.setTextSize(56f);
        ic.setGravity(android.view.Gravity.CENTER);
        box.addView(ic);
        android.widget.TextView t = new android.widget.TextView(a);
        t.setText(L.t("Nowa odznaka!"));
        t.setTextSize(13f);
        t.setTextColor(0xFFE8820C);
        t.setGravity(android.view.Gravity.CENTER);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(t);
        android.widget.TextView n = new android.widget.TextView(a);
        n.setText(m.label());
        n.setTextSize(19f);
        n.setGravity(android.view.Gravity.CENTER);
        n.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        n.setPadding(0, (int) (6 * d), 0, (int) (6 * d));
        box.addView(n);
        if ("pl".equals(L.lang()) && m.msgPl != null) {
            android.widget.TextView msg = new android.widget.TextView(a);
            msg.setText(m.msgPl);
            msg.setTextSize(14f);
            msg.setGravity(android.view.Gravity.CENTER);
            box.addView(msg);
        }
        if (list.size() > 1) {
            android.widget.TextView more = new android.widget.TextView(a);
            more.setText("+ " + (list.size() - 1) + " " + L.t("kolejne odznaki w Statystykach"));
            more.setTextSize(12f);
            more.setGravity(android.view.Gravity.CENTER);
            more.setPadding(0, (int) (8 * d), 0, 0);
            box.addView(more);
        }
        new android.app.AlertDialog.Builder(a).setView(box).setPositiveButton("🎉 " + L.t("Super!"), null).show();
    }

    public static String group(A a) {
        String id = a.id;
        if (id.startsWith("total")) return L.t("Łącznie");
        if (id.startsWith("sklepy")) return L.cat("Sklepy");
        if (id.startsWith("przech")) return L.cat("Przechodzień");
        if (id.startsWith("tel")) return L.t("Telefony");
        if (id.startsWith("wyst")) return L.cat("Wystąpienie");
        if (id.startsWith("mono")) return L.cat("Monolog / Czytanie");
        if (id.startsWith("miasto")) return L.t("Miasto");
        return "McDrive";
    }
}
