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
        RateNames.init(this);
        NsClient.initForms(this);
        AudioFileLoader.cacheDir = new File(getFilesDir(), ".dawcache");
        new Thread(() -> { // sprzatanie pamieci podrecznej DAW: najwyzej 300 najnowszych
            File[] cf = AudioFileLoader.cacheDir.listFiles();
            // jednorazowo: stare wpisy (klucz z data pliku) — moglyby pokazac wykres innego nagrania
            android.content.SharedPreferences cp = getSharedPreferences("app_settings", MODE_PRIVATE);
            if (cf != null && !cp.getBoolean("dawcache_v2", false)) {
                for (File x : cf) x.delete();
                cp.edit().putBoolean("dawcache_v2", true).apply();
                cf = AudioFileLoader.cacheDir.listFiles();
            }
            if (cf != null && cf.length > 300) {
                java.util.Arrays.sort(cf, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
                for (int i = 300; i < cf.length; i++) cf[i].delete();
            }
        }, "dawcache-clean").start();
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
        if (BuildConfig.PLAY) Premium.refresh(this, this::onPremiumChanged); // pelna wersja (zakup w Google Play)
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
            if (pcm != null) {
                // odsluch w pauzie nagrywania: play/pauza bez zaczynania od nowa
                if (pcm.isPlaying()) pcm.pause();
                else { pcm.resume(); startPcmLoop(); }
                syncButtons();
                return;
            }
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
            if (pcm != null) { // odsluch w pauzie: przeskok = start od wskazanego miejsca
                boolean was = pcm.isPlaying();
                if (was) previewDuringPause(sample); else { releasePlayer(); }
                return;
            }
            if (currentPlayer != null) {
                try {
                    seekExact(currentPlayer, (int) ms);
                    anchorPosMs = ms; anchorNanos = System.nanoTime(); lastReportedMs = -1;
                    timeText.setText(formatMs(ms) + " / " + formatMs(lastRecordingTotalDurationMs));
                } catch (IllegalStateException e) { /* odtwarzacz w nietypowym stanie */ }
            }
        });
        resetButton.setOnClickListener(v -> resetRecording());

        setupBottomNav();
        setupBadges();
        redrawHandler.postDelayed(buttonGuard, 400);
        setupGoalLine();
        applySliderVisibility();
        showPage("daw");
        // Przypomnienie o 20:00 (nagranie + dziennik) — domyslnie wlaczone
        ReminderReceiver.schedule(this);
        TrainerWatch.schedule(this);
        TrainerWatch.check(this, null); // oceny trenera od ostatniego otwarcia
        AvailWatch.schedule(this);
        AvailWatch.check(this, null);   // kto teraz chetnie porozmawia
        Push.register(this);            // powiadomienia push "Chcę porozmawiać" (od razu)
        ContactList.refresh(this, false, null); // lista telefonow od trenera (kategoria Phone do Kursanta)
        CallDays.check(this, null);             // dni telefonu do trenera (powiadomienia)
        askNotificationPermissionOnce();
        UpdateChecker.check(this, false);      // nowa wersja aplikacji (GitHub Releases)
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
        // Wersja Google Play: STATS zawsze widoczne — pelna wersja: statystyki z telefonu,
        // podstawowa: okienko z oferta pelnej wersji
        View st = findViewById(R.id.navStats);
        if (st != null && BuildConfig.PLAY) st.setVisibility(View.VISIBLE);
        if (!logged && !"expired".equals(nsAuthState)) nsAuthState = "none";
        // Bez logowania nie mozna zostac na stronie sekcji NS (wyjatek: statystyki z telefonu w pelnej wersji Play)
        boolean localStats = "stats".equals(currentPage) && BuildConfig.PLAY && Access.full(this);
        if (!logged && !localStats && ("fix".equals(currentPage) || "diary".equals(currentPage) || "stats".equals(currentPage))) showPage("daw");
    }

    // Zmiana stanu pelnej wersji (zakup / przywrocenie / zwrot) — odswiez, co trzeba
    private void onPremiumChanged() {
        if (isFinishing()) return;
        updateNavForLogin();
        if ("set".equals(currentPage)) renderSettingsPage();
        if ("recs".equals(currentPage)) renderRecsPage();
    }

    // Statystyki z nagran na telefonie (pelna wersja Play bez konta NewSpeech)
    private StatsPage.Host localStatsHost() {
        return new StatsPage.Host() {
            public String token() { return null; }
            public String email() { return ""; }
            public boolean isCurrent() { return "stats".equals(currentPage); }
        };
    }

    // ── KROPKI NA DOLNYM MENU: nieprzeczytane od trenera (Nagrania, Dziennik, Plan) i liczba poprawek (Korekta) ──
    private final java.util.Map<Integer, BadgeDrawable> badges = new java.util.HashMap<>();

    private void setupBadges() {
        float d = getResources().getDisplayMetrics().density;
        for (int id : new int[]{R.id.navRecsIcon, R.id.navFixIcon, R.id.navDiaryIcon, R.id.navStatsIcon}) {
            android.widget.ImageView ic = findViewById(id);
            if (ic == null) continue;
            BadgeDrawable b = new BadgeDrawable(d);
            badges.put(id, b);
            ic.addOnLayoutChangeListener((v, l, t, r, bo, ol, ot, or, ob) -> b.setBounds(0, 0, r - l, bo - t));
            ic.getOverlay().add(b);
        }
        TrainerInbox.listener = this::updateBadges;
        updateBadges();
    }

    private int fixCount() {
        if (fixListCache != null) return fixListCache.size();
        return NsStatus.count("bad");
    }

    void updateBadges() {
        boolean logged = isLoggedIn();
        BadgeDrawable b;
        if ((b = badges.get(R.id.navRecsIcon)) != null) b.setCount(logged ? TrainerInbox.unread(this, TrainerInbox.T_OK, TrainerInbox.T_BAD, TrainerInbox.T_VOICE) : 0);
        if ((b = badges.get(R.id.navFixIcon)) != null) b.setCount(logged ? fixCount() : 0);
        if ((b = badges.get(R.id.navDiaryIcon)) != null) b.setCount(logged ? TrainerInbox.unread(this, TrainerInbox.T_DIARY) : 0);
        if ((b = badges.get(R.id.navStatsIcon)) != null) b.setCount(logged ? TrainerInbox.unread(this, TrainerInbox.T_CALLS, TrainerInbox.T_LIST) : 0);
    }

    // Otwarcie pozycji z listy "Od trenera" — prosto w konkretne miejsce
    void openInboxItem(TrainerInbox.Ev e) {
        if (TrainerInbox.T_DIARY.equals(e.type)) { TrainerInbox.markRead(this, null, e.date, TrainerInbox.T_DIARY); openLoginOnlySection("diary", L.t("DZIENNIK")); return; }
        if (TrainerInbox.T_CALLS.equals(e.type) || TrainerInbox.T_LIST.equals(e.type)) { TrainerInbox.markRead(this, null, null, e.type); return; }
        if (e.id.startsWith("special:")) {
            // zaliczenie zadania specjalnego przez trenera — pokaz zadania w Statystykach
            TrainerInbox.markRead(this, "", null, TrainerInbox.T_OK);
            prefs().edit().putString("stats_tab", "stats").apply();
            openLoginOnlySection("stats", L.t("STATYSTYKI"));
            return;
        }
        if (!e.rec.isEmpty()) openRating(e.rec);
    }

    // Ocena konkretnego nagrania (z powiadomienia / z listy "Od trenera")
    private void openRating(String rec) {
        if (rec == null || rec.isEmpty()) return;
        // tylko okno oceny (dane z NS) — bez przechodzenia do listy i bez wczytywania nagrania
        File local = null;
        File[] files = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files != null) for (File f : files) {
            RecMeta m = RecMeta.load(this, f.getName());
            if (rec.equals(m.nsRecordId) || rec.equals(m.fixRecordId)) { local = f; break; }
        }
        final File lf = local;
        NsStatus.refresh(this, null);
        new Handler(Looper.getMainLooper()).postDelayed(() -> showTrainerRating(rec, "bad".equals(NsStatus.get(rec)), lf), 250);
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
        if ("com.pitchrec.nativetest.QUICK_REC".equals(in.getAction())) {
            in.setAction(null);
            showPage("daw");
            // skrot "Nagraj" z ikony apki — start nagrywania od razu
            if (!isRecording) new Handler(Looper.getMainLooper()).postDelayed(this::startRecordingFlow, 400);
            return;
        }
        String pg = in.getStringExtra("open_page");
        if (pg == null) return;
        in.removeExtra("open_page");
        String oRec = in.getStringExtra("open_rec");
        in.removeExtra("open_rec");
        if ("callrec".equals(pg)) { handlePendingCallRec(); return; }
        if ("callrec_setup".equals(pg)) { CallRecUi.setup(this); return; }
        if ("rate".equals(pg)) { if (isLoggedIn() && oRec != null) openRating(oRec); else showPage("recs"); return; }
        if ("recs".equals(pg)) { showPage("recs"); return; }
        if ("inbox".equals(pg)) { if (isLoggedIn()) { prefs().edit().putString("stats_tab", "plan").apply(); openLoginOnlySection("stats", L.t("STATYSTYKI")); } return; }
        if ("map".equals(pg)) { showPage("daw"); if (isLoggedIn()) openMap(); return; }
        if ("diary".equals(pg) && isLoggedIn()) { openLoginOnlySection("diary", L.t("DZIENNIK")); return; }
        if ("fix".equals(pg) && isLoggedIn()) { openLoginOnlySection("fix", L.t("KOREKTA")); return; }
        if ("stats".equals(pg) && isLoggedIn()) { prefs().edit().putString("stats_tab", "plan").apply(); openLoginOnlySection("stats", L.t("STATYSTYKI")); return; }
        showPage("daw");
    }

    // Android 13+: bez tej zgody NIE dochodzi zadne powiadomienie (przypomnienia, oceny trenera,
    // "Chcę porozmawiać"). Wczesniej pytalismy tylko raz w zyciu aplikacji — kto wtedy odmowil
    // albo zamknal okienko, nie dostawal nic. Teraz: ponownie co 3 dni, dopoki nie ma zgody.
    private void askNotificationPermissionOnce() {
        if (android.os.Build.VERSION.SDK_INT < 33) return;
        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) return;
        long last = prefs().getLong("notif_asked_at", 0L);
        if (System.currentTimeMillis() - last < 3L * 24 * 60 * 60 * 1000) return;
        prefs().edit().putLong("notif_asked_at", System.currentTimeMillis()).apply();
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 7301);
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
        } catch (Exception e) { PhoneHelp.openAppInfo(this); }
    }

    // Stan powiadomien w Ustawieniach: pokazuje TYLKO to, co blokuje powiadomienia, z przyciskiem naprawy
    private void addNotificationHealth(android.widget.LinearLayout box) {
        boolean notif = PhoneHelp.notifOk(this);
        boolean bat = PhoneHelp.batteryFree(this);
        if (notif && bat) {
            box.addView(hint("✅ " + L.t("Powiadomienia działają.")));
            return;
        }
        if (!notif) {
            box.addView(hint("⚠️ " + L.t("Powiadomienia są WYŁĄCZONE w telefonie — nie dotrze żadne przypomnienie ani wiadomość „Chcę porozmawiać”.")));
            Button b = Ui.button(this, "🔔 " + L.t("Włącz powiadomienia"), R.color.pr_accent, true);
            b.setOnClickListener(v -> {
                if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
                        && System.currentTimeMillis() - prefs().getLong("notif_btn_at", 0L) > 60000L) {
                    prefs().edit().putLong("notif_btn_at", System.currentTimeMillis()).apply();
                    ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 7301);
                } else openNotificationSettings();
            });
            box.addView(b);
        }
        if (!bat) {
            box.addView(hint("⚠️ " + L.t("Telefon usypia aplikację w tle — powiadomienia mogą przychodzić z opóźnieniem albo wcale.")));
            Button b = Ui.button(this, "🔋 " + L.t("Pozwól działać w tle"), R.color.pr_accent, false);
            b.setOnClickListener(v -> PhoneHelp.askBattery(this));
            box.addView(b);
            Intent vendor = PhoneHelp.vendorIntent(this);
            if (vendor != null) {
                box.addView(hint("📱 " + PhoneHelp.vendorText()));
                Button vb = Ui.button(this, L.t("Otwórz"), R.color.pr_muted, false);
                vb.setOnClickListener(v -> { try { startActivity(vendor); } catch (Exception e) { PhoneHelp.openAppInfo(this); } });
                box.addView(vb);
            }
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
        if ("recs".equals(page)) { renderRecsPage(); TrainerInbox.markRead(this, null, null, TrainerInbox.T_OK, TrainerInbox.T_BAD); }
        updateBadges();
        if ("set".equals(page)) renderSettingsPage();
        if ("daw".equals(page)) refreshGoal();
    }

    // ── CEL DNIA na ekranie nagrywania (z harmonogramu, pewne dane z NS) ──
    private TextView goalText;

    private void setupGoalLine() {
        View st = findViewById(R.id.statusText);
        if (st == null || !(st.getParent() instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout parent = (android.widget.LinearLayout) st.getParent();
        goalText = Ui.text(this, "", 12f, R.color.pr_text);
        goalText.setGravity(android.view.Gravity.CENTER);
        float d = getResources().getDisplayMetrics().density;
        goalText.setPadding((int) (8 * d), (int) (5 * d), (int) (8 * d), (int) (5 * d));
        goalText.setBackgroundColor(getResources().getColor(R.color.pr_card));
        goalText.setVisibility(View.GONE);
        goalText.setOnClickListener(v -> openLoginOnlySection("stats", L.t("STATYSTYKI")));
        parent.addView(goalText, parent.indexOfChild(st));
    }

    private int sentTodayLocal() {
        File[] files = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files == null) return 0;
        java.util.Calendar a = java.util.Calendar.getInstance(), b = java.util.Calendar.getInstance();
        int n = 0;
        for (File f : files) {
            b.setTimeInMillis(f.lastModified());
            if (a.get(java.util.Calendar.YEAR) != b.get(java.util.Calendar.YEAR) || a.get(java.util.Calendar.DAY_OF_YEAR) != b.get(java.util.Calendar.DAY_OF_YEAR)) continue;
            RecMeta m = RecMeta.load(this, f.getName());
            if ("sent".equals(m.ns) && m.fixRecordId.isEmpty()) n++;
        }
        return n;
    }

    private void updateGoalLine() {
        if (goalText == null) return;
        android.content.SharedPreferences p = prefs();
        if (!isLoggedIn() || !p.getBoolean("goal_active", false)) { goalText.setVisibility(View.GONE); return; }
        int per = p.getInt("goal_per_day", 0), missing = p.getInt("goal_missing", 0);
        String dl = p.getString("goal_deadline", "");
        String txt;
        int col;
        if (missing == 0) { txt = "🎉 " + L.t("Harmonogram wykonany!") + " · " + L.t("dalej trenuj swobodnie"); col = R.color.pr_accent; }
        else {
            int today = Math.max(sentTodayLocal(), NsStatus.todayCount);
            boolean done = per > 0 && today >= per;
            txt = (done ? "✅ " : "🎯 ") + L.f("Dziś: {0}/{1} nagrań", today, Math.max(1, per)) + "  ·  "
                    + (p.getBoolean("goal_on_track", false) ? L.t("w tym tempie zdążysz ✓") : L.f("harmonogram do {0}", dl));
            col = done ? R.color.pr_accent : R.color.pr_text;
        }
        goalText.setText(txt);
        goalText.setTextColor(getResources().getColor(col));
        goalText.setVisibility(View.VISIBLE);
    }

    private void refreshGoal() {
        updateGoalLine();
        if (!isLoggedIn()) return;
        HarmoGoal.refresh(this, () -> runOnUiThread(this::updateGoalLine));
        NsStatus.refresh(this, () -> runOnUiThread(() -> { updateGoalLine(); if ("recs".equals(currentPage)) renderRecsPage(); }));
    }

    // MIC i ZOOM mozna schowac, gdy mikrofon jest juz dobrze ustawiony
    private void applySliderVisibility() {
        int v = prefs().getBoolean("show_sliders", true) ? View.VISIBLE : View.GONE;
        View a = findViewById(R.id.micRow), b = findViewById(R.id.zoomRow);
        if (a != null) a.setVisibility(v);
        if (b != null) b.setVisibility(v);
    }

    // Po wyslaniu: swiezy status/cel dnia i ewentualne nowe odznaki (pelna historia z NS)
    private void afterSendRefresh() {
        NsStatus.invalidate();
        HarmoGoal.invalidate(this);
        refreshGoal();
        String q = "/harmonogram-stats/from-ns?ns_token=" + NsClient.enc(nsToken()) + "&ns_email=" + NsClient.enc(nsEmail())
                + "&ns_server=new&date_from=1970-01-01&date_to=2999-12-31";
        NsClient.backend("GET", q, null, r -> {
            try {
                org.json.JSONObject dj = new org.json.JSONObject(r.body);
                org.json.JSONObject by = dj.optJSONObject("byCategory");
                if (!dj.optBoolean("ok", false) || by == null) return;
                java.util.Map<String, Integer> sent = new java.util.LinkedHashMap<>();
                int total = 0;
                java.util.Iterator<String> it = by.keys();
                while (it.hasNext()) {
                    org.json.JSONObject c = by.optJSONObject(it.next());
                    if (c == null) continue;
                    String nm = c.optString("category_name", "");
                    int sn = c.optInt("sent", 0);
                    sent.put(nm, sent.getOrDefault(nm, 0) + sn);
                    total += sn;
                }
                Achievements.celebrate(this, Achievements.newlyEarned(this, sent, total));
            } catch (Exception e) { }
        });
    }

    // Wejscie do sekcji wymagajacej konta: najpierw sprawdzamy sesje (jak w PWA) —
    // wygasla sesja = czytelny komunikat i przejscie do logowania, nie pusty ekran.
    private void openLoginOnlySection(String page, String title) {
        if (!isLoggedIn() && BuildConfig.PLAY && "stats".equals(page)) {
            if (!Access.full(this)) { PremiumUi.offer(this, L.t("Statystyki są w pełnej wersji."), this::onPremiumChanged); return; }
            showPage(page);
            android.widget.LinearLayout c = findViewById(R.id.nsContent);
            c.removeAllViews();
            new StatsPage(this, c, localStatsHost()).render();
            return;
        }
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
                    public void openInbox(TrainerInbox.Ev e) { openInboxItem(e); }
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
            public void recordMonologue() {
                // MONOLOG — PODSUMOWANIE DNIA: nagranie z gotowa kategoria "Monolog / Czytanie"
                if (isRecording) { showPage("daw"); return; }
                diaryMonologue = true;
                showPage("daw");
                new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::startRecordingFlow, 300);
            }
            public void playUrl(String url, android.widget.Button btn) { playRemoteAudio(url, btn); }
        };
    }

    private boolean diaryMonologue = false;
    private String callRecLabel = null; // != null: opisujemy nagrana rozmowe telefoniczna

    // NAGRANA ROZMOWA (CallRecService) -> wczytanie do DAW i ekran opisu z kategoria "Phone do Kursanta/Trenera"
    private void handlePendingCallRec() {
        String v = prefs().getString("callrec_pending", null);
        if (v == null || isRecording || saving || callRecLabel != null) return;
        prefs().edit().remove("callrec_pending").apply();
        String[] pr = v.split("\n", -1);
        File f = new File(pr[0]);
        if (!f.exists()) return;
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(4802);
        final String label = pr.length > 1 ? pr[1] : "";
        boolean fromList = pr.length < 3 || "list".equals(pr[2]);
        Runnable go = () -> {
            showPage("daw");
            lastSavedFilePath = f.getAbsolutePath();
            loadAndDisplayFile(f);
            callRecLabel = label;
            callRecFromList = fromList;
            describeNewRecording(f);
        };
        if (fromList) { go.run(); return; }
        // zwykla rozmowa (np. do miasta, rodzina) — najpierw pytamy, czy to cwiczenie
        long sec = 0;
        try { sec = Long.parseLong(pr[3]) / 1000; } catch (Exception e) { }
        new android.app.AlertDialog.Builder(this)
                .setTitle("📞 " + L.t("Rozmowa nagrana") + (sec > 0 ? String.format(Locale.US, " (%d:%02d)", sec / 60, sec % 60) : ""))
                .setMessage(L.t("Zapisać tę rozmowę jako ćwiczenie? Wybierzesz kategorię, np. Telefon do miasta."))
                .setPositiveButton(L.t("Zapisz i opisz"), (d, w) -> go.run())
                .setNegativeButton(L.t("Usuń nagranie"), (d, w) -> { f.delete(); Toast.makeText(this, L.t("Nagranie rozmowy usunięte"), Toast.LENGTH_SHORT).show(); })
                .setCancelable(false)
                .show();
    }
    private boolean callRecFromList = true;

    // Odtwarzanie nagrania z serwera (odpowiedz glosowa trenera) — przycisk ▶ / ■
    private void playRemoteAudio(String url, android.widget.Button btn) {
        if (voicePlayer != null) { stopVoiceReview(); btn.setText("▶ " + L.t("Posłuchaj")); return; }
        try {
            releasePlayer();
            MediaPlayer mp = new MediaPlayer();
            voicePlayer = mp;
            mp.setDataSource(url);
            mp.setOnPreparedListener(m -> { if (voicePlayer == m) { m.start(); btn.setText("■ " + L.t("Zatrzymaj")); } });
            mp.setOnCompletionListener(m -> { stopVoiceReview(); btn.setText("▶ " + L.t("Posłuchaj")); });
            mp.setOnErrorListener((m, w, e) -> { stopVoiceReview(); btn.setText("▶ " + L.t("Posłuchaj")); Toast.makeText(this, L.t("Nie udało się odtworzyć komentarza"), Toast.LENGTH_SHORT).show(); return true; });
            btn.setText("⏳ " + L.t("Wczytywanie…"));
            mp.prepareAsync();
        } catch (Exception e) { stopVoiceReview(); }
    }

    private TextView pageTitle(String t) {
        TextView tv = Ui.text(this, t, 18f, R.color.pr_muted);
        tv.setLetterSpacing(0.3f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding((int) Ui.dp(this, 4), (int) Ui.dp(this, 4), 0, (int) Ui.dp(this, 12));
        return tv;
    }

    private void promptLogin(String message) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(L.t("Wymagane logowanie"))
                .setMessage(message)
                .setPositiveButton(L.t("Przejdź do logowania"), (d, w) -> showPage("set"))
                .setNegativeButton(getString(R.string.btn_cancel), null);
        // nie jestes jeszcze kursantem? — od razu oferta analizy mowy (nie w Google Play: zasady platnosci)
        if (!BuildConfig.PLAY) b.setNeutralButton("🎯 " + L.t("Analiza mowy"), (d, w) -> AnalysisOffer.dialog(this));
        b.show();
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
                loadProfileFromNs(true, () -> { Push.register(this); if ("set".equals(currentPage)) renderSettingsPage(); });
                TrainerWatch.schedule(this);
                AvailWatch.schedule(this);
                TrainerWatch.check(this, null); // pierwsze sprawdzenie tylko zapamietuje stan (bez powiadomien)
                updateNavForLogin();
                refreshAccountSection();
                Toast.makeText(this, L.t("✓ Zalogowano do NewSpeech"), Toast.LENGTH_SHORT).show();
            } else {
                errView.setText("✗ " + r.err);
            }
        });
    }

    private void doLogout() {
        Push.unregister(this); // przed usunieciem danych konta (potrzebny identyfikator)
        prefs().edit().remove("ns_token").remove("ns_user_id").remove("contact_list_json").remove("contact_list_at").remove("call_days_json").remove("call_days_seen").remove("call_days_done").remove("inbox_json").remove("special_credit").remove("special_credit_init").remove("harmo_cache").putBoolean("ns_session_expired", false).apply();
        nsAuthState = "none";
        nsAuthCheckedAt = 0L;
        updateNavForLogin();
        refreshAccountSection();
    }

    // Sekcja "Konto NewSpeech" na gorze Ustawien — zawsze pokazuje JASNY stan:
    // niezalogowany / sprawdzam / zalogowano / sesja wygasla / brak polaczenia.
    private android.widget.LinearLayout accountSection;

    private void refreshAccountSection() {
        LiveAudioData.loggedIn = isLoggedIn();
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
            accountSection.addView(divider());
            addWantTalk(accountSection); // "Chcę porozmawiać" — od razu pod "Wyloguj"
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
    // Pliki w trakcie wysylki — blokada wielokrotnego klikniecia (kazde klikniecie = nowy rekord w NS)
    private boolean limitOverride = false;
    private final java.util.Set<String> sendingNow = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    // ── POSTEP WYSYLKI na przycisku "Wyślij NS": wypelnia sie na pomaranczowo od lewej (0–100%) ──
    private final java.util.Map<String, Button> sendButtons = new java.util.HashMap<>();
    private final android.os.Handler sendTick = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean sendTicking = false;

    private void startSendTicker() {
        if (sendTicking) return;
        sendTicking = true;
        sendTick.postDelayed(new Runnable() {
            @Override public void run() {
                for (java.util.Map.Entry<String, Button> e : sendButtons.entrySet()) {
                    if (sendingNow.contains(e.getKey())) paintSendProgress(e.getKey(), e.getValue());
                }
                if (sendingNow.isEmpty()) { sendTicking = false; return; }
                sendTick.postDelayed(this, 400);
            }
        }, 400);
    }

    private void paintSendProgress(String name, Button b) {
        float[] p = NsClient.PROGRESS.get(name);
        float all = p == null ? 0f : p[0];
        int stage = p == null ? 1 : (int) p[1];
        int pct = Math.round((p == null ? 0f : p[2]) * 100);
        String txt = stage == 0 ? L.t("⏳ Zmniejszanie") + " " + pct + "%"
                : stage == 2 ? L.t("⏳ Serwer przetwarza…")
                : L.t("⏳ Wysyłanie") + " " + pct + "%";
        if (!txt.contentEquals(b.getText())) b.setText(txt);
        android.graphics.drawable.Drawable bg = b.getBackground();
        android.graphics.drawable.ClipDrawable clip;
        if (bg instanceof android.graphics.drawable.LayerDrawable && ((android.graphics.drawable.LayerDrawable) bg).getNumberOfLayers() == 2
                && ((android.graphics.drawable.LayerDrawable) bg).getDrawable(1) instanceof android.graphics.drawable.ClipDrawable) {
            clip = (android.graphics.drawable.ClipDrawable) ((android.graphics.drawable.LayerDrawable) bg).getDrawable(1);
        } else {
            float r = 9 * getResources().getDisplayMetrics().density;
            int orange = (Ui.col(this, R.color.pr_accent) & 0x00FFFFFF) | 0xA0000000;
            clip = new android.graphics.drawable.ClipDrawable(Ui.rounded(orange, 0, 0, r), android.view.Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
            b.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bg, clip}));
        }
        clip.setLevel(Math.round(all * 10000));
    }

    private void sendToNs(File file) {
        if (!isLoggedIn()) { promptLogin(L.t("Aby wysłać nagranie do NewSpeech, zaloguj się.")); return; }
        RecMeta meta = RecMeta.load(this, file.getName());
        if (meta.cat.isEmpty()) {
            describeExisting(file, true);
            return;
        }
        if ("sent".equals(meta.ns)) { Toast.makeText(this, L.t("☁✓ To nagranie jest już wysłane do NS"), Toast.LENGTH_SHORT).show(); return; }
        if (meta.fixRecordId.isEmpty() && limitHitToday() && !limitOverride) {
            new AlertDialog.Builder(this)
                    .setCustomTitle(nsLogoTitle(L.t("⛔ Limit na dziś wyczerpany")))
                    .setMessage(L.t("Dziś NS już odrzucił nowe nagranie z powodu dziennego limitu. Nagranie zostaje w telefonie — wyślij je jutro."))
                    .setPositiveButton(L.t("OK"), null)
                    .setNeutralButton(L.t("Spróbuj mimo to"), (d, w) -> { limitOverride = true; sendToNs(file); limitOverride = false; })
                    .show();
            return;
        }
        if (!sendingNow.add(file.getName())) { Toast.makeText(this, L.t("⏳ Wysyłanie już trwa…"), Toast.LENGTH_SHORT).show(); return; }
        startSendTicker();
        if ("recs".equals(currentPage)) renderRecsPage();
        String catId = NsClient.categoryId(meta.cat);
        if (catId == null) { sendingNow.remove(file.getName()); Toast.makeText(this, L.t("Nieznana kategoria: ") + meta.cat, Toast.LENGTH_LONG).show(); return; }
        boolean isFix = !meta.fixRecordId.isEmpty();
        setStatus(isFix ? L.t("☁ Wysyłanie poprawki…") : L.t("☁ Wysyłanie do NS…"));
        NsClient.Callback cb = r -> {
            sendingNow.remove(file.getName());
            if (r.ok) {
                markNs(file, "sent");
                if (!isFix) { rememberNsId(file, r.body); getSharedPreferences("app_settings", MODE_PRIVATE).edit().remove("ns_limit_day").apply(); }
                setStatus(isFix ? L.t("☁✓ Poprawka wysłana! Czeka na ocenę trenera.") : L.t("☁✓ NS: wysłano (") + L.cat(meta.cat) + ")");
                Toast.makeText(this, isFix ? L.t("☁✓ Poprawka wysłana") : L.t("☁✓ Wysłano do NS"), Toast.LENGTH_SHORT).show();
                StatsPage.invalidateHarmonogram(this); // nowe nagranie — harmonogram odswiezy sie przy nastepnym wejsciu
                if (isFix) fixListCache = null;
                afterSendRefresh();
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
                // nagranie NIE jest bledne — zostaje "do wyslania" (bez ☁✗), jutro zwykle "Wyslij NS"
                showDailyLimit(null);
            } else {
                markNs(file, "error");
                setStatus(L.t("☁✗ NS: ") + r.err);
                Toast.makeText(this, L.t("Błąd wysyłki: ") + r.err, Toast.LENGTH_LONG).show();
            }
            if ("recs".equals(currentPage)) renderRecsPage();
        };
        if (UploadShrink.needed(file)) setStatus(L.t("☁ Długie nagranie — zmniejszam plik przed wysłaniem (to może potrwać kilka minut)…"));
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
            if ("sent".equals(m.ns) || sendingNow.contains(f.getName())) continue;
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

    // Wyrazny komunikat o dziennym limicie NS (zamiast "Blad 429")
    private void showDailyLimit(String extra) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        getSharedPreferences("app_settings", MODE_PRIVATE).edit().putString("ns_limit_day", today).apply();
        setStatus(L.t("⛔ Dzienny limit nowych nagrań wyczerpany — wyślij jutro"));
        new AlertDialog.Builder(this)
                .setCustomTitle(nsLogoTitle(L.t("⛔ Limit na dziś wyczerpany")))
                .setMessage(L.t("Dzienny limit NOWYCH nagrań w NewSpeech na dziś się skończył.") + (extra == null ? "" : "\n" + extra)
                        + "\n\n" + L.t("Nagranie jest bezpiecznie zapisane w telefonie (NAGRANIA → ☁ Niewysłane) — wyślij je jutro.")
                        + "\n\n" + L.t("Poprawki odrzuconych nagrań (KOREKTA) nie mają limitu."))
                .setPositiveButton(L.t("OK"), null).show();
        if ("recs".equals(currentPage)) renderRecsPage();
    }

    private boolean limitHitToday() {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        return today.equals(getSharedPreferences("app_settings", MODE_PRIVATE).getString("ns_limit_day", ""));
    }

    private void sendNext(java.util.List<File> todo, int idx, int okCount) {
        if (idx >= todo.size()) {
            setStatus(L.t("☁✓ Wysłano ") + okCount + L.t(" nagrań do NS"));
            StatsPage.invalidateHarmonogram(this);
            if ("recs".equals(currentPage)) renderRecsPage();
            return;
        }
        File f = todo.get(idx);
        RecMeta fm = RecMeta.load(this, f.getName());
        String catId = NsClient.categoryId(fm.cat);
        if (catId == null) { sendNext(todo, idx + 1, okCount); return; }
        setStatus(L.t("☁ Wysyłanie ") + (idx + 1) + "/" + todo.size() + "…");
        sendingNow.add(f.getName());
        startSendTicker();
        if ("recs".equals(currentPage)) renderRecsPage();
        NsClient.Callback cb = r -> {
            sendingNow.remove(f.getName());
            if (r.ok) {
                markNs(f, "sent");
                rememberNsId(f, r.body);
                sendNext(todo, idx + 1, okCount + 1);
            } else if (r.isLimitError() && fm.fixRecordId.isEmpty()) {
                showDailyLimit(L.f("Wysłano teraz: {0}, czeka: {1}.", okCount, todo.size() - idx));
            } else {
                if (r.isAuthError()) markSessionExpired(); else markNs(f, "error");
                setStatus(L.t("☁✗ Wysłano ") + okCount + "/" + todo.size() + L.t(" — zatrzymano: ") + r.err);
                Toast.makeText(this, L.t("Zatrzymano wysyłkę: ") + r.err, Toast.LENGTH_LONG).show();
                if ("recs".equals(currentPage)) renderRecsPage();
            }
        };
        if (UploadShrink.needed(f)) setStatus(L.t("☁ Wysyłanie ") + (idx + 1) + "/" + todo.size() + L.t(" — zmniejszam długie nagranie…"));
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
        // GPS Z CZASU NAGRANIA zapisujemy od razu przy pliku — opis moze byc pozniej, np. w domu
        android.location.Location rf = GpsHelper.recordingFix;
        // tylko pozycja z TEGO nagrania (nie np. z poprzedniego, przy nagranej rozmowie telefonicznej)
        if (rf != null && Math.abs(file.lastModified() - rf.getTime()) > 3 * 3600 * 1000L) rf = null;
        GpsHelper.recordingFix = null;
        if (rf != null) {
            RecMeta pg = RecMeta.load(this, file.getName());
            if (!pg.hasGps()) { pg.lat = rf.getLatitude(); pg.lon = rf.getLongitude(); pg.save(this, file.getName()); }
        }
        if (!Access.full(this)) {
            // WERSJA PODSTAWOWA (Play, bez zakupu): nagranie zapisane do pliku, bez opisu
            callRecLabel = null;
            diaryMonologue = false;
            exportToFolder(file);
            setStatus(L.t("💾 Zapisano"));
            nudgePremium();
            return;
        }
        String[] fix = loadFixTarget();
        RecMeta init = new RecMeta();
        if (rf != null) { init.lat = rf.getLatitude(); init.lon = rf.getLongitude(); }
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
        if (fix == null && diaryMonologue) {
            init.cat = "Monolog / Czytanie";
            init.note = "📔 " + L.t("Podsumowanie dnia");
            banner = "🎙️ " + L.t("Monolog — podsumowanie dnia (do dziennika)");
        }
        if (fix == null && callRecLabel != null) {
            if (callRecFromList) {
                init.cat = ContactList.CATEGORY;
                init.note = callRecLabel.isEmpty() ? "" : "📞 " + callRecLabel;
                banner = "📞 " + L.t("Nagrana rozmowa telefoniczna") + (callRecLabel.isEmpty() ? " — " + L.t("wybierz, z kim rozmawiałeś") : " — " + callRecLabel);
            } else {
                banner = "📞 " + L.t("Nagrana rozmowa telefoniczna") + " — " + L.t("wybierz kategorię (np. Telefon do miasta)");
            }
        }
        callRecLabel = null;
        diaryMonologue = false;
        final boolean isFix = fix != null;
        DescribeSheet.show(this, init, true, isFix ? L.t("ZAPISZ POPRAWKĘ") : L.t("ZAPISZ NAGRANIE"), banner, meta -> {
            File renamed = RecMeta.renameWithMeta(this, file, meta);
            if (file.getAbsolutePath().equals(loadedFilePath)) loadedFilePath = renamed.getAbsolutePath();
            afterDescribed(meta, file.lastModified());
            afterLocalDescribed();
            if (file.getAbsolutePath().equals(lastSavedFilePath)) lastSavedFilePath = renamed.getAbsolutePath();
            exportToFolder(renamed);
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
            exportToFolder(file);
            setStatus(L.t("💾 Zapisano bez opisu — opisz i wyślij w NAGRANIACH"));
        });
    }

    // Wersja podstawowa: co 5. nagranie krotka informacja o pelnej wersji (bez natretnych okienek)
    private void nudgePremium() {
        int n = prefs().getInt("basic_saved", 0) + 1;
        prefs().edit().putInt("basic_saved", n).apply();
        if (n % 5 == 0) Toast.makeText(this, "⭐ " + L.t("Kategorie, statystyki i odznaki — w pełnej wersji (Ustawienia)."), Toast.LENGTH_LONG).show();
    }

    // Pelna wersja bez konta: odznaki i zadania specjalne liczone z opisanych nagran na telefonie
    private void afterLocalDescribed() {
        if (!Access.localMode(this)) return;
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        int total = 0;
        File[] files = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
        if (files != null) for (File f : files) {
            String cat = RecMeta.load(this, f.getName()).cat;
            if (cat == null || cat.isEmpty()) continue;
            counts.put(cat, counts.getOrDefault(cat, 0) + 1);
            total++;
        }
        Achievements.celebrate(this, Achievements.newlyEarned(this, counts, total));
        SpecialTasks.Task un = SpecialTasks.newlyUnlocked(this);
        if (un != null) Toast.makeText(this, "🎬 " + L.t("Nowe zadanie specjalne") + ": " + un.icon + " " + un.shortNameL(), Toast.LENGTH_LONG).show();
    }

    // Opisywanie istniejacego pliku z listy (przycisk "📝 Opisz") — wymagana tylko kategoria.
    private void describeExisting(File file, boolean sendAfter) {
        if (!Access.full(this)) { PremiumUi.offer(this, L.t("Opisywanie nagrań (kategoria, gwiazdki) jest w pełnej wersji."), this::onPremiumChanged); return; }
        RecMeta existing = RecMeta.load(this, file.getName());
        DescribeSheet.show(this, existing, false, L.t("OPISZ NAGRANIE"), meta -> {
            File renamed = RecMeta.renameWithMeta(this, file, meta);
            if (file.getAbsolutePath().equals(loadedFilePath)) loadedFilePath = renamed.getAbsolutePath();
            afterDescribed(meta, file.lastModified());
            afterLocalDescribed();
            if ("recs".equals(currentPage)) renderRecsPage();
            if (sendAfter && isLoggedIn()) sendToNs(renamed);
        });
    }

    // Po opisaniu nagrania — jak w PitchRec: kwestionariusz (Google Forms) + punkt na MAPIE
    // (historia zawsze, gdy jest GPS; "na zywo" dla Sklepow i Przechodniow, gdy wlaczone).
    private void afterDescribed(RecMeta m, long timeMs) {
        // dom zapamietywany sam; nagranie "terenowe" z okolicy domu -> podpowiedz
        if (m.hasGps()) {
            int homeDist = HomeGps.onRecording(this, m.cat, m.lat, m.lon);
            if (homeDist >= 0) {
                String msg = "🏠 " + L.f("Nagranie „{0}” z okolicy domu ({1} m). Spróbuj wyjść dalej do miasta!", L.cat(m.cat), homeDist);
                setStatus(msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }
        // Wersja Play bez konta: opis zostaje TYLKO na telefonie (bez formularza kursu i mapy)
        if (Access.localMode(this)) return;
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
            // wysylka w tle, bez komunikatow (nieudane ida do kolejki i sa ponawiane automatycznie)
            NsClient.postForm("https://docs.google.com/forms/d/e/1FAIpQLSfTippGzWsqV6vX9ZovUTqsGYW-GQyqcvmTiJkIsvpoRysJ9g/formResponse", f.toString(), (ok, info) -> { });
        } catch (Exception e) { /* nieblokujace */ }
        if (!m.hasGps() || m.cat.isEmpty()) return;
        String name = mapName(m.name);
        String uid = nsUserId().isEmpty() ? name : nsUserId();
        final boolean live = prefs().getBoolean("map_share", true) && isLiveCategory(m.cat);
        // miasto z miejsca nagrania (GPS -> Geocoder), awaryjnie z profilu NS — w tle
        new Thread(() -> {
            String city = cityAt(m.lat, m.lon);
            if (city.isEmpty()) city = prefs().getString("ns_city", "");
            try {
                org.json.JSONObject h = new org.json.JSONObject();
                h.put("lat", m.lat); h.put("lon", m.lon); h.put("cat", m.cat); h.put("name", name);
                h.put("userId", uid); h.put("city", city); h.put("ts", timeMs); h.put("sys", m.sys);
                byte[] body = h.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    NsClient.request("POST", "/map/hist", null, null, "application/json", body, r -> { });
                    // NA ZYWO na mapie (ok. 20 min): tylko miejsca publiczne — imie, miasto, system, kategoria
                    if (live) NsClient.request("POST", "/map/ping", null, null, "application/json", body, r -> { });
                });
            } catch (Exception e) { /* nieblokujace */ }
        }, "map-ping").start();
    }

    // Kategorie pokazywane "na zywo" na mapie (miejsca publiczne)
    static boolean isLiveCategory(String cat) {
        return "Sklepy".equals(cat) || "Przechodzień".equals(cat) || "Special".equals(cat) || "Miasto - inne".equals(cat);
    }

    private String cityAt(double lat, double lon) {
        try {
            android.location.Geocoder g = new android.location.Geocoder(this, new Locale("pl", "PL"));
            java.util.List<android.location.Address> r = g.getFromLocation(lat, lon, 1);
            if (r != null && !r.isEmpty()) {
                android.location.Address ad = r.get(0);
                String c = ad.getLocality() != null ? ad.getLocality() : ad.getSubAdminArea();
                return c == null ? "" : c;
            }
        } catch (Exception e) { }
        return "";
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

    private void loadProfileFromNs(boolean force) { loadProfileFromNs(force, null); }

    private void loadProfileFromNs(boolean force, Runnable done) {
        if (!isLoggedIn()) { if (done != null) done.run(); return; }
        String uid = nsUserId();
        String path = uid.isEmpty() ? "/users/me" : "/users/" + uid;
        NsClient.request("GET", path, nsToken(), nsEmail(), null, null, r -> {
            if (!r.ok) { if (done != null) done.run(); return; }
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
                if (!phone.isEmpty()) e.putString("map_phone", phone); // telefon ZAWSZE z profilu kursanta w NS
                if (!city.isEmpty()) e.putString("ns_city", city);
                e.apply();
                if ("set".equals(currentPage) && done == null) renderSettingsPage();
            } catch (Exception ex) { }
            if (done != null) done.run();
        });
    }

    // ── "CHCĘ POROZMAWIAĆ" — jeden przycisk (Ustawienia i mapa). Imie, telefon i miejscowosc
    //    pobieraja sie same z konta NewSpeech (gdy brak telefonu — jedno pytanie o numer),
    //    pozycja z GPS (zaokraglona do ok. 1 km). Inni kursanci dostaja powiadomienie.
    //    Wylacza sie samo po 2 h.
    private static final long AVAIL_MS = 2 * 60 * 60 * 1000L;

    private boolean availOn() {
        return prefs().getBoolean("map_avail", false) && System.currentTimeMillis() < prefs().getLong("map_avail_until", 0L);
    }

    private String availUntilText() {
        return new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(prefs().getLong("map_avail_until", 0L)));
    }

    private void wantTalk(Runnable after) {
        if (!isLoggedIn()) { promptLogin(L.t("„Chcę porozmawiać” wymaga zalogowania do NewSpeech.")); return; }
        if (availOn()) {
            setAvailability(false, prefs().getString("map_phone", ""));
            Toast.makeText(this, L.t("📞 Wyłączono „Chcę porozmawiać”"), Toast.LENGTH_SHORT).show();
            if (after != null) after.run();
            return;
        }
        Toast.makeText(this, L.t("⏳ Pobieram dane z konta…"), Toast.LENGTH_SHORT).show();
        loadProfileFromNs(false, () -> {
            if (prefs().getString("map_phone", "").replaceAll("\\D", "").length() < 6) askPhone(true, after);
            else goAvail(after);
        });
    }

    private void askPhone(boolean thenAvail, Runnable after) {
        android.widget.EditText e = new android.widget.EditText(this);
        e.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        e.setHint("+48 600 000 000");
        e.setText(prefs().getString("map_phone", ""));
        android.widget.FrameLayout wrap = new android.widget.FrameLayout(this);
        int pd = (int) (20 * getResources().getDisplayMetrics().density);
        wrap.setPadding(pd, pd / 2, pd, 0);
        wrap.addView(e);
        new AlertDialog.Builder(this)
                .setTitle("📞 " + L.t("Twój numer telefonu"))
                .setMessage(thenAvail ? L.t("Na Twoim koncie NewSpeech nie ma numeru telefonu. Podaj numer, pod który inni kursanci mogą zadzwonić:")
                        : L.t("Numer, pod który inni kursanci mogą zadzwonić:"))
                .setView(wrap)
                .setPositiveButton(thenAvail ? L.t("Chcę porozmawiać") : L.t("Zapisz"), (d, w) -> {
                    String ph = e.getText().toString().trim();
                    if (ph.replaceAll("\\D", "").length() < 6) { Toast.makeText(this, L.t("To nie wygląda na numer telefonu"), Toast.LENGTH_LONG).show(); return; }
                    prefs().edit().putString("map_phone", ph).apply();
                    if (thenAvail) goAvail(after);
                    else { if (availOn()) postAvail(true); if (after != null) after.run(); }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void goAvail(Runnable after) {
        prefs().edit().putLong("notif_asked_at", 0L).apply();
        askNotificationPermissionOnce(); // zeby samemu tez dostawac powiadomienia od innych
        setAvailability(true, prefs().getString("map_phone", ""));
        Toast.makeText(this, L.f("✅ Inni kursanci dostali powiadomienie. Dostępny do {0}.", availUntilText()), Toast.LENGTH_LONG).show();
        // pozycja na mape — gdy jeszcze jej nie ma, dosylamy, gdy GPS ja znajdzie
        // pozycja na mape: zawsze swieza (stara pozycja z innego miejsca dawala punkt "w losowym miejscu")
        if (GpsHelper.hasPermission(this))
            GpsHelper.requestFix(this, loc -> { if (loc != null && availOn()) postAvail(true); });
        if (after != null) after.run();
    }

    private void setAvailability(boolean on, String phone) {
        long until = on ? System.currentTimeMillis() + AVAIL_MS : 0L;
        prefs().edit().putBoolean("map_avail", on).putLong("map_avail_until", until).putString("map_phone", phone == null ? "" : phone).apply();
        postAvail(on);
    }

    private void postAvail(boolean on) {
        try {
            org.json.JSONObject b = new org.json.JSONObject();
            String name = mapName(prefs().getString("student_name", ""));
            prefs().edit().putString("map_self_name", name).apply();
            String phone = prefs().getString("map_phone", "");
            b.put("userId", nsUserId().isEmpty() ? (nsEmail().isEmpty() ? name : nsEmail()) : nsUserId());
            b.put("name", name);
            b.put("phone", phone.isEmpty() ? org.json.JSONObject.NULL : phone);
            b.put("available", on);
            b.put("city", prefs().getString("ns_city", ""));
            // tylko swieza pozycja (do 10 min) — z dokladnoscia ok. 100 m
            android.location.Location loc = GpsHelper.lastFix;
            if (on && loc != null && System.currentTimeMillis() - loc.getTime() < 10 * 60 * 1000L) {
                b.put("lat", Math.round(loc.getLatitude() * 1000) / 1000.0);
                b.put("lon", Math.round(loc.getLongitude() * 1000) / 1000.0);
            }
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
        i.putExtra("phone", prefs().getString("map_phone", ""));
        i.putExtra("city", prefs().getString("ns_city", ""));
        i.putExtra("availUntil", availOn() ? prefs().getLong("map_avail_until", 0L) : 0L);
        startActivityForResult(i, REQ_MAP);
    }

    // Tytul okienka z logo "new speech" (jak w naglowku apki)
    private View nsLogoTitle(String question) {
        float d = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding((int) (22 * d), (int) (18 * d), (int) (22 * d), (int) (4 * d));
        android.widget.LinearLayout logo = new android.widget.LinearLayout(this);
        logo.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        logo.setGravity(android.view.Gravity.BOTTOM);
        TextView n1 = new TextView(this);
        n1.setText("new ");
        n1.setTextSize(24f);
        n1.setTextColor(getResources().getColor(R.color.pr_text));
        TextView n2 = new TextView(this);
        n2.setText("speech");
        n2.setTextSize(24f);
        n2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        n2.setTextColor(getResources().getColor(R.color.pr_accent));
        logo.addView(n1);
        logo.addView(n2);
        box.addView(logo);
        TextView q = new TextView(this);
        q.setText(question);
        q.setTextSize(16f);
        q.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        q.setPadding(0, (int) (8 * d), 0, 0);
        box.addView(q);
        return box;
    }

    private void askSendAfterSave(File file) {
        if (!isLoggedIn()) return;
        new AlertDialog.Builder(this)
                .setCustomTitle(nsLogoTitle(L.t("Wysłać do NewSpeech?")))
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
        NoiseGate.strength = Math.max(1, Math.min(5, p.getInt("gate_strength", 2)));
        keepScreenOnEnabled = p.getBoolean("keep_screen_on", true); // domyslnie ekran NIE gasnie
        selectedFormat = p.getString("format", "mp3"); // domyslnie MP3
        LiveAudioData.pauseMinS = p.getFloat("pause_min", 1.0f);
        LiveAudioData.pauseMaxS = p.getFloat("pause_max", 2.5f);
        LiveAudioData.showPauses = p.getBoolean("show_pauses", true);
        LiveAudioData.showNorms = p.getBoolean("show_norms", true);
        LiveAudioData.showArrows = p.getBoolean("show_arrows", true);
        LiveAudioData.showTempo = p.getBoolean("show_tempo", false);
        if (!p.getBoolean("tempo_off_v1", false)) {
            // liczenie sylab/tempa na razie domyslnie WYLACZONE (do poprawy) — jednorazowo dla wszystkich
            LiveAudioData.showTempo = false;
            p.edit().putBoolean("tempo_off_v1", true).putBoolean("show_tempo", false).apply();
        }
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
                .putInt("gate_strength", NoiseGate.strength)
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
        if (BuildConfig.PLAY) c.addView(PremiumUi.card(this, this::onPremiumChanged)); // pelna wersja (zakup w Google Play)
        else if (!isLoggedIn()) c.addView(AnalysisOffer.card(this, false)); // niezalogowany: mozna wykupic analize mowy

        // 2) MAPA NAGRAN — duzy przycisk od razu pod logowaniem, obok NORMY
        final boolean logged = isLoggedIn();
        android.widget.LinearLayout tiles = Ui.row(this);
        Button mapBtn = Ui.button(this, "🗺  " + L.t("MAPA NAGRAŃ"), R.color.pr_accent, true);
        mapBtn.setTextSize(14f);
        mapBtn.setPadding((int) (12 * d), (int) (16 * d), (int) (12 * d), (int) (16 * d));
        mapBtn.setOnClickListener(v -> openMap());
        if (logged) tiles.addView(mapBtn, Ui.weight(2f, 8 * d)); // mapa wymaga logowania NS
        Button normBtn = Ui.button(this, "🎯  " + L.t("NORMY"), R.color.pr_purple, false);
        normBtn.setTextSize(13f);
        normBtn.setPadding((int) (12 * d), (int) (16 * d), (int) (12 * d), (int) (16 * d));
        normBtn.setOnClickListener(v -> renderNormsPage());
        if (logged) tiles.addView(normBtn, Ui.weight(1f, 0)); // normy tylko dla kursantow (po zalogowaniu)
        android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = (int) (8 * d);
        c.addView(tiles, tlp);

        // 3) Nagrywanie — ZWIJANE grupy (dotkniecie naglowka rozwija/zwija)
        android.widget.LinearLayout rec = section(c, "rec", "🎙  " + L.t("NAGRYWANIE") + "  ·  " + L.t("format i folder"));
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
        // GDZIE SA NAGRANIA + wybor folderu docelowego (kopia kazdego nowego nagrania)
        String tree = prefs().getString("export_tree_uri", null);
        String where = tree == null
                ? L.t("W pamięci aplikacji (lista NAGRANIA)") + "\n" + getFilesDir().getAbsolutePath()
                : L.t("W pamięci aplikacji + kopia w folderze:") + "\n📁 " + treeLabel(tree);
        rec.addView(settingRow("📁", L.t("Folder nagrań"), where, null));
        android.widget.LinearLayout fb = Ui.row(this);
        Button pick = Ui.button(this, tree == null ? L.t("Wybierz folder") : L.t("Zmień folder"), R.color.pr_accent, false);
        pick.setOnClickListener(v -> {
            Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(it, REQUEST_EXPORT_TREE); }
            catch (Exception e) { Toast.makeText(this, L.t("Brak systemowego wyboru folderu"), Toast.LENGTH_LONG).show(); }
        });
        fb.addView(pick, Ui.weight(1f, 6 * d));
        if (tree != null) {
            Button off = Ui.button(this, L.t("Bez kopii"), R.color.pr_muted, false);
            off.setOnClickListener(v -> { prefs().edit().remove("export_tree_uri").apply(); renderSettingsPage(); });
            fb.addView(off, Ui.weight(1f, 0));
        }
        rec.addView(fb);
        rec.addView(hint(L.t("Po wybraniu folderu każde nowe nagranie (MP3/WAV) zapisuje się też tam — widać je w Plikach telefonu i w komputerze.")));
        android.widget.LinearLayout snd = section(c, "snd", "🔊  " + L.t("DŹWIĘK") + "  ·  " + L.t("głośność i bramka szumów"));
        snd.addView(toggleRow("🔊 " + L.t("Automatyczna głośność (0 dB)"), LiveAudioData.autoNormalize, on -> { LiveAudioData.autoNormalize = on; saveDawSettings(); }));
        snd.addView(hint(LiveAudioData.autoNormalize ? L.t("Ciche nagrania po zapisie są podgłaśniane do 0 dB (bez zniekształceń)") : L.t("Wyłączone — nagranie zostaje bez zmian")));
        // GLOSNOSC WYNIKOWA: 0 dB albo dodatkowe wzmocnienie ×2…×10 (z miekkim limiterem)
        final int[] boosts = {1, 2, 4, 6, 8, 10};
        int bi = 0;
        for (int i = 0; i < boosts.length; i++) if (boosts[i] == LiveAudioData.outputBoost) bi = i;
        TextView boostLbl = Ui.text(this, boostLabel(LiveAudioData.outputBoost), 13f, R.color.pr_text);
        boostLbl.setPadding(0, (int) (8 * d), 0, 0);
        snd.addView(boostLbl);
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
        snd.addView(boostSlider);
        android.widget.LinearLayout ticks = Ui.row(this);
        for (int i = 0; i < boosts.length; i++) {
            TextView tk = Ui.text(this, i == 0 ? "0 dB" : "×" + boosts[i], 10f, R.color.pr_muted);
            tk.setGravity(i == 0 ? android.view.Gravity.START : i == boosts.length - 1 ? android.view.Gravity.END : android.view.Gravity.CENTER);
            ticks.addView(tk, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        snd.addView(ticks);
        snd.addView(hint(L.t("Gdy nagranie po zapisie jest za ciche — podgłośnij ×2…×10. Najgłośniejsze miejsca są łagodnie ograniczane (bez trzasków).")));
        snd.addView(toggleRow("🎚 " + L.t("Bramka szumów (tłumi cichy szum tła)"), LiveAudioData.noiseGateEnabled, on -> { LiveAudioData.noiseGateEnabled = on; saveDawSettings(); renderSettingsPage(); }));
        if (LiveAudioData.noiseGateEnabled) {
            // SILA BRAMKI 1..5 — ile razy mowa musi byc glosniejsza od zmierzonego szumu tla
            final String[] gNames = {L.t("delikatna"), L.t("lekka"), L.t("średnia"), L.t("mocna"), L.t("bardzo mocna")};
            TextView gLbl = Ui.text(this, L.f("Siła bramki: {0}", gNames[NoiseGate.strength - 1]), 13f, R.color.pr_text);
            gLbl.setPadding(0, (int) (6 * d), 0, 0);
            snd.addView(gLbl);
            SliderView gSlider = new SliderView(this);
            gSlider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (36 * d)));
            gSlider.setValue((NoiseGate.strength - 1) / 4f);
            gSlider.setOnValueChangeListener(v -> {
                int k = 1 + Math.max(0, Math.min(4, Math.round(v * 4)));
                if (k != NoiseGate.strength) {
                    NoiseGate.strength = k;
                    gLbl.setText(L.f("Siła bramki: {0}", gNames[k - 1]));
                    saveDawSettings();
                }
            });
            snd.addView(gSlider);
            String measured = NoiseGate.noiseRms > 0 && !NoiseGate.learning
                    ? " " + L.f("Ostatnio zmierzony szum tła: {0} dB.", String.format(Locale.US, "%.0f", NoiseGate.dbOf(NoiseGate.noiseRms))) : "";
            snd.addView(hint(L.t("W pierwszej sekundzie nagrania bramka sama mierzy szum tła (najlepiej chwilę poczekaj, zanim zaczniesz mówić). Potem ścisza tylko to, co nie jest wyraźnie głośniejsze od szumu. Gdy ucina ciche słowa — zmniejsz siłę; gdy szum dalej słychać — zwiększ.") + measured));
        }
        android.widget.LinearLayout scr = section(c, "scr", "📱  " + L.t("EKRAN NAGRYWANIA"));
        scr.addView(toggleRow("🎚 " + L.t("Suwaki MIC i ZOOM na ekranie nagrywania"), prefs().getBoolean("show_sliders", true), on -> { prefs().edit().putBoolean("show_sliders", on).apply(); applySliderVisibility(); }));
        scr.addView(hint(L.t("Gdy mikrofon jest już dobrze ustawiony, możesz je schować — ekran nagrywania będzie prostszy.")));
        scr.addView(toggleRow("🔆 " + L.t("Ekran nie gaśnie (wyłączony wygaszacz)"), keepScreenOnEnabled, on -> { keepScreenOnEnabled = on; applyKeepScreenOnSetting(); saveDawSettings(); }));
        final View[] gpsHolder = {null};
        boolean gpsOk = GpsHelper.hasPermission(this);
        View gpsRow = settingRow("📍", L.t("Lokalizacja GPS"), gpsOk ? L.t("zezwolono ✓") : L.t("brak zgody — dotknij, aby zezwolić"), null);
        gpsRow.setOnClickListener(v -> requestLocationPermission(this));
        gpsHolder[0] = gpsRow;

        // 3a) Przypomnienia (tylko po zalogowaniu — sprawdzaja nagrania i dziennik w NS)
        android.widget.LinearLayout rem = logged ? section(c, "rem", "🔔  " + L.t("PRZYPOMNIENIA")) : new android.widget.LinearLayout(this);
        if (logged && HarmoGoal.ended(this)) {
            TextView paused = Ui.text(this, "⏸ " + L.t("Harmonogram się zakończył — powiadomienia są wstrzymane. Wrócą same, gdy trener doda nowy harmonogram."), 12f, R.color.pr_warn);
            paused.setPadding(0, 0, 0, (int) (6 * d));
            rem.addView(paused);
        }
        rem.setOrientation(android.widget.LinearLayout.VERTICAL);
        addNotificationHealth(rem);
        rem.addView(toggleRow("🔔 " + L.t("Przypomnienie o 20:00"), ReminderReceiver.enabled(this), on -> {
            prefs().edit().putBoolean("reminder_on", on).apply();
            ReminderReceiver.schedule(this);
            if (on && android.os.Build.VERSION.SDK_INT >= 33
                    && ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 7301);
        }));
        rem.addView(hint(L.t("Jeśli do 20:00 nie wgrasz nagrania do NewSpeech albo nie napiszesz dziennika, telefon Ci o tym przypomni.")));
        rem.addView(toggleRow("✅ " + L.t("Powiadomienia o ocenie trenera"), TrainerWatch.enabled(this), on -> {
            prefs().edit().putBoolean("trainer_notif", on).apply();
            TrainerWatch.schedule(this);
        }));
        rem.addView(hint(L.t("Gdy trener zaliczy nagranie, poprosi o poprawkę albo odpowie na dziennik — dostaniesz powiadomienie.")));
        rem.addView(hint("📞 " + L.t("Gdy inny kursant dotknie „Chcę porozmawiać”, dostaniesz powiadomienie z jego imieniem, miejscowością i telefonem (bez powiadomień w nocy).")));
        rem.addView(toggleRow("📅 " + L.t("Dni telefonu do trenera"), prefs().getBoolean("call_days_notif", true), on -> {
            prefs().edit().putBoolean("call_days_notif", on).apply();
            AvailWatch.schedule(this);
        }));
        rem.addView(hint(L.t("Gdy trener wyznaczy dni, w które masz do niego zadzwonić, dostaniesz powiadomienie; w dniu telefonu — przypomnienie (godzinę wcześniej).")));

        // 3b) Podpowiedzi na wykresie i w statystykach
        android.widget.LinearLayout hints = section(c, "hints", "💡  " + L.t("PODPOWIEDZI") + "  ·  " + L.t("pauzy, strzałki, tempo"));
        hints.addView(toggleRow("⏸ " + L.t("Pauzy na wykresie"), LiveAudioData.showPauses, on -> { LiveAudioData.showPauses = on; saveDawSettings(); }));
        // normy i strzalki — tylko dla zalogowanych kursantow
        if (logged) hints.addView(toggleRow("🎯 " + L.t("Ocena emisji wg norm"), LiveAudioData.showNorms, on -> { LiveAudioData.showNorms = on; saveDawSettings(); }));
        if (logged) hints.addView(toggleRow("↗ " + L.t("Strzałki intonacji"), LiveAudioData.showArrows, on -> { LiveAudioData.showArrows = on; saveDawSettings(); }));
        if (logged && LiveAudioData.showArrows) {
            hints.addView(normStepper("   " + L.t("Strzałka od zmiany tonu o"), LiveAudioData.arrowThresholdSt, 0.5f, 6f, 0.5f, "%.1f st", v -> { LiveAudioData.arrowThresholdSt = v; saveDawSettings(); }));
            hints.addView(hint(L.t("st = półton. Mniejsza zmiana tonu w sylabie nie daje strzałki; im większa zmiana, tym bardziej stroma strzałka.")));
        }
        hints.addView(toggleRow("🗣 " + L.t("Pomiar sylab i tempo (sylaby na minutę)"), LiveAudioData.showTempo, on -> { LiveAudioData.showTempo = on; saveDawSettings(); }));
        hints.addView(toggleRow("📈 " + L.t("Licz pitch od razu przy otwieraniu nagrania"), prefs().getBoolean("auto_pitch", false), on -> prefs().edit().putBoolean("auto_pitch", on).apply()));
        if (logged) hints.addView(toggleRow("💡 " + L.t("Inspiracje na dziś (Statystyki)"), prefs().getBoolean("show_insp", true), on -> prefs().edit().putBoolean("show_insp", on).apply()));
        hints.addView(hint(L.t("Podpowiedzi pojawiają się na wykresie w czasie nagrywania i przy odsłuchu.")));

        // (Zakres pauz jest teraz w NORMY)

        // 5) Wyglad DAW
        android.widget.LinearLayout daw = section(c, "daw", "🎨  " + L.t("WYGLĄD DAW") + "  ·  " + L.t("kolory i linie"));
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
        android.widget.LinearLayout mp = section(c, "map", logged ? "📍  " + L.t("MAPA, GPS I DOSTĘPNOŚĆ") : "📍  GPS");
        if (gpsHolder[0] != null) { mp.addView(gpsHolder[0]); if (logged) mp.addView(divider()); }
        if (logged) addMapSettings(mp); // mapa i "Chętnie porozmawiam" — tylko dla zalogowanych

        // 7) Jezyk
        if (!BuildConfig.PLAY) { // samoaktualizacja — tylko wersja kursantow (w Google Play aktualizuje sklep)
            android.widget.LinearLayout upd = section(c, "upd", "⬆  " + L.t("AKTUALIZACJE") + "  ·  " + UpdateChecker.currentName(this));
            upd.addView(toggleRow("⬆ " + L.t("Automatycznie pobieraj nowe wersje"), UpdateChecker.enabled(this), on -> prefs().edit().putBoolean("auto_update", on).apply()));
            upd.addView(hint(L.t("Nowa wersja pobiera się sama w tle; zostaniesz zapytany o instalację. Nagrania i ustawienia zostają.")));
            android.widget.Button updBtn = Ui.button(this, L.t("Sprawdź teraz"), R.color.pr_accent, false);
            updBtn.setOnClickListener(v -> { Toast.makeText(this, L.t("Sprawdzanie…"), Toast.LENGTH_SHORT).show(); UpdateChecker.check(this, true); });
            upd.addView(updBtn);
        }
        if (logged && !BuildConfig.PLAY) { // nagrywanie rozmow — tylko dla kursantow (nie ma go w wersji Google Play)
            android.widget.LinearLayout crs = section(c, "callrec", "📞  " + L.t("NAGRYWANIE ROZMÓW") + "  ·  " + (CallRecService.enabled(this) ? L.t("WŁ") : L.t("WYŁ")));
            CallRecUi.fillSettings(this, crs, this::renderSettingsPage);
        }
        renderLangAndAbout(c, d);
    }

    private void addMapSettings(android.widget.LinearLayout mp) {
        mp.addView(toggleRow("📡 " + L.t("Pokazuj mnie na mapie na żywo"), prefs().getBoolean("map_share", true), on -> prefs().edit().putBoolean("map_share", on).apply()));
        mp.addView(hint(L.t("Po zapisaniu nagrania w kategorii Sklepy, Przechodzień, Special albo Miasto – inne inni kursanci widzą Cię na mapie przez ok. 20 min: imię, miasto, system mowy i kategorię.")));
        mp.addView(hint(L.t("Na mapie jako:") + " " + mapName(prefs().getString("student_name", ""))));
    }

    // Jeden przycisk "Chcę porozmawiać" + co zobacza inni
    private void addWantTalk(android.widget.LinearLayout mp) {
        boolean avail = availOn();
        Button av = Ui.button(this, avail ? "✅ " + L.t("Chcę porozmawiać") + " · " + L.f("do {0}", availUntilText()) : "📞 " + L.t("Chcę porozmawiać"),
                avail ? R.color.pr_warn : R.color.pr_accent, true);
        av.setTextSize(15f);
        av.setOnClickListener(v -> wantTalk(this::renderSettingsPage));
        mp.addView(av, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        mp.addView(hint(avail ? L.f("Inni kursanci dostali powiadomienie i widzą Cię na mapie. Dotknij, żeby wyłączyć (wyłączy się samo o {0}).", availUntilText())
                : L.t("Jedno dotknięcie: inni kursanci dostaną powiadomienie z Twoim imieniem, miejscowością i telefonem i zobaczą Cię na mapie. Wyłącza się samo po 2 godzinach.")));
        String phone = prefs().getString("map_phone", "");
        if (!phone.isEmpty()) {
            String city = prefs().getString("ns_city", "");
            mp.addView(hint(L.t("Inni zobaczą:") + " " + mapName(prefs().getString("student_name", "")) + (city.isEmpty() ? "" : " · " + city) + " · ☎ " + phone));
        }
    }

    private void renderLangAndAbout(android.widget.LinearLayout c, float d) {
        android.widget.LinearLayout lang = section(c, "lang", "🌐  " + L.t("JĘZYK") + "  ·  " + currentLangName());
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

        // 8) O aplikacji
        TextView about = Ui.text(this, L.t("PitchRec · v") + appVersion() + L.t(" · NewSpeech"), 10f, R.color.pr_muted);
        about.setGravity(android.view.Gravity.CENTER);
        about.setPadding(0, (int) (10 * d), 0, 0);
        c.addView(about);
        c.addView(Ui.spacer(this, 20));
    }

    // ZWIJANE sekcje Ustawien: naglowek zawsze widoczny, tresc po dotknieciu (stan zapamietany)
    private java.util.Set<String> openSections = null;

    private android.widget.LinearLayout section(android.widget.LinearLayout parent, String key, String title) {
        if (openSections == null) openSections = new java.util.HashSet<>(prefs().getStringSet("set_open", new java.util.HashSet<>()));
        float d = getResources().getDisplayMetrics().density;
        boolean open = openSections.contains(key);
        android.widget.LinearLayout card = Ui.card(this);
        android.widget.LinearLayout head = Ui.row(this);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, title, 13f, R.color.pr_text);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(t, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(Ui.text(this, open ? "▲" : "▼", 14f, R.color.pr_accent));
        head.setPadding(0, (int) (6 * d), 0, (int) (6 * d));
        head.setOnClickListener(v -> {
            if (!openSections.remove(key)) openSections.add(key);
            prefs().edit().putStringSet("set_open", new java.util.HashSet<>(openSections)).apply();
            renderSettingsPage();
        });
        card.addView(head);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        parent.addView(card, lp);
        android.widget.LinearLayout body = new android.widget.LinearLayout(this);
        body.setOrientation(android.widget.LinearLayout.VERTICAL);
        body.setPadding(0, (int) (8 * d), 0, 0);
        if (open) card.addView(body);
        return body;
    }

    private String currentLangName() {
        switch (getSavedLanguage(this)) {
            case "en": return "English"; case "cs": return "Čeština"; case "sk": return "Slovenčina";
            case "de": return "Deutsch"; case "es": return "Español"; case "hu": return "Magyar";
            default: return "Polski";
        }
    }

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

        c.addView(sectionHeader(L.t("PAUZY — ZAKRES PRAWIDŁOWY")));
        android.widget.LinearLayout pz = Ui.card(this);
        android.widget.LinearLayout pzRow = Ui.row(this);
        pzRow.addView(pauseStepper("MIN", true), Ui.weight(1f, 10 * d));
        pzRow.addView(pauseStepper("MAX", false), Ui.weight(1f, 0));
        pz.addView(pzRow);
        pz.addView(hint(String.format(Locale.US, L.t("Pauza między porcjami mowy %.1f–%.1f s liczy się jako prawidłowa (regulacja 1–4 s)"), LiveAudioData.pauseMinS, LiveAudioData.pauseMaxS)));
        c.addView(pz);

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
            // odswiez strone NORMY, zachowujac miejsce przewiniecia
            View sv = findViewById(R.id.setPage);
            int y = sv != null ? sv.getScrollY() : 0;
            renderNormsPage();
            if (sv != null) sv.post(() -> sv.scrollTo(0, y));
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
            fixListCache = list; updateBadges();
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
            // komentarz GLOSOWY trenera do tego nagrania (jesli nagral)
            android.widget.LinearLayout vrBox = new android.widget.LinearLayout(this);
            vrBox.setOrientation(android.widget.LinearLayout.VERTICAL);
            card.addView(vrBox);
            checkVoiceReview(fe.id, vrBox, d, true);
            // poprawka juz wyslana? (nagranie z telefonu z fixRecordId = to nagranie, wyslane po ocenie)
            File sentFix = null;
            File[] allF = getFilesDir().listFiles((dir, n) -> n.endsWith(".wav") || n.endsWith(".mp3"));
            if (allF != null) for (File f : allF) {
                RecMeta m = RecMeta.load(this, f.getName());
                if (fe.id.equals(m.fixRecordId) && "sent".equals(m.ns) && f.lastModified() > fe.reviewedAt * 1000L
                        && (sentFix == null || f.lastModified() > sentFix.lastModified())) sentFix = f;
            }
            // POPRAWKA JUZ WYSLANA — czeka na ponowna ocene
            if (sentFix != null) {
                TextView pend = Ui.text(this, "⏳ " + L.f("Poprawka wysłana {0} — czeka na ponowną ocenę trenera", new java.text.SimpleDateFormat("d.MM, HH:mm", Locale.getDefault()).format(new java.util.Date(sentFix.lastModified()))), 13f, R.color.pr_pause);
                pend.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                pend.setPadding(0, (int) (10 * d), 0, 0);
                card.addView(pend);
                card.setBackground(Ui.rounded(getResources().getColor(R.color.pr_card), getResources().getColor(R.color.pr_pause), d, 14 * d));
            }
            card.addView(Ui.spacer(this, 10));
            Button rec = Ui.button(this, sentFix != null ? L.t("🎤 Nagraj jeszcze raz") : L.t("🎤 Nagraj poprawkę"), sentFix != null ? R.color.pr_muted : R.color.pr_accent, true);
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
        hidePitchButton();
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
        releasePlayer(); // konczy tez petle bialej linii
        pendingSeekSample = 0L;
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
        syncButtons();
    }

    private void stopRecordingFlow() {
        // jeszcze nie ma pozycji z nagrania (GPS sie spoznil) — probujemy teraz, na miejscu nagrania
        if (GpsHelper.recordingFix == null && GpsHelper.hasPermission(this))
            GpsHelper.requestFix(this, loc -> { if (loc != null && GpsHelper.recordingFix == null) GpsHelper.recordingFix = loc; });
        releasePlayer(); // podglad z pauzy nie moze grac dalej po STOP
        saving = true;
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
        syncButtons();
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
        syncButtons();
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
        lastDisplayedSecond = -1L; // licznik czasu od razu wraca do czasu nagrania (po odsluchu w pauzie)
        recordButton.setButtonText(getString(R.string.btn_pause));
        statusText.setText(getString(R.string.status_recording));
        headerStatus.setText(L.t("● NAGRYWA"));
        headerStatus.setTextColor(getResources().getColor(R.color.pr_warn));
        playButton.setButtonEnabled(false);
        // podglad z pauzy konczymy — inaczej petla odtwarzacza dalej rysowala biala linie
        releasePlayer();
        pendingSeekSample = 0L;
        pitchWaveView.resetPan(); // czysci biala linie (playhead) — niepotrzebna podczas nagrywania na zywo
        pitchWaveView.setLiveMode(true); // wraca do auto-przewijania najnowszych probek
        syncButtons();
    }

    private void resetRecording() {
        if (isRecording) stopRecordingFlow();
        releasePlayer();
        loadedFilePath = null;
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
        if (requestCode == CallRecUi.REQ_PERMS || requestCode == 7301) { CallRecUi.refreshOpenSetup(); return; } // kreator nagrywania rozmow / powiadomienia
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
        runOnUiThread(() -> afterRecordingSaved(durationMs, () -> saveRecordingForPlayback(base64, mimeType)));
    }

    @Override
    public void onSuccessFile(String path, long durationMs, String mimeType) {
        runOnUiThread(() -> afterRecordingSaved(durationMs, () -> {
            // plik z cache przenosimy do katalogu nagran (ten sam dysk = natychmiast, bez kopiowania)
            File src = new File(path);
            String ext = "audio/mpeg".equals(mimeType) ? ".mp3" : ".wav";
            File dest = new File(getFilesDir(), "recording_" + System.currentTimeMillis() + ext);
            if (!src.renameTo(dest)) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(src); FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] b = new byte[1 << 16];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    src.delete();
                } catch (IOException e) {
                    statusText.setText(L.t("Błąd zapisu: ") + e.getMessage());
                    return;
                }
            }
            lastSavedFilePath = dest.getAbsolutePath();
        }));
    }

    private void afterRecordingSaved(long durationMs, Runnable save) {
        {
            statusText.setText(L.t("Nagrano ") + (durationMs / 1000) + L.t("s — zapisano"));
            save.run();
            loadedFilePath = null; // swiezo nagrany plik jest juz w LiveAudioData, nie trzeba wczytywac ponownie
            pendingSeekSample = 0L;
            lastRecordingTotalDurationMs = durationMs;
            timeText.setText("00:00 / " + formatMs(durationMs)); // jak odtwarzacz: pozycja/calkowita dlugosc
            pitchWaveView.setLiveMode(false); // pozwala przewijac/wskazac miejsce w tym co wlasnie nagrano
            pitchWaveView.resetPan();
            pitchWaveView.invalidate();
            saving = false;
            syncButtons();
            headerStatus.setText(L.t("● GOTOWY"));
            headerStatus.setTextColor(getResources().getColor(R.color.pr_muted));
            // Jak w PitchRec: po STOP od razu ekran "ZAPISZ NAGRANIE" (opis nagrania)
            if (lastSavedFilePath != null) describeNewRecording(new File(lastSavedFilePath));
        }
    }

    private String formatMs(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    @Override
    public void onError(String code, String message) {
        runOnUiThread(() -> { saving = false; statusText.setText(L.t("Błąd: ") + code + " — " + message); syncButtons(); });
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
        if (path == null) return;
        releasePlayer();
        try {
            final PcmPlayer me = new PcmPlayer(new File(path), sampleIndex);
            pcm = me;
            me.start(() -> {
                if (pcm != me) return;
                pcm = null;
                statusText.setText(getString(R.string.status_paused));
                syncButtons();
            });
            playButton.setButtonText(getString(R.string.btn_pause_play));
            statusText.setText(getString(R.string.status_playing));
            pitchWaveView.clearTouchHold();
            startPcmLoop();
        } catch (Exception e) {
            pcm = null;
            statusText.setText(L.t("Błąd podglądu: ") + e.getMessage());
        }
        syncButtons();
    }

    // ODSLUCH W PAUZIE — pozycja prosto z odtwarzacza PCM (dokladna), odswiezanie co klatke ekranu
    private PcmPlayer pcm = null;
    private boolean pcmLoopOn = false;
    private final Runnable pcmLoop = new Runnable() {
        @Override public void run() {
            PcmPlayer p = pcm;
            if (p == null || !isPaused) { pcmLoopOn = false; return; }
            long pos = p.positionSample();
            pitchWaveView.setPlayheadSample(pos);
            long ms = pos * 1000L / LiveAudioData.SAMPLE_RATE;
            timeText.setText(formatMs(ms) + " / " + formatMs(p.totalSamples() * 1000L / LiveAudioData.SAMPLE_RATE));
            if (p.isPlaying()) pitchWaveView.postOnAnimation(this); else pcmLoopOn = false;
        }
    };

    private void startPcmLoop() {
        if (pcmLoopOn) return;
        pcmLoopOn = true;
        pitchWaveView.postOnAnimation(pcmLoop);
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
    // Wczytywanie w TLE (wczesniej na glownym watku — ekran stal, az caly plik sie przeliczyl)
    private int loadGen = 0;

    private void loadAndDisplayFile(File file) {
        final int gen = ++loadGen;
        hidePitchButton();
        // STARY odtwarzacz (poprzednie nagranie) musi zniknac — inaczej PLAY wznawial go
        // i grala poprzednia pamiec, choc na wykresie byl juz nowy plik.
        releasePlayer();
        pendingSeekSample = 0L;
        loadedFilePath = file.getAbsolutePath();
        AudioFileLoader.cancelPending();
        syncButtons();
        pitchWaveView.setLiveMode(false);
        pitchWaveView.resetPan();
        statusText.setText("⏳ " + L.t("Wczytywanie nagrania…"));
        // 1) sama fala — szybko
        new Thread(() -> {
            String err = null;
            boolean withPitch = false;
            try { withPitch = AudioFileLoader.loadWave(file); }
            catch (Exception e) { err = e.getMessage(); }
            final String fe = err;
            final boolean hasPitch = withPitch;
            runOnUiThread(() -> {
                if (gen != loadGen) return;
                pitchWaveView.resetPan();
                pitchWaveView.invalidate();
                if (fe != null) { statusText.setText(L.t("Błąd wczytywania: ") + fe); return; }
                // dlugosc wczytanego nagrania na liczniku (pozycja / calosc), jak po nagraniu
                lastRecordingTotalDurationMs = LiveAudioData.getTotalSamplesWritten() * 1000L / LiveAudioData.SAMPLE_RATE;
                timeText.setText(formatMs(0) + " / " + formatMs(lastRecordingTotalDurationMs));
                if (hasPitch) { statusText.setText(L.t("Wczytano nagranie z linią pitch")); return; } // z pamieci podrecznej
                statusText.setText(L.t("Wczytano nagranie"));
                // 2) pitch tylko na zyczenie (pauz przy odsluchu nie liczymy)
                if (prefs().getBoolean("auto_pitch", false)) { loadPitchFor(file, gen); return; }
                // bez pytania — przycisk "Policz linię pitch" pod wykresem
                showPitchButton(file, gen);
            });
        }, "load-daw").start();
    }

    // ── PRZYCISK "📈 Policz linię pitch" pod wykresem (zamiast pytania przy otwieraniu nagrania) ──
    private Button pitchButton;

    private void showPitchButton(File file, int gen) {
        if (pitchButton == null) {
            View st = findViewById(R.id.statusText);
            if (st == null || !(st.getParent() instanceof android.widget.LinearLayout)) return;
            android.widget.LinearLayout parent = (android.widget.LinearLayout) st.getParent();
            pitchButton = Ui.button(this, "📈 " + L.t("Policz linię pitch"), R.color.pr_accent, true);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            int m = (int) (6 * getResources().getDisplayMetrics().density);
            lp.setMargins(m, m / 2, m, m / 2);
            parent.addView(pitchButton, parent.indexOfChild(st) + 1, lp);
        }
        pitchButton.setOnClickListener(v -> loadPitchFor(file, gen));
        pitchButton.setVisibility(View.VISIBLE);
    }

    private void hidePitchButton() { if (pitchButton != null) pitchButton.setVisibility(View.GONE); }

    private void loadPitchFor(File file, int gen) {
        hidePitchButton();
        if (gen != loadGen) return;
        statusText.setText("⏳ " + L.t("Liczenie linii pitch…"));
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final boolean[] done = {false};
        final Runnable tick = new Runnable() {
            @Override public void run() {
                if (done[0] || gen != loadGen) return;
                pitchWaveView.invalidate();
                h.postDelayed(this, 300);
            }
        };
        h.postDelayed(tick, 300);
        new Thread(() -> {
            String err = null;
            try { AudioFileLoader.addPitch(file, false); }
            catch (Exception e) { err = e.getMessage(); }
            final String fe = err;
            runOnUiThread(() -> {
                done[0] = true;
                if (gen != loadGen) return;
                pitchWaveView.invalidate();
                statusText.setText(fe == null ? L.t("Wczytano nagranie z linią pitch") : L.t("Błąd wczytywania: ") + fe);
            });
        }, "load-pitch").start();
    }

    // startSampleIndex: pozycja (w próbkach), od której ma zacząć się odtwarzanie — 0 =
    // od początku. Odpowiada dotknięciu wykresu (suwak odtwarzania).
    // Odtwarzanie pliku: przygotowanie W TLE (prepareAsync — duze MP3 nie blokuja ekranu),
    // wywolania od starego odtwarzacza sa ignorowane, a gdy telefon nie potwierdzi przewiniecia,
    // start i tak nastepuje po 0,7 s.
    private void playFile(String path, long startSampleIndex) {
        releasePlayer();
        final MediaPlayer player = new MediaPlayer();
        currentPlayer = player;
        playingPath = path;
        final int startMs = (int) (startSampleIndex * 1000L / LiveAudioData.SAMPLE_RATE);
        final boolean[] started = {false};
        final Runnable go = () -> {
            if (currentPlayer != player || started[0]) return;
            started[0] = true;
            try { player.start(); } catch (Exception e) { releasePlayer(); syncButtons(); return; }
            statusText.setText(getString(R.string.status_playing));
            pitchWaveView.clearTouchHold();
            startPlayheadUpdateLoop();
            syncButtons();
        };
        player.setOnCompletionListener(mp -> {
            if (currentPlayer != mp) return;
            try { mp.release(); } catch (Exception e) { }
            currentPlayer = null;
            playLoopOn = false;
            syncButtons();
        });
        player.setOnErrorListener((mp, what, extra) -> {
            if (currentPlayer == mp) { releasePlayer(); statusText.setText(L.t("Błąd odtwarzania: ") + what); syncButtons(); }
            return true;
        });
        player.setOnPreparedListener(mp -> {
            if (currentPlayer != mp) return;
            if (startMs > 0) {
                mp.setOnSeekCompleteListener(m2 -> go.run());
                try { seekExact(mp, startMs); } catch (Exception e) { go.run(); return; }
                redrawHandler.postDelayed(go, 700);
            } else go.run();
        });
        try {
            player.setDataSource(path);
            player.prepareAsync();
            playButton.setButtonText(getString(R.string.btn_pause_play));
        } catch (Exception e) {
            releasePlayer();
            statusText.setText(L.t("Błąd odtwarzania: ") + e.getMessage());
            syncButtons();
        }
    }

    // PRZYCISKI zawsze zgodne z faktycznym stanem (nagrywanie / pauza / odtwarzanie) —
    // wczesniej zdarzalo sie, ze REC i PLAY jednoczesnie pokazywaly "pauze".
    private volatile boolean saving = false; // po STOP, zanim plik jest gotowy — PLAY zablokowany (nie gra starego pliku)

    private boolean anyPlaying() {
        try {
            if (pcm != null && pcm.isPlaying()) return true;
            return currentPlayer != null && currentPlayer.isPlaying();
        } catch (Exception e) { return false; }
    }

    private void syncButtons() {
        if (recordButton == null || playButton == null || pauseButton == null) return;
        recordButton.setButtonText(!isRecording ? getString(R.string.btn_rec) : isPaused ? getString(R.string.btn_resume) : getString(R.string.btn_pause));
        boolean hasFile = loadedFilePath != null || lastSavedFilePath != null;
        boolean playEn = isRecording ? isPaused : (hasFile && !saving);
        playButton.setButtonEnabled(playEn);
        playButton.setButtonText(playEn && anyPlaying() ? getString(R.string.btn_pause_play) : getString(R.string.btn_play));
        pauseButton.setButtonEnabled(isRecording);
    }

    // co 0,4 s kontrola spojnosci przycisków (tani test — tylko gdy widac ekran nagrywania)
    private final Runnable buttonGuard = new Runnable() {
        @Override public void run() {
            if ("daw".equals(currentPage)) syncButtons();
            redrawHandler.postDelayed(this, 400);
        }
    };

    private String playingPath = null;

    // Zwalnia odtwarzacz (np. przy wczytaniu innego nagrania) i zatrzymuje suwak
    private void releasePlayer() {
        if (pcm != null) { pcm.release(); pcm = null; }
        pcmLoopOn = false;
        if (currentPlayer != null) {
            try { currentPlayer.release(); } catch (Exception e) { }
            currentPlayer = null;
        }
        playingPath = null;
        playLoopOn = false;
        if (playButton != null) playButton.setButtonText(getString(R.string.btn_play));
    }

    // Dokladne przewijanie: SEEK_CLOSEST (Android 8+) — zwykle seekTo w MP3 na czesci telefonow
    // skacze do najblizszej "klatki kluczowej" i dzwiek nie zgadzal sie z miejscem na wykresie.
    private static void seekExact(MediaPlayer p, int ms) {
        if (android.os.Build.VERSION.SDK_INT >= 26) p.seekTo((long) ms, MediaPlayer.SEEK_CLOSEST);
        else p.seekTo(ms);
    }

    // PLYNNY suwak: odswiezanie z kazda klatka ekranu (jak podczas nagrywania), a pozycja
    // przewidywana z zegara — odtwarzacz na wielu telefonach podaje pozycje skokami co
    // 100-200 ms, przez co siatka i sekundy "skakaly". Pozycje z odtwarzacza sluza tylko do korekty.
    private boolean playLoopOn = false;
    private long anchorPosMs = 0L, anchorNanos = 0L;
    private int lastReportedMs = -1;

    private final Runnable playheadUpdateLoop = new Runnable() {
        @Override
        public void run() {
            MediaPlayer mp = currentPlayer;
            if (mp == null || !playLoopOn || (isRecording && !isPaused)) { playLoopOn = false; return; }
            try {
                boolean playing = mp.isPlaying();
                int rep = mp.getCurrentPosition();
                long now = System.nanoTime();
                long pos;
                if (!playing) {
                    pos = rep; anchorPosMs = rep; anchorNanos = now; lastReportedMs = rep;
                } else {
                    long predicted = anchorPosMs + (now - anchorNanos) / 1_000_000L;
                    if (rep != lastReportedMs) {
                        lastReportedMs = rep;
                        long drift = rep - predicted;
                        if (Math.abs(drift) > 120) { anchorPosMs = rep; anchorNanos = now; predicted = rep; }
                        else { anchorPosMs += drift / 8; predicted += drift / 8; } // lagodna korekta
                    }
                    pos = predicted;
                }
                if (lastRecordingTotalDurationMs > 0) pos = Math.min(pos, lastRecordingTotalDurationMs);
                pitchWaveView.setPlayheadSample(pos * LiveAudioData.SAMPLE_RATE / 1000L);
                timeText.setText(formatMs(pos) + " / " + formatMs(lastRecordingTotalDurationMs));
            } catch (IllegalStateException e) { /* odtwarzacz mogl sie juz zwolnic */ }
            pitchWaveView.postOnAnimation(this);
        }
    };

    private void startPlayheadUpdateLoop() {
        MediaPlayer mp = currentPlayer;
        try { anchorPosMs = mp != null ? mp.getCurrentPosition() : 0; } catch (Exception e) { anchorPosMs = 0; }
        anchorNanos = System.nanoTime();
        lastReportedMs = (int) anchorPosMs;
        if (playLoopOn) return;
        playLoopOn = true;
        pitchWaveView.postOnAnimation(playheadUpdateLoop);
    }

    // ── STRONA NAGRANIA (jak #pg-recs w PitchRec) ──
    private static final int REQUEST_IMPORT_FILE = 200;
    private static final int REQUEST_EXPORT_TREE = 201;
    private static final int REQUEST_SAVE_FILE = 202;
    private File pendingSaveFile = null;

    // Czytelna nazwa wybranego folderu, np. "Pamięć wewnętrzna/Music/NewSpeech"
    private String treeLabel(String treeUri) {
        try {
            String id = android.provider.DocumentsContract.getTreeDocumentId(android.net.Uri.parse(treeUri));
            int k = id.indexOf(':');
            String vol = k >= 0 ? id.substring(0, k) : id, path = k >= 0 ? id.substring(k + 1) : "";
            String v = "primary".equalsIgnoreCase(vol) ? L.t("Pamięć wewnętrzna") : vol;
            return path.isEmpty() ? v : v + "/" + path;
        } catch (Exception e) { return treeUri; }
    }

    // Kopia nagrania do folderu wybranego w Ustawieniach (w tle, bez komunikatow przy sukcesie)
    private void exportToFolder(File f) {
        final String t = prefs().getString("export_tree_uri", null);
        if (t == null || f == null || !f.exists()) return;
        new Thread(() -> {
            try {
                android.net.Uri tree = android.net.Uri.parse(t);
                android.net.Uri dir = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, android.provider.DocumentsContract.getTreeDocumentId(tree));
                String mime = f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
                android.net.Uri out = android.provider.DocumentsContract.createDocument(getContentResolver(), dir, mime, f.getName());
                if (out == null) throw new IOException("createDocument");
                try (java.io.InputStream in = new java.io.FileInputStream(f); java.io.OutputStream os = getContentResolver().openOutputStream(out)) {
                    byte[] b = new byte[1 << 16];
                    int n;
                    while ((n = in.read(b)) > 0) os.write(b, 0, n);
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, L.t("⚠ Nie udało się zapisać kopii w wybranym folderze — wybierz folder ponownie w Ustawieniach"), Toast.LENGTH_LONG).show());
            }
        }, "export-rec").start();
    }

    private void showRecordingsList() {
        if ("recs".equals(currentPage)) renderRecsPage(); else showPage("recs");
    }

    private void renderRecsPage() {
        android.widget.LinearLayout c = findViewById(R.id.recsContent);
        if (c == null) return;
        c.removeAllViews();
        sendButtons.clear();
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
        dlAll.setOnClickListener(v -> downloadAllRecordings(finalFiles));
        Button shAll = Ui.button(this, "📤", R.color.pr_accent, false);
        shAll.setOnClickListener(v -> shareAllRecordings(finalFiles));
        actions.addView(dlAll, Ui.weight(1f, 8 * d));
        actions.addView(shAll, Ui.weight(0.35f, 8 * d));
        Button imp = Ui.button(this, L.t("📁 Wgraj plik"), R.color.pr_warn, false);
        imp.setOnClickListener(v -> importExternalFile());
        actions.addView(imp, Ui.weight(1f, 0));
        c.addView(actions);
        c.addView(Ui.spacer(this, 14));

        // Filtry wg statusu (pewne dane: wysylka z telefonu + ocena w NS)
        if (!isLoggedIn()) recsFilter = "all";
        if (finalFiles.length > 0 && isLoggedIn()) { // bez logowania filtry (do poprawy / czekaja / niewyslane) nic nie znacza
            if (isLoggedIn()) { NsStatus.refresh(this, () -> runOnUiThread(() -> { if ("recs".equals(currentPage)) renderRecsPage(); })); scheduleRecsPoll(); }
            android.widget.LinearLayout fl = Ui.row(this);
            String[][] fs = {{"all", L.t("Wszystkie")}, {"bad", "↺ " + L.t("Do poprawy")}, {"wait", "⏳ " + L.t("Czekają")}, {"unsent", "☁ " + L.t("Niewysłane")}};
            for (String[] f : fs) {
                if (!isLoggedIn() && !"all".equals(f[0]) && !"unsent".equals(f[0])) continue;
                int cnt = 0;
                if (!"all".equals(f[0])) for (File x : finalFiles) if (recMatches(x, f[0])) cnt++;
                boolean on = f[0].equals(recsFilter);
                TextView chip = Ui.text(this, f[1] + ("all".equals(f[0]) ? "" : " " + cnt), 11f, on ? R.color.pr_bg : R.color.pr_text);
                chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                chip.setPadding((int) (10 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
                int ac = getResources().getColor(R.color.pr_accent);
                chip.setBackground(Ui.rounded(on ? ac : 0x00000000, on ? ac : 0x55FFFFFF, d, 14 * d));
                chip.setOnClickListener(v -> { recsFilter = f[0]; renderRecsPage(); });
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = (int) (6 * d);
                fl.addView(chip, lp);
            }
            android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            hs.addView(fl);
            c.addView(hs);
            c.addView(Ui.spacer(this, 10));
        }

        if (finalFiles.length == 0) {
            TextView empty = Ui.text(this, getString(R.string.recordings_empty), 13f, R.color.pr_muted);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, (int) (30 * d), 0, 0);
            c.addView(empty);
            return;
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("d.M HH:mm", Locale.getDefault());
        int shownN = 0;
        for (File f : finalFiles) if ("all".equals(recsFilter) || recMatches(f, recsFilter)) { c.addView(buildRecordingCard(f, fmt)); shownN++; }
        if (shownN == 0) {
            TextView none = Ui.text(this, L.t("Brak nagrań w tym filtrze"), 13f, R.color.pr_muted);
            none.setGravity(android.view.Gravity.CENTER);
            none.setPadding(0, (int) (20 * d), 0, 0);
            c.addView(none);
        }
    }

    private String recsFilter = "all";

    // Gdy lista NAGRANIA jest otwarta i cos czeka na ocene — sprawdzaj NS co 30 s,
    // zeby ocena trenera pojawila sie bez wychodzenia z ekranu.
    private final Runnable recsPoll = () -> {
        if (!"recs".equals(currentPage) || !isLoggedIn() || !NsStatus.anyWaiting()) return;
        NsStatus.refresh(this, () -> runOnUiThread(() -> { if ("recs".equals(currentPage)) renderRecsPage(); }));
        scheduleRecsPoll();
    };

    @Override
    protected void onResume() {
        super.onResume();
        LiveAudioData.loggedIn = isLoggedIn();
        if (!badges.isEmpty()) { updateBadges(); TrainerWatch.check(this, null); }
        // nagrana rozmowa telefoniczna czeka na opis
        new Handler(Looper.getMainLooper()).postDelayed(this::handlePendingCallRec, 350);
        CallRecUi.refreshOpenSetup(); // powrot z ustawien systemu — odswiez ✅/⚠️ w kreatorze
        new Handler(Looper.getMainLooper()).postDelayed(() -> { if (!isFinishing() && prefs().getString("callrec_pending", null) == null) CallRecUi.checkOnResume(this); }, 4000); // 4 s: system zdazy podpiac usluge po starcie apki
        // Powrot do apki (np. z powiadomienia o ocenie) — odswiez statusy na liscie nagran
        if ("recs".equals(currentPage) && isLoggedIn())
            NsStatus.refresh(this, () -> runOnUiThread(() -> { if ("recs".equals(currentPage)) renderRecsPage(); }));
    }

    private void scheduleRecsPoll() {
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        root.removeCallbacks(recsPoll);
        root.postDelayed(recsPoll, 30000L);
    }

    // Status nagrania w NS: "ok" / "bad" / "wait" albo null (nieznany / niewyslane)
    private String recStatus(RecMeta m) {
        if (!"sent".equals(m.ns)) return null;
        String id = !m.nsRecordId.isEmpty() ? m.nsRecordId : m.fixRecordId;
        return NsStatus.get(id);
    }

    private boolean recMatches(File f, String filter) {
        RecMeta m = RecMeta.load(this, f.getName());
        if ("unsent".equals(filter)) return !"sent".equals(m.ns);
        String st = recStatus(m);
        return filter.equals(st);
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
        String st = recStatus(meta);
        if (st != null) {
            String stTxt = ("ok".equals(st) ? "✅ " + L.t("zaliczone przez trenera") : "bad".equals(st) ? "↺ " + L.t("do poprawy") : "⏳ " + L.t("czeka na ocenę trenera")) + "  ·  " + L.t("szczegóły ›");
            TextView stv = Ui.text(this, stTxt, 11f, "ok".equals(st) ? R.color.pr_accent : "bad".equals(st) ? R.color.pr_warn : R.color.pr_pause);
            stv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            stv.setPadding(0, (int) (4 * d), 0, 0);
            final String rid = !meta.nsRecordId.isEmpty() ? meta.nsRecordId : meta.fixRecordId;
            stv.setOnClickListener(v -> showTrainerRating(rid, "bad".equals(st), file));
            card.addView(stv);
        }

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
        } else if (logged && !"sent".equals(meta.ns)) {
            // Wyslane nagranie nie ma juz przycisku wysylki (poprawka idzie przez KOREKTA)
            boolean busy = sendingNow.contains(file.getName());
            Button send = Ui.button(this, busy ? L.t("⏳ Wysyłanie…") : L.t("☁ Wyślij NS"), R.color.pr_purple, false);
            send.setEnabled(!busy);
            send.setOnClickListener(v -> sendToNs(file));
            sendButtons.put(file.getName(), send);
            if (busy) { paintSendProgress(file.getName(), send); startSendTicker(); }
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
        Button sh = Ui.button(this, "📤", R.color.pr_accent, false); // udostepnij (WhatsApp, mail…)
        sh.setOnClickListener(v -> shareRecording(file));
        btns.addView(sh, Ui.weight(0.5f, 6 * d));
        Button dl = Ui.button(this, "⬇", R.color.pr_accent, false);   // pobierz do telefonu
        dl.setOnClickListener(v -> downloadRecording(file));
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

    // ── OCENA TRENERA dla nagrania (z NS: kryteria i ich ocena w %, komentarz) ──
    private String nameOf(org.json.JSONObject o) {
        if (o == null) return "";
        RateNames.learn(this, o);
        String l = L.lang();
        String v = "cs".equals(l) || "sk".equals(l) ? o.optString("name_cz", "") : "pl".equals(l) ? o.optString("name_pl", "") : "en".equals(l) ? o.optString("name_en", "") : "";
        if (!v.isEmpty() && !"null".equals(v)) return v;
        String pl = o.optString("name_pl", "");
        if (pl.isEmpty() || "null".equals(pl)) pl = o.optString("name", "");
        return RateNames.tr("null".equals(pl) ? "" : pl);
    }

    // Karta nagrania -> pelny podglad: wszystkie dane nagrania + cala ocena trenera
    // (kazde kryterium z wynikiem %, opis poziomu, komentarz tekstowy i GLOSOWY trenera)
    private MediaPlayer voicePlayer = null;

    private void showTrainerRating(String recordId, boolean toFix, File file) {
        org.json.JSONObject rec = NsStatus.record(recordId);
        if (rec == null && recordId != null && !recordId.isEmpty() && isLoggedIn()) {
            // nagranie spoza ostatnich 100 — pobieramy sam rekord
            NsClient.request("GET", "/records/" + recordId, nsToken(), nsEmail(), null, null, r -> {
                org.json.JSONObject o = null;
                try { if (r.ok) { o = new org.json.JSONObject(r.body); if (o.optJSONObject("record") != null) o = o.optJSONObject("record"); } } catch (Exception e) { }
                buildRatingDialog(o, recordId, toFix, file);
            });
            return;
        }
        buildRatingDialog(rec, recordId, toFix, file);
    }

    private void buildRatingDialog(org.json.JSONObject rec, String recordId, boolean toFix, File file) {
        float d = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding((int) (20 * d), (int) (6 * d), (int) (20 * d), (int) (6 * d));
        RecMeta meta = file != null ? RecMeta.load(this, file.getName()) : new RecMeta();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("d.MM.yyyy, HH:mm", Locale.getDefault());
        if (rec == null) {
            box.addView(Ui.text(this, L.t("Nie udało się wczytać oceny — spróbuj za chwilę."), 13f, R.color.pr_text));
        } else {
            String cat = nameOf(rec.optJSONObject("record_category"));
            if (cat.isEmpty()) cat = meta.cat;
            boolean reviewed = NsStatus.isoMs(rec.optString("reviewed_at", "")) > 0 && !rec.isNull("is_correct");
            boolean ok = reviewed && rec.optBoolean("is_correct", false) && !toFix;
            TextView head = Ui.text(this, (!reviewed ? "⏳ " + L.t("czeka na ocenę trenera") : ok ? "✅ " + L.t("Zaliczone") : "↺ " + L.t("Do poprawy")) + (cat.isEmpty() ? "" : "  ·  " + L.cat(cat)), 15f, ok ? R.color.pr_accent : reviewed ? R.color.pr_warn : R.color.pr_pause);
            head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            box.addView(head);

            // ── NAGRANIE: wszystkie dane ──
            TextView l1 = Ui.label(this, "📋 " + L.t("NAGRANIE"));
            l1.setPadding(0, (int) (12 * d), 0, (int) (2 * d));
            box.addView(l1);
            infoRow(box, L.t("Kategoria"), cat.isEmpty() ? "—" : L.cat(cat));
            if (!meta.special.isEmpty()) infoRow(box, L.t("Zadanie specjalne"), SpecialTasks.find(meta.special) != null ? SpecialTasks.find(meta.special).shortNameL() : meta.special);
            if (!meta.sys.isEmpty()) infoRow(box, L.t("System mowy"), meta.sys);
            if (meta.emotion > 0 && meta.emotion < RecMeta.EMOTION_LABELS.length) infoRow(box, L.t("Samopoczucie"), L.t(RecMeta.EMOTION_LABELS[meta.emotion]));
            String rd = rec.optString("record_date", rec.optString("date", ""));
            if (rd.length() >= 10 && !"null".equals(rd)) infoRow(box, L.t("Dzień w NS"), rd.substring(8, 10) + "." + rd.substring(5, 7) + "." + rd.substring(0, 4));
            if (file != null && file.exists()) infoRow(box, L.t("Nagrano"), df.format(new java.util.Date(file.lastModified())));
            long sent = NsStatus.isoMs(rec.optString("created_at", ""));
            if (sent > 0) infoRow(box, L.t("Wysłano"), df.format(new java.util.Date(sent)));
            long upd = NsStatus.isoMs(rec.optString("record_file_updated_by_author_at", rec.optString("updated_by_author_at", "")));
            if (upd > 0 && upd - sent > 60000) infoRow(box, L.t("Podmienione (poprawka)"), df.format(new java.util.Date(upd)));
            long rv = NsStatus.isoMs(rec.optString("reviewed_at", ""));
            if (rv > 0) infoRow(box, L.t("Oceniono"), df.format(new java.util.Date(rv)));
            String who = "";
            org.json.JSONObject rb = rec.optJSONObject("reviewed_by");
            if (rb == null) rb = rec.optJSONObject("reviewer");
            if (rb != null) who = (rb.optString("first_name", "") + " " + rb.optString("last_name", "")).trim();
            if (!who.isEmpty()) infoRow(box, L.t("Trener"), who);
            if (meta.hasGps()) infoRow(box, "GPS", String.format(Locale.US, "%.4f, %.4f", meta.lat, meta.lon));
            String desc = rec.optString("description", "").trim();
            if (desc.isEmpty() || "null".equals(desc)) desc = meta.note;
            if (desc != null && !desc.isEmpty()) infoRow(box, L.t("Opis"), desc);

            // ── OCENA TRENERA: wszystkie kryteria w kolejnosci trenera ──
            org.json.JSONArray rates = rec.optJSONArray("record_rates");
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            int full = 0;
            for (int i = 0; rates != null && i < rates.length(); i++) {
                org.json.JSONObject r = rates.optJSONObject(i);
                if (r == null) continue;
                org.json.JSONObject u = r.optJSONObject("record_rate_unit");
                String crit = nameOf(r.optJSONObject("record_rate_category"));
                String unit = nameOf(u);
                int pct = u != null ? u.optInt("percent_mark", -1) : -1;
                if (crit.isEmpty() && unit.isEmpty()) continue;
                if (pct >= 100) full++;
                rows.add(new Object[]{crit, unit, pct});
            }
            TextView l2 = Ui.label(this, "🎯 " + L.t("OCENA TRENERA"));
            l2.setPadding(0, (int) (14 * d), 0, (int) (2 * d));
            box.addView(l2);
            if (rows.isEmpty()) {
                box.addView(Ui.text(this, reviewed ? L.t("Trener nie zapisał szczegółowych kryteriów dla tego nagrania.") : L.t("Ocena pojawi się tutaj, gdy trener oceni nagranie."), 12f, R.color.pr_muted));
            } else {
                int sum = 0, cnt = 0;
                for (Object[] r : rows) if ((Integer) r[2] >= 0) { sum += (Integer) r[2]; cnt++; }
                TextView smry = Ui.text(this, L.f("{0} z {1} kryteriów na 100%", full, rows.size()) + (cnt > 0 ? "  ·  " + L.f("średnio {0}%", Math.round(sum / (float) cnt)) : ""), 12f, R.color.pr_text);
                smry.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                box.addView(smry);
                addRateGroup(box, null, rows, d);
                java.util.List<Object[]> weak = new java.util.ArrayList<>();
                for (Object[] r : rows) if ((Integer) r[2] >= 0 && (Integer) r[2] < 100) weak.add(r);
                if (!weak.isEmpty()) {
                    java.util.Collections.sort(weak, (x, y) -> Integer.compare((Integer) x[2], (Integer) y[2]));
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.min(3, weak.size()); i++) sb.append(i == 0 ? "" : ", ").append(weak.get(i)[0]);
                    TextView focus = Ui.text(this, "💡 " + L.t("Na tym skup się w kolejnych nagraniach: ") + sb, 12f, R.color.pr_warn);
                    focus.setPadding(0, (int) (10 * d), 0, 0);
                    box.addView(focus);
                } else {
                    TextView bravo = Ui.text(this, "🎉 " + L.t("Wszystkie kryteria na 100% — tak trzymaj!"), 12f, R.color.pr_accent);
                    bravo.setPadding(0, (int) (10 * d), 0, 0);
                    box.addView(bravo);
                }
            }
            boolean hasText = false;
            for (String k : new String[]{"trainer_comment", "comment", "review_comment", "review", "description_trainer"}) {
                String c = rec.optString(k, "").trim();
                if (!c.isEmpty() && !"null".equals(c) && !c.startsWith("{")) {
                    TextView lb = Ui.label(this, "💬 " + L.t("KOMENTARZ TRENERA"));
                    lb.setPadding(0, (int) (12 * d), 0, (int) (4 * d));
                    box.addView(lb);
                    box.addView(Ui.text(this, c, 13f, R.color.pr_text));
                    hasText = true;
                    break;
                }
            }
            // odpowiedz kursanta (Rozumiem / Mam pytanie) — tylko gdy trener cos napisal lub nagral
            if (hasText) StudentAck.addButtons(this, box, "record", recordId);
            // komentarz GLOSOWY trenera (jesli nagral)
            android.widget.LinearLayout voiceBox = new android.widget.LinearLayout(this);
            voiceBox.setOrientation(android.widget.LinearLayout.VERTICAL);
            box.addView(voiceBox);
            checkVoiceReview(recordId, voiceBox, d, !hasText);
            TrainerInbox.markRead(this, recordId, null);
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(box);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setCustomTitle(nsLogoTitle(L.t("Nagranie i ocena trenera")))
                .setView(sv)
                .setPositiveButton(getString(R.string.btn_close), null)
                .setOnDismissListener(dd -> stopVoiceReview());
        if (toFix) b.setNeutralButton("↺ " + L.t("KOREKTA"), (dd, w) -> openLoginOnlySection("fix", L.t("KOREKTA")));
        b.show();
    }

    private void infoRow(android.widget.LinearLayout box, String k, String v) {
        android.widget.LinearLayout r = Ui.row(this);
        r.setPadding(0, (int) Ui.dp(this, 3), 0, 0);
        TextView kt = Ui.text(this, k, 12f, R.color.pr_muted);
        r.addView(kt, new android.widget.LinearLayout.LayoutParams((int) Ui.dp(this, 118), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(Ui.text(this, v, 12f, R.color.pr_text), new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(r);
    }

    private static final String BACKEND = "https://newspeech-backend.mowaiemocje.workers.dev";

    private void checkVoiceReview(String recordId, android.widget.LinearLayout holder, float d) { checkVoiceReview(recordId, holder, d, false); }

    // withAck: pod odtwarzaczem dodaj „Rozumiem / Mam pytanie” (tylko gdy trener nagral komentarz)
    private void checkVoiceReview(String recordId, android.widget.LinearLayout holder, float d, boolean withAck) {
        if (recordId == null || recordId.isEmpty() || !isLoggedIn()) return;
        String q = "/voice-review/check?record_ids=" + NsClient.enc(recordId) + "&ns_token=" + NsClient.enc(nsToken()) + "&ns_email=" + NsClient.enc(nsEmail());
        NsClient.backend("GET", q, null, r -> {
            try {
                if (!r.ok) return;
                org.json.JSONArray has = new org.json.JSONObject(r.body).optJSONArray("has_review");
                boolean yes = false;
                for (int i = 0; has != null && i < has.length(); i++) if (recordId.equals(has.optString(i, ""))) yes = true;
                if (!yes) return;
                TextView lb = Ui.label(this, "🎧 " + L.t("KOMENTARZ GŁOSOWY TRENERA"));
                lb.setPadding(0, (int) (12 * d), 0, (int) (4 * d));
                holder.addView(lb);
                Button play = Ui.button(this, "▶ " + L.t("Posłuchaj"), R.color.pr_accent, true);
                play.setOnClickListener(v -> {
                    StudentAck.listened(this, "record", recordId);
                    TrainerInbox.markRead(this, recordId, null, TrainerInbox.T_VOICE);
                    if (voicePlayer != null) { stopVoiceReview(); play.setText("▶ " + L.t("Posłuchaj")); return; }
                    String url = BACKEND + "/voice-review/audio?record_id=" + NsClient.enc(recordId) + "&ns_token=" + NsClient.enc(nsToken()) + "&ns_email=" + NsClient.enc(nsEmail());
                    try {
                        releasePlayer(); // nie grajmy dwoch rzeczy naraz
                        MediaPlayer mp = new MediaPlayer();
                        voicePlayer = mp;
                        mp.setDataSource(url);
                        mp.setOnPreparedListener(m -> { if (voicePlayer == m) { m.start(); play.setText("■ " + L.t("Zatrzymaj")); } });
                        mp.setOnCompletionListener(m -> { stopVoiceReview(); play.setText("▶ " + L.t("Posłuchaj")); });
                        mp.setOnErrorListener((m, w, e) -> { stopVoiceReview(); play.setText("▶ " + L.t("Posłuchaj")); Toast.makeText(this, L.t("Nie udało się odtworzyć komentarza"), Toast.LENGTH_SHORT).show(); return true; });
                        play.setText("⏳ " + L.t("Wczytywanie…"));
                        mp.prepareAsync();
                    } catch (Exception e) { stopVoiceReview(); }
                });
                holder.addView(play, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                if (withAck) StudentAck.addButtons(this, holder, "record", recordId);
            } catch (Exception e) { }
        });
    }

    private void stopVoiceReview() {
        MediaPlayer mp = voicePlayer;
        voicePlayer = null;
        if (mp != null) try { mp.release(); } catch (Exception e) { }
    }

    private void addRateGroup(android.widget.LinearLayout box, String title, java.util.List<Object[]> rows, float d) {
        if (title != null) {
            TextView lb = Ui.label(this, title);
            lb.setPadding(0, (int) (12 * d), 0, (int) (4 * d));
            box.addView(lb);
        }
        for (Object[] r : rows) {
            int pct = (Integer) r[2];
            int col = pct >= 100 ? 0xFF00C853 : pct >= 50 ? 0xFFE8820C : 0xFFE53935;
            android.widget.LinearLayout line = Ui.row(this);
            line.setPadding(0, (int) (5 * d), 0, 0);
            TextView nm = Ui.text(this, (String) r[0], 13f, R.color.pr_text);
            nm.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            line.addView(nm, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (pct >= 0) { TextView pv = Ui.text(this, pct + "%", 13f, R.color.pr_text); pv.setTextColor(col); pv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); line.addView(pv); }
            box.addView(line);
            if (pct >= 0) {
                android.widget.LinearLayout track = new android.widget.LinearLayout(this);
                track.setBackground(Ui.rounded(Ui.col(this, R.color.pr_border), 0, 0, 3 * d));
                float w = Math.max(0.03f, Math.min(1f, pct / 100f));
                View fill = new View(this);
                fill.setBackground(Ui.rounded(col, 0, 0, 3 * d));
                track.addView(fill, new android.widget.LinearLayout.LayoutParams(0, (int) (6 * d), w));
                track.addView(new View(this), new android.widget.LinearLayout.LayoutParams(0, (int) (6 * d), 1f - w + 0.0001f));
                android.widget.LinearLayout.LayoutParams tl = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                tl.topMargin = (int) (3 * d);
                box.addView(track, tl);
            }
            String unit = (String) r[1];
            if (!unit.isEmpty()) box.addView(Ui.text(this, unit, 11f, R.color.pr_muted));
        }
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

    private static final int REQ_MAP = 4401;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MAP && data != null && data.hasExtra("availUntil")) {
            // "Chcę porozmawiać" przelaczone na mapie — ten sam stan w aplikacji
            long until = data.getLongExtra("availUntil", 0L);
            android.content.SharedPreferences.Editor e = prefs().edit().putBoolean("map_avail", until > System.currentTimeMillis()).putLong("map_avail_until", until);
            String ph = data.getStringExtra("phone");
            if (ph != null && !ph.isEmpty()) e.putString("map_phone", ph);
            e.apply();
            if ("set".equals(currentPage)) renderSettingsPage();
            return;
        }
        if (requestCode == REQUEST_SAVE_FILE) {
            File src = pendingSaveFile;
            pendingSaveFile = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && src != null) {
                try (java.io.InputStream in = new java.io.FileInputStream(src); java.io.OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    byte[] b = new byte[1 << 16]; int n;
                    while ((n = in.read(b)) > 0) os.write(b, 0, n);
                    Toast.makeText(this, "⬇ " + L.t("Zapisano nagranie"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, L.t("Błąd zapisu: ") + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        if (requestCode == REQUEST_EXPORT_TREE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                android.net.Uri t = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(t, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) { }
                prefs().edit().putString("export_tree_uri", t.toString()).apply();
                Toast.makeText(this, "📁 " + L.t("Nowe nagrania będą zapisywane też w:") + " " + treeLabel(t.toString()), Toast.LENGTH_LONG).show();
            }
            if ("set".equals(currentPage)) renderSettingsPage();
            return;
        }
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
                        this, getPackageName() + ".fileprovider", f));
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

    // ⬇ POBIERZ: kopia nagrania do "Pobrane/NewSpeech" (Android 10+ bez zadnych zgod);
    // na starszych Androidach systemowe okno "Zapisz jako".
    private boolean saveToDownloads(File f) throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 29) return false;
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, f.getName());
        cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") ? "audio/mpeg" : "audio/wav");
        cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/NewSpeech");
        android.net.Uri out = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        if (out == null) throw new IOException("MediaStore");
        try (java.io.InputStream in = new java.io.FileInputStream(f); java.io.OutputStream os = getContentResolver().openOutputStream(out)) {
            byte[] b = new byte[1 << 16]; int n;
            while ((n = in.read(b)) > 0) os.write(b, 0, n);
        }
        return true;
    }

    private void downloadRecording(File file) {
        try {
            if (saveToDownloads(file)) {
                Toast.makeText(this, "⬇ " + L.t("Zapisano w Pobrane/NewSpeech"), Toast.LENGTH_SHORT).show();
                return;
            }
            pendingSaveFile = file;
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(file.getName().toLowerCase(Locale.ROOT).endsWith(".mp3") ? "audio/mpeg" : "audio/wav");
            i.putExtra(Intent.EXTRA_TITLE, file.getName());
            startActivityForResult(i, REQUEST_SAVE_FILE);
        } catch (Exception e) {
            Toast.makeText(this, L.t("Błąd zapisu: ") + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadAllRecordings(File[] files) {
        if (files.length == 0) { Toast.makeText(this, L.t("Brak nagrań"), Toast.LENGTH_SHORT).show(); return; }
        if (android.os.Build.VERSION.SDK_INT < 29) { shareAllRecordings(files); return; }
        new Thread(() -> {
            int ok = 0; String err = null;
            for (File f : files) { try { if (saveToDownloads(f)) ok++; } catch (Exception e) { err = e.getMessage(); } }
            final int fok = ok; final String fe = err;
            runOnUiThread(() -> Toast.makeText(this, fe == null ? "⬇ " + L.f("Zapisano {0} nagrań w Pobrane/NewSpeech", fok) : L.t("Błąd zapisu: ") + fe, Toast.LENGTH_LONG).show());
        }, "download-all").start();
    }

    private void shareRecording(File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
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
