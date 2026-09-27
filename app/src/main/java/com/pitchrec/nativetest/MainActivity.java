package com.pitchrec.nativetest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pitchrec.backgroundrecorder.BackgroundRecorderService;
import com.pitchrec.backgroundrecorder.RecordingResultHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

// Ekran DAW — odtworzony uklad z PitchRec: suwaki MIC/ZOOM, miernik poziomu, przyciski
// PLAY/REC/PAUZA/RESET, wyswietlacz czasu, wybor formatu WAV/MP3, lista nagran.
// Bez logowania na razie (Faza 1) — menu NS to kolejny etap.
public class MainActivity extends AppCompatActivity implements RecordingResultHolder.Listener {

    private NeonButton recordButton;
    private NeonButton pauseButton;
    private NeonButton playButton;
    private Button resetButton;
    private TextView headerStatus;
    private String currentPage = "daw";
    private Button autoGainButton;
    private Button blackScreenButton;
    private TextView statusText;
    private TextView timeText;
    private TextView gainValueText;
    private TextView zoomValueText;
    private PitchWaveView pitchWaveView;
    private SliderView gainSlider;
    private SliderView zoomSlider;
    private ProgressBar levelMeter;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private String selectedFormat = "mp3";
    private long recordingStartedAtMs = 0L;
    private long pausedAccumMs = 0L;
    private long lastResumeAtMs = 0L;
    private String lastSavedFilePath = null;
    private long pendingSeekSample = 0L;
    private static final int REQUEST_MIC_PERMISSION = 100;

    private final Handler redrawHandler = new Handler(Looper.getMainLooper());
    private final Runnable redrawLoop = new Runnable() {
        @Override
        public void run() {
            pitchWaveView.invalidate();
            updateTimeDisplay();
            updateLevelMeter();
            if (isRecording) {
                // postOnAnimation zamiast postDelayed — synchronizuje sie z odswiezaniem
                // ekranu (VSYNC), dajac plynniejszy efekt niz sztywny czasomierz co 50ms.
                pitchWaveView.postOnAnimation(this);
            }
        }
    };

    // Zmiana jezyka aplikacji — zapisujemy wybor w SharedPreferences, i nakladamy go TUTAJ
    // (attachBaseContext wywolywane PRZED onCreate), zamiast dynamicznie w trakcie dzialania
    // — to standardowy, niezawodny sposob na zmiane locale na Androidzie.
    @Override
    protected void attachBaseContext(android.content.Context base) {
        String lang = getSavedLanguage(base);
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    private static String getSavedLanguage(android.content.Context ctx) {
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getString("language", "en"); // domyslnie angielski
    }

    private void setLanguage(String langCode) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit().putString("language", langCode).apply();
        recreate(); // ponowne uruchomienie aktywnosci z nowym locale (attachBaseContext zadziala znowu)
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        NsClient.initForms(this);
        setContentView(R.layout.activity_main);

        View rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            androidx.core.graphics.Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemInsets.top, v.getPaddingRight(), systemInsets.bottom);
            return insets;
        });

        recordButton = findViewById(R.id.recordButton);
        pauseButton = findViewById(R.id.pauseButton);
        playButton = findViewById(R.id.playButton);

        // Kolory neonowe — jak w prawdziwym studio: PLAY zielony, REC czerwony, STOP bialy.
        playButton.setNeonColor(Color.parseColor("#00FF66"));
        playButton.setButtonText(getString(R.string.btn_play));
        playButton.setButtonEnabled(false);
        recordButton.setNeonColor(Color.parseColor("#FF1A1A"));
        recordButton.setButtonText(getString(R.string.btn_rec));
        pauseButton.setNeonColor(Color.parseColor("#FFFFFF"));
        pauseButton.setButtonText(getString(R.string.btn_stop));
        pauseButton.setButtonEnabled(false);
        resetButton = findViewById(R.id.resetButton);
        headerStatus = findViewById(R.id.headerStatus);
        autoGainButton = findViewById(R.id.autoGainButton);
        blackScreenButton = findViewById(R.id.blackScreenButton);
        statusText = findViewById(R.id.statusText);
        timeText = findViewById(R.id.timeText);
        gainValueText = findViewById(R.id.gainValueText);
        zoomValueText = findViewById(R.id.zoomValueText);
        pitchWaveView = findViewById(R.id.pitchWaveView);
        gainSlider = findViewById(R.id.gainSlider);
        zoomSlider = findViewById(R.id.zoomSlider);
        levelMeter = findViewById(R.id.levelMeter);

        RecordingResultHolder.setListener(this);

        loadDawSettings();
        updateNavForLogin();
        if (isLoggedIn()) {
            NsClient.loadCategories(nsToken(), nsEmail());
            verifySession(null);
            loadMapNameFromNs();
        }
        checkAvailabilityExpiry();

        gainSlider.setValue(0.65f); // +6dB domyslnie w zakresie -20/+20
        gainSlider.setOnValueChangeListener(v -> {
            float db = -20f + v * 40f; // zakres -20dB do +20dB
            float linearGain = (float) Math.pow(10.0, db / 20.0);
            LiveAudioData.gainMultiplier = linearGain;
            gainValueText.setText(String.format(Locale.getDefault(), L.t("%+.1f dB"), db));
        });
        LiveAudioData.gainMultiplier = (float) Math.pow(10.0, 6.0 / 20.0); // +6dB domyslnie
        gainValueText.setText(L.t("+6.0 dB"));

        zoomSlider.setValue(0.12f); // domyslnie ~8s
        zoomValueText.setText("8s");
        pitchWaveView.setZoomSeconds(8f);
        zoomSlider.setOnValueChangeListener(v -> {
            if (v < 0.02f) {
                zoomValueText.setText("ALL");
                pitchWaveView.setZoomSeconds(0);
            } else {
                float seconds = 1f + v * 59f;
                zoomValueText.setText(String.format(Locale.getDefault(), "%.0fs", seconds));
                pitchWaveView.setZoomSeconds(seconds);
            }
        });

        // Schemat jak w RecForge: jeden przycisk cykluje REC<->PAUZA (rozpoczyna nagranie,
        // albo pauzuje/wznawia trwajace), drugi (STOP) finalizuje i zapisuje. Po Stop —
        // tylko odtwarzanie, dopóki nie wcisnie się REC ponownie (co zaczyna NOWE nagranie).
        recordButton.setOnButtonClickListener(v -> {
            if (!isRecording) {
                startRecordingFlow();
            } else if (!isPaused) {
                pauseRecordingFlow();
            } else {
                resumeRecordingFlow();
            }
        });

        pauseButton.setOnButtonClickListener(v -> stopRecordingFlow());

        playButton.setOnButtonClickListener(v -> {
            if (currentPlayer != null) {
                // Jest juz odtwarzacz — przelacz play/pauza (nie zaczynaj od nowa).
                try {
                    if (currentPlayer.isPlaying()) {
                        currentPlayer.pause();
                        playButton.setButtonText(getString(R.string.btn_play));
                    } else {
                        currentPlayer.start();
                        playButton.setButtonText(getString(R.string.btn_pause_play));
                        startPlayheadUpdateLoop();
                    }
                } catch (IllegalStateException e) { /* odtwarzacz w nietypowym stanie — ignorujemy */ }
            } else if (isPaused) {
                previewDuringPause(pendingSeekSample);
            } else {
                String path = loadedFilePath != null ? loadedFilePath : lastSavedFilePath;
                if (path != null) playFile(path, pendingSeekSample);
            }
        });
        pitchWaveView.setOnSeekListener(sample -> {
            pendingSeekSample = sample;
            long ms = sample * 1000L / LiveAudioData.SAMPLE_RATE;
            statusText.setText(getString(R.string.status_indicated, formatMs(ms)));
            // Jesli odtwarzacz jest AKTYWNIE odtwarzany, przewijamy go NAPRAWDE, nie tylko
            // ustawiamy zmienna — bez tego, biala linia wracala natychmiast do
            // rzeczywistej pozycji odtwarzacza przy nastepnej aktualizacji (petla
            // playheadUpdateLoop nadpisywala dotkniecie).
            if (currentPlayer != null) {
                try {
                    currentPlayer.seekTo((int) ms);
                    timeText.setText(formatMs(ms) + " / " + formatMs(lastRecordingTotalDurationMs));
                } catch (IllegalStateException e) { /* odtwarzacz w nietypowym stanie */ }
            }
        });
        resetButton.setOnClickListener(v -> resetRecording());

        setupBottomNav();
        showPage("daw");
        // Dotkniecie paska statusu w trybie poprawki = anulowanie poprawki
        statusText.setOnClickListener(v -> {
            String[] fx = loadFixTarget();
            if (fx == null || isRecording) return;
            new AlertDialog.Builder(this).setTitle(L.t("Anulować poprawkę?"))
                    .setMessage(L.t("Kolejne nagranie będzie zwykłym nagraniem, nie poprawką dla: ") + fx[1])
                    .setPositiveButton(L.t("Anuluj poprawkę"), (d, w) -> { clearFixTarget(); setStatus(getString(R.string.status_ready)); })
                    .setNegativeButton(L.t("Zostaw"), null).show();
        });
        String[] fxStart = loadFixTarget();
        if (fxStart != null) setStatus(L.t("🔄 Nagrywasz poprawkę dla: ") + fxStart[1] + L.t("  (dotknij, aby anulować)"));
        autoGainButton.setOnClickListener(v -> toggleAutoGain());
        blackScreenButton.setOnClickListener(v -> showBlackScreen());
    }

    // Wyswietlanie wzmocnienia w dB (jak "Wzmocnienie programowe +7,00 dB" w RecForge),
    // zamiast surowego mnoznika — bardziej naturalne dla uzytkownika. Sam mnoznik liniowy
    // (LiveAudioData.gainMultiplier) zostaje bez zmian dla faktycznego przetwarzania audio.
    private String gainToDbString(float linearGain) {
        float absGain = Math.abs(linearGain);
        float db = absGain > 0.001f ? (float) (20 * Math.log10(absGain)) : -100f;
        String sign = linearGain < 0 ? "-" : "+";
        return String.format(Locale.getDefault(), L.t("%s%.2f dB"), sign, Math.abs(db));
    }

    private void selectFormat(String format) {
        if (isRecording) {
            Toast.makeText(this, L.t("Zatrzymaj nagrywanie, żeby zmienić format"), Toast.LENGTH_SHORT).show();
            return;
        }
        selectedFormat = format;
    }

    // Ekran Ustawień — format nagrywania, bramka szumów, blokada wygaszania ekranu.
    // Budowany programowo (bez osobnego pliku layoutu), podobnie jak lista nagrań.
    // Pokazuje okno z siatka 16 kolorow do wyboru (4x4) — uzywane dla koloru linii pitch i
    // koloru siatki DAW.
    private void showColorGridPicker(java.util.function.IntConsumer onColorSelected) {
        int[] colors = {
                0xFFFF3B30, 0xFFFF9500, 0xFFFFE600, 0xFF00E000,
                0xFF00C7C7, 0xFF00A0FF, 0xFF7EC8E3, 0xFF5856D6,
                0xFFAF52DE, 0xFFFF2D95, 0xFFFFFFFF, 0xFFB0B0B0,
                0xFF808080, 0xFF3A3A3A, 0xFF8B5A2B, 0xFF000000
        };
        android.widget.LinearLayout grid = new android.widget.LinearLayout(this);
        grid.setOrientation(android.widget.LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int swatchSize = (int) (56 * density);

        android.app.AlertDialog[] dialogHolder = new android.app.AlertDialog[1];

        for (int row = 0; row < 4; row++) {
            android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(this);
            rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            for (int col = 0; col < 4; col++) {
                final int color = colors[row * 4 + col];
                View swatch = new View(this);
                swatch.setBackgroundColor(color);
                android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(swatchSize, swatchSize);
                swatch.setLayoutParams(p);
                swatch.setOnClickListener(v -> {
                    onColorSelected.accept(color);
                    if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                });
                rowLayout.addView(swatch);
            }
            grid.addView(rowLayout);
        }

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.pick_color_title))
                .setView(grid)
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private boolean autoGainEnabled = false;
    private final Runnable autoGainLoop = new Runnable() {
        @Override
        public void run() {
            if (autoGainEnabled && isRecording && !isPaused) {
                float[] recent = LiveAudioData.snapshotEnvelopeTail(10);
                float maxLevel = 0f;
                for (float v : recent) if (v > maxLevel) maxLevel = v;
                // Cel: poziom okolo 0.5 (w polu skali 0-1). Delikatna korekta, nie skokowa.
                float target = 0.5f;
                if (maxLevel > 0.01f) {
                    float error = target - maxLevel;
                    float currentDb = (float) (20 * Math.log10(LiveAudioData.gainMultiplier));
                    float newDb = currentDb + error * 2f; // delikatny wspolczynnik korekcji
                    newDb = Math.max(-20f, Math.min(20f, newDb));
                    LiveAudioData.gainMultiplier = (float) Math.pow(10.0, newDb / 20.0);
                    gainSlider.setValue((newDb + 20f) / 40f);
                    gainValueText.setText(String.format(Locale.getDefault(), L.t("%+.1f dB"), newDb));
                }
                redrawHandler.postDelayed(this, 500);
            }
        }
    };

    private void toggleAutoGain() {
        autoGainEnabled = !autoGainEnabled;
        autoGainButton.setText(autoGainEnabled ? "AG\nON" : "AG\nOFF");
        autoGainButton.setTextColor(getResources().getColor(autoGainEnabled ? R.color.pr_accent : R.color.pr_muted));
        if (autoGainEnabled) redrawHandler.post(autoGainLoop);
    }

    // Czarny ekran (jak w PitchRec) — pelnoekranowa czarna nakladka, dotkniecie wraca.
    private void showBlackScreen() {
        View overlay = new View(this);
        overlay.setBackgroundColor(0xFF000000);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(params);
        // Dodajemy do android.R.id.content (prawdziwy, PELNOEKRANOWY kontener aktywnosci,
        // BEZ paddingu na belki systemowe) — wczesniej dodawalismy do rootLayout, ktory MA
        // padding na te belki (dodany wczesniej dla poprawnego wyswietlania przycisku),
        // przez co czarna naklada NIE zakrywala calego ekranu.
        android.view.ViewGroup contentRoot = findViewById(android.R.id.content);
        overlay.setOnClickListener(v -> contentRoot.removeView(overlay));
        contentRoot.addView(overlay);
    }

    // ── LOGOWANIE / MENU ──
    // Korekta, Dziennik i Statystyki wymagaja konta NewSpeech — bez zalogowania sa UKRYTE.
    // Po zalogowaniu (Ustawienia -> Konto NewSpeech) pojawiaja sie w dolnym menu.
    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("app_settings", MODE_PRIVATE);
    }
    private String nsToken() { return prefs().getString("ns_token", null); }
    private String nsEmail() { return prefs().getString("ns_email", ""); }
    private String nsUserId() { return prefs().getString("ns_user_id", ""); }

    private boolean isLoggedIn() {
        String tok = nsToken();
        return tok != null && !tok.trim().isEmpty();
    }

    // Stan sesji do wyswietlenia: "none" | "checking" | "ok" | "expired" | "offline"
    private String nsAuthState = "none";
    private long nsAuthCheckedAt = 0L;

    private void updateNavForLogin() {
        boolean logged = isLoggedIn();
        int vis = logged ? View.VISIBLE : View.GONE;
        int[] ids = {R.id.navFix, R.id.navDiary, R.id.navStats};
        for (int id : ids) {
            View b = findViewById(id);
            if (b != null) b.setVisibility(vis);
        }
        if (!logged && !"expired".equals(nsAuthState)) nsAuthState = "none";
        // Bez logowania nie mozna zostac na stronie sekcji NS
        if (!logged && ("fix".equals(currentPage) || "diary".equals(currentPage) || "stats".equals(currentPage))) showPage("daw");
    }

    // ── DOLNE MENU I STRONY (jak showPage w PitchRec) ──
    private void setupBottomNav() {
        findViewById(R.id.navDaw).setOnClickListener(v -> showPage("daw"));
        findViewById(R.id.navRecs).setOnClickListener(v -> showPage("recs"));
        findViewById(R.id.navSet).setOnClickListener(v -> showPage("set"));
        findViewById(R.id.navFix).setOnClickListener(v -> openLoginOnlySection("fix", L.t("KOREKTA")));
        findViewById(R.id.navDiary).setOnClickListener(v -> openLoginOnlySection("diary", L.t("DZIENNIK")));
        findViewById(R.id.navStats).setOnClickListener(v -> openLoginOnlySection("stats", L.t("STATYSTYKI")));
        // Podpisy dolnego menu w wybranym jezyku
        int[] navLabels = {R.id.navRecsLabel, R.id.navFixLabel, R.id.navDiaryLabel, R.id.navStatsLabel, R.id.navSetLabel};
        String[] navTexts = {"NAGRANIA", "KOREKTA", "DZIENNIK", "STATS", "USTAWIENIA"};
        for (int i = 0; i < navLabels.length; i++) {
            TextView lb = findViewById(navLabels[i]);
            if (lb != null) lb.setText(L.t(navTexts[i]));
        }
    }

    private void showPage(String page) {
        currentPage = page;
        findViewById(R.id.dawPage).setVisibility("daw".equals(page) ? View.VISIBLE : View.GONE);
        findViewById(R.id.recsPage).setVisibility("recs".equals(page) ? View.VISIBLE : View.GONE);
        findViewById(R.id.setPage).setVisibility("set".equals(page) ? View.VISIBLE : View.GONE);
        boolean ns = "fix".equals(page) || "diary".equals(page) || "stats".equals(page);
        findViewById(R.id.nsPage).setVisibility(ns ? View.VISIBLE : View.GONE);
        String[][] nav = {{"daw", "navDaw"}, {"recs", "navRecs"}, {"fix", "navFix"}, {"diary", "navDiary"}, {"stats", "navStats"}, {"set", "navSet"}};
        int[] icons = {R.id.navDawIcon, R.id.navRecsIcon, R.id.navFixIcon, R.id.navDiaryIcon, R.id.navStatsIcon, R.id.navSetIcon};
        int[] labels = {R.id.navDawLabel, R.id.navRecsLabel, R.id.navFixLabel, R.id.navDiaryLabel, R.id.navStatsLabel, R.id.navSetLabel};
        for (int i = 0; i < nav.length; i++) {
            int c = getResources().getColor(nav[i][0].equals(page) ? R.color.pr_accent : R.color.pr_muted);
            android.widget.ImageView ic = findViewById(icons[i]);
            if (ic != null) ic.setColorFilter(c);
            TextView lb = findViewById(labels[i]);
            if (lb != null) lb.setTextColor(c);
        }
        if ("recs".equals(page)) renderRecsPage();
        if ("set".equals(page)) renderSettingsPage();
    }

    // Wejscie do sekcji wymagajacej konta: najpierw sprawdzamy sesje (jak w PWA) —
    // wygasla sesja = czytelny komunikat i przejscie do logowania, nie pusty ekran.
    private void openLoginOnlySection(String page, String title) {
        if (!isLoggedIn()) { promptLogin(L.t("Aby otworzyć sekcję ") + title + L.t(", zaloguj się do NewSpeech.")); return; }
        showPage(page);
        android.widget.LinearLayout c = findViewById(R.id.nsContent);
        c.removeAllViews();
        c.addView(pageTitle(title));
        TextView msg = Ui.text(this, L.t("⏳ Sprawdzam logowanie…"), 14f, R.color.pr_muted);
        c.addView(msg);
        verifySession(state -> {
            if (!page.equals(currentPage)) return;
            if ("ok".equals(state) && "fix".equals(page)) {
                renderFixPage(false);
            } else if ("ok".equals(state) && "diary".equals(page)) {
                new DiaryPage(this, c, diaryHost()).render();
            } else if ("ok".equals(state) && "stats".equals(page)) {
                new StatsPage(this, c, new StatsPage.Host() {
                    public String token() { return nsToken(); }
                    public String email() { return nsEmail(); }
                    public boolean isCurrent() { return "stats".equals(currentPage); }
                }).render();
            } else if ("ok".equals(state)) {
                msg.setText(L.t("Jesteś zalogowany ✓\n\nSekcja ") + title + L.t(" jest w przygotowaniu i pojawi się w kolejnej wersji aplikacji."));
                msg.setTextColor(getResources().getColor(R.color.pr_text));
            } else if ("expired".equals(state)) {
                showPage("daw");
                promptLogin(L.t("Twoja sesja NewSpeech wygasła. Zaloguj się ponownie."));
            } else {
                msg.setText(L.t("📡 Brak połączenia z NewSpeech.\nSprawdź internet i spróbuj ponownie."));
                msg.setTextColor(getResources().getColor(R.color.pr_warn));
            }
        });
    }

    private DiaryPage.Host diaryHost() {
        return new DiaryPage.Host() {
            public String token() { return nsToken(); }
            public String email() { return nsEmail(); }
            public String studentName() { return prefs().getString("student_name", ""); }
            public void onAuthExpired() {
                markSessionExpired();
                showPage("daw");
                promptLogin(L.t("Twoja sesja NewSpeech wygasła. Zaloguj się ponownie."));
            }
            public boolean isCurrent() { return "diary".equals(currentPage); }
        };
    }

    private TextView pageTitle(String t) {
        TextView tv = Ui.text(this, t, 18f, R.color.pr_muted);
        tv.setLetterSpacing(0.3f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding((int) Ui.dp(this, 4), (int) Ui.dp(this, 4), 0, (int) Ui.dp(this, 12));
        return tv;
    }

    private void promptLogin(String message) {
        new AlertDialog.Builder(this)
                .setTitle(L.t("Wymagane logowanie"))
                .setMessage(message)
                .setPositiveButton(L.t("Przejdź do logowania"), (d, w) -> showPage("set"))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    public interface AuthStateCallback { void onState(String state); }

    // Sprawdza waznosc tokenu. Wynik "ok" trzymamy 5 minut, zeby nie pytac serwera przy
    // kazdym kliknieciu. 401/403 = sesja wygasla -> token usuwany (email zostaje).
    private void verifySession(AuthStateCallback cb) {
        if (!isLoggedIn()) { if (cb != null) cb.onState(nsAuthState.equals("expired") ? "expired" : "none"); return; }
        if ("ok".equals(nsAuthState) && System.currentTimeMillis() - nsAuthCheckedAt < 5 * 60 * 1000L) {
            if (cb != null) cb.onState("ok");
            return;
        }
        nsAuthState = "checking";
        refreshAccountSection();
        final String tokAtStart = nsToken();
        NsClient.verify(tokAtStart, nsEmail(), nsUserId(), r -> {
            if (tokAtStart == null || !tokAtStart.equals(nsToken())) { if (cb != null) cb.onState(nsAuthState); return; }
            if (r.isAuthError()) {
                markSessionExpired();
            } else if (r.ok) {
                nsAuthState = "ok";
                nsAuthCheckedAt = System.currentTimeMillis();
            } else {
                nsAuthState = "offline";
            }
            refreshAccountSection();
            if (cb != null) cb.onState(nsAuthState);
        });
    }

    private void markSessionExpired() {
        boolean was = isLoggedIn();
        prefs().edit().remove("ns_token").putBoolean("ns_session_expired", true).apply();
        nsAuthState = "expired";
        nsAuthCheckedAt = 0L;
        updateNavForLogin();
        refreshAccountSection();
        if (was) Toast.makeText(this, L.t("Sesja NewSpeech wygasła — zaloguj się ponownie w Ustawieniach"), Toast.LENGTH_LONG).show();
    }

    private void doLogin(String email, String password, TextView errView) {
        if (email.isEmpty() || password.isEmpty()) { errView.setText(L.t("Wpisz email i hasło")); return; }
        errView.setText(L.t("⏳ Łączenie…"));
        NsClient.login(email, password, r -> {
            if (r.ok) {
                prefs().edit()
                        .putString("ns_token", r.token)
                        .putString("ns_email", r.email)
                        .putString("ns_user_id", r.userId == null ? "" : r.userId)
                        .putBoolean("ns_session_expired", false)
                        .apply();
                nsAuthState = "ok";
                nsAuthCheckedAt = System.currentTimeMillis();
                NsClient.loadCategories(r.token, r.email);
                prefs().edit().remove("map_name").apply();
                loadMapNameFromNs();
                updateNavForLogin();
                refreshAccountSection();
                Toast.makeText(this, L.t("✓ Zalogowano do NewSpeech"), Toast.LENGTH_SHORT).show();
            } else {
                errView.setText("✗ " + r.err);
            }
        });
    }

    private void doLogout() {
        prefs().edit().remove("ns_token").remove("ns_user_id").putBoolean("ns_session_expired", false).apply();
        nsAuthState = "none";
        nsAuthCheckedAt = 0L;
        updateNavForLogin();
        refreshAccountSection();
    }

    // Sekcja "Konto NewSpeech" na gorze Ustawien — zawsze pokazuje JASNY stan:
    // niezalogowany / sprawdzam / zalogowano / sesja wygasla / brak polaczenia.
    private android.widget.LinearLayout accountSection;

    private void refreshAccountSection() {
        if (accountSection == null) return;
        accountSection.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        // Naglowek konta: okragly awatar z inicjalem + nazwa/email
        android.widget.LinearLayout head = Ui.row(this);
        String who = isLoggedIn() ? nsEmail() : "";
        String nm = prefs().getString("student_name", "");
        TextView avatar = new TextView(this);
        String ini = !nm.isEmpty() ? nm.substring(0, 1) : (!who.isEmpty() ? who.substring(0, 1) : "?");
        avatar.setText(ini.toUpperCase(Locale.ROOT));
        avatar.setTextSize(20f);
        avatar.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setGravity(android.view.Gravity.CENTER);
        int av = (int) (46 * density);
        avatar.setBackground(Ui.rounded(getResources().getColor(isLoggedIn() ? R.color.pr_accent : R.color.pr_muted), 0, 0, av / 2f));
        android.widget.LinearLayout.LayoutParams alp = new android.widget.LinearLayout.LayoutParams(av, av);
        alp.rightMargin = (int) (12 * density);
        head.addView(avatar, alp);
        android.widget.LinearLayout headTxt = new android.widget.LinearLayout(this);
        headTxt.setOrientation(android.widget.LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(L.t("KONTO NEWSPEECH"));
        title.setTextSize(10f);
        title.setLetterSpacing(0.2f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.pr_muted));
        headTxt.addView(title);
        TextView whoTv = new TextView(this);
        whoTv.setText(!nm.isEmpty() && isLoggedIn() ? nm : (who.isEmpty() ? L.t("NewSpeech") : who));
        whoTv.setTextSize(16f);
        whoTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        whoTv.setTextColor(getResources().getColor(R.color.pr_text));
        headTxt.addView(whoTv);
        head.addView(headTxt, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        accountSection.addView(head);

        TextView status = new TextView(this);
        status.setTextSize(13f);
        status.setPadding(0, (int) (8 * density), 0, (int) (6 * density));
        accountSection.addView(status);

        if (isLoggedIn()) {
            String st = nsAuthState;
            if ("none".equals(st) || "expired".equals(st)) st = "checking";
            if ("ok".equals(st)) {
                status.setText(L.t("✓ Zalogowano: ") + nsEmail());
                status.setTextColor(getResources().getColor(R.color.pr_accent));
            } else if ("offline".equals(st)) {
                status.setText(L.t("⚠ Brak połączenia z NS — logowanie niepotwierdzone (") + nsEmail() + ")");
                status.setTextColor(getResources().getColor(R.color.pr_warn));
                Button retry = makeOutlinedButton(L.t("Spróbuj ponownie"), R.color.pr_accent, density);
                retry.setOnClickListener(v -> { nsAuthCheckedAt = 0L; verifySession(null); });
                accountSection.addView(retry);
            } else {
                status.setText(L.t("⏳ Sprawdzam logowanie… (") + nsEmail() + ")");
                status.setTextColor(getResources().getColor(R.color.pr_muted));
            }
            Button logout = makeOutlinedButton(L.t("Wyloguj"), R.color.pr_warn, density);
            logout.setOnClickListener(v -> doLogout());
            accountSection.addView(logout);
        } else {
            boolean expired = "expired".equals(nsAuthState) || prefs().getBoolean("ns_session_expired", false);
            status.setText(expired ? L.t("⚠ Sesja wygasła — zaloguj się ponownie") : L.t("Nie jesteś zalogowany"));
            status.setTextColor(getResources().getColor(expired ? R.color.pr_warn : R.color.pr_muted));

            android.widget.EditText emailInput = new android.widget.EditText(this);
            emailInput.setHint("Email");
            emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailInput.setText(nsEmail());
            accountSection.addView(emailInput);

            android.widget.EditText passInput = new android.widget.EditText(this);
            passInput.setHint(L.t("Hasło"));
            passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            accountSection.addView(passInput);

            TextView err = new TextView(this);
            err.setTextColor(getResources().getColor(R.color.pr_warn));
            err.setTextSize(12f);

            Button login = makeOutlinedButton(L.t("Zaloguj"), R.color.pr_accent, density);
            login.setOnClickListener(v -> doLogin(emailInput.getText().toString().trim(), passInput.getText().toString(), err));
            accountSection.addView(login);
            accountSection.addView(err);
        }

        TextView note = new TextView(this);
        note.setText(L.t("Po zalogowaniu pojawią się Korekta, Dziennik i Statystyki, a w Nagraniach zadziała „Wyślij NS”."));
        note.setTextSize(11f);
        note.setTextColor(getResources().getColor(R.color.pr_muted));
        note.setPadding(0, (int) (4 * density), 0, (int) (14 * density));
        accountSection.addView(note);
    }

    // ── WYSYLKA DO NS ──
    private void markNs(File f, String status) {
        RecMeta m = RecMeta.load(this, f.getName());
        m.ns = status;
        m.save(this, f.getName());
    }

    private void pickCategory(java.util.function.Consumer<String> onPicked) {
        new AlertDialog.Builder(this)
                .setTitle(L.t("Kategoria nagrania"))
                .setItems(catLabels(), (d, which) -> onPicked.accept(NsClient.CATEGORIES[which]))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // Wysyla nagranie w kategorii z jego OPISU (jak w PitchRec). Bez opisu -> najpierw ekran opisu.
    private void sendToNs(File file) {
        if (!isLoggedIn()) { promptLogin(L.t("Aby wysłać nagranie do NewSpeech, zaloguj się.")); return; }
        RecMeta meta = RecMeta.load(this, file.getName());
        if (meta.cat.isEmpty()) {
            describeExisting(file, true);
            return;
        }
        String catId = NsClient.categoryId(meta.cat);
        if (catId == null) { Toast.makeText(this, L.t("Nieznana kategoria: ") + meta.cat, Toast.LENGTH_LONG).show(); return; }
        boolean isFix = !meta.fixRecordId.isEmpty();
        setStatus(isFix ? L.t("☁ Wysyłanie poprawki…") : L.t("☁ Wysyłanie do NS…"));
        NsClient.Callback cb = r -> {
            if (r.ok) {
                markNs(file, "sent");
                setStatus(isFix ? L.t("☁✓ Poprawka wysłana! Czeka na ocenę trenera.") : L.t("☁✓ NS: wysłano (") + L.cat(meta.cat) + ")");
                Toast.makeText(this, isFix ? L.t("☁✓ Poprawka wysłana") : L.t("☁✓ Wysłano do NS"), Toast.LENGTH_SHORT).show();
                if (isFix) fixListCache = null;
            } else if (isFix && r.isLimitError()) {
                markNs(file, "error");
                setStatus(L.t("⛔ Poprawka odrzucona z powodu limitu"));
                new AlertDialog.Builder(this).setTitle(L.t("Limit nagrań"))
                        .setMessage(L.t("Wysłanie poprawki nie powiodło się z powodu limitu nagrań (") + r.err + L.t("). Poprawki powinny być możliwe bez limitu — jeśli to się powtarza, zgłoś to trenerowi."))
                        .setPositiveButton(getString(R.string.btn_close), null).show();
            } else if (r.isAuthError()) {
                markSessionExpired();
                setStatus(L.t("⚠ Sesja wygasła — zaloguj się ponownie"));
            } else if (r.isLimitError()) {
                markNs(file, "error");
                setStatus(L.t("⛔ Dzienny limit NOWYCH nagrań wyczerpany"));
                new AlertDialog.Builder(this).setTitle(L.t("Limit nagrań"))
                        .setMessage(L.t("Serwer NS: ") + r.err + L.t("\n\nDzienny limit dotyczy tylko nowych nagrań. Poprawki odrzuconych nagrań nie mają limitu (sekcja Korekta)."))
                        .setPositiveButton(getString(R.string.btn_close), null).show();
            } else {
                markNs(file, "error");
                setStatus(L.t("☁✗ NS: ") + r.err);
                Toast.makeText(this, L.t("Błąd wysyłki: ") + r.err, Toast.LENGTH_LONG).show();
            }
            if ("recs".equals(currentPage)) renderRecsPage();
        };
        if (isFix) NsClient.correctRecording(nsToken(), nsEmail(), file, meta.fixRecordId, catId, cb);
        else NsClient.uploadRecording(nsToken(), nsEmail(), file, catId, cb);
    }

    // "Wyslij wszystkie" — wszystkie OPISANE i jeszcze niewyslane nagrania, kazde w swojej
    // kategorii, po kolei. Zatrzymuje sie na pierwszym bledzie (np. limit dzienny).
    private void sendAllToNs(File[] files) {
        if (!isLoggedIn()) { promptLogin(L.t("Aby wysłać nagrania do NewSpeech, zaloguj się.")); return; }
        java.util.List<File> todo = new java.util.ArrayList<>();
        int undescribed = 0;
        for (File f : files) {
            RecMeta m = RecMeta.load(this, f.getName());
            if ("sent".equals(m.ns)) continue;
            if (m.cat.isEmpty()) { undescribed++; continue; }
            todo.add(f);
        }
        if (todo.isEmpty()) {
            Toast.makeText(this, undescribed > 0 ? L.t("Najpierw opisz nagrania (📝 Opisz)") : L.t("Wszystkie nagrania są już wysłane"), Toast.LENGTH_LONG).show();
            return;
        }
        String extra = undescribed > 0 ? "\n\n" + undescribed + L.t(" nagrań bez opisu zostanie pominiętych.") : "";
        new AlertDialog.Builder(this)
                .setTitle(L.t("Wysłać ") + todo.size() + L.t(" nagrań do NS?"))
                .setMessage(L.t("Każde nagranie trafi do NS w kategorii ze swojego opisu.") + extra)
                .setPositiveButton(L.t("Wyślij"), (d, w) -> sendNext(todo, 0, 0))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void sendNext(java.util.List<File> todo, int idx, int okCount) {
        if (idx >= todo.size()) {
            setStatus(L.t("☁✓ Wysłano ") + okCount + L.t(" nagrań do NS"));
            if ("recs".equals(currentPage)) renderRecsPage();
            return;
        }
        File f = todo.get(idx);
        RecMeta fm = RecMeta.load(this, f.getName());
        String catId = NsClient.categoryId(fm.cat);
        if (catId == null) { sendNext(todo, idx + 1, okCount); return; }
        setStatus(L.t("☁ Wysyłanie ") + (idx + 1) + "/" + todo.size() + "…");
        NsClient.Callback cb = r -> {
            if (r.ok) {
                markNs(f, "sent");
                sendNext(todo, idx + 1, okCount + 1);
            } else {
                if (r.isAuthError()) markSessionExpired(); else markNs(f, "error");
                setStatus(L.t("☁✗ Wysłano ") + okCount + "/" + todo.size() + L.t(" — zatrzymano: ") + r.err);
                Toast.makeText(this, L.t("Zatrzymano wysyłkę: ") + r.err, Toast.LENGTH_LONG).show();
                if ("recs".equals(currentPage)) renderRecsPage();
            }
        };
        if (!fm.fixRecordId.isEmpty()) NsClient.correctRecording(nsToken(), nsEmail(), f, fm.fixRecordId, catId, cb);
        else NsClient.uploadRecording(nsToken(), nsEmail(), f, catId, cb);
    }

    private void setStatus(String s) {
        statusText.setText(s);
    }

    // ── OPIS NAGRANIA (ekran "ZAPISZ NAGRANIE") ──
    // Po STOP: nowe nagranie -> pelny opis (wymagane: imie, kategoria, emocje, GPS),
    // potem zmiana nazwy pliku jak w PitchRec i pytanie o wyslanie do NS.
    private void describeNewRecording(File file) {
        String[] fix = loadFixTarget();
        RecMeta init = new RecMeta();
        String banner = null;
        if (fix != null) {
            init.cat = fix[1];
            init.fixRecordId = fix[0];
            banner = L.t("🔄 Nagrywasz poprawkę dla: ") + fix[1];
            // zapamietujemy od razu na pliku — nawet "Zamknij" nie zgubi informacji, ze to poprawka
            RecMeta pre = RecMeta.load(this, file.getName());
            pre.fixRecordId = fix[0];
            pre.save(this, file.getName());
        }
        final boolean isFix = fix != null;
        DescribeSheet.show(this, init, true, isFix ? L.t("ZAPISZ POPRAWKĘ") : L.t("ZAPISZ NAGRANIE"), banner, meta -> {
            File renamed = RecMeta.renameWithMeta(this, file, meta);
            afterDescribed(meta, file.lastModified());
            if (file.getAbsolutePath().equals(lastSavedFilePath)) lastSavedFilePath = renamed.getAbsolutePath();
            if (isFix) {
                clearFixTarget();
                setStatus(L.t("💾 Poprawka zapisana (") + L.cat(meta.cat) + ")");
                if (isLoggedIn()) sendToNs(renamed); // poprawka idzie od razu (PUT, bez limitu)
            } else {
                setStatus(L.t("💾 Zapisano (") + L.cat(meta.cat) + ")");
                askSendAfterSave(renamed);
            }
        }, () -> {
            if (isFix) clearFixTarget();
            setStatus(L.t("💾 Zapisano bez opisu — opisz i wyślij w NAGRANIACH"));
        });
    }

    // Opisywanie istniejacego pliku z listy (przycisk "📝 Opisz") — wymagana tylko kategoria.
    private void describeExisting(File file, boolean sendAfter) {
        RecMeta existing = RecMeta.load(this, file.getName());
        DescribeSheet.show(this, existing, false, L.t("OPISZ NAGRANIE"), meta -> {
            File renamed = RecMeta.renameWithMeta(this, file, meta);
            afterDescribed(meta, file.lastModified());
            if ("recs".equals(currentPage)) renderRecsPage();
            if (sendAfter && isLoggedIn()) sendToNs(renamed);
        });
    }

    // Po opisaniu nagrania — jak w PitchRec: kwestionariusz (Google Forms) + punkt na MAPIE
    // (historia zawsze, gdy jest GPS; "na zywo" dla Sklepow i Przechodniow, gdy wlaczone).
    private void afterDescribed(RecMeta m, long timeMs) {
        try {
            java.text.SimpleDateFormat hf = new java.text.SimpleDateFormat("d.M.yyyy HH:mm", Locale.US);
            StringBuilder f = new StringBuilder();
            formAdd(f, "entry.702109481", m.name);
            formAdd(f, "entry.157752890", m.cat);
            formAdd(f, "entry.815782810", m.hasGps() ? m.lat + ", " + m.lon : "");
            formAdd(f, "entry.1804891302", String.valueOf(m.emotion));
            formAdd(f, "entry.528183348", m.sys);
            formAdd(f, "entry.242728015", hf.format(new java.util.Date(timeMs)));
            formAdd(f, "entry.1345317394", m.note);
            NsClient.postForm("https://docs.google.com/forms/d/e/1FAIpQLSfTippGzWsqV6vX9ZovUTqsGYW-GQyqcvmTiJkIsvpoRysJ9g/formResponse", f.toString(), (ok, info) -> {
                if (ok) Toast.makeText(this, L.t("📋 Formularz Google: wysłano ✓"), Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, L.t("⚠ Formularz Google nie wysłany") + " (" + info + ") — " + L.t("ponowię automatycznie"), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) { /* nieblokujace */ }
        if (!m.hasGps() || m.cat.isEmpty()) return;
        String name = mapName(m.name);
        String uid = nsUserId().isEmpty() ? name : nsUserId();
        try {
            org.json.JSONObject h = new org.json.JSONObject();
            h.put("lat", m.lat); h.put("lon", m.lon); h.put("cat", m.cat); h.put("name", name);
            h.put("userId", uid); h.put("city", ""); h.put("ts", timeMs); h.put("sys", m.sys);
            NsClient.request("POST", "/map/hist", null, null, "application/json", h.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), r -> { });
            if (prefs().getBoolean("map_share", true) && ("Sklepy".equals(m.cat) || "Przechodzień".equals(m.cat))) {
                NsClient.request("POST", "/map/ping", null, null, "application/json", h.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), r -> { });
            }
        } catch (Exception e) { /* nieblokujace */ }
    }

    private static void formAdd(StringBuilder f, String k, String v) {
        if (f.length() > 0) f.append('&');
        f.append(NsClient.enc(k)).append('=').append(NsClient.enc(v == null ? "" : v));
    }

    // Imie na mapie — jak w PWA: imie z profilu NS, potem imie kursanta, potem email
    private String mapName(String fallback) {
        String n = prefs().getString("map_name", "");
        if (n.isEmpty()) n = fallback == null ? "" : fallback;
        if (n.isEmpty()) n = nsEmail();
        return n.isEmpty() ? "Kursant" : n;
    }

    private void loadMapNameFromNs() {
        if (!isLoggedIn() || !prefs().getString("map_name", "").isEmpty()) return;
        NsClient.request("GET", "/users/me", nsToken(), nsEmail(), null, null, r -> {
            if (!r.ok) return;
            try {
                org.json.JSONObject d = new org.json.JSONObject(r.body);
                String n = d.optString("name", "");
                if (n.isEmpty()) n = d.optString("first_name", "");
                if (!n.isEmpty()) prefs().edit().putString("map_name", n).apply();
            } catch (Exception e) { }
        });
    }

    // ── DOSTEPNY DO ROZMOWY (mapa) — jak "Chętnie porozmawiam" w PitchRec, wylacza sie po 2 h ──
    private void setAvailability(boolean on, String phone) {
        long until = on ? System.currentTimeMillis() + 2 * 60 * 60 * 1000L : 0L;
        prefs().edit().putBoolean("map_avail", on).putLong("map_avail_until", until).putString("map_phone", phone == null ? "" : phone).apply();
        try {
            org.json.JSONObject b = new org.json.JSONObject();
            String name = mapName(prefs().getString("student_name", ""));
            b.put("userId", nsUserId().isEmpty() ? (nsEmail().isEmpty() ? name : nsEmail()) : nsUserId());
            b.put("name", name);
            b.put("phone", phone == null || phone.isEmpty() ? org.json.JSONObject.NULL : phone);
            b.put("available", on);
            b.put("city", "");
            NsClient.request("POST", "/map/avail", null, null, "application/json", b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), r -> {
                if (!r.ok) Toast.makeText(this, L.t("Nie udało się zmienić dostępności: ") + r.err, Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) { }
    }

    private void checkAvailabilityExpiry() {
        if (prefs().getBoolean("map_avail", false) && System.currentTimeMillis() > prefs().getLong("map_avail_until", 0L)) {
            setAvailability(false, prefs().getString("map_phone", ""));
        }
    }

    private void openMap() {
        if (!isLoggedIn()) { promptLogin(L.t("Mapa nagrań jest dostępna po zalogowaniu do NewSpeech.")); return; }
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra("token", nsToken());
        i.putExtra("email", nsEmail());
        i.putExtra("userId", nsUserId());
        i.putExtra("name", mapName(prefs().getString("student_name", "")));
        startActivity(i);
    }

    private void askSendAfterSave(File file) {
        if (!isLoggedIn()) return;
        new AlertDialog.Builder(this)
                .setTitle(L.t("Wysłać do NewSpeech?"))
                .setMessage(L.t("Nagranie zostało zapisane. Wysłać je teraz do NS?"))
                .setPositiveButton(L.t("☁ Wyślij"), (d, w) -> sendToNs(file))
                .setNegativeButton(L.t("Później"), null)
                .show();
    }

    // ── ZAPIS USTAWIEN WYGLADU/DZWIEKU (przetrwaja zamkniecie aplikacji) ──
    private void loadDawSettings() {
        android.content.SharedPreferences p = getSharedPreferences("app_settings", MODE_PRIVATE);
        LiveAudioData.pitchLineWidthDp = p.getFloat("pitch_width", LiveAudioData.pitchLineWidthDp);
        LiveAudioData.pitchLineColor = p.getInt("pitch_color", LiveAudioData.pitchLineColor);
        LiveAudioData.gridLineWidthDp = p.getFloat("grid_width", LiveAudioData.gridLineWidthDp);
        LiveAudioData.gridLineColor = p.getInt("grid_color", LiveAudioData.gridLineColor);
        LiveAudioData.dawBackgroundColor = p.getInt("daw_bg", LiveAudioData.dawBackgroundColor);
        LiveAudioData.waveColor = p.getInt("wave_color", LiveAudioData.waveColor);
        LiveAudioData.autoNormalize = p.getBoolean("auto_normalize", true);
        LiveAudioData.noiseGateEnabled = p.getBoolean("noise_gate", LiveAudioData.noiseGateEnabled);
        keepScreenOnEnabled = p.getBoolean("keep_screen_on", true); // domyslnie ekran NIE gasnie
        selectedFormat = p.getString("format", "mp3"); // domyslnie MP3
        LiveAudioData.pauseMinS = p.getFloat("pause_min", 1.0f);
        LiveAudioData.pauseMaxS = p.getFloat("pause_max", 2.5f);
        applyKeepScreenOnSetting();
    }

    private void saveDawSettings() {
        getSharedPreferences("app_settings", MODE_PRIVATE).edit()
                .putFloat("pitch_width", LiveAudioData.pitchLineWidthDp)
                .putInt("pitch_color", LiveAudioData.pitchLineColor)
                .putFloat("grid_width", LiveAudioData.gridLineWidthDp)
                .putInt("grid_color", LiveAudioData.gridLineColor)
                .putInt("daw_bg", LiveAudioData.dawBackgroundColor)
                .putInt("wave_color", LiveAudioData.waveColor)
                .putBoolean("auto_normalize", LiveAudioData.autoNormalize)
                .putBoolean("noise_gate", LiveAudioData.noiseGateEnabled)
                .putBoolean("keep_screen_on", keepScreenOnEnabled)
                .putString("format", selectedFormat)
                .putFloat("pause_min", LiveAudioData.pauseMinS)
                .putFloat("pause_max", LiveAudioData.pauseMaxS)
                .apply();
        if (pitchWaveView != null) pitchWaveView.refreshStyle();
    }

    // ── STRONA USTAWIEN — uklad "profesjonalny": konto na gorze, pod nim MAPA, dalej sekcje ──
    private void renderSettingsPage() {
        android.widget.LinearLayout c = findViewById(R.id.setContent);
        c.removeAllViews();
        float d = getResources().getDisplayMetrics().density;

        android.widget.LinearLayout titleRow = Ui.row(this);
        titleRow.addView(pageTitle(L.t("USTAWIENIA")), new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(titleRow);

        // 1) Konto NewSpeech
        android.widget.LinearLayout acc = Ui.card(this);
        accountSection = new android.widget.LinearLayout(this);
        accountSection.setOrientation(android.widget.LinearLayout.VERTICAL);
        acc.addView(accountSection);
        c.addView(acc);
        refreshAccountSection();
        if (isLoggedIn()) verifySession(null);

        // 2) MAPA NAGRAN — duzy przycisk od razu pod logowaniem, obok NORMY
        android.widget.LinearLayout tiles = Ui.row(this);
        Button mapBtn = Ui.button(this, "🗺  " + L.t("MAPA NAGRAŃ"), R.color.pr_accent, true);
        mapBtn.setTextSize(14f);
        mapBtn.setPadding((int) (12 * d), (int) (16 * d), (int) (12 * d), (int) (16 * d));
        mapBtn.setOnClickListener(v -> openMap());
        tiles.addView(mapBtn, Ui.weight(2f, 8 * d));
        Button normBtn = Ui.button(this, "🎯  " + L.t("NORMY"), R.color.pr_purple, false);
        normBtn.setTextSize(13f);
        normBtn.setPadding((int) (12 * d), (int) (16 * d), (int) (12 * d), (int) (16 * d));
        normBtn.setOnClickListener(v -> comingSoon(L.t("Normy")));
        tiles.addView(normBtn, Ui.weight(1f, 0));
        android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = (int) (8 * d);
        c.addView(tiles, tlp);

        // 3) Nagrywanie
        c.addView(sectionHeader(L.t("NAGRYWANIE")));
        android.widget.LinearLayout rec = Ui.card(this);
        android.widget.LinearLayout fr = Ui.row(this);
        Button wav = Ui.button(this, "WAV", "wav".equals(selectedFormat) ? R.color.pr_accent : R.color.pr_muted, "wav".equals(selectedFormat));
        Button mp3 = Ui.button(this, "MP3", "mp3".equals(selectedFormat) ? R.color.pr_accent : R.color.pr_muted, "mp3".equals(selectedFormat));
        wav.setOnClickListener(v -> { selectFormat("wav"); saveDawSettings(); renderSettingsPage(); });
        mp3.setOnClickListener(v -> { selectFormat("mp3"); saveDawSettings(); renderSettingsPage(); });
        fr.addView(wav, Ui.weight(1f, 6 * d));
        fr.addView(mp3, Ui.weight(1f, 0));
        rec.addView(settingRow("🎵", L.t("Format nagrania"), "mp3".equals(selectedFormat) ? L.t("MP3 — mniejsze pliki, szybsza wysyłka") : L.t("WAV — pełna jakość, duże pliki"), null));
        rec.addView(fr);
        rec.addView(divider());
        rec.addView(toggleRow("🔊 " + L.t("Automatyczna głośność (0 dB)"), LiveAudioData.autoNormalize, on -> { LiveAudioData.autoNormalize = on; saveDawSettings(); }));
        rec.addView(hint(LiveAudioData.autoNormalize ? L.t("Ciche nagrania po zapisie są podgłaśniane do 0 dB (bez zniekształceń)") : L.t("Wyłączone — nagranie zostaje bez zmian")));
        rec.addView(divider());
        rec.addView(toggleRow("🔆 " + L.t("Ekran nie gaśnie (wyłączony wygaszacz)"), keepScreenOnEnabled, on -> { keepScreenOnEnabled = on; applyKeepScreenOnSetting(); saveDawSettings(); }));
        rec.addView(toggleRow("🎚 " + L.t("Bramka szumów (tłumi cichy szum tła)"), LiveAudioData.noiseGateEnabled, on -> { LiveAudioData.noiseGateEnabled = on; saveDawSettings(); }));
        rec.addView(divider());
        boolean gpsOk = GpsHelper.hasPermission(this);
        View gpsRow = settingRow("📍", L.t("Lokalizacja GPS"), gpsOk ? L.t("zezwolono ✓") : L.t("brak zgody — dotknij, aby zezwolić"), null);
        gpsRow.setOnClickListener(v -> requestLocationPermission(this));
        rec.addView(gpsRow);
        c.addView(rec);

        // 4) Analiza pauz
        c.addView(sectionHeader(L.t("PAUZY — ZAKRES PRAWIDŁOWY")));
        android.widget.LinearLayout pz = Ui.card(this);
        android.widget.LinearLayout pzRow = Ui.row(this);
        pzRow.addView(pauseStepper("MIN", true), Ui.weight(1f, 10 * d));
        pzRow.addView(pauseStepper("MAX", false), Ui.weight(1f, 0));
        pz.addView(pzRow);
        pz.addView(hint(String.format(Locale.US, L.t("Pauza między porcjami mowy %.1f–%.1f s liczy się jako prawidłowa (regulacja 1–4 s)"), LiveAudioData.pauseMinS, LiveAudioData.pauseMaxS)));
        c.addView(pz);

        // 5) Wyglad DAW
        c.addView(sectionHeader(L.t("WYGLĄD DAW")));
        android.widget.LinearLayout daw = Ui.card(this);
        TextView pitchWidthLabel = Ui.text(this, String.format(Locale.getDefault(), L.t("Grubość linii pitch: %.0f"), LiveAudioData.pitchLineWidthDp), 13f, R.color.pr_text);
        daw.addView(pitchWidthLabel);
        SliderView pitchWidthSlider = new SliderView(this);
        pitchWidthSlider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (36 * d)));
        pitchWidthSlider.setValue((LiveAudioData.pitchLineWidthDp - 1f) / 9f);
        pitchWidthSlider.setOnValueChangeListener(v -> {
            LiveAudioData.pitchLineWidthDp = Math.round(1f + v * 9f);
            pitchWidthLabel.setText(String.format(Locale.getDefault(), L.t("Grubość linii pitch: %.0f"), LiveAudioData.pitchLineWidthDp));
            saveDawSettings();
        });
        daw.addView(pitchWidthSlider);
        addColorRow(daw, Ui.text(this, L.t("Kolor linii pitch"), 13f, R.color.pr_text), LiveAudioData.pitchLineColor, color -> { LiveAudioData.pitchLineColor = color; saveDawSettings(); });
        daw.addView(divider());
        TextView gridWidthLabel = Ui.text(this, String.format(Locale.getDefault(), L.t("Grubość siatki (poziome i pionowe): ×%.1f"), LiveAudioData.gridLineWidthDp), 13f, R.color.pr_text);
        daw.addView(gridWidthLabel);
        SliderView gridWidthSlider = new SliderView(this);
        gridWidthSlider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (36 * d)));
        gridWidthSlider.setValue((LiveAudioData.gridLineWidthDp - 0.5f) / 9.5f);
        gridWidthSlider.setOnValueChangeListener(v -> {
            LiveAudioData.gridLineWidthDp = Math.round((0.5f + v * 9.5f) * 2f) / 2f; // krok 0,5
            gridWidthLabel.setText(String.format(Locale.getDefault(), L.t("Grubość siatki (poziome i pionowe): ×%.1f"), LiveAudioData.gridLineWidthDp));
            saveDawSettings();
        });
        daw.addView(gridWidthSlider);
        addColorRow(daw, Ui.text(this, L.t("Kolor siatki"), 13f, R.color.pr_text), LiveAudioData.gridLineColor, color -> { LiveAudioData.gridLineColor = color; saveDawSettings(); });
        addColorRow(daw, Ui.text(this, L.t("Kolor tła DAW"), 13f, R.color.pr_text), LiveAudioData.dawBackgroundColor, color -> { LiveAudioData.dawBackgroundColor = color; saveDawSettings(); });
        addColorRow(daw, Ui.text(this, L.t("Kolor fali"), 13f, R.color.pr_text), LiveAudioData.waveColor, color -> { LiveAudioData.waveColor = color; saveDawSettings(); });
        c.addView(daw);

        // 6) Mapa i dostepnosc do rozmowy
        c.addView(sectionHeader(L.t("MAPA I DOSTĘPNOŚĆ")));
        android.widget.LinearLayout mp = Ui.card(this);
        mp.addView(toggleRow("📡 " + L.t("Pokazuj mnie na mapie na żywo (Sklepy, Przechodzień)"), prefs().getBoolean("map_share", true), on -> prefs().edit().putBoolean("map_share", on).apply()));
        mp.addView(hint(L.t("Na mapie jako:") + " " + mapName(prefs().getString("student_name", ""))));
        mp.addView(divider());
        android.widget.EditText phone = new android.widget.EditText(this);
        phone.setHint(L.t("Telefon (opcjonalnie, widoczny dla kursantów)"));
        phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phone.setText(prefs().getString("map_phone", ""));
        phone.setTextSize(14f);
        mp.addView(phone);
        boolean avail = prefs().getBoolean("map_avail", false) && System.currentTimeMillis() < prefs().getLong("map_avail_until", 0L);
        Button av = Ui.button(this, "📞 " + L.t("Chętnie porozmawiam") + ": " + (avail ? L.t("WŁ") : L.t("WYŁ")), avail ? R.color.pr_accent : R.color.pr_muted, avail);
        av.setOnClickListener(v -> {
            if (!isLoggedIn()) { promptLogin(L.t("Dostępność do rozmowy wymaga zalogowania do NewSpeech.")); return; }
            setAvailability(!avail, phone.getText().toString().trim());
            renderSettingsPage();
        });
        mp.addView(av, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        mp.addView(hint(avail ? L.t("Widoczny na mapie jako dostępny do rozmowy — wyłączy się sam po 2 godzinach.") : L.t("Włącz, gdy możesz porozmawiać przez telefon z innym kursantem (wyłącza się po 2 h).")));
        c.addView(mp);

        // 7) Jezyk
        c.addView(sectionHeader(L.t("JĘZYK")));
        android.widget.LinearLayout lang = Ui.card(this);
        String cur = getSavedLanguage(this);
        String[][] langs = {{"pl", L.t("🇵🇱 Polski")}, {"en", L.t("🇬🇧 English")}, {"cs", L.t("🇨🇿 Čeština")}, {"sk", L.t("🇸🇰 Slovenčina")}, {"de", L.t("🇩🇪 Deutsch")}, {"es", L.t("🇪🇸 Español")}};
        android.widget.LinearLayout lr = null;
        for (int i = 0; i < langs.length; i++) {
            if (i % 2 == 0) { lr = Ui.row(this); lang.addView(lr); if (i > 0) lr.setPadding(0, (int) (6 * d), 0, 0); }
            String code = langs[i][0];
            boolean sel = code.equals(cur);
            Button b = Ui.button(this, langs[i][1], sel ? R.color.pr_accent : R.color.pr_muted, sel);
            b.setOnClickListener(v -> { if (!code.equals(getSavedLanguage(this))) setLanguage(code); });
            lr.addView(b, Ui.weight(1f, i % 2 == 0 ? 6 * d : 0));
        }
        c.addView(lang);

        // 8) O aplikacji
        TextView about = Ui.text(this, L.t("PitchRec · v") + appVersion() + L.t(" · NewSpeech"), 10f, R.color.pr_muted);
        about.setGravity(android.view.Gravity.CENTER);
        about.setPadding(0, (int) (10 * d), 0, 0);
        c.addView(about);
        c.addView(Ui.spacer(this, 20));
    }

    private TextView sectionHeader(String text) {
        TextView t = Ui.label(this, text);
        t.setTextColor(getResources().getColor(R.color.pr_accent));
        t.setPadding((int) Ui.dp(this, 4), (int) Ui.dp(this, 10), 0, (int) Ui.dp(this, 6));
        return t;
    }

    private TextView hint(String text) {
        TextView t = Ui.text(this, text, 11f, R.color.pr_muted);
        t.setPadding(0, (int) Ui.dp(this, 2), 0, (int) Ui.dp(this, 6));
        return t;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(getResources().getColor(R.color.pr_border));
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) Ui.dp(this, 1)));
        lp.topMargin = (int) Ui.dp(this, 8);
        lp.bottomMargin = (int) Ui.dp(this, 8);
        v.setLayoutParams(lp);
        return v;
    }

    // Wiersz ustawien: ikona | tytul + opis | (opcjonalna kontrolka po prawej)
    private android.widget.LinearLayout settingRow(String icon, String title, String sub, View control) {
        android.widget.LinearLayout r = Ui.row(this);
        r.setPadding(0, (int) Ui.dp(this, 4), 0, (int) Ui.dp(this, 8));
        TextView ic = Ui.text(this, icon, 18f, R.color.pr_text);
        r.addView(ic, new android.widget.LinearLayout.LayoutParams((int) Ui.dp(this, 32), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        android.widget.LinearLayout col = new android.widget.LinearLayout(this);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);
        TextView tt = Ui.text(this, title, 14f, R.color.pr_text);
        tt.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        col.addView(tt);
        if (sub != null && !sub.isEmpty()) col.addView(Ui.text(this, sub, 11f, R.color.pr_muted));
        r.addView(col, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (control != null) r.addView(control);
        return r;
    }

    private android.widget.LinearLayout pauseStepper(String label, boolean isMin) {
        android.widget.LinearLayout r = Ui.row(this);
        r.addView(Ui.text(this, label, 11f, R.color.pr_muted));
        Button minus = Ui.button(this, "−", R.color.pr_muted, false);
        Button plus = Ui.button(this, "+", R.color.pr_accent, false);
        float v = isMin ? LiveAudioData.pauseMinS : LiveAudioData.pauseMaxS;
        TextView val = Ui.text(this, String.format(Locale.US, "%.1fs", v), 15f, R.color.pr_accent);
        val.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        val.setGravity(android.view.Gravity.CENTER);
        View.OnClickListener step = b -> {
            float dir = b == plus ? 0.5f : -0.5f;
            if (isMin) {
                LiveAudioData.pauseMinS = Math.max(1f, Math.min(3.5f, LiveAudioData.pauseMinS + dir));
                if (LiveAudioData.pauseMaxS < LiveAudioData.pauseMinS + 0.5f) LiveAudioData.pauseMaxS = LiveAudioData.pauseMinS + 0.5f;
            } else {
                LiveAudioData.pauseMaxS = Math.max(1.5f, Math.min(4f, LiveAudioData.pauseMaxS + dir));
                if (LiveAudioData.pauseMinS > LiveAudioData.pauseMaxS - 0.5f) LiveAudioData.pauseMinS = LiveAudioData.pauseMaxS - 0.5f;
            }
            saveDawSettings();
            renderSettingsPage();
        };
        minus.setOnClickListener(step);
        plus.setOnClickListener(step);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        r.addView(minus, new android.widget.LinearLayout.LayoutParams((int) Ui.dp(this, 40), (int) Ui.dp(this, 40)));
        r.addView(val, lp);
        r.addView(plus, new android.widget.LinearLayout.LayoutParams((int) Ui.dp(this, 40), (int) Ui.dp(this, 40)));
        return r;
    }

    // ── KOREKTA (jak zakladka KOREKTA w PitchRec) ──
    private java.util.List<NsClient.FixEntry> fixListCache = null;
    private long fixListCacheAt = 0L;
    private int fixShowCount = 10;

    private void renderFixPage(boolean force) {
        android.widget.LinearLayout c = findViewById(R.id.nsContent);
        c.removeAllViews();
        android.widget.LinearLayout head = Ui.row(this);
        head.addView(pageTitle(L.t("KOREKTA")), new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button refresh = Ui.button(this, "↻", R.color.pr_muted, false);
        refresh.setOnClickListener(v -> renderFixPage(true));
        head.addView(refresh);
        c.addView(head);
        // Najpierw (jesli jest) poprzednia lista — od razu widoczna — a w tle ZAWSZE swieze dane
        // z serwera (force), zeby nowe oceny trenera pojawialy sie bez czekania na cache.
        TextView loading = Ui.text(this, fixListCache != null ? L.t("⟳ Odświeżanie…") : L.t("⏳ Wczytywanie…"), 12f, R.color.pr_muted);
        c.addView(loading);
        if (fixListCache != null) fillFixList(c, fixListCache);
        scheduleFixAutoRefresh();
        NsClient.fetchNeedsCorrection(nsToken(), nsEmail(), true, (list, err) -> {
            if (!"fix".equals(currentPage)) return;
            c.removeAllViews();
            c.addView(head);
            if ("AUTH".equals(err)) { markSessionExpired(); showPage("daw"); promptLogin(L.t("Twoja sesja NewSpeech wygasła. Zaloguj się ponownie.")); return; }
            if (err != null && list.isEmpty()) {
                if (fixListCache != null) fillFixList(c, fixListCache);
                TextView er = Ui.text(this, "📡 " + err + L.t(" — sprawdź internet (↻ odśwież)"), 12f, R.color.pr_warn);
                c.addView(er, 1);
                return;
            }
            fixListCache = list;
            fixListCacheAt = System.currentTimeMillis();
            fillFixList(c, list);
            TextView upd = Ui.text(this, L.t("Zaktualizowano ") + new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()) + L.t(" · odświeża się co minutę"), 10f, R.color.pr_muted);
            upd.setGravity(android.view.Gravity.CENTER);
            c.addView(upd);
        });
    }

    // Automatyczne odswiezanie listy KOREKTA co 60 s, dopoki zakladka jest otwarta
    private final Runnable fixAutoRefresh = () -> { if ("fix".equals(currentPage) && isLoggedIn()) renderFixPage(true); };

    private void scheduleFixAutoRefresh() {
        redrawHandler.removeCallbacks(fixAutoRefresh);
        redrawHandler.postDelayed(fixAutoRefresh, 60000);
    }

    private void fillFixList(android.widget.LinearLayout c, java.util.List<NsClient.FixEntry> list) {
        float d = getResources().getDisplayMetrics().density;
        if (list.isEmpty()) {
            TextView e = Ui.text(this, L.t("Brak nagrań do poprawy. Wszystko zaliczone albo czeka na pierwszą ocenę."), 14f, R.color.pr_muted);
            e.setPadding(0, (int) (20 * d), 0, 0);
            c.addView(e);
            return;
        }
        int shown = Math.min(fixShowCount, list.size());
        if (list.size() > 10) {
            TextView info = Ui.text(this, L.t("Pokazuję ") + shown + " z " + list.size() + L.t(" — od najstarszych"), 11f, R.color.pr_muted);
            info.setGravity(android.view.Gravity.CENTER);
            info.setPadding(0, 0, 0, (int) (10 * d));
            c.addView(info);
        }
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("d.MM.yyyy", Locale.getDefault());
        for (int i = 0; i < shown; i++) {
            NsClient.FixEntry fe = list.get(i);
            android.widget.LinearLayout card = Ui.card(this);
            card.setBackground(Ui.rounded(getResources().getColor(R.color.pr_card), getResources().getColor(R.color.pr_warn), d, 14 * d));
            android.widget.LinearLayout top = Ui.row(this);
            TextView cat = Ui.text(this, L.cat(fe.categoryName), 15f, R.color.pr_text);
            cat.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            top.addView(cat, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView pill = Ui.text(this, L.t("nie zaliczone"), 10f, R.color.pr_warn);
            pill.setPadding((int) (8 * d), (int) (2 * d), (int) (8 * d), (int) (2 * d));
            pill.setBackground(Ui.rounded((getResources().getColor(R.color.pr_warn) & 0x00FFFFFF) | 0x26000000, 0, 0, 6 * d));
            top.addView(pill);
            card.addView(top);
            if (fe.reviewedAt > 0) {
                TextView dt = Ui.text(this, df.format(new java.util.Date(fe.reviewedAt * 1000L)), 11f, R.color.pr_muted);
                dt.setPadding(0, (int) (4 * d), 0, (int) (6 * d));
                card.addView(dt);
            }
            if (fe.allGood) {
                card.addView(Ui.text(this, L.t("Wszystkie elementy techniki ocenione dobrze, ale całość nie została zaliczona. Częsty powód: nagranie było słabej jakości (zbyt cicho, szum, przerwa) i trener nie mógł go w pełni ocenić. Spróbuj nagrać jeszcze raz w spokojniejszym miejscu."), 12f, R.color.pr_muted));
            } else if (!fe.weakText.isEmpty()) {
                card.addView(Ui.text(this, L.t("Słabe elementy:"), 11f, R.color.pr_muted));
                TextView wk = Ui.text(this, fe.weakText, 12f, R.color.pr_warn);
                wk.setPadding(0, (int) (2 * d), 0, 0);
                card.addView(wk);
            }
            card.addView(Ui.spacer(this, 10));
            Button rec = Ui.button(this, L.t("🎤 Nagraj poprawkę"), R.color.pr_accent, true);
            rec.setOnClickListener(v -> startFixRecording(fe.id, fe.categoryName));
            card.addView(rec, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            c.addView(card);
        }
        if (shown < list.size()) {
            Button more = Ui.button(this, L.t("Pokaż kolejne ") + Math.min(10, list.size() - shown), R.color.pr_accent, false);
            more.setOnClickListener(v -> { fixShowCount += 10; renderFixPage(false); });
            c.addView(more);
        }
    }

    // Tryb poprawki: zapamietany w ustawieniach (z czasem), zeby przetrwal np. zamkniecie
    // aplikacji w trakcie nagrywania. Starszy niz 3 h jest ignorowany (bezpiecznik jak w PWA).
    private void startFixRecording(String recordId, String catName) {
        prefs().edit().putString("fix_record_id", recordId).putString("fix_cat", catName)
                .putLong("fix_saved_at", System.currentTimeMillis()).apply();
        showPage("daw");
        setStatus(L.t("🔄 Nagrywasz poprawkę dla: ") + catName + L.t("  (dotknij, aby anulować)"));
        Toast.makeText(this, L.t("Nagraj poprawkę — REC"), Toast.LENGTH_SHORT).show();
    }

    private String[] loadFixTarget() {
        android.content.SharedPreferences p = prefs();
        String id = p.getString("fix_record_id", null);
        if (id == null || id.isEmpty()) return null;
        if (System.currentTimeMillis() - p.getLong("fix_saved_at", 0L) > 3 * 60 * 60 * 1000L) { clearFixTarget(); return null; }
        return new String[]{id, p.getString("fix_cat", "")};
    }

    private void clearFixTarget() {
        prefs().edit().remove("fix_record_id").remove("fix_cat").remove("fix_saved_at").apply();
    }

    private android.widget.LinearLayout tile(String icon, String label, int colorRes, View.OnClickListener l) {
        android.widget.LinearLayout t = Ui.card(this);
        t.setGravity(android.view.Gravity.CENTER);
        TextView i = Ui.text(this, icon, 26f, R.color.pr_text);
        i.setGravity(android.view.Gravity.CENTER);
        t.addView(i);
        TextView lb = Ui.text(this, label, 11f, colorRes);
        lb.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        lb.setGravity(android.view.Gravity.CENTER);
        t.addView(lb);
        t.setOnClickListener(l);
        return t;
    }

    public interface BoolConsumer { void accept(boolean b); }

    private android.widget.LinearLayout toggleRow(String text, boolean initial, BoolConsumer onChange) {
        android.widget.LinearLayout r = Ui.row(this);
        r.setPadding(0, (int) Ui.dp(this, 6), 0, (int) Ui.dp(this, 6));
        TextView t = Ui.text(this, text, 13f, R.color.pr_text);
        r.addView(t, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final boolean[] state = {initial};
        Button b = Ui.button(this, initial ? "ON" : "OFF", initial ? R.color.pr_accent : R.color.pr_muted, initial);
        b.setMinWidth((int) Ui.dp(this, 58));
        b.setMinimumWidth((int) Ui.dp(this, 58));
        b.setOnClickListener(v -> {
            state[0] = !state[0];
            onChange.accept(state[0]);
            renderSettingsPage();
        });
        r.addView(b);
        return r;
    }

    private String[] catLabels() {
        String[] l = new String[NsClient.CATEGORIES.length];
        for (int i = 0; i < l.length; i++) l[i] = L.cat(NsClient.CATEGORIES[i]);
        return l;
    }

    private void comingSoon(String name) {
        new AlertDialog.Builder(this).setTitle(name)
                .setMessage(name + L.t(" — w przygotowaniu, pojawi się w kolejnej wersji aplikacji."))
                .setPositiveButton(getString(R.string.btn_close), null).show();
    }

    private String appVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }

    // ── ZGODA NA LOKALIZACJE (GPS jak w PitchRec) ──
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    public static void requestLocationPermission(android.app.Activity a) {
        ActivityCompat.requestPermissions(a, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    // Buduje jeden, estetyczny wiersz "Etykieta: [kwadrat koloru]" — zamiast etykiety i
    // kwadratu na osobnych, pelnej-szerokosci wierszach (co wygladalo niechlujnie, z
    // duza iloscia pustej przestrzeni). Kwadrat ma zaokraglone rogi + obramowanie.
    private void addColorRow(android.widget.LinearLayout container, TextView label, int initialColor, java.util.function.IntConsumer onColorChange) {
        float density = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, (int) (4 * density), 0, (int) (12 * density));

        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(labelParams);
        row.addView(label);

        View swatch = new View(this);
        android.graphics.drawable.GradientDrawable swatchBg = new android.graphics.drawable.GradientDrawable();
        swatchBg.setColor(initialColor);
        swatchBg.setCornerRadius(6 * density);
        swatchBg.setStroke((int) density, getResources().getColor(R.color.pr_border));
        swatch.setBackground(swatchBg);
        int swatchSize = (int) (36 * density);
        swatch.setLayoutParams(new android.widget.LinearLayout.LayoutParams(swatchSize, swatchSize));
        swatch.setOnClickListener(v -> showColorGridPicker(color -> {
            onColorChange.accept(color);
            swatchBg.setColor(color);
        }));
        row.addView(swatch);

        container.addView(row);
    }

    private boolean keepScreenOnEnabled = true;

    private void applyKeepScreenOnSetting() {
        if (keepScreenOnEnabled) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void startRecordingFlow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            return;
        }

        // GPS jak w PitchRec: pozycja z czasu NAGRYWANIA (zapisywana w opisie i nazwie pliku)
        GpsHelper.recordingFix = null;
        if (GpsHelper.hasPermission(this)) {
            GpsHelper.requestFix(this, loc -> { if (loc != null) GpsHelper.recordingFix = loc; });
        } else if (!prefs().getBoolean("location_asked", false)) {
            prefs().edit().putBoolean("location_asked", true).apply();
            requestLocationPermission(this);
        }

        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_START);
        intent.putExtra(BackgroundRecorderService.EXTRA_FORMAT, selectedFormat);

        // Zatrzymujemy jakiekolwiek trwajace odtwarzanie — bez tego, stara petla
        // aktualizujaca pozycje odtwarzacza mogla nadpisywac wyswietlacz czasu nowego
        // nagrania (walka o ten sam TextView).
        if (currentPlayer != null) {
            try { currentPlayer.release(); } catch (Exception e) { /* ignorowane */ }
            currentPlayer = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isRecording = true;
        isPaused = false;
        recordingStartedAtMs = System.currentTimeMillis();
        lastDisplayedSecond = -1L;
        pausedAccumMs = 0L;
        lastResumeAtMs = recordingStartedAtMs;
        recordButton.setButtonText(getString(R.string.btn_pause));
        pauseButton.setButtonEnabled(true);
        pauseButton.setButtonText(getString(R.string.btn_stop));
        playButton.setButtonEnabled(false);
        pitchWaveView.setLiveMode(true);
        // Podczas nagrywania zoom zablokowany na 8s — zapobiega przypadkowej zmianie
        // widoku w trakcie mowienia.
        zoomSlider.setEnabled(false);
        zoomSlider.setValue(0.12f);
        zoomValueText.setText("8s");
        pitchWaveView.setZoomSeconds(8f);
        statusText.setText(getString(R.string.status_recording));
        String[] fxRec = loadFixTarget();
        if (fxRec != null) statusText.setText(L.t("🔴 Nagrywasz poprawkę dla: ") + fxRec[1]);
        headerStatus.setText(L.t("● NAGRYWA"));
        headerStatus.setTextColor(getResources().getColor(R.color.pr_warn));
        pitchWaveView.postOnAnimation(redrawLoop);
    }

    private void stopRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_STOP);
        startService(intent);
        isRecording = false;
        isPaused = false;
        recordButton.setButtonText(getString(R.string.btn_rec));
        pauseButton.setButtonEnabled(false);
        zoomSlider.setEnabled(true);
        statusText.setText(getString(R.string.status_processing));
        headerStatus.setText(L.t("● ZAPIS…"));
        headerStatus.setTextColor(getResources().getColor(R.color.pr_accent));
    }

    private long pauseStartedAtMs = 0L;

    private void pauseRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_PAUSE);
        startService(intent);
        isPaused = true;
        pauseStartedAtMs = System.currentTimeMillis(); // zapamiętujemy KIEDY zaczela sie pauza
        recordButton.setButtonText(getString(R.string.btn_resume));
        statusText.setText(getString(R.string.status_paused));
        headerStatus.setText(L.t("● PAUZA"));
        headerStatus.setTextColor(getResources().getColor(R.color.pr_pause));
        playButton.setButtonEnabled(true);
        pitchWaveView.pauseKeepingPosition(); // zachowuje pozycje, nie skacze do poczatku
    }

    private void resumeRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_RESUME);
        startService(intent);
        isPaused = false;
        // POPRAWKA: dopisujemy do pausedAccumMs FAKTYCZNY czas spedzony w pauzie (teraz
        // minus kiedy pauza zaczela sie) — wczesniej blad dodawal tu czas AKTYWNEGO
        // nagrywania (od ostatniego wznowienia), co bylo odwrotnoscia tego co potrzebne.
        pausedAccumMs += System.currentTimeMillis() - pauseStartedAtMs;
        recordButton.setButtonText(getString(R.string.btn_pause));
        statusText.setText(getString(R.string.status_recording));
        headerStatus.setText(L.t("● NAGRYWA"));
        headerStatus.setTextColor(getResources().getColor(R.color.pr_warn));
        playButton.setButtonEnabled(false);
        pitchWaveView.resetPan(); // czysci biala linie (playhead) — niepotrzebna podczas nagrywania na zywo
        pitchWaveView.setLiveMode(true); // wraca do auto-przewijania najnowszych probek
    }

    private void resetRecording() {
        if (isRecording) stopRecordingFlow();
        LiveAudioData.reset();
        pitchWaveView.invalidate();
        timeText.setText("00:00:00");
        statusText.setText(getString(R.string.status_ready));
    }

    private long lastDisplayedSecond = -1L;

    private void updateTimeDisplay() {
        if (isPaused) return; // ZAMROZONY czas podczas pauzy — bez tego elapsedMs rosl
                               // dalej, bo pausedAccumMs byl dodawany tylko RAZ przy pauzie,
                               // nie kompensowal biezaco upływajacego czasu w pauzie.
        long elapsedMs = System.currentTimeMillis() - recordingStartedAtMs - pausedAccumMs;
        long totalSec = elapsedMs / 1000;
        if (totalSec == lastDisplayedSecond) return; // bez zmiany — pomijamy String.format
        lastDisplayedSecond = totalSec;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        timeText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
    }

    private long lastLevelMeterUpdateMs = 0L;

    private void updateLevelMeter() {
        long now = System.currentTimeMillis();
        if (now - lastLevelMeterUpdateMs < 100) return; // co ~100ms wystarczy dla miernika
        lastLevelMeterUpdateMs = now;
        float[] recent = LiveAudioData.snapshotEnvelopeTail(5);
        float maxLevel = 0f;
        for (float v : recent) if (v > maxLevel) maxLevel = v;
        levelMeter.setProgress((int) Math.min(100, maxLevel * 100));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (GpsHelper.hasPermission(this)) GpsHelper.requestFix(this, null);
            if ("set".equals(currentPage)) renderSettingsPage();
            return;
        }
        if (requestCode == REQUEST_MIC_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecordingFlow();
        } else {
            Toast.makeText(this, L.t("Brak zgody na mikrofon"), Toast.LENGTH_SHORT).show();
        }
    }

    private long lastRecordingTotalDurationMs = 0L;

    @Override
    public void onSuccess(String base64, long durationMs, String mimeType) {
        runOnUiThread(() -> {
            statusText.setText(L.t("Nagrano ") + (durationMs / 1000) + L.t("s — zapisano"));
            saveRecordingForPlayback(base64, mimeType);
            loadedFilePath = null; // swiezo nagrany plik jest juz w LiveAudioData, nie trzeba wczytywac ponownie
            pendingSeekSample = 0L;
            lastRecordingTotalDurationMs = durationMs;
            timeText.setText("00:00 / " + formatMs(durationMs)); // jak odtwarzacz: pozycja/calkowita dlugosc
            pitchWaveView.setLiveMode(false); // pozwala przewijac/wskazac miejsce w tym co wlasnie nagrano
            pitchWaveView.resetPan();
            pitchWaveView.invalidate();
            playButton.setButtonEnabled(true);
            headerStatus.setText(L.t("● GOTOWY"));
            headerStatus.setTextColor(getResources().getColor(R.color.pr_muted));
            // Jak w PitchRec: po STOP od razu ekran "ZAPISZ NAGRANIE" (opis nagrania)
            if (lastSavedFilePath != null) describeNewRecording(new File(lastSavedFilePath));
        });
    }

    private String formatMs(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    @Override
    public void onError(String code, String message) {
        runOnUiThread(() -> statusText.setText(L.t("Błąd: ") + code + " — " + message));
    }

    private void saveRecordingForPlayback(String base64, String mimeType) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            String ext = "audio/mpeg".equals(mimeType) ? ".mp3" : ".wav";
            File savedFile = new File(getFilesDir(), "recording_" + System.currentTimeMillis() + ext);
            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                fos.write(bytes);
            }
            lastSavedFilePath = savedFile.getAbsolutePath();
        } catch (IOException e) {
            statusText.setText(L.t("Błąd zapisu: ") + e.getMessage());
        }
    }

    private MediaPlayer currentPlayer;
    private String loadedFilePath = null;

    // Odtworzenie fragmentu nagrania PODCZAS PAUZY (przed zatrzymaniem/zapisaniem) — działa
    // dla WAV (surowe PCM, łatwo dorobić poprawny nagłówek na podstawie aktualnego rozmiaru
    // pliku). Dla MP3 podgląd w trakcie pauzy nie jest wspierany (częściowy strumień MP3 nie
    // jest łatwo bezpiecznie odtwarzalny w środku nagrywania) — trzeba zatrzymać nagranie.
    private void previewDuringPause(long sampleIndex) {
        String path = com.pitchrec.backgroundrecorder.BackgroundRecorderService.currentOutputFilePath;
        String format = com.pitchrec.backgroundrecorder.BackgroundRecorderService.currentOutputFormat;
        if (path == null) return;

        if ("mp3".equals(format)) {
            Toast.makeText(this, L.t("Podgląd MP3 podczas pauzy nie jest wspierany — zatrzymaj nagranie"), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            File source = new File(path);
            File tempCopy = new File(getCacheDir(), "preview_temp.wav");
            copyWavWithFixedHeader(source, tempCopy);
            playFile(tempCopy.getAbsolutePath(), sampleIndex);
        } catch (IOException e) {
            statusText.setText(L.t("Błąd podglądu: ") + e.getMessage());
        }
    }

    // Kopiuje aktualną (jeszcze niekompletną) treść pliku WAV, dopisując POPRAWNY nagłówek
    // bazujący na FAKTYCZNYM, biezacym rozmiarze danych — oryginał ma na razie tylko
    // placeholder (zera), poprawiany dopiero przy Stop.
    private void copyWavWithFixedHeader(File source, File dest) throws IOException {
        long fileLen = source.length();
        long dataSize = Math.max(0, fileLen - 44);
        try (java.io.RandomAccessFile in = new java.io.RandomAccessFile(source, "r");
             java.io.RandomAccessFile out = new java.io.RandomAccessFile(dest, "rw")) {
            out.setLength(0);
            long totalDataLen = dataSize + 36;
            out.writeBytes("RIFF");
            writeIntLE(out, (int) totalDataLen);
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            writeIntLE(out, 16);
            writeShortLE(out, (short) 1);
            writeShortLE(out, (short) 1);
            writeIntLE(out, LiveAudioData.SAMPLE_RATE);
            writeIntLE(out, LiveAudioData.SAMPLE_RATE * 2);
            writeShortLE(out, (short) 2);
            writeShortLE(out, (short) 16);
            out.writeBytes("data");
            writeIntLE(out, (int) dataSize);

            in.seek(44);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void writeIntLE(java.io.RandomAccessFile raf, int value) throws IOException {
        raf.write(value & 0xff);
        raf.write((value >> 8) & 0xff);
        raf.write((value >> 16) & 0xff);
        raf.write((value >> 24) & 0xff);
    }

    private void writeShortLE(java.io.RandomAccessFile raf, short value) throws IOException {
        raf.write(value & 0xff);
        raf.write((value >> 8) & 0xff);
    }

    // Wczytuje plik z powrotem do wykresu (fala + pitch), przełącza widok w tryb statyczny
    // (przewijalny palcem, z możliwością wskazania miejsca odtwarzania).
    private void loadAndDisplayFile(File file) {
        try {
            AudioFileLoader.loadIntoLiveData(file);
            loadedFilePath = file.getAbsolutePath();
            pitchWaveView.setLiveMode(false);
            pitchWaveView.resetPan();
            pitchWaveView.invalidate();
            statusText.setText(L.t("Wczytano nagranie"));
        } catch (IOException e) {
            statusText.setText(L.t("Błąd wczytywania: ") + e.getMessage());
        }
    }

    // startSampleIndex: pozycja (w próbkach), od której ma zacząć się odtwarzanie — 0 =
    // od początku. Odpowiada dotknięciu wykresu (suwak odtwarzania).
    private void playFile(String path, long startSampleIndex) {
        try {
            if (currentPlayer != null) {
                currentPlayer.release();
                currentPlayer = null;
            }
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(path);
            player.setOnCompletionListener(mp -> {
                mp.release();
                currentPlayer = null;
                playButton.setButtonText(getString(R.string.btn_play));
            });
            player.prepare();
            int startMs = (int) (startSampleIndex * 1000L / LiveAudioData.SAMPLE_RATE);
            currentPlayer = player;
            if (startMs > 0) {
                // Niektóre urządzenia nie przewijają poprawnie, jeśli start() jest wołane
                // natychmiast po seekTo() — czekamy na potwierdzenie zakończenia przewijania.
                player.setOnSeekCompleteListener(mp -> {
                    mp.start();
                    statusText.setText(getString(R.string.status_playing));
                    playButton.setButtonText(getString(R.string.btn_pause_play));
                    startPlayheadUpdateLoop();
                });
                player.seekTo(startMs);
            } else {
                player.start();
                statusText.setText(getString(R.string.status_playing));
                playButton.setButtonText(getString(R.string.btn_pause_play));
                startPlayheadUpdateLoop();
            }
        } catch (IOException e) {
            statusText.setText(L.t("Błąd odtwarzania: ") + e.getMessage());
        }
    }

    // Przesuwa suwak (biala linia) w takt aktualnej pozycji odtwarzacza.
    private final Runnable playheadUpdateLoop = new Runnable() {
        @Override
        public void run() {
            if (currentPlayer != null) {
                try {
                    int posMs = currentPlayer.getCurrentPosition();
                    long sample = (long) posMs * LiveAudioData.SAMPLE_RATE / 1000L;
                    pitchWaveView.setPlayheadSample(sample);
                    timeText.setText(formatMs(posMs) + " / " + formatMs(lastRecordingTotalDurationMs));
                } catch (IllegalStateException e) { /* odtwarzacz mogl sie juz zwolnic */ }
                redrawHandler.postDelayed(this, 50);
            }
        }
    };

    private void startPlayheadUpdateLoop() {
        redrawHandler.post(playheadUpdateLoop);
    }

    // ── STRONA NAGRANIA (jak #pg-recs w PitchRec) ──
    private static final int REQUEST_IMPORT_FILE = 200;

    private void showRecordingsList() {
        if ("recs".equals(currentPage)) renderRecsPage(); else showPage("recs");
    }

    private void renderRecsPage() {
        android.widget.LinearLayout c = findViewById(R.id.recsContent);
        if (c == null) return;
        c.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        File[] files = getFilesDir().listFiles((dir, name) -> name.endsWith(".wav") || name.endsWith(".mp3"));
        if (files == null) files = new File[0];
        java.util.Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        final File[] finalFiles = files;

        // Naglowek: ← NAGRANIA + ☁ Wyslij wszystkie
        android.widget.LinearLayout head = Ui.row(this);
        TextView back = Ui.text(this, "←", 22f, R.color.pr_muted);
        back.setPadding(0, 0, (int) (12 * d), 0);
        back.setOnClickListener(v -> showPage("daw"));
        head.addView(back);
        TextView title = pageTitle(L.t("NAGRANIA"));
        title.setPadding(0, 0, 0, 0);
        head.addView(title, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (isLoggedIn()) {
            Button sendAll = Ui.button(this, L.t("☁ Wyślij wszystkie"), R.color.pr_purple, false);
            sendAll.setOnClickListener(v -> sendAllToNs(finalFiles));
            head.addView(sendAll);
        }
        c.addView(head);
        c.addView(Ui.spacer(this, 10));

        android.widget.LinearLayout actions = Ui.row(this);
        Button dlAll = Ui.button(this, L.t("⬇ Pobierz wszystkie"), R.color.pr_accent, false);
        dlAll.setOnClickListener(v -> shareAllRecordings(finalFiles));
        actions.addView(dlAll, Ui.weight(1f, 8 * d));
        Button imp = Ui.button(this, L.t("📁 Wgraj plik"), R.color.pr_warn, false);
        imp.setOnClickListener(v -> importExternalFile());
        actions.addView(imp, Ui.weight(1f, 0));
        c.addView(actions);
        c.addView(Ui.spacer(this, 14));

        if (finalFiles.length == 0) {
            TextView empty = Ui.text(this, getString(R.string.recordings_empty), 13f, R.color.pr_muted);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, (int) (30 * d), 0, 0);
            c.addView(empty);
            return;
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("d.M HH:mm", Locale.getDefault());
        for (File f : finalFiles) c.addView(buildRecordingCard(f, fmt));
    }

    // Karta nagrania = .rec-item z PitchRec: nazwa + kategoria (albo "bez opisu") + chmurka NS,
    // notatka + data, przyciski: [📝 Opisz (i wyslij) | ☁ Wyslij NS] [▶ DAW] [⬇] [✕ Usun].
    private android.widget.LinearLayout buildRecordingCard(File file, java.text.SimpleDateFormat fmt) {
        float d = getResources().getDisplayMetrics().density;
        RecMeta meta = RecMeta.load(this, file.getName());
        boolean described = !meta.cat.isEmpty();
        boolean logged = isLoggedIn();

        android.widget.LinearLayout card = Ui.card(this);

        android.widget.LinearLayout top = Ui.row(this);
        TextView name = Ui.text(this, file.getName(), 13f, R.color.pr_text);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        top.addView(name, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView cat = Ui.text(this, (meta.fixRecordId.isEmpty() ? "" : L.t("🔄 poprawka · ")) + (described ? L.cat(meta.cat) : L.t("bez opisu")), 10f, described ? R.color.pr_purple : R.color.pr_pause);
        cat.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cat.setPadding((int) (7 * d), (int) (2 * d), (int) (7 * d), (int) (2 * d));
        int catColor = getResources().getColor(described ? R.color.pr_purple : R.color.pr_pause);
        cat.setBackground(Ui.rounded((catColor & 0x00FFFFFF) | 0x22000000, 0, 0, 5 * d));
        android.widget.LinearLayout.LayoutParams catLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        catLp.leftMargin = (int) (8 * d);
        top.addView(cat, catLp);

        String cloud = "sent".equals(meta.ns) ? "☁✓" : "error".equals(meta.ns) ? "☁✗" : "☁";
        int cloudCol = "sent".equals(meta.ns) ? R.color.pr_accent : "error".equals(meta.ns) ? R.color.pr_warn : R.color.pr_muted;
        TextView ns = Ui.text(this, cloud, 16f, cloudCol);
        ns.setPadding((int) (8 * d), 0, 0, 0);
        top.addView(ns);
        card.addView(top);

        android.widget.LinearLayout metaRow = Ui.row(this);
        metaRow.setPadding(0, (int) (4 * d), 0, 0);
        String noteTxt = meta.note;
        if (!meta.name.isEmpty()) noteTxt = meta.name + (noteTxt.isEmpty() ? "" : " — " + noteTxt);
        TextView note = Ui.text(this, noteTxt, 11f, R.color.pr_muted);
        note.setMaxLines(1);
        note.setEllipsize(android.text.TextUtils.TruncateAt.END);
        metaRow.addView(note, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        metaRow.addView(Ui.text(this, fmt.format(new java.util.Date(file.lastModified())), 10f, R.color.pr_muted));
        card.addView(metaRow);

        android.widget.LinearLayout btns = Ui.row(this);
        btns.setPadding(0, (int) (10 * d), 0, 0);
        if (!described) {
            Button desc = Ui.button(this, logged ? L.t("📝 Opisz i wyślij") : L.t("📝 Opisz"), R.color.pr_pause, true);
            desc.setOnClickListener(v -> describeExisting(file, logged));
            btns.addView(desc, Ui.weight(1.3f, 6 * d));
        } else if (logged) {
            Button send = Ui.button(this, "sent".equals(meta.ns) ? L.t("☁ Wyślij ponownie") : L.t("☁ Wyślij NS"), R.color.pr_purple, false);
            send.setOnClickListener(v -> sendToNs(file));
            btns.addView(send, Ui.weight(1.3f, 6 * d));
        }
        Button daw = Ui.button(this, L.t("▶ DAW"), R.color.pr_purple, false);
        daw.setOnClickListener(v -> {
            // Tylko wczytanie do DAW (bez automatycznego odtwarzania) — PLAY uruchamia recznie
            showPage("daw");
            loadAndDisplayFile(file);
            pendingSeekSample = 0L;
            playButton.setButtonEnabled(true);
        });
        btns.addView(daw, Ui.weight(1f, 6 * d));
        Button dl = Ui.button(this, "⬇", R.color.pr_accent, false);
        dl.setOnClickListener(v -> shareRecording(file));
        btns.addView(dl, Ui.weight(0.5f, 6 * d));
        Button del = Ui.button(this, L.t("✕ Usuń"), R.color.pr_warn, false);
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(file.getName())
                .setPositiveButton(getString(R.string.btn_delete), (d2, w2) -> {
                    file.delete();
                    RecMeta.delete(this, file.getName());
                    Toast.makeText(this, getString(R.string.deleted_toast), Toast.LENGTH_SHORT).show();
                    renderRecsPage();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show());
        btns.addView(del, Ui.weight(1f, 0));
        card.addView(btns);
        // Opisane nagranie mozna tez edytowac — dotkniecie nazwy otwiera opis
        name.setOnClickListener(v -> describeExisting(file, false));
        return card;
    }

    // Buduje przycisk z obramowaniem (bez wypelnienia) — dokladnie jak przyciski w karcie
    // nagrania ze wzorca (Wyslij NS / DAW / Usun).
    private Button makeOutlinedButton(String text, int colorRes, float density) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setTextSize(12f);
        btn.setPadding((int) (4 * density), (int) (12 * density), (int) (4 * density), (int) (12 * density));
        int color = getResources().getColor(colorRes);
        btn.setTextColor(color);

        android.graphics.drawable.GradientDrawable normalBg = new android.graphics.drawable.GradientDrawable();
        normalBg.setColor(0x00000000);
        normalBg.setCornerRadius(8 * density);
        normalBg.setStroke((int) density, color);

        // Stan "wcisniety" — wypelnienie kolorem przycisku (przezroczyste), jak prawdziwy,
        // fizyczny przycisk reagujacy na dotyk.
        android.graphics.drawable.GradientDrawable pressedBg = new android.graphics.drawable.GradientDrawable();
        pressedBg.setColor((color & 0x00FFFFFF) | 0x40000000);
        pressedBg.setCornerRadius(8 * density);
        pressedBg.setStroke((int) density, color);

        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        states.addState(new int[]{}, normalBg);
        btn.setBackground(states);
        return btn;
    }

    // Import zewnetrznego pliku audio (jak "Wgraj plik" w PitchRec) — otwiera systemowy
    // wybornik plikow, kopiuje wybrany plik do folderu nagran.
    // Odczytuje prawdziwa nazwe pliku z content:// URI (standardowy sposob na Androidzie —
    // sam URI zwykle nie zawiera czytelnej nazwy, trzeba zapytac ContentResolver).
    private String getDisplayNameFromUri(android.net.Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception e) { /* ignorowane, uzyjemy nazwy zastepczej */ }
        return null;
    }

    private void importExternalFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_IMPORT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_FILE && data != null && data.getData() != null) {
            try {
                android.net.Uri sourceUri = data.getData();
                String originalName = getDisplayNameFromUri(sourceUri);
                if (originalName == null || originalName.trim().isEmpty()) {
                    originalName = "import_" + System.currentTimeMillis() + ".wav";
                }
                // Zachowujemy oryginalna nazwe pliku (nie zmieniamy na "recording_...")
                File destFile = new File(getFilesDir(), originalName);
                // Jesli plik o tej nazwie juz istnieje, dopisz numer, zeby nie nadpisac.
                int counter = 1;
                while (destFile.exists()) {
                    String base = originalName.replaceAll("\\.(wav|mp3)$", "");
                    String ext = originalName.endsWith(".mp3") ? ".mp3" : ".wav";
                    destFile = new File(getFilesDir(), base + "_" + counter + ext);
                    counter++;
                }
                try (java.io.InputStream in = getContentResolver().openInputStream(sourceUri);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while (in != null && (read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                Toast.makeText(this, getString(R.string.imported_toast, destFile.getName()), Toast.LENGTH_SHORT).show();
                showRecordingsList();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.import_error_toast, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Udostepnia WSZYSTKIE nagrania naraz (odpowiednik "Pobierz wszystkie" z PitchRec) —
    // otwiera systemowy wybornik z wieloma plikami do wyslania/zapisania.
    private void shareAllRecordings(File[] files) {
        if (files.length == 0) {
            Toast.makeText(this, L.t("Brak nagrań"), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            java.util.ArrayList<android.net.Uri> uris = new java.util.ArrayList<>();
            for (File f : files) {
                uris.add(androidx.core.content.FileProvider.getUriForFile(
                        this, "com.pitchrec.nativetest.fileprovider", f));
            }
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("audio/*");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, L.t("Udostępnij wszystkie nagrania")));
        } catch (Exception e) {
            Toast.makeText(this, L.t("Błąd: ") + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareRecording(File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.pitchrec.nativetest.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(file.getName().endsWith(".mp3") ? "audio/mpeg" : "audio/wav");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, L.t("Udostępnij nagranie")));
        } catch (Exception e) {
            Toast.makeText(this, L.t("Błąd udostępniania: ") + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
