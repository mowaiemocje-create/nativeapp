package com.pitchrec.nativetest;

// Podpowiedzi "Inspiracje na dziś" — typ miejsca z OpenStreetMap -> ikona, kategoria nagrania i
// pomysł na rozmowę (PL/EN/CS z PitchRec; SK/DE/ES ogólne wg kategorii). Wygenerowane z PWA.
public final class Places {
    public static final class P { public final String icon, cat; public final String[] pl, en, cs; P(String i,String c,String[] pl,String[] en,String[] cs){icon=i;cat=c;this.pl=pl;this.en=en;this.cs=cs;} }
    public static final java.util.Map<String,P> MAP = new java.util.HashMap<>();
    static {
        MAP.put("restaurant", new P("🍽","Restauracja Kelner",new String[]{"Zapytaj kelnera co dziś polecają w {n}","Zapytaj o stolik na wieczór w {n}","Dowiedz się jakie danie dnia jest w {n}"},new String[]{"Ask what they recommend today at {n}","Ask about reservations at {n}"},new String[]{"Zeptejte se v {n} co dnes doporučují","Zeptejte se na rezervaci v {n}"}));
        MAP.put("cafe", new P("☕","Restauracja Kelner",new String[]{"Zapytaj w {n} skąd pochodzi kawa","Zapytaj o godziny otwarcia {n}"},new String[]{"Ask where the coffee comes from at {n}","Ask about opening hours at {n}"},new String[]{"Zeptejte se v {n} odkud pochází káva"}));
        MAP.put("fast_food", new P("🍔","Restauracja Kelner",new String[]{"Zapytaj o skład burgera w {n}","Zapytaj co jest dziś w promocji w {n}"},new String[]{"Ask about today special at {n}"},null));
        MAP.put("bar", new P("🍺","Restauracja Kelner",new String[]{"Zapytaj o piwo rzemieślnicze w {n}","Zapytaj o godziny happy hour w {n}"},null,null));
        MAP.put("pharmacy", new P("💊","Sklepy",new String[]{"Zapytaj farmaceutę w {n} co polecają na przeziębienie","Zapytaj w {n} czy mają konkretny lek bez recepty"},new String[]{"Ask the pharmacist at {n} what they recommend for a cold"},new String[]{"Zeptejte se lékárníka v {n} co doporučují na nachlazení"}));
        MAP.put("bank", new P("🏦","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o godziny otwarcia","Zapytaj w {n} o aktualny kurs euro"},new String[]{"Call {n} and ask about opening hours","Ask about exchange rates at {n}"},new String[]{"Zavolejte do {n} a zeptejte se na otevírací dobu"}));
        MAP.put("post_office", new P("📮","Telefon do miasta",new String[]{"Zapytaj w {n} ile kosztuje paczka do Niemiec","Zapytaj o czas dostarczenia listu poleconego z {n}"},new String[]{"Ask at {n} how much a package to Germany costs"},new String[]{"Zeptejte se v {n} kolik stojí balík do Německa"}));
        MAP.put("school", new P("🎓","Wystąpienie",new String[]{"Zatrzymaj się przy {n} i opowiedz o swoim dniu nową mową","Przeczytaj na głos tablicę przy {n}"},new String[]{"Read the notice board at {n} aloud","Talk to someone near {n}"},new String[]{"Přečtěte nahlas nástěnku u {n}"}));
        MAP.put("kindergarten", new P("🏫","Przechodzień",new String[]{"Zapytaj o godziny pracy {n}"},new String[]{"Ask someone near {n} about their opening hours"},new String[]{"Zeptejte se někoho u {n} na otevírací dobu"}));
        MAP.put("library", new P("📚","Monolog / Czytanie",new String[]{"Zapytaj w {n} jaką książkę teraz polecają","Zapytaj o godziny otwarcia czytelni w {n}"},new String[]{"Ask at {n} what book they recommend now"},new String[]{"Zeptejte se v {n} jakou knihu teď doporučují"}));
        MAP.put("fuel", new P("⛽","Sklepy",new String[]{"Zapytaj na {n} jaka jest cena diesla","Dowiedz się czy {n} ma myjnię samochodową"},new String[]{"Ask at {n} about the diesel price","Ask if {n} has a car wash"},new String[]{"Zeptejte se v {n} na cenu nafty"}));
        MAP.put("parcel_locker", new P("📦","Przechodzień",new String[]{"Zapytaj przechodnia przy {n} czy paczkomat działa","Zapytaj kogoś jak otworzyć skrytkę w {n}"},new String[]{"Ask a passerby near {n} if the locker is working"},new String[]{"Zeptejte se kolemjdoucího u {n} jestli funguje"}));
        MAP.put("hospital", new P("🏥","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o godziny przyjęć w poradni","Zapytaj w {n} jak zapisać się do specjalisty"},new String[]{"Call {n} and ask about clinic hours"},new String[]{"Zavolejte do {n} a zeptejte se na ordinační hodiny"}));
        MAP.put("clinic", new P("🏥","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o wolny termin","Zapytaj w {n} czy przyjmują bez skierowania"},null,null));
        MAP.put("doctors", new P("🏥","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o rejestrację","Zapytaj w {n} o godziny przyjęć"},null,null));
        MAP.put("supermarket", new P("🛒","Sklepy",new String[]{"Zapytaj ekspedientkę w {n} gdzie są produkty bezglutenowe","Zapytaj w {n} kiedy przyjeżdża dostawa świeżego pieczywa"},new String[]{"Ask staff at {n} where the gluten-free products are"},new String[]{"Zeptejte se personálu v {n} kde jsou bezlepkové produkty"}));
        MAP.put("convenience", new P("🏪","Sklepy",new String[]{"Zapytaj w {n} czy mają świeże kanapki","Zapytaj w {n} o godziny otwarcia w niedzielę"},new String[]{"Ask at {n} about Sunday opening hours"},null));
        MAP.put("bakery", new P("🥖","Sklepy",new String[]{"Zapytaj w {n} co dziś upiekli","Zapytaj o skład chleba na zakwasie w {n}"},new String[]{"Ask at {n} what they baked today"},new String[]{"Zeptejte se v {n} co dnes upekli"}));
        MAP.put("hairdresser", new P("✂️","Sklepy",new String[]{"Zapytaj w {n} o najbliższy wolny termin","Zapytaj w {n} o cenę strzyżenia"},new String[]{"Ask about the price list at {n}","Ask for the earliest appointment at {n}"},new String[]{"Zeptejte se na ceník v {n}"}));
        MAP.put("park", new P("🌳","Przechodzień",new String[]{"Zapytaj kogoś w {n} o drogę do centrum","Zagadaj spacerowicza w {n} o pogodę"},new String[]{"Ask someone walking in {n} for directions"},new String[]{"Zeptejte se někoho v {n} na cestu do centra"}));
        MAP.put("museum", new P("🏛","Wystąpienie",new String[]{"Zapytaj w {n} o aktualną wystawę","Dowiedz się ile kosztuje bilet do {n}"},new String[]{"Ask about the current exhibition at {n}"},new String[]{"Zeptejte se v {n} o aktuální výstavě"}));
        MAP.put("cinema", new P("🎭","Wystąpienie",new String[]{"Zapytaj w kasie {n} co grają w weekend","Dowiedz się o seanse dla dzieci w {n}"},new String[]{"Ask what is showing at the weekend at {n}"},null));
        MAP.put("townhall", new P("🏛","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o godziny otwarcia urzędu","Zapytaj w {n} gdzie złożyć wniosek o dowód"},new String[]{"Call {n} and ask about office hours"},null));
        MAP.put("place_of_worship", new P("⛪","Przechodzień",new String[]{"Zapytaj kogoś przy {n} o godziny niedzielnych nabożeństw","Dowiedz się o godziny mszy świętej w {n}","Zapytaj przechodnia jak dojść do {n}"},new String[]{"Ask someone near {n} about Sunday service times"},new String[]{"Zeptejte se někoho u {n} na časy nedělních bohoslužeb"}));
        MAP.put("community_centre", new P("🏢","Wystąpienie",new String[]{"Zapytaj w {n} jakie mają zajęcia dla dorosłych","Dowiedz się o harmonogram wydarzeń w {n}"},new String[]{"Ask at {n} about adult classes"},null));
        MAP.put("sports_centre", new P("💪","Przechodzień",new String[]{"Zapytaj o cennik karnetów w {n}","Dowiedz się jakie zajęcia są w {n} w tygodniu"},new String[]{"Ask about membership prices at {n}"},null));
        MAP.put("fitness_centre", new P("💪","Przechodzień",new String[]{"Zapytaj o godziny otwarcia {n}","Dowiedz się o darmowy trening próbny w {n}"},null,null));
        MAP.put("_shop", new P("🏪","Sklepy",new String[]{"Zapytaj w {n} o godziny otwarcia","Dowiedz się co mają w ofercie w {n}","Zapytaj w {n} o aktualną promocję"},new String[]{"Ask at {n} about opening hours","Ask about current promotions at {n}"},new String[]{"Zeptejte se v {n} na otevírací dobu","Zeptejte se v {n} na aktuální akci"}));
        MAP.put("_office", new P("🏢","Telefon do miasta",new String[]{"Zadzwoń do {n} i zapytaj o godziny otwarcia","Zapytaj w {n} jakie dokumenty są potrzebne"},new String[]{"Call {n} and ask about opening hours"},new String[]{"Zavolejte do {n} a zeptejte se na otevírací dobu"}));
    }
    private static final java.util.Map<String,String[]> GEN_SK=new java.util.HashMap<>(), GEN_DE=new java.util.HashMap<>(), GEN_ES=new java.util.HashMap<>();
    static {
        GEN_SK.put("Restauracja Kelner", new String[]{"Opýtajte sa v {n}, čo dnes odporúčajú"});
        GEN_SK.put("Sklepy", new String[]{"Opýtajte sa v {n} na otváracie hodiny","Opýtajte sa v {n} na aktuálnu akciu"});
        GEN_SK.put("Telefon do miasta", new String[]{"Zavolajte do {n} a opýtajte sa na otváracie hodiny"});
        GEN_SK.put("Wystąpienie", new String[]{"Povedzte pri {n} nahlas pár viet o svojom dni"});
        GEN_SK.put("Przechodzień", new String[]{"Opýtajte sa okoloidúceho pri {n} na cestu do centra"});
        GEN_SK.put("Monolog / Czytanie", new String[]{"Opýtajte sa v {n}, akú knihu odporúčajú"});
        GEN_DE.put("Restauracja Kelner", new String[]{"Frag in {n}, was heute empfohlen wird"});
        GEN_DE.put("Sklepy", new String[]{"Frag in {n} nach den Öffnungszeiten","Frag in {n} nach aktuellen Angeboten"});
        GEN_DE.put("Telefon do miasta", new String[]{"Ruf bei {n} an und frag nach den Öffnungszeiten"});
        GEN_DE.put("Wystąpienie", new String[]{"Erzähl bei {n} laut ein paar Sätze über deinen Tag"});
        GEN_DE.put("Przechodzień", new String[]{"Frag einen Passanten bei {n} nach dem Weg ins Zentrum"});
        GEN_DE.put("Monolog / Czytanie", new String[]{"Frag in {n}, welches Buch sie empfehlen"});
        GEN_ES.put("Restauracja Kelner", new String[]{"Pregunta en {n} qué recomiendan hoy"});
        GEN_ES.put("Sklepy", new String[]{"Pregunta en {n} por el horario","Pregunta en {n} por las ofertas actuales"});
        GEN_ES.put("Telefon do miasta", new String[]{"Llama a {n} y pregunta por el horario"});
        GEN_ES.put("Wystąpienie", new String[]{"Cuenta en voz alta junto a {n} unas frases sobre tu día"});
        GEN_ES.put("Przechodzień", new String[]{"Pregunta a un transeúnte junto a {n} cómo llegar al centro"});
        GEN_ES.put("Monolog / Czytanie", new String[]{"Pregunta en {n} qué libro recomiendan"});
    }
    public static P get(String type){ P p=MAP.get(type); return p!=null?p:MAP.get("_shop"); }
    public static String tip(String type, String name, int seed){
        P p=get(type); String l=L.lang(); String[] t;
        if("en".equals(l)&&p.en!=null) t=p.en; else if("cs".equals(l)&&p.cs!=null) t=p.cs;
        else if("sk".equals(l)) t=GEN_SK.getOrDefault(p.cat,GEN_SK.get("Sklepy"));
        else if("de".equals(l)) t=GEN_DE.getOrDefault(p.cat,GEN_DE.get("Sklepy"));
        else if("es".equals(l)) t=GEN_ES.getOrDefault(p.cat,GEN_ES.get("Sklepy"));
        else t=p.pl;
        return t[Math.abs(seed)%t.length].replace("{n}",name);
    }
}
