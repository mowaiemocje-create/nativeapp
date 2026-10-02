package com.pitchrec.nativetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

// ODPOWIEDZ KURSANTA DO TRENERA: pod ocena nagrania i pod odpowiedzia na dziennik —
// „👍 Rozumiem” albo „❓ Mam pytanie” (tekst). Odsluchanie komentarza glosowego zapisuje sie samo.
// Trener widzi to w swoim panelu. kind: "record" (ref = id nagrania) | "diary" (ref = data wpisu)
public final class StudentAck {

    public static volatile String lastError = "";

    private StudentAck() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE); }

    public interface Done { void done(boolean ok); }

    private static String key(String kind, String ref, String ack) { return "ack_" + kind + "_" + ref + "_" + ack; }

    public static boolean sent(Context c, String kind, String ref, String ack) { return prefs(c).getBoolean(key(kind, ref, ack), false); }

    public static void send(Context c, String kind, String ref, String ack, String text, Done cb) {
        SharedPreferences p = prefs(c);
        String token = p.getString("ns_token", null);
        if (token == null || ref == null || ref.isEmpty()) { if (cb != null) cb.done(false); return; }
        JSONObject b = new JSONObject();
        try {
            b.put("ns_token", token).put("ns_email", p.getString("ns_email", "")).put("ns_server", "new")
                    .put("kind", kind).put("ref", ref).put("ack", ack);
            if (text != null) b.put("text", text);
        } catch (Exception e) { }
        NsClient.backend("POST", "/student-ack", b.toString(), r -> {
            boolean ok = r.ok;
            // powod bledu do komunikatu (np. 404 = serwer bez tej funkcji, 401 = sesja wygasla)
            lastError = ok ? "" : r.status == 0 ? L.t("brak połączenia") : r.status == 404 ? L.t("serwer jeszcze tego nie obsługuje (worker)") : r.status == 401 ? L.t("zaloguj się ponownie") : "HTTP " + r.status;
            if (ok) p.edit().putBoolean(key(kind, ref, ack), true).apply();
            if (cb != null) cb.done(ok);
        });
    }

    // automatycznie, raz: kursant odsluchal komentarz glosowy
    public static void listened(Context c, String kind, String ref) {
        if (!sent(c, kind, ref, "listened")) send(c, kind, ref, "listened", null, null);
    }

    // Wiersz przyciskow pod komentarzem trenera
    public static void addButtons(Activity a, LinearLayout box, String kind, String ref) {
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, (int) (8 * d), 0, 0);
        box.addView(wrap);
        render(a, wrap, kind, ref);
    }

    private static void render(Activity a, LinearLayout wrap, String kind, String ref) {
        float d = a.getResources().getDisplayMetrics().density;
        wrap.removeAllViews();
        boolean ok = sent(a, kind, ref, "ok"), q = sent(a, kind, ref, "question");
        if (ok || q) {
            TextView t = Ui.text(a, (ok ? "👍 " + L.t("Wysłano trenerowi: Rozumiem") : "") + (ok && q ? "\n" : "") + (q ? "❓ " + L.t("Twoje pytanie trafiło do trenera") : ""), 12f, R.color.pr_accent);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            wrap.addView(t);
        }
        LinearLayout row = Ui.row(a);
        if (!ok) {
            Button b1 = Ui.button(a, "👍 " + L.t("Rozumiem"), R.color.pr_accent, false);
            b1.setOnClickListener(v -> { b1.setEnabled(false); send(a, kind, ref, "ok", null, s -> {
                if (!s) { b1.setEnabled(true); Toast.makeText(a, L.t("Nie udało się wysłać") + " — " + lastError, Toast.LENGTH_LONG).show(); }
                render(a, wrap, kind, ref); }); });
            row.addView(b1, Ui.weight(1f, 6 * d));
        }
        Button b2 = Ui.button(a, "❓ " + L.t(q ? "Kolejne pytanie" : "Mam pytanie"), R.color.pr_pause, false);
        b2.setOnClickListener(v -> {
            EditText in = new EditText(a);
            in.setHint(L.t("Napisz pytanie do trenera…"));
            in.setMinLines(3);
            int p = (int) (16 * d);
            LinearLayout fr = new LinearLayout(a);
            fr.setPadding(p, p / 2, p, 0);
            fr.addView(in, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            new AlertDialog.Builder(a).setTitle("❓ " + L.t("Pytanie do trenera")).setView(fr)
                    .setPositiveButton(L.t("Wyślij"), (dd, w) -> {
                        String txt = in.getText().toString().trim();
                        if (txt.isEmpty()) return;
                        send(a, kind, ref, "question", txt, s -> {
                            Toast.makeText(a, s ? L.t("Wysłano do trenera ✓") : L.t("Nie udało się wysłać") + " — " + lastError, Toast.LENGTH_LONG).show();
                            render(a, wrap, kind, ref);
                        });
                    })
                    .setNegativeButton(L.t("Anuluj"), null).show();
        });
        row.addView(b2, Ui.weight(1f, 0));
        wrap.addView(row);
    }
}
