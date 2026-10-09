package com.pitchrec.nativetest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// PELNA WERSJA — jednorazowy zakup przez Google Play (Google Play Billing).
// Produkt w Play Console: Zarabianie -> Produkty -> Produkty w aplikacji, ID: "pelna_wersja".
// Zakup jest przypisany do konta Google: po reinstalacji / na nowym telefonie wraca sam
// (sprawdzamy przy kazdym starcie) albo przyciskiem "Przywróć zakup".
// Stan zapamietany w ustawieniach ("premium_owned"), wiec bez internetu pelna wersja dziala dalej.
public final class Premium {

    public static final String PRODUCT_ID = "pelna_wersja";

    private static BillingClient client;
    private static Context app;
    private static boolean connecting = false;
    private static final List<Runnable> waiting = new ArrayList<>();
    private static ProductDetails details;
    private static Runnable onChangeAfterBuy;
    private static final Handler main = new Handler(Looper.getMainLooper());

    private Premium() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    public static boolean owned(Context c) { return prefs(c).getBoolean("premium_owned", false); }

    // Cena w walucie uzytkownika (np. "29,99 zł"), zapamietana z ostatniego zapytania do Google Play
    public static String price(Context c) { return prefs(c).getString("premium_price", ""); }

    // ── polaczenie z Google Play (jedno na cala aplikacje) ──
    private static void connect(Context c, Runnable ready, Runnable fail) {
        app = c.getApplicationContext();
        if (client != null && client.isReady()) { ready.run(); return; }
        synchronized (waiting) {
            waiting.add(ready);
            if (connecting) return;
            connecting = true;
        }
        if (client == null) {
            client = BillingClient.newBuilder(app)
                    .setListener((r, list) -> main.post(() -> onPurchasesUpdated(r, list)))
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                    .build();
        }
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult r) {
                List<Runnable> run;
                synchronized (waiting) { connecting = false; run = new ArrayList<>(waiting); waiting.clear(); }
                boolean ok = r.getResponseCode() == BillingClient.BillingResponseCode.OK;
                main.post(() -> {
                    if (ok) for (Runnable x : run) x.run();
                    else if (fail != null) fail.run();
                });
            }
            @Override public void onBillingServiceDisconnected() {
                synchronized (waiting) { connecting = false; }
            }
        });
    }

    // Przy starcie aplikacji: czy zakup jest na koncie Google (tez po reinstalacji) + cena
    public static void refresh(Activity a, Runnable onChange) {
        connect(a, () -> {
            loadDetails(null);
            queryOwned(onChange, false);
        }, null);
    }

    // Przycisk "Przywróć zakup"
    public static void restore(Activity a, Runnable onChange) {
        Toast.makeText(a, L.t("Sprawdzanie zakupu…"), Toast.LENGTH_SHORT).show();
        connect(a, () -> queryOwned(onChange, true),
                () -> Toast.makeText(a, L.t("Brak połączenia ze Sklepem Google Play."), Toast.LENGTH_LONG).show());
    }

    // Przycisk "Kup pełną wersję"
    public static void buy(Activity a, Runnable onChange) {
        connect(a, () -> {
            if (details != null) { launch(a, onChange); return; }
            loadDetails(() -> {
                if (details != null) launch(a, onChange);
                else Toast.makeText(a, L.t("Zakup jest chwilowo niedostępny. Spróbuj ponownie za chwilę."), Toast.LENGTH_LONG).show();
            });
        }, () -> Toast.makeText(a, L.t("Brak połączenia ze Sklepem Google Play."), Toast.LENGTH_LONG).show());
    }

    private static void launch(Activity a, Runnable onChange) {
        if (a.isFinishing()) return;
        onChangeAfterBuy = onChange;
        BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details).build();
        BillingFlowParams fp = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdp)).build();
        client.launchBillingFlow(a, fp);
    }

    private static void loadDetails(Runnable done) {
        QueryProductDetailsParams q = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()))
                .build();
        client.queryProductDetailsAsync(q, (r, res) -> main.post(() -> {
            try {
                List<ProductDetails> l = res == null ? null : res.getProductDetailsList();
                if (r.getResponseCode() == BillingClient.BillingResponseCode.OK && l != null && !l.isEmpty()) {
                    details = l.get(0);
                    ProductDetails.OneTimePurchaseOfferDetails o = details.getOneTimePurchaseOfferDetails();
                    if (o != null && app != null) prefs(app).edit().putString("premium_price", o.getFormattedPrice()).apply();
                }
            } catch (Exception e) { /* zostaje poprzednia cena */ }
            if (done != null) done.run();
        }));
    }

    // Lista zakupow z Google Play. Gdy odpowiedz poprawna — zapisujemy stan (takze cofniecie
    // przy zwrocie pieniedzy). Bez polaczenia zostaje poprzedni stan.
    private static void queryOwned(Runnable onChange, boolean manual) {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (r, purchases) -> main.post(() -> {
                    if (app == null) return;
                    if (r.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        if (manual) Toast.makeText(app, L.t("Nie udało się sprawdzić zakupu. Spróbuj ponownie."), Toast.LENGTH_LONG).show();
                        return;
                    }
                    boolean has = false;
                    if (purchases != null) for (Purchase p : purchases) if (isOurs(p) && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) { has = true; acknowledge(p); }
                    boolean was = owned(app);
                    prefs(app).edit().putBoolean("premium_owned", has).apply();
                    if (manual) Toast.makeText(app, has ? "⭐ " + L.t("Pełna wersja aktywna!") : L.t("Na tym koncie Google nie ma zakupu pełnej wersji."), Toast.LENGTH_LONG).show();
                    if ((was != has || manual) && onChange != null) onChange.run();
                }));
    }

    private static void onPurchasesUpdated(BillingResult r, List<Purchase> list) {
        if (app == null) return;
        int code = r.getResponseCode();
        if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { queryOwned(onChangeAfterBuy, true); return; }
        if (code != BillingClient.BillingResponseCode.OK || list == null) return; // anulowane / blad — nic nie zmieniamy
        for (Purchase p : list) {
            if (!isOurs(p)) continue;
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                prefs(app).edit().putBoolean("premium_owned", true).apply();
                acknowledge(p);
                Toast.makeText(app, "⭐ " + L.t("Dziękujemy! Pełna wersja jest aktywna."), Toast.LENGTH_LONG).show();
                if (onChangeAfterBuy != null) onChangeAfterBuy.run();
            } else if (p.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Toast.makeText(app, L.t("Płatność oczekuje na potwierdzenie — pełna wersja włączy się sama po jej zaksięgowaniu."), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static boolean isOurs(Purchase p) {
        return p.getProducts() != null && p.getProducts().contains(PRODUCT_ID);
    }

    // Google wymaga potwierdzenia zakupu w ciagu 3 dni — inaczej zwraca pieniadze
    private static void acknowledge(Purchase p) {
        if (p.isAcknowledged() || client == null) return;
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(), r -> { });
    }
}
