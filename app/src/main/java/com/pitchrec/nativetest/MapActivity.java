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
        setContentView(root);

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
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Krok 1: strona z tej samej domeny -> wpisujemy logowanie do localStorage
                // (map.html czyta token NS z localStorage, tak jak w przegladarce)
                if (url != null && url.contains("map.html")) {
                    // Mapa gotowa: czyscimy historie (przycisk Wstecz nie cofnie do strony
                    // pomocniczej z kodem) i pokazujemy
                    view.clearHistory();
                    view.setVisibility(android.view.View.VISIBLE);
                    loading.setVisibility(android.view.View.GONE);
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
                }
            }
        });
        web.loadUrl(PWA_BASE + "/manifest.json");
    }

    private static String q(String s) {
        if (s == null) s = "";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    // Wstecz = zawsze powrot do aplikacji (bez cofania sie po stronach w WebView)
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (web != null) { try { web.destroy(); } catch (Exception e) { } }
        super.onDestroy();
    }
}
