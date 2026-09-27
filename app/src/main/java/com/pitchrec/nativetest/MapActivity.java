package com.pitchrec.nativetest;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

// MAPA NAGRAN — ta sama mapa co w PitchRec (map.html z hostingu PWA), wyswietlona w WebView.
// WAZNE: ta aktywnosc dziala w OSOBNYM PROCESIE (android:process=":map" w manifescie), bo
// WebView w tym samym procesie co nagrywanie blokuje mikrofon w tle na Androidzie 15+
// (powod, dla ktorego cala aplikacja jest natywna). Nagrywanie w glownym procesie nie jest
// wiec w zaden sposob dotkniete.
public class MapActivity extends Activity {

    public static final String PWA_BASE = "https://wispy-leaf-f9b9.mowaiemocje.workers.dev";

    private WebView web;
    private boolean injected = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        web = new WebView(this);
        // Strona pomocnicza (do wpisania logowania) nie moze byc widoczna — pokazujemy dopiero mape
        web.setVisibility(android.view.View.INVISIBLE);
        android.widget.TextView loading = new android.widget.TextView(this);
        loading.setText("⏳");
        loading.setTextSize(28f);
        loading.setGravity(android.view.Gravity.CENTER);
        root.addView(loading, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Zawsze widoczny przycisk zamkniecia mapy (prawy gorny rog, pod paskiem mapy)
        android.widget.TextView closeBtn = new android.widget.TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18f);
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable cbg = new android.graphics.drawable.GradientDrawable();
        cbg.setColor(0xCCE8820C);
        cbg.setCornerRadius(100f);
        closeBtn.setBackground(cbg);
        float dd = getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams((int) (44 * dd), (int) (44 * dd), android.view.Gravity.TOP | android.view.Gravity.END);
        clp.topMargin = (int) (64 * dd);
        clp.rightMargin = (int) (10 * dd);
        closeBtn.setOnClickListener(v -> finish());
        root.addView(closeBtn, clp);
        setContentView(root);
        registerBack();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);

        final String token = getIntent().getStringExtra("token");
        final String email = getIntent().getStringExtra("email");
        final String userId = getIntent().getStringExtra("userId");
        final String name = getIntent().getStringExtra("name");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        // "← Wróć" na mapie (history.back / index.html) = zamkniecie mapy i powrot do aplikacji
        web.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface public void close() { runOnUiThread(() -> finish()); }
        }, "PitchRecApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("mailto:") || url.startsWith("geo:")) {
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); } catch (Exception e) { }
                    return true;
                }
                // powrot do strony glownej PWA = powrot do aplikacji
                if (mapShown && url.startsWith(PWA_BASE) && (url.contains("index.html") || url.equals(PWA_BASE + "/") || url.equals(PWA_BASE))) {
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Krok 1: strona z tej samej domeny -> wpisujemy logowanie do localStorage
                // (map.html czyta token NS z localStorage, tak jak w przegladarce)
                // Krok 2: KAZDA strona inna niz pomocnicza (manifest.json) to juz mapa — serwer
                // przekierowuje "map.html" na "/map" (bez .html), dlatego nie sprawdzamy nazwy.
                if (injected && url != null && !url.contains("manifest.json")) {
                    view.evaluateJavascript("try{history.back=function(){PitchRecApp.close();};}catch(e){}", null);
                    showMap(view, loading);
                    return;
                }
                if (!injected) {
                    injected = true;
                    String js = "try{localStorage.setItem('ns_jwt'," + q(token) + ");"
                            + "localStorage.setItem('ns_email'," + q(email) + ");"
                            + "localStorage.setItem('ns_user_id'," + q(userId) + ");"
                            + "localStorage.setItem('pitchrec_map_name'," + q(name) + ");"
                            + "localStorage.setItem('pitchrec_ns_server','test');}catch(e){}";
                    view.evaluateJavascript(js, v -> view.loadUrl(PWA_BASE + "/map.html"));
                    // Zabezpieczenie: gdyby strona nie zglosila konca ladowania, pokaz po 8 s
                    view.postDelayed(() -> { if (!mapShown && !isFinishing()) showMap(view, loading); }, 8000);
                }
            }
        });
        web.loadUrl(PWA_BASE + "/manifest.json");
    }

    private boolean mapShown = false;

    // Mapa gotowa: czyscimy historie (Wstecz nie cofnie do strony pomocniczej) i pokazujemy
    private void showMap(WebView view, android.widget.TextView loading) {
        mapShown = true;
        view.clearHistory();
        view.setVisibility(android.view.View.VISIBLE);
        loading.setVisibility(android.view.View.GONE);
    }

    private static String q(String s) {
        if (s == null) s = "";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    // Wstecz = zawsze powrot do aplikacji (bez cofania sie po stronach w WebView).
    // Android 13+ (targetSdk 36) uzywa nowego mechanizmu "przewidywanego powrotu" — tam
    // onBackPressed() nie jest juz wolane, dlatego rejestrujemy tez OnBackInvokedCallback.
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) { finish(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    private void registerBack() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, this::finish);
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) { try { web.destroy(); } catch (Exception e) { } }
        super.onDestroy();
    }
}
