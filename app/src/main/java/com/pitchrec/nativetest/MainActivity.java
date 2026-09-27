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
        // Przypomnienie o 20:00 (nagranie + dziennik) — domyslnie wlaczone
        ReminderReceiver.schedule(this);
        askNotificationPermissionOnce();
        openPageFromIntent(getIntent());
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openPageFromIntent(intent);
    }

    // Powiadomienie "przypomnienie" otwiera DAW (brak nagrania) albo dziennik
    private void openPageFromIntent(Intent in) {
        if (in == null) return;
        String pg = in.getStringExtra("open_page");
        if (pg == null) return;
        in.removeExtra("open_page");
        if ("diary".equals(pg) && isLoggedIn()) showPage("diary"); else showPage("daw");
    }

    private void askNotificationPermissionOnce() {
        if (android.os.Build.VERSION.SDK_INT < 33 || !ReminderReceiver.enabled(this)) return;
        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) return;
        if (prefs().getBoolean("notif_asked", false)) return;
        prefs().edit().putBoolean("notif_asked", true).apply();
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 7301);
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
                loadProfileFromNs(true);
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
                String ph = prefs().getString("map_phone", "");
                status.setText(L.t("✓ Zalogowano: ") + nsEmail() + (ph.isEmpty() ? "" : "\n📞 " + ph));
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
    // ID rekordu w NS — pozniej po nim rozpoznajemy system mowy przy poprawce
    private void rememberNsId(File f, String body) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            if (o.optJSONObject("record") != null) o = o.optJSONObject("record");
            String id = o.optString("id", "");
            if (id.isEmpty()) return;
            RecMeta m = RecMeta.load(this, f.getName());
            m.nsRecordId = id;
            m.save(this, f.getName());
        } catch (Exception e) { }
    }

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
                if (!isFix) rememberNsId(file, r.body);
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
                rememberNsId(f, r.body);
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
            init.fixSys = fix[2];
            banner = L.t("🔄 Nagrywasz poprawkę dla: ") + L.cat(fix[1]) + (fix[2].isEmpty() ? "" : " · " + L.t("system") + " " + fix[2]);
            // zapamietujemy od razu na pliku — nawet "Zamknij" nie zgubi informacji, ze to poprawka
            RecMeta pre = RecMeta.load(this, file.getName());
            pre.fixRecordId = fix[0];
            pre.fixSys = fix[2];
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

    // Profil z NewSpeech (jak nsFetchProfile w PWA): imie i nazwisko -> imie kursanta w opisie
    // nagrania + nazwa na mapie, telefon -> "Chetnie porozmawiam", miasto -> dziennik.
    // force = po swiezym zalogowaniu nadpisujemy imie danymi z konta.
    private void loadMapNameFromNs() { loadProfileFromNs(false); }

    private void loadProfileFromNs(boolean force) {
        if (!isLoggedIn()) return;
        String uid = nsUserId();
        String path = uid.isEmpty() ? "/users/me" : "/users/" + uid;
        NsClient.request("GET", path, nsToken(), nsEmail(), null, null, r -> {
            if (!r.ok) return;
            try {
                org.json.JSONObject d = new org.json.JSONObject(r.body);
                if (d.optJSONObject("user") != null) d = d.optJSONObject("user");
                String full = (d.optString("first_name", "") + " " + d.optString("last_name", "")).trim();
                if (full.isEmpty()) full = d.optString("name", "").trim();
                String phone = d.optString("phone", "").trim();
                if (phone.isEmpty()) phone = d.optString("phone_number", "").trim();
                if ("null".equals(phone)) phone = "";
                String city = d.optString("city", "").trim();
                if ("null".equals(city)) city = "";
                if (uid.isEmpty() && !d.optString("id", "").isEmpty()) prefs().edit().putString("ns_user_id", d.optString("id", "")).apply();
                android.content.SharedPreferences.Editor e = prefs().edit();
                if (!full.isEmpty()) {
                    e.putString("map_name", full);
                    if (force || prefs().getString("student_name", "").isEmpty()) e.putString("student_name", full);
                }
                if (!phone.isEmpty() && (force || prefs().getString("map_phone", "").isEmpty())) e.putString("map_phone", phone);
                if (!city.isEmpty()) e.putString("ns_city", city);
                e.apply();
                if ("set".equals(currentPage)) renderSettingsPage();
            } catch (Exception ex) { }
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
        LiveAudioData.outputBoost = p.getInt("out_boost", 1);
        LiveAudioData.noiseGateEnabled = p.getBoolean("noise_gate", LiveAudioData.noiseGateEnabled);
        keepScreenOnEnabled = p.getBoolean("keep_screen_on", true); // domyslnie ekran NIE gasnie
        selectedFormat = p.getString("format", "mp3"); // domyslnie MP3
        LiveAudioData.pauseMinS = p.getFloat("pause_min", 1.0f);
        LiveAudioData.pauseMaxS = p.getFloat("pause_max", 2.5f);
        LiveAudioData.showPauses = p.getBoolean("show_pauses", true);
        LiveAudioData.showNorms = p.getBoolean("show_norms", true);
        LiveAudioData.showArrows = p.getBoolean("show_arrows", true);
        LiveAudioData.showTempo = p.getBoolean("show_tempo", true);
        LiveAudioData.arrowThresholdSt = p.getFloat("arrow_thr_st", 1.5f);
        Norms.load(p);
        applyKeepScreenOnSetting();
    }

    private static String boostLabel(int b) {
        return "🔊 " + L.t("Głośność wynikowa") + ": " + (b <= 1 ? "0 dB" : "×" + b + "  (+" + Math.round(20 * Math.log10(b)) + " dB)");
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
                .putInt("out_boost", LiveAudioData.outputBoost)
                .putBoolean("noise_gate", LiveAudioData.noiseGateEnabled)
                .putBoolean("keep_screen_on", keepScreenOnEnabled)
                .putString("format", selectedFormat)
                .putFloat("pause_min", LiveAudioData.pauseMinS)
                .putFloat("pause_max", LiveAudioData.pauseMaxS)
                .putBoolean("show_pauses", LiveAudioData.showPauses)
                .putBoolean("show_norms", LiveAudioData.showNorms)
                .putBoolean("show_arrows", LiveAudioData.showArrows)
                .putBoolean("show_tempo", LiveAudioData.showTempo)
                .putFloat("arrow_thr_st", LiveAudioData.arrowThresholdSt)
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
        normBtn.setOnClickListener(v -> renderNormsPage());
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
        // GLOSNOSC WYNIKOWA: 0 dB albo dodatkowe wzmocnienie ×2…×10 (z miekkim limiterem)
        final int[] boosts = {1, 2, 4, 6, 8, 10};
        int bi = 0;
        for (int i = 0; i < boosts.length; i++) if (boosts[i] == LiveAudioData.outputBoost) bi = i;
        TextView boostLbl = Ui.text(this, boostLabel(LiveAudioData.outputBoost), 13f, R.color.pr_text);
        boostLbl.setPadding(0, (int) (8 * d), 0, 0);
        rec.addView(boostLbl);
        SliderView boostSlider = new SliderView(this);
        boostSlider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (36 * d)));
        boostSlider.setValue(bi / 5f);
        boostSlider.setOnValueChangeListener(v -> {
            int k = Math.max(0, Math.min(5, Math.round(v * 5)));
            if (boosts[k] != LiveAudioData.outputBoost) {
                LiveAudioData.outputBoost = boosts[k];
                boostLbl.setText(boostLabel(boosts[k]));
                saveDawSettings();
            }
        });
        rec.addView(boostSlider);
        android.widget.LinearLayout ticks = Ui.row(this);
        for (int i = 0; i < boosts.length; i++) {
            TextView tk = Ui.text(this, i == 0 ? "0 dB" : "×" + boosts[i], 10f, R.color.pr_muted);
            tk.setGravity(i == 0 ? android.view.Gravity.START : i == boosts.length - 1 ? android.view.Gravity.END : android.view.Gravity.CENTER);
            ticks.addView(tk, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        rec.addView(ticks);
        rec.addView(hint(L.t("Gdy nagranie po zapisie jest za ciche — podgłośnij ×2…×10. Najgłośniejsze miejsca są łagodnie ograniczane (bez trzasków).")));
        rec.addView(divider());
        rec.addView(toggleRow("🔆 " + L.t("Ekran nie gaśnie (wyłączony wygaszacz)"), keepScreenOnEnabled, on -> { keepScreenOnEnabled = on; applyKeepScreenOnSetting(); saveDawSettings(); }));
        rec.addView(toggleRow("🎚 " + L.t("Bramka szumów (tłumi cichy szum tła)"), LiveAudioData.noiseGateEnabled, on -> { LiveAudioData.noiseGateEnabled = on; saveDawSettings(); }));
        rec.addView(divider());
        boolean gpsOk = GpsHelper.hasPermission(this);
        View gpsRow = settingRow("📍", L.t("Lokalizacja GPS"), gpsOk ? L.t("zezwolono ✓") : L.t("brak zgody — dotknij, aby zezwolić"), null);
        gpsRow.setOnClickListener(v -> requestLocationPermission(this));
        rec.addView(gpsRow);
        c.addView(rec);

        // 3a) Przypomnienia
        c.addView(sectionHeader(L.t("PRZYPOMNIENIA")));
        android.widget.LinearLayout rem = Ui.card(this);
        rem.addView(toggleRow("🔔 " + L.t("Przypomnienie o 20:00"), ReminderReceiver.enabled(this), on -> {
            prefs().edit().putBoolean("reminder_on", on).apply();
            ReminderReceiver.schedule(this);
            if (on && android.os.Build.VERSION.SDK_INT >= 33
                    && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 7301);
        }));
        rem.addView(hint(L.t("Jeśli do 20:00 nie wgrasz nagrania do NewSpeech albo nie napiszesz dziennika, telefon Ci o tym przypomni.")));
        c.addView(rem);

        // 3b) Podpowiedzi na wykresie i w statystykach
        c.addView(sectionHeader(L.t("PODPOWIEDZI")));
        android.widget.LinearLayout hints = Ui.card(this);
        hints.addView(toggleRow("⏸ " + L.t("Pauzy na wykresie"), LiveAudioData.showPauses, on -> { LiveAudioData.showPauses = on; saveDawSettings(); }));
        hints.addView(toggleRow("🎯 " + L.t("Ocena emisji wg norm"), LiveAudioData.showNorms, on -> { LiveAudioData.showNorms = on; saveDawSettings(); }));
        hints.addView(toggleRow("↗ " + L.t("Strzałki intonacji"), LiveAudioData.showArrows, on -> { LiveAudioData.showArrows = on; saveDawSettings(); }));
        if (LiveAudioData.showArrows) {
            hints.addView(normStepper("   " + L.t("Strzałka od zmiany tonu o"), LiveAudioData.arrowThresholdSt, 0.5f, 6f, 0.5f, "%.1f st", v -> { LiveAudioData.arrowThresholdSt = v; saveDawSettings(); }));
            hints.addView(hint(L.t("st = półton. Mniejsza zmiana tonu w sylabie nie daje strzałki; im większa zmiana, tym bardziej stroma strzałka.")));
        }
        hints.addView(toggleRow("🗣 " + L.t("Pomiar sylab i tempo (sylaby na minutę)"), LiveAudioData.showTempo, on -> { LiveAudioData.showTempo = on; saveDawSettings(); }));
        hints.addView(toggleRow("💡 " + L.t("Inspiracje na dziś (Statystyki)"), prefs().getBoolean("show_insp", true), on -> prefs().edit().putBoolean("show_insp", on).apply()));
        hints.addView(hint(L.t("Podpowiedzi pojawiają się na wykresie w czasie nagrywania i przy odsłuchu.")));
        c.addView(hints);

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
        android.widget.LinearLayout dawCard = Ui.card(this);
        android.widget.LinearLayout dawHead = Ui.row(this);
        TextView dawTitle = Ui.text(this, "🎨  " + L.t("WYGLĄD DAW") + "  ·  " + L.t("kolory i linie"), 13f, R.color.pr_text);
        dawTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dawHead.addView(dawTitle, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        dawHead.addView(Ui.text(this, dawLookOpen ? "▲" : "▼", 14f, R.color.pr_accent));
        dawHead.setPadding(0, (int) (4 * d), 0, (int) (4 * d));
        dawHead.setOnClickListener(v -> { dawLookOpen = !dawLookOpen; renderSettingsPage(); });
        dawCard.addView(dawHead);
        c.addView(dawCard);
        android.widget.LinearLayout daw = new android.widget.LinearLayout(this);
        daw.setOrientation(android.widget.LinearLayout.VERTICAL);
        daw.setPadding(0, (int) (10 * d), 0, 0);
        if (dawLookOpen) dawCard.addView(daw);
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
        String[][] langs = {{"pl", "🇵🇱 Polski"}, {"en", "🇬🇧 English"}, {"cs", "🇨🇿 Čeština"}, {"sk", "🇸🇰 Slovenčina"}, {"de", "🇩🇪 Deutsch"}, {"es", "🇪🇸 Español"}, {"hu", "🇭🇺 Magyar"}};
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

    private boolean dawLookOpen = false;

    // ── NORMY (jak train.html w PitchRec): czasy 4 faz emisji, prog ciszy, tolerancja,
    // stosunek szczyt/cisza; kalibracja z nagrania wzorcowego trenera ──
    private void renderNormsPage() {
        android.widget.LinearLayout c = findViewById(R.id.setContent);
        c.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout head = Ui.row(this);
        Button back = Ui.button(this, "← " + L.t("USTAWIENIA"), R.color.pr_muted, false);
        back.setOnClickListener(v -> renderSettingsPage());
        head.addView(back);
        c.addView(head);
        c.addView(pageTitle("🎯 " + L.t("NORMY")));

        android.widget.LinearLayout info = Ui.card(this);
        info.addView(hint(L.t("Pierwsza sylaba każdej porcji mowy (sylaba 4-fazowa) jest oceniana w 4 fazach: 1) faza leniwa — cichy start, 2) narastanie, 3) szczyt, 4) opadanie — wyciszenie. Dalsze, zwykłe sylaby nie są oceniane wg faz (liczone są do tempa). Wynik pojawia się na wykresie jako „4F …%” i na żywo w czasie nagrywania.")));
        info.addView(new PhaseDiagram(this));
        c.addView(info);

        c.addView(sectionHeader(L.t("CZASY FAZ")));
        android.widget.LinearLayout t = Ui.card(this);
        t.addView(normStepper("1 · " + L.t("Faza leniwa (cichy start)"), Norms.ph1, 0.5f, 3.0f, 0.1f, "%.1f s", v -> Norms.ph1 = v));
        t.addView(normStepper("2 · " + L.t("Narastanie"), Norms.ph2, 0.5f, 4.0f, 0.1f, "%.1f s", v -> Norms.ph2 = v));
        t.addView(normStepper("4 · " + L.t("Opadanie"), Norms.ph3, 0.1f, 1.5f, 0.1f, "%.1f s", v -> Norms.ph3 = v));
        c.addView(t);

        c.addView(sectionHeader(L.t("CZUŁOŚĆ OCENY")));
        android.widget.LinearLayout q = Ui.card(this);
        if (Norms.calibrated) {
            TextView ci = Ui.text(this, "✓ " + L.f("Skalibrowano z {0} próbek wzorca — ocena porównuje kursanta z wzorcem.", Norms.calSamples), 12f, R.color.pr_accent);
            ci.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            ci.setPadding(0, 0, 0, (int) (6 * d));
            q.addView(ci);
        } else {
            q.addView(normStepper(L.t("Próg ciszy na starcie"), Norms.quietThresh * 100, 5, 90, 5, "%.0f%%", v -> Norms.quietThresh = v / 100f));
        }
        q.addView(normStepper(L.t("Tolerancja"), Norms.tolerance * 100, 5, 30, 1, "±%.0f%%", v -> Norms.tolerance = v / 100f));
        if (!Norms.calibrated) q.addView(normStepper(L.t("Szczyt / cisza (wzorzec)"), Norms.pkRatio, 2, 50, 0.5f, "×%.1f", v -> Norms.pkRatio = v));
        q.addView(hint(Norms.shapeRef != null ? "✓ " + L.t("Zapisany kształt wzorca — porównanie podobieństwa jest włączone.") : L.t("Brak wzorca kształtu — nagraj wzorzec, aby porównywać podobieństwo.")));
        c.addView(q);

        c.addView(sectionHeader(L.t("KALIBRACJA Z NAGRANIA")));
        android.widget.LinearLayout cal = Ui.card(this);
        cal.addView(hint(L.t("Nagraj 3–5 wzorcowych porcji mowy (z pauzą przed każdą) — w jednym albo w kilku nagraniach — i wybierz je tutaj. Aplikacja wytnie porcje tak samo jak przy ocenie, wyliczy czasy faz i progi, a próbki wzorca dostaną 100%.")));
        Button pick = Ui.button(this, "🎙 " + L.t("Kalibruj z nagrania"), R.color.pr_accent, true);
        pick.setOnClickListener(v -> pickCalibrationRecording());
        cal.addView(pick, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        c.addView(cal);

        Button reset = Ui.button(this, "↺ " + L.t("Przywróć domyślne"), R.color.pr_warn, false);
        reset.setOnClickListener(v -> { Norms.resetDefaults(); Norms.save(prefs()); renderNormsPage(); });
        android.widget.LinearLayout.LayoutParams rl = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        rl.topMargin = (int) (8 * d);
        c.addView(reset, rl);
        c.addView(Ui.spacer(this, 20));
        View sv = findViewById(R.id.setPage);
        if (sv instanceof android.widget.ScrollView) ((android.widget.ScrollView) sv).scrollTo(0, 0);
    }

    public interface FloatSetter { void set(float v); }

    private android.widget.LinearLayout normStepper(String label, float value, float min, float max, float step, String fmt, FloatSetter setter) {
        android.widget.LinearLayout r = Ui.row(this);
        r.setPadding(0, (int) Ui.dp(this, 4), 0, (int) Ui.dp(this, 4));
        r.addView(Ui.text(this, label, 13f, R.color.pr_text), new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button minus = Ui.button(this, "−", R.color.pr_muted, false);
        Button plus = Ui.button(this, "+", R.color.pr_accent, false);
        TextView val = Ui.text(this, String.format(Locale.US, fmt, value), 14f, R.color.pr_accent);
        val.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        val.setGravity(android.view.Gravity.CENTER);
        final float[] cur = {value};
        View.OnClickListener st = b -> {
            cur[0] = Math.max(min, Math.min(max, Math.round((cur[0] + (b == plus ? step : -step)) / step) * step));
            setter.set(cur[0]);
            Norms.save(prefs());
            val.setText(String.format(Locale.US, fmt, cur[0]));
        };
        minus.setOnClickListener(st);
        plus.setOnClickListener(st);
        int sz = (int) Ui.dp(this, 38);
        r.addView(minus, new android.widget.LinearLayout.LayoutParams(sz, sz));
        r.addView(val, new android.widget.LinearLayout.LayoutParams((int) Ui.dp(this, 70), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(plus, new android.widget.LinearLayout.LayoutParams(sz, sz));
        return r;
    }

    private void pickCalibrationRecording() {
        if (LiveAudioData.isRecordingActive) { Toast.makeText(this, L.t("Zatrzymaj nagrywanie, żeby skalibrować normy"), Toast.LENGTH_SHORT).show(); return; }
        File[] files = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files == null || files.length == 0) { Toast.makeText(this, L.t("Brak nagrań"), Toast.LENGTH_SHORT).show(); return; }
        java.util.Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        int n = Math.min(files.length, 30);
        String[] labels = new String[n];
        boolean[] checked = new boolean[n];
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("d.MM HH:mm:ss", Locale.US);
        for (int i = 0; i < n; i++) {
            RecMeta m = RecMeta.load(this, files[i].getName());
            labels[i] = df.format(new java.util.Date(files[i].lastModified())) + (m.cat.isEmpty() ? "" : " · " + L.cat(m.cat));
        }
        new AlertDialog.Builder(this).setTitle(L.t("Wybierz nagrania wzorcowe (można kilka)"))
                .setMultiChoiceItems(labels, checked, (dlg, which, on) -> checked[which] = on)
                .setPositiveButton(L.t("Kalibruj"), (dlg, w) -> {
                    java.util.List<File> sel = new java.util.ArrayList<>();
                    for (int i = 0; i < n; i++) if (checked[i]) sel.add(files[i]);
                    if (sel.isEmpty()) { Toast.makeText(this, L.t("Nie wybrano nagrań"), Toast.LENGTH_SHORT).show(); return; }
                    calibrateFrom(sel);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null).show();
    }

    private void calibrateFrom(java.util.List<File> sel) {
        Toast.makeText(this, "⏳ " + L.t("Analizuję nagranie…"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            java.util.List<float[]> bufs = new java.util.ArrayList<>();
            java.util.List<Integer> pres = new java.util.ArrayList<>();
            for (File f : sel) {
                try {
                    for (Norms.Segment sg : AudioFileLoader.normSegments(f)) { bufs.add(sg.buf); pres.add(sg.preLen); }
                } catch (Exception e) { }
            }
            // kopia zapasowa ustawien — "Anuluj" przywraca poprzednie normy
            android.content.SharedPreferences p = prefs();
            final Norms.Calib res = bufs.isEmpty() ? null : Norms.calibrateSegments(bufs, pres);
            runOnUiThread(() -> {
                if (res == null) {
                    Norms.load(p);
                    new AlertDialog.Builder(this).setTitle(L.t("NORMY")).setMessage(L.t("Nagranie zbyt ciche albo za krótkie — nagraj wzorzec bliżej mikrofonu.")).setPositiveButton("OK", null).show();
                    return;
                }
                StringBuilder sc = new StringBuilder();
                for (int v : res.scoresAfter) { if (sc.length() > 0) sc.append(", "); sc.append(v).append('%'); }
                String msg = L.f("Znalezione sylaby 4-fazowe wzorca: {0}", res.samples) + "\n"
                        + L.f("Wynik próbek wzorca po kalibracji: {0}", sc) + "\n"
                        + (res.rejected > 0 ? "⚠ " + L.f("Pominięte próbki wyraźnie inne niż reszta: {0}", res.rejected) + "\n" : "") + "\n"
                        + String.format(Locale.US, "1 · %s: %.1f s\n2 · %s: %.1f s\n3 · %s: %.1f s\n4 · %s: %.1f s",
                        L.t("Faza leniwa (cichy start)"), res.lazyT, L.t("Narastanie"), res.riseT, L.t("Plateau"), res.platT, L.t("Opadanie"), res.fallT)
                        + "\n\n" + L.t("Wskazówka: nagraj 3–5 próbek (mogą być w jednym nagraniu, oddzielone pauzą) — ocena będzie stabilniejsza.");
                new AlertDialog.Builder(this).setTitle(L.t("Wynik kalibracji")).setMessage(msg)
                        .setPositiveButton(L.t("Zastosuj"), (dd, w) -> {
                            Norms.save(p);
                            renderNormsPage();
                            Toast.makeText(this, "✓ " + L.t("Zastosowano"), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.btn_cancel), (dd, w) -> Norms.load(p)).show();
            });
        }).start();
    }

    // Maly schemat 4 faz (ksztalt obwiedni wzorca) z czasami z ustawien
    private static class PhaseDiagram extends View {
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        PhaseDiagram(android.content.Context c) {
            super(c);
            setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) Ui.dp(c, 90)));
        }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float w = getWidth(), h = getHeight(), dd = getResources().getDisplayMetrics().density;
            float plat = Math.max(0.5f, Norms.ph2 * 0.6f);
            float tot = Norms.ph1 + Norms.ph2 + plat + Norms.ph3;
            float x1 = w * Norms.ph1 / tot, x2 = x1 + w * Norms.ph2 / tot, x3 = x2 + w * plat / tot;
            float base = h - 16 * dd, topY = 10 * dd, quietY = base - (base - topY) * 0.15f;
            int[] cols = {0xFF8888BB, 0xFFE8820C, 0xFF00C853, 0xFF00CFFF};
            float[][] xs = {{0, x1}, {x1, x2}, {x2, x3}, {x3, w}};
            for (int i = 0; i < 4; i++) { p.setColor((cols[i] & 0x00FFFFFF) | 0x30000000); p.setStyle(android.graphics.Paint.Style.FILL); cv.drawRect(xs[i][0], topY, xs[i][1], base, p); }
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(0, quietY); path.lineTo(x1, quietY); path.lineTo(x2, topY); path.lineTo(x3, topY); path.lineTo(w, base - 4 * dd);
            p.setStyle(android.graphics.Paint.Style.STROKE); p.setStrokeWidth(3 * dd); p.setColor(0xFFE8820C);
            cv.drawPath(path, p);
            p.setStyle(android.graphics.Paint.Style.FILL); p.setTextSize(10 * dd); p.setFakeBoldText(true);
            String[] n = {"1", "2", "3", "4"};
            for (int i = 0; i < 4; i++) { p.setColor(cols[i]); cv.drawText(n[i], (xs[i][0] + xs[i][1]) / 2 - 3 * dd, h - 3 * dd, p); }
        }
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

    private static String fixDayKey(NsClient.FixEntry fe) {
        if (fe.recordDate != null && fe.recordDate.length() >= 10) return fe.recordDate.substring(0, 10);
        long t = fe.recordedAt > 0 ? fe.recordedAt : fe.reviewedAt;
        if (t <= 0) return "";
        java.text.SimpleDateFormat k = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
        k.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Warsaw"));
        return k.format(new java.util.Date(t * 1000L));
    }

    private void fillFixList(android.widget.LinearLayout c, java.util.List<NsClient.FixEntry> list) {
        float d = getResources().getDisplayMetrics().density;
        if (list.isEmpty()) {
            TextView e = Ui.text(this, L.t("Brak nagrań do poprawy. Wszystko zaliczone albo czeka na pierwszą ocenę."), 14f, R.color.pr_muted);
            e.setPadding(0, (int) (20 * d), 0, 0);
            c.addView(e);
            return;
        }
        // sortowanie po record_date (dzien w NS), od najstarszych; ten sam dzien — po ocenie
        list = new java.util.ArrayList<>(list);
        java.util.Collections.sort(list, (x, y) -> {
            int k = fixDayKey(x).compareTo(fixDayKey(y));
            return k != 0 ? k : Long.compare(x.reviewedAt, y.reviewedAt);
        });
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
            // Daty: glowna = record_date (dzien, pod ktorym nagranie widac w NS), pod spodem ocena;
            // gdy kursant podmienil nagranie innego dnia (recorded_at) — dopisek. Czas polski.
            java.util.TimeZone pl = java.util.TimeZone.getTimeZone("Europe/Warsaw");
            java.text.SimpleDateFormat dHm = new java.text.SimpleDateFormat("dd.MM, HH:mm", Locale.US);
            java.text.SimpleDateFormat dDm = new java.text.SimpleDateFormat("dd.MM", Locale.US);
            java.text.SimpleDateFormat dKey = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            dHm.setTimeZone(pl); dDm.setTimeZone(pl); dKey.setTimeZone(pl);
            String rd = fe.recordDate != null && fe.recordDate.length() >= 10 ? fe.recordDate.substring(0, 10) : "";
            if (!rd.isEmpty()) {
                TextView rec1 = Ui.text(this, "📅 " + rd.substring(8, 10) + "." + rd.substring(5, 7), 13f, R.color.pr_text);
                rec1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                rec1.setPadding(0, (int) (4 * d), 0, 0);
                card.addView(rec1);
                StringBuilder sub = new StringBuilder();
                if (fe.reviewedAt > 0) sub.append(L.t("Oceniono")).append(": ").append(dDm.format(new java.util.Date(fe.reviewedAt * 1000L)));
                if (fe.recordedAt > 0 && !rd.equals(dKey.format(new java.util.Date(fe.recordedAt * 1000L)))) {
                    if (sub.length() > 0) sub.append("  ·  ");
                    sub.append(L.t("podmienione przez kursanta")).append(" ").append(dHm.format(new java.util.Date(fe.recordedAt * 1000L)));
                }
                if (sub.length() > 0) {
                    TextView rec2 = Ui.text(this, sub.toString(), 10f, R.color.pr_muted);
                    rec2.setPadding(0, (int) (1 * d), 0, (int) (6 * d));
                    card.addView(rec2);
                }
            } else if (fe.reviewedAt > 0) {
                // stary cache backendu — jak dotad: data oceny
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
            TextView sysTv = Ui.text(this, "🗣 " + L.t("System mowy") + ": …", 12f, R.color.pr_purple);
            sysTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            sysTv.setPadding(0, (int) (6 * d), 0, 0);
            card.addView(sysTv);
            resolveFixSys(fe.id, sys -> sysTv.setText(sys.isEmpty()
                    ? "🗣 " + L.t("System mowy") + ": ? — " + L.t("nagraj w tym samym systemie co oryginał")
                    : "🗣 " + L.t("System mowy") + ": " + sys + " — " + L.t("poprawka musi być w tym samym systemie")));
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
        prefs().edit().putString("fix_record_id", recordId).putString("fix_cat", catName).remove("fix_sys")
                .putLong("fix_saved_at", System.currentTimeMillis()).apply();
        showPage("daw");
        setStatus(L.t("🔄 Nagrywasz poprawkę dla: ") + L.cat(catName) + L.t("  (dotknij, aby anulować)"));
        resolveFixSys(recordId, sys -> {
            if (!recordId.equals(prefs().getString("fix_record_id", ""))) return;
            prefs().edit().putString("fix_sys", sys).apply();
            String msg = sys.isEmpty()
                    ? L.t("Nagraj poprawkę w TYM SAMYM systemie mowy, co nagranie z błędem.")
                    : L.f("Poprawkę nagraj w systemie {0} — takim samym jak w nagraniu z błędem. Inny system nie będzie dostępny.", sys);
            new AlertDialog.Builder(this).setTitle("🔄 " + L.t("Nagraj poprawkę") + (sys.isEmpty() ? "" : " · " + sys))
                    .setMessage(msg).setPositiveButton("OK", null).show();
            setStatus(L.t("🔄 Nagrywasz poprawkę dla: ") + L.cat(catName) + (sys.isEmpty() ? "" : " · " + sys) + L.t("  (dotknij, aby anulować)"));
        });
    }

    // System mowy (Basic/U1/U1K/FIX/K1/K2/Full) nagrania, ktore trzeba poprawic.
    // 1) nagranie z tego telefonu (zapamietane ID z NS), 2) nazwa pliku w NS — PitchRec
    // zapisuje system w nazwie (np. "Sklepy_3-maj-2026-(14h-5m-2s)-U1K-stres.mp3").
    private static final java.util.Map<String, String> FIX_SYS_CACHE = new java.util.HashMap<>();
    // System mowy w nazwie pliku NS, po godzinie nagrania, np.:
    //   "..._20h-15m-29s_-FIX-luz_GPS..."  albo  "...20h 15m 29s)-FIX..." (tez zakodowane w URL)
    private static final java.util.regex.Pattern SYS_IN_NAME = java.util.regex.Pattern.compile(
            "\\d{1,2}s(?:\\)|%29|\\\\u0029|_|%5F|\\s|%20|\\+)*-(Basic|U1K|U1|FIX|K1|K2|Full)(?=[^A-Za-z0-9]|$)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final String[] SYS_CANON = {"Basic", "U1K", "U1", "FIX", "K1", "K2", "Full"};

    static String sysFromName(String s) {
        if (s == null) return "";
        java.util.regex.Matcher m = SYS_IN_NAME.matcher(s);
        if (!m.find()) return "";
        for (String c : SYS_CANON) if (c.equalsIgnoreCase(m.group(1))) return c;
        return "";
    }

    public interface StrCb { void done(String s); }

    // Zapasowo: nagranie z tego telefonu w tej samej kategorii, wyslane w ciagu 30 min od
    // utworzenia rekordu w NS — jego opis (system mowy) jest systemem oryginalu.
    private String matchLocalByTime(String body, File[] files) {
        if (files == null) return "";
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            if (o.optJSONObject("record") != null) o = o.optJSONObject("record");
            String created = o.optString("created_at", "");
            if (created.isEmpty()) return "";
            long ts = java.time.OffsetDateTime.parse(created.replace(" ", "T")).toInstant().toEpochMilli();
            org.json.JSONObject cat = o.optJSONObject("record_category");
            String catName = cat != null ? cat.optString("name_pl", cat.optString("name", "")) : "";
            String best = "";
            long bestD = 30 * 60 * 1000L;
            for (File f : files) {
                RecMeta m = RecMeta.load(this, f.getName());
                if (m.sys.isEmpty() || !"sent".equals(m.ns) || !m.fixRecordId.isEmpty()) continue;
                if (!catName.isEmpty() && !catName.equalsIgnoreCase(m.cat)) continue;
                long d = Math.abs(f.lastModified() - ts);
                if (d < bestD) { bestD = d; best = m.sys; }
            }
            return best;
        } catch (Exception e) { return ""; }
    }

    private void resolveFixSys(String recordId, StrCb cb) {
        String cached = FIX_SYS_CACHE.get(recordId);
        if (cached != null) { cb.done(cached); return; }
        File[] files = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files != null) for (File f : files) {
            RecMeta m = RecMeta.load(this, f.getName());
            if (recordId.equals(m.nsRecordId) && !m.sys.isEmpty()) { FIX_SYS_CACHE.put(recordId, m.sys); cb.done(m.sys); return; }
        }
        if (!isLoggedIn()) { cb.done(""); return; }
        getRecordInfo(recordId, (status, rbody) -> {
            NsClient.Result r = new NsClient.Result();
            r.status = status; r.body = rbody; r.ok = status >= 200 && status < 300;
            String sys = "";
            if (r.ok && r.body != null) {
                sys = sysFromName(r.body);
            }
            if (!sys.isEmpty() || !r.ok) {
                if (r.ok) FIX_SYS_CACHE.put(recordId, sys);
                cb.done(sys);
                return;
            }
            // Mapa nagran: przy kazdym opisie wysylamy tam system mowy razem z kategoria
            // i data — szukamy punktu tego kursanta, tej kategorii, najblizszego w czasie.
            final String body = r.body;
            matchFromMap(body, mapSys -> {
                String res = !mapSys.isEmpty() ? mapSys : matchLocalByTime(body, files);
                FIX_SYS_CACHE.put(recordId, res);
                cb.done(res);
            });
        });
    }

    // Rekord NS (GET /records/{id}) z pamiecia podreczna — uzywany do daty nagrania, systemu mowy
    // i sprawdzenia, czy nagranie nadal istnieje na serwerze.
    public interface RecCb { void done(int status, String body); }
    private static final java.util.Map<String, Object[]> RECORD_CACHE = new java.util.HashMap<>();

    private void getRecordInfo(String id, RecCb cb) {
        Object[] c = RECORD_CACHE.get(id);
        if (c != null && System.currentTimeMillis() - (Long) c[2] < 10 * 60 * 1000L) { cb.done((Integer) c[0], (String) c[1]); return; }
        if (!isLoggedIn()) { cb.done(0, ""); return; }
        NsClient.request("GET", "/records/" + id, nsToken(), nsEmail(), null, null, r -> {
            if (r.ok || r.status == 404) RECORD_CACHE.put(id, new Object[]{r.status, r.body == null ? "" : r.body, System.currentTimeMillis()});
            cb.done(r.status, r.body == null ? "" : r.body);
        });
    }

    private static String recordDate(String body) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            if (o.optJSONObject("record") != null) o = o.optJSONObject("record");
            String d = o.optString("date", "");
            if (d.length() >= 10 && d.charAt(4) == '-') return d.substring(8, 10) + "." + d.substring(5, 7) + "." + d.substring(0, 4);
            String c = o.optString("created_at", "");
            if (c.length() >= 10) {
                java.time.ZonedDateTime z = java.time.OffsetDateTime.parse(c.replace(" ", "T")).atZoneSameInstant(java.time.ZoneId.systemDefault());
                return String.format(Locale.US, "%02d.%02d.%d %02d:%02d", z.getDayOfMonth(), z.getMonthValue(), z.getYear(), z.getHour(), z.getMinute());
            }
        } catch (Exception e) { }
        return "";
    }

    private static org.json.JSONArray MAP_POINTS = null;
    private static long MAP_POINTS_AT = 0L;

    private void matchFromMap(String recordBody, StrCb cb) {
        final long ts; final String catName;
        try {
            org.json.JSONObject o = new org.json.JSONObject(recordBody);
            if (o.optJSONObject("record") != null) o = o.optJSONObject("record");
            String created = o.optString("created_at", "");
            ts = created.isEmpty() ? 0L : java.time.OffsetDateTime.parse(created.replace(" ", "T")).toInstant().toEpochMilli();
            org.json.JSONObject cat = o.optJSONObject("record_category");
            catName = cat != null ? cat.optString("name_pl", cat.optString("name", "")) : "";
        } catch (Exception e) { cb.done(""); return; }
        if (ts == 0L) { cb.done(""); return; }
        Runnable match = () -> {
            String me1 = mapName(prefs().getString("student_name", "")).trim().toLowerCase(Locale.ROOT);
            String me2 = prefs().getString("student_name", "").trim().toLowerCase(Locale.ROOT);
            String best = "";
            long bestD = 3 * 60 * 60 * 1000L;
            org.json.JSONArray pts = MAP_POINTS;
            for (int i = 0; pts != null && i < pts.length(); i++) {
                org.json.JSONObject p = pts.optJSONObject(i);
                if (p == null || p.optString("sys", "").isEmpty()) continue;
                String nm = p.optString("name", "").trim().toLowerCase(Locale.ROOT);
                if (!nm.equals(me1) && !nm.equals(me2)) continue;
                if (!catName.isEmpty() && !catName.equalsIgnoreCase(p.optString("cat", ""))) continue;
                long d = Math.abs(p.optLong("ts", 0L) - ts);
                if (d < bestD) { bestD = d; best = p.optString("sys", ""); }
            }
            cb.done(best);
        };
        if (MAP_POINTS != null && System.currentTimeMillis() - MAP_POINTS_AT < 5 * 60 * 1000L) { match.run(); return; }
        NsClient.request("GET", "/map/points", nsToken(), nsEmail(), null, null, mr -> {
            if (mr.ok) {
                try {
                    org.json.JSONObject d = new org.json.JSONObject(mr.body);
                    org.json.JSONArray all = new org.json.JSONArray();
                    for (String k : new String[]{"history", "live"}) {
                        org.json.JSONArray a = d.optJSONArray(k);
                        for (int i = 0; a != null && i < a.length(); i++) all.put(a.opt(i));
                    }
                    MAP_POINTS = all;
                    MAP_POINTS_AT = System.currentTimeMillis();
                } catch (Exception e) { }
            }
            match.run();
        });
    }

    private String[] loadFixTarget() {
        android.content.SharedPreferences p = prefs();
        String id = p.getString("fix_record_id", null);
        if (id == null || id.isEmpty()) return null;
        if (System.currentTimeMillis() - p.getLong("fix_saved_at", 0L) > 3 * 60 * 60 * 1000L) { clearFixTarget(); return null; }
        return new String[]{id, p.getString("fix_cat", ""), p.getString("fix_sys", "")};
    }

    private void clearFixTarget() {
        prefs().edit().remove("fix_record_id").remove("fix_cat").remove("fix_sys").remove("fix_saved_at").apply();
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
