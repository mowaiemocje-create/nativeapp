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
    private Button recordingsListButton;
    private Button settingsButton;
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
    private String selectedFormat = "wav";
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
        recordingsListButton = findViewById(R.id.recordingsListButton);
        settingsButton = findViewById(R.id.settingsButton);
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

        gainSlider.setValue(0.65f); // +6dB domyslnie w zakresie -20/+20
        gainSlider.setOnValueChangeListener(v -> {
            float db = -20f + v * 40f; // zakres -20dB do +20dB
            float linearGain = (float) Math.pow(10.0, db / 20.0);
            LiveAudioData.gainMultiplier = linearGain;
            gainValueText.setText(String.format(Locale.getDefault(), "%+.1f dB", db));
        });
        LiveAudioData.gainMultiplier = (float) Math.pow(10.0, 6.0 / 20.0); // +6dB domyslnie
        gainValueText.setText("+6.0 dB");

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
        recordingsListButton.setOnClickListener(v -> showRecordingsList());

        settingsButton.setOnClickListener(v -> showSettingsDialog());
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
        return String.format(Locale.getDefault(), "%s%.2f dB", sign, Math.abs(db));
    }

    private void selectFormat(String format) {
        if (isRecording) {
            Toast.makeText(this, "Zatrzymaj nagrywanie, żeby zmienić format", Toast.LENGTH_SHORT).show();
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
                    gainValueText.setText(String.format(Locale.getDefault(), "%+.1f dB", newDb));
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

    private void showSettingsDialog() {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        TextView formatLabel = new TextView(this);
        formatLabel.setText("Format nagrywania:");
        container.addView(formatLabel);

        android.widget.RadioGroup formatGroup = new android.widget.RadioGroup(this);
        formatGroup.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.RadioButton wavRadio = new android.widget.RadioButton(this);
        wavRadio.setText("WAV");
        android.widget.RadioButton mp3Radio = new android.widget.RadioButton(this);
        mp3Radio.setText("MP3");
        formatGroup.addView(wavRadio);
        formatGroup.addView(mp3Radio);
        wavRadio.setChecked("wav".equals(selectedFormat));
        mp3Radio.setChecked("mp3".equals(selectedFormat));
        wavRadio.setOnClickListener(v -> selectFormat("wav"));
        mp3Radio.setOnClickListener(v -> selectFormat("mp3"));
        container.addView(formatGroup);

        android.widget.CheckBox noiseGateCheck = new android.widget.CheckBox(this);
        noiseGateCheck.setText("Bramka szumów (tłumi cichy szum tła)");
        noiseGateCheck.setChecked(LiveAudioData.noiseGateEnabled);
        noiseGateCheck.setOnCheckedChangeListener((btn, checked) -> LiveAudioData.noiseGateEnabled = checked);
        container.addView(noiseGateCheck);

        android.widget.CheckBox keepScreenOnCheck = new android.widget.CheckBox(this);
        keepScreenOnCheck.setText("Nie wygaszaj ekranu podczas nagrywania");
        keepScreenOnCheck.setChecked(keepScreenOnEnabled);
        keepScreenOnCheck.setOnCheckedChangeListener((btn, checked) -> {
            keepScreenOnEnabled = checked;
            applyKeepScreenOnSetting();
        });
        container.addView(keepScreenOnCheck);

        // Wybor jezyka aplikacji
        TextView langLabel = new TextView(this);
        langLabel.setText("Język / Language:");
        container.addView(langLabel);

        android.widget.RadioGroup langGroup = new android.widget.RadioGroup(this);
        langGroup.setOrientation(android.widget.LinearLayout.VERTICAL);
        String currentLang = getSavedLanguage(this);
        String[][] languages = {
                {"en", "English"}, {"pl", "Polski"}, {"sk", "Slovenčina"},
                {"cs", "Čeština"}, {"de", "Deutsch"}, {"es", "Español"}
        };
        for (String[] lang : languages) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(lang[1]);
            rb.setChecked(lang[0].equals(currentLang));
            rb.setOnClickListener(v -> setLanguage(lang[0]));
            langGroup.addView(rb);
        }
        container.addView(langGroup);

        // Grubosc linii pitch
        TextView pitchWidthLabel = new TextView(this);
        pitchWidthLabel.setText("Grubość linii pitch:");
        container.addView(pitchWidthLabel);

        SliderView pitchWidthSlider = new SliderView(this);
        android.widget.LinearLayout.LayoutParams sliderParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int) (36 * getResources().getDisplayMetrics().density));
        pitchWidthSlider.setLayoutParams(sliderParams);
        pitchWidthSlider.setValue((LiveAudioData.pitchLineWidthDp - 1f) / 9f); // zakres 1-10dp
        pitchWidthSlider.setOnValueChangeListener(v -> LiveAudioData.pitchLineWidthDp = 1f + v * 9f);
        container.addView(pitchWidthSlider);

        // Kolor linii pitch — kwadracik pokazujacy aktualny kolor, klikniecie otwiera
        // siatke 16 kolorow do wyboru.
        TextView pitchColorLabel = new TextView(this);
        pitchColorLabel.setText("Kolor linii pitch:");
        addColorRow(container, pitchColorLabel, LiveAudioData.pitchLineColor, color -> LiveAudioData.pitchLineColor = color);

        // Grubosc siatki DAW
        TextView gridWidthLabel = new TextView(this);
        gridWidthLabel.setText("Grubość siatki:");
        container.addView(gridWidthLabel);

        SliderView gridWidthSlider = new SliderView(this);
        gridWidthSlider.setLayoutParams(sliderParams);
        gridWidthSlider.setValue((LiveAudioData.gridLineWidthDp - 0.5f) / 3.5f); // zakres 0.5-4dp
        gridWidthSlider.setOnValueChangeListener(v -> LiveAudioData.gridLineWidthDp = 0.5f + v * 3.5f);
        container.addView(gridWidthSlider);

        TextView gridColorLabel = new TextView(this);
        gridColorLabel.setText("Kolor siatki:");
        addColorRow(container, gridColorLabel, LiveAudioData.gridLineColor, color -> LiveAudioData.gridLineColor = color);

        TextView dawBgLabel = new TextView(this);
        dawBgLabel.setText("Kolor tła DAW:");
        addColorRow(container, dawBgLabel, LiveAudioData.dawBackgroundColor, color -> LiveAudioData.dawBackgroundColor = color);

        TextView waveColorLabel = new TextView(this);
        waveColorLabel.setText("Kolor fali:");
        addColorRow(container, waveColorLabel, LiveAudioData.waveColor, color -> LiveAudioData.waveColor = color);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_title))
                .setView(container)
                .setPositiveButton(getString(R.string.btn_close), null)
                .show();
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

    private boolean keepScreenOnEnabled = false;

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
        playButton.setButtonEnabled(false);
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
        if (requestCode == REQUEST_MIC_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecordingFlow();
        } else {
            Toast.makeText(this, "Brak zgody na mikrofon", Toast.LENGTH_SHORT).show();
        }
    }

    private long lastRecordingTotalDurationMs = 0L;

    @Override
    public void onSuccess(String base64, long durationMs, String mimeType) {
        runOnUiThread(() -> {
            statusText.setText("Nagrano " + (durationMs / 1000) + "s — zapisano");
            saveRecordingForPlayback(base64, mimeType);
            loadedFilePath = null; // swiezo nagrany plik jest juz w LiveAudioData, nie trzeba wczytywac ponownie
            pendingSeekSample = 0L;
            lastRecordingTotalDurationMs = durationMs;
            timeText.setText("00:00 / " + formatMs(durationMs)); // jak odtwarzacz: pozycja/calkowita dlugosc
            pitchWaveView.setLiveMode(false); // pozwala przewijac/wskazac miejsce w tym co wlasnie nagrano
            pitchWaveView.resetPan();
            pitchWaveView.invalidate();
            playButton.setButtonEnabled(true);
        });
    }

    private String formatMs(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    @Override
    public void onError(String code, String message) {
        runOnUiThread(() -> statusText.setText("Błąd: " + code + " — " + message));
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
            statusText.setText("Błąd zapisu: " + e.getMessage());
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
            Toast.makeText(this, "Podgląd MP3 podczas pauzy nie jest wspierany — zatrzymaj nagranie", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            File source = new File(path);
            File tempCopy = new File(getCacheDir(), "preview_temp.wav");
            copyWavWithFixedHeader(source, tempCopy);
            playFile(tempCopy.getAbsolutePath(), sampleIndex);
        } catch (IOException e) {
            statusText.setText("Błąd podglądu: " + e.getMessage());
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
            statusText.setText("Wczytano: " + file.getName());
        } catch (IOException e) {
            statusText.setText("Błąd wczytywania: " + e.getMessage());
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
            statusText.setText("Błąd odtwarzania: " + e.getMessage());
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

    // Prosta lista zapisanych nagrań — kliknięcie wczytuje plik do wykresu i odtwarza od
    // początku. Pełny ekran (jak "NAGRANIA" w PitchRec, z wysyłką/pobieraniem) to kolejny etap.
    private static final int REQUEST_IMPORT_FILE = 200;

    private android.app.AlertDialog[] recordingsDialogRef = new android.app.AlertDialog[1];

    private void showRecordingsList() {
        // WAZNE: zamykamy PRZEDNIA instancje dialogu (jesli istnieje) przed pokazaniem
        // nowej — bez tego, wywolanie showRecordingsList() po usunieciu/imporcie
        // NAKLADALO nowy dialog NA STARY (ktory nadal byl otwarty, pokazujac NIEAKTUALNA
        // liste), i klikniecie "Zamknij" na nowym ujawnialo stary, ze "usunietymi"
        // wciaz widocznymi elementami.
        if (recordingsDialogRef[0] != null) {
            try { recordingsDialogRef[0].dismiss(); } catch (Exception e) { /* ignorowane */ }
        }
        File[] files = getFilesDir().listFiles((dir, name) -> name.endsWith(".wav") || name.endsWith(".mp3"));
        if (files == null) files = new File[0];
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        final File[] finalFiles = files;

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout listContainer = new android.widget.LinearLayout(this);
        listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        listContainer.setPadding(pad, pad, pad, pad);
        float density = getResources().getDisplayMetrics().density;

        // Wyslij wszystkie / Pobierz wszystkie — jak w PitchRec. Wyslij wszystkie wymaga
        // logowania (kolejny etap); Pobierz wszystkie pakuje wszystkie nagrania do udostepnienia.
        android.widget.LinearLayout topActionsRow = new android.widget.LinearLayout(this);
        topActionsRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        Button sendAllBtn = makeOutlinedButton(getString(R.string.btn_send_all), R.color.pr_purple, density);
        sendAllBtn.setOnClickListener(v -> Toast.makeText(this, getString(R.string.requires_login), Toast.LENGTH_LONG).show());
        android.widget.LinearLayout.LayoutParams sendAllParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sendAllParams.rightMargin = (int) (6 * density);
        sendAllBtn.setLayoutParams(sendAllParams);
        topActionsRow.addView(sendAllBtn);

        Button downloadAllBtn = makeOutlinedButton(getString(R.string.btn_download_all), R.color.pr_accent, density);
        downloadAllBtn.setOnClickListener(v -> shareAllRecordings(finalFiles));
        downloadAllBtn.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topActionsRow.addView(downloadAllBtn);

        listContainer.addView(topActionsRow);

        Button importBtn = new Button(this);
        importBtn.setText(getString(R.string.btn_import_file));
        importBtn.setMinWidth(0);
        importBtn.setMinimumWidth(0);
        importBtn.setTextColor(getResources().getColor(R.color.pr_accent));
        importBtn.setBackgroundColor(getResources().getColor(R.color.pr_card));
        importBtn.setOnClickListener(v -> importExternalFile());
        listContainer.addView(importBtn);

        android.app.AlertDialog[] dialogRef = recordingsDialogRef;

        if (finalFiles.length == 0) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.recordings_empty));
            empty.setTextColor(getResources().getColor(R.color.pr_muted));
            listContainer.addView(empty);
        } else {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("d.M HH:mm", Locale.getDefault());
            for (File f : finalFiles) {
                listContainer.addView(buildRecordingCard(f, fmt, dialogRef));
            }
        }

        // Naprawa "przezroczystosci" — bez jawnego, pelnego tla dialog w trybie
        // pelnoekranowym mogl przepuszczac dotyk do ekranu pod nim.
        scrollView.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        listContainer.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        scrollView.addView(listContainer);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.recordings_title))
                .setView(scrollView)
                .setNegativeButton(getString(R.string.btn_close), null)
                .show();
        android.app.AlertDialog recsDialog = dialogRef[0];
        // Pelny ekran (jak strona "NAGRANIA" w PitchRec) — domyslnie AlertDialog ma
        // marginesy i nie wypelnia calego ekranu, wiec wymuszamy wymiary okna.
        if (recsDialog.getWindow() != null) {
            recsDialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    // Odpowiednik ".rec-item" z PitchRec (JS) — gorny wiersz: nazwa + kategoria (placeholder
    // "bez opisu", kategorie to kolejny etap) + ikona NS; wiersz daty; wiersz 4 przyciskow
    // (Opisz i wyslij / Otworz / Udostepnij / Usun) — dokladnie ta sama struktura, tylko
    // "Wyslij do NS" jest na razie zablokowane (wymaga logowania, kolejny etap).
    private android.widget.LinearLayout buildRecordingCard(File file, java.text.SimpleDateFormat fmt, android.app.AlertDialog[] dialogRef) {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (10 * density);
        int marginBottom = (int) (12 * density);

        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setPadding((int) (12 * density), pad, (int) (12 * density), pad);

        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(getResources().getColor(R.color.pr_card));
        cardBg.setCornerRadius(10 * density);
        cardBg.setStroke((int) density, getResources().getColor(R.color.pr_border));
        card.setBackground(cardBg);

        // Wiersz gorny: nazwa + kategoria (pigulka) + chmurka + status
        android.widget.LinearLayout topRow = new android.widget.LinearLayout(this);
        topRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        TextView nameView = new TextView(this);
        String displayName = file.getName().replaceAll("\\.(wav|mp3)$", "");
        if (displayName.length() > 22) displayName = displayName.substring(0, 20) + "…";
        nameView.setText(displayName);
        nameView.setTextColor(getResources().getColor(R.color.pr_text));
        nameView.setTextSize(15f);
        topRow.addView(nameView);

        TextView catBadge = new TextView(this);
        catBadge.setText("  " + getString(R.string.no_category) + "  ");
        catBadge.setTextSize(11f);
        catBadge.setTextColor(getResources().getColor(R.color.pr_purple));
        android.graphics.drawable.GradientDrawable catBg = new android.graphics.drawable.GradientDrawable();
        catBg.setColor(0x335856D6);
        catBg.setCornerRadius(12 * density);
        catBadge.setBackground(catBg);
        topRow.addView(catBadge);

        TextView nsIcon = new TextView(this);
        String fmtLabel = file.getName().endsWith(".mp3") ? "MP3" : "WAV";
        nsIcon.setText("  ☁ " + fmtLabel);
        nsIcon.setTextColor(getResources().getColor(R.color.pr_muted));
        nsIcon.setTextSize(11f);
        topRow.addView(nsIcon);

        card.addView(topRow);

        // Wiersz autor + data (na razie "Ja" jako placeholder — prawdziwy autor po
        // zalogowaniu, kolejny etap).
        android.widget.LinearLayout metaRow = new android.widget.LinearLayout(this);
        metaRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText(getString(R.string.author_placeholder));
        authorView.setTextColor(getResources().getColor(R.color.pr_muted));
        authorView.setTextSize(12f);
        metaRow.addView(authorView);

        TextView dateView = new TextView(this);
        dateView.setText("   " + fmt.format(new java.util.Date(file.lastModified())));
        dateView.setTextColor(getResources().getColor(R.color.pr_muted));
        dateView.setTextSize(12f);
        metaRow.addView(dateView);

        card.addView(metaRow);

        // Wiersz przyciskow — 4, jak w PitchRec: Wyslij NS / DAW / pobierz / Usun.
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int btnMarginEnd = (int) (6 * density);

        Button sendBtn = makeOutlinedButton(getString(R.string.btn_send_ns), R.color.pr_purple, density);
        sendBtn.setOnClickListener(v -> Toast.makeText(this, getString(R.string.requires_login), Toast.LENGTH_LONG).show());
        android.widget.LinearLayout.LayoutParams sendParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sendParams.rightMargin = btnMarginEnd;
        sendBtn.setLayoutParams(sendParams);
        btnRow.addView(sendBtn);

        Button playBtn = makeOutlinedButton(getString(R.string.btn_daw_short), R.color.pr_accent, density);
        playBtn.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            loadAndDisplayFile(file);
            playFile(file.getAbsolutePath(), 0L);
        });
        android.widget.LinearLayout.LayoutParams playParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        playParams.rightMargin = btnMarginEnd;
        playBtn.setLayoutParams(playParams);
        btnRow.addView(playBtn);

        Button shareBtn = makeOutlinedButton("⬇", R.color.pr_accent, density);
        shareBtn.setOnClickListener(v -> shareRecording(file));
        android.widget.LinearLayout.LayoutParams shareParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f);
        shareParams.rightMargin = btnMarginEnd;
        shareBtn.setLayoutParams(shareParams);
        btnRow.addView(shareBtn);

        Button delBtn = makeOutlinedButton(getString(R.string.btn_delete_short), R.color.pr_warn, density);
        delBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(file.getName())
                .setPositiveButton(getString(R.string.btn_delete), (d2, w2) -> {
                    file.delete();
                    Toast.makeText(this, getString(R.string.deleted_toast), Toast.LENGTH_SHORT).show();
                    showRecordingsList();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show());
        delBtn.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(delBtn);

        card.addView(btnRow);

        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = marginBottom;
        card.setLayoutParams(cardParams);

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
            Toast.makeText(this, "Brak nagrań", Toast.LENGTH_SHORT).show();
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
            startActivity(Intent.createChooser(shareIntent, "Udostępnij wszystkie nagrania"));
        } catch (Exception e) {
            Toast.makeText(this, "Błąd: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            startActivity(Intent.createChooser(shareIntent, "Udostępnij nagranie"));
        } catch (Exception e) {
            Toast.makeText(this, "Błąd udostępniania: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
