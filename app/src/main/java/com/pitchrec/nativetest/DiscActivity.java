package com.pitchrec.nativetest;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

// TEST DISC — ta sama strona co w PitchRec (assets/disc/disc.html + disc-core.js) w WebView.
// Dziala w OSOBNYM PROCESIE (":disc" w manifescie) — WebView w procesie nagrywania blokuje
// mikrofon w tle na Androidzie 15+. Logowanie (token NS) przekazane mostkiem NSBridge.auth().
public class DiscActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 33)
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, this::back);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // postep testu zapisuje sie lokalnie

        final String token = getIntent().getStringExtra("token");
        final String email = getIntent().getStringExtra("email");
        final String student = getIntent().getStringExtra("student");
        web.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface public String auth() {
                try {
                    return new org.json.JSONObject().put("token", token == null ? "" : token).put("email", email == null ? "" : email).put("server", "new").toString();
                } catch (Exception e) { return "{}"; }
            }
            @android.webkit.JavascriptInterface public void close() { runOnUiThread(() -> finish()); }
        }, "NSBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // linki zewnetrzne (np. sklep) — w przegladarce
                if (url.startsWith("http")) {
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); } catch (Exception e) { }
                    return true;
                }
                return false;
            }
        });
        String q = student != null && !student.isEmpty() ? "?student=" + android.net.Uri.encode(student) : "";
        web.loadUrl("file:///android_asset/disc/disc.html" + q);
    }

    // Wstecz = "‹" na stronie (cofniecie pytania / powrot do startu / zamkniecie).
    // Android 13+ (targetSdk 36): onBackPressed nie jest wolane — dlatego OnBackInvokedCallback.
    private void back() {
        if (web != null) web.evaluateJavascript("try{goBack()}catch(e){NSBridge.close()}", null);
        else finish();
    }

    @Override
    public void onBackPressed() { back(); }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) { back(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
