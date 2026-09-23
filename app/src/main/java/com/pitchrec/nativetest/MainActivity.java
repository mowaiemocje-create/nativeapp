package com.pitchrec.nativetest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    private Button recordButton;
    private Button pauseButton;
    private Button playButton;
    private Button resetButton;
    private Button recordingsListButton;
    private Button settingsButton;
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

    @Override
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
        resetButton = findViewById(R.id.resetButton);
        recordingsListButton = findViewById(R.id.recordingsListButton);
        settingsButton = findViewById(R.id.settingsButton);
        statusText = findViewById(R.id.statusText);
        timeText = findViewById(R.id.timeText);
        gainValueText = findViewById(R.id.gainValueText);
        zoomValueText = findViewById(R.id.zoomValueText);
        pitchWaveView = findViewById(R.id.pitchWaveView);
        gainSlider = findViewById(R.id.gainSlider);
        zoomSlider = findViewById(R.id.zoomSlider);
        levelMeter = findViewById(R.id.levelMeter);

        RecordingResultHolder.setListener(this);

        gainSlider.setValue(0.183f); // ~1.0x przy zakresie -10x do +50x
        gainSlider.setOnValueChangeListener(v -> {
            float gain = -10f + v * 60f; // zakres -10x do +50x
            LiveAudioData.gainMultiplier = gain;
            gainValueText.setText(gainToDbString(gain));
        });
        LiveAudioData.gainMultiplier = -10f + 0.183f * 60f;
        gainValueText.setText(gainToDbString(LiveAudioData.gainMultiplier));

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
        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecordingFlow();
            } else if (!isPaused) {
                pauseRecordingFlow();
            } else {
                resumeRecordingFlow();
            }
        });

        pauseButton.setOnClickListener(v -> stopRecordingFlow());

        playButton.setOnClickListener(v -> {
            if (isPaused) {
                previewDuringPause(pendingSeekSample);
            } else {
                String path = loadedFilePath != null ? loadedFilePath : lastSavedFilePath;
                if (path != null) playFile(path, pendingSeekSample);
            }
        });
        pitchWaveView.setOnSeekListener(sample -> {
            pendingSeekSample = sample;
        });
        resetButton.setOnClickListener(v -> resetRecording());
        recordingsListButton.setOnClickListener(v -> showRecordingsList());

        settingsButton.setOnClickListener(v -> showSettingsDialog());
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

        // Kolor linii pitch — kilka gotowych opcji (prosciej niz pelny wybor koloru)
        TextView pitchColorLabel = new TextView(this);
        pitchColorLabel.setText("Kolor linii pitch:");
        container.addView(pitchColorLabel);

        android.widget.LinearLayout colorRow = new android.widget.LinearLayout(this);
        colorRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int[] presetColors = {0xFFFF3B30, 0xFFFFE600, 0xFF00E000, 0xFFFFFFFF, 0xFF7EC8E3};
        String[] presetNames = {"Czerwony", "Żółty", "Zielony", "Biały", "Niebieski"};
        for (int idx = 0; idx < presetColors.length; idx++) {
            final int colorValue = presetColors[idx];
            Button colorBtn = new Button(this);
            colorBtn.setText(presetNames[idx]);
            colorBtn.setBackgroundColor(colorValue);
            colorBtn.setOnClickListener(v -> LiveAudioData.pitchLineColor = colorValue);
            colorRow.addView(colorBtn);
        }
        container.addView(colorRow);

        // Grubosc siatki DAW
        TextView gridWidthLabel = new TextView(this);
        gridWidthLabel.setText("Grubość siatki:");
        container.addView(gridWidthLabel);

        SliderView gridWidthSlider = new SliderView(this);
        gridWidthSlider.setLayoutParams(sliderParams);
        gridWidthSlider.setValue((LiveAudioData.gridLineWidthDp - 0.5f) / 3.5f); // zakres 0.5-4dp
        gridWidthSlider.setOnValueChangeListener(v -> LiveAudioData.gridLineWidthDp = 0.5f + v * 3.5f);
        container.addView(gridWidthSlider);

        // Kolor siatki DAW
        TextView gridColorLabel = new TextView(this);
        gridColorLabel.setText("Kolor siatki:");
        container.addView(gridColorLabel);

        android.widget.LinearLayout gridColorRow = new android.widget.LinearLayout(this);
        gridColorRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int[] gridPresetColors = {0x30FFFFFF, 0x50FFFFFF, 0x40F0973A, 0x400000FF, 0x30FF3B30};
        String[] gridPresetNames = {"Subtelna", "Wyraźna", "Pomarańcz.", "Niebieska", "Czerwona"};
        for (int idx = 0; idx < gridPresetColors.length; idx++) {
            final int colorValue = gridPresetColors[idx];
            Button gridColorBtn = new Button(this);
            gridColorBtn.setText(gridPresetNames[idx]);
            gridColorBtn.setOnClickListener(v -> LiveAudioData.gridLineColor = colorValue);
            gridColorRow.addView(gridColorBtn);
        }
        container.addView(gridColorRow);

        new AlertDialog.Builder(this)
                .setTitle("Ustawienia")
                .setView(container)
                .setPositiveButton("Zamknij", null)
                .show();
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
        recordButton.setText("⏸ PAUZA");
        pauseButton.setEnabled(true);
        pauseButton.setText("■ STOP");
        playButton.setEnabled(false);
        pitchWaveView.setLiveMode(true);
        // Podczas nagrywania zoom zablokowany na 8s — zapobiega przypadkowej zmianie
        // widoku w trakcie mowienia.
        zoomSlider.setEnabled(false);
        zoomSlider.setValue(0.12f);
        zoomValueText.setText("8s");
        pitchWaveView.setZoomSeconds(8f);
        statusText.setText("Nagrywanie… (możesz zablokować ekran)");
        pitchWaveView.postOnAnimation(redrawLoop);
    }

    private void stopRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_STOP);
        startService(intent);
        isRecording = false;
        isPaused = false;
        recordButton.setText("● REC");
        pauseButton.setEnabled(false);
        zoomSlider.setEnabled(true);
        statusText.setText("Przetwarzanie…");
    }

    private void pauseRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_PAUSE);
        startService(intent);
        isPaused = true;
        pausedAccumMs += System.currentTimeMillis() - lastResumeAtMs;
        recordButton.setText("▶ WZNÓW");
        statusText.setText("Pauza — dotknij wykresu, potem PLAY");
        playButton.setEnabled(true);
        pitchWaveView.pauseKeepingPosition(); // zachowuje pozycje, nie skacze do poczatku
    }

    private void resumeRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_RESUME);
        startService(intent);
        isPaused = false;
        lastResumeAtMs = System.currentTimeMillis();
        recordButton.setText("⏸ PAUZA");
        statusText.setText("Nagrywanie…");
        playButton.setEnabled(false);
        pitchWaveView.setLiveMode(true); // wraca do auto-przewijania najnowszych probek
    }

    private void resetRecording() {
        if (isRecording) stopRecordingFlow();
        LiveAudioData.reset();
        pitchWaveView.invalidate();
        timeText.setText("00:00:00");
        statusText.setText("Gotowy");
    }

    private long lastDisplayedSecond = -1L;

    private void updateTimeDisplay() {
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

    @Override
    public void onSuccess(String base64, long durationMs, String mimeType) {
        runOnUiThread(() -> {
            statusText.setText("Nagrano " + (durationMs / 1000) + "s — zapisano");
            saveRecordingForPlayback(base64, mimeType);
            loadedFilePath = null; // swiezo nagrany plik jest juz w LiveAudioData, nie trzeba wczytywac ponownie
            pendingSeekSample = 0L;
            pitchWaveView.setLiveMode(false); // pozwala przewijac/wskazac miejsce w tym co wlasnie nagrano
            pitchWaveView.resetPan();
            pitchWaveView.invalidate();
            playButton.setEnabled(true);
        });
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
            });
            player.prepare();
            int startMs = (int) (startSampleIndex * 1000L / LiveAudioData.SAMPLE_RATE);
            currentPlayer = player;
            if (startMs > 0) {
                // Niektóre urządzenia nie przewijają poprawnie, jeśli start() jest wołane
                // natychmiast po seekTo() — czekamy na potwierdzenie zakończenia przewijania.
                player.setOnSeekCompleteListener(mp -> {
                    mp.start();
                    statusText.setText("Odtwarzanie…");
                    startPlayheadUpdateLoop();
                });
                player.seekTo(startMs);
            } else {
                player.start();
                statusText.setText("Odtwarzanie…");
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
                    long sample = (long) currentPlayer.getCurrentPosition() * LiveAudioData.SAMPLE_RATE / 1000L;
                    pitchWaveView.setPlayheadSample(sample);
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

    private void showRecordingsList() {
        File[] files = getFilesDir().listFiles((dir, name) -> name.startsWith("recording_"));
        if (files == null) files = new File[0];
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        final File[] finalFiles = files;

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout listContainer = new android.widget.LinearLayout(this);
        listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        listContainer.setPadding(pad, pad, pad, pad);

        if (finalFiles.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("Brak nagrań");
            empty.setTextColor(getResources().getColor(R.color.pr_muted));
            listContainer.addView(empty);
        } else {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("d.M HH:mm", Locale.getDefault());
            for (File f : finalFiles) {
                listContainer.addView(buildRecordingCard(f, fmt));
            }
        }

        scrollView.addView(listContainer);

        new AlertDialog.Builder(this)
                .setTitle("Nagrania")
                .setView(scrollView)
                .setNegativeButton("Zamknij", null)
                .setNeutralButton("📁 Wgraj plik", (d, w) -> importExternalFile())
                .show();
    }

    // Odpowiednik ".rec-item" z PitchRec (JS) — gorny wiersz: nazwa + kategoria (placeholder
    // "bez opisu", kategorie to kolejny etap) + ikona NS; wiersz daty; wiersz 4 przyciskow
    // (Opisz i wyslij / Otworz / Udostepnij / Usun) — dokladnie ta sama struktura, tylko
    // "Wyslij do NS" jest na razie zablokowane (wymaga logowania, kolejny etap).
    private android.widget.LinearLayout buildRecordingCard(File file, java.text.SimpleDateFormat fmt) {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (10 * density);
        int marginBottom = (int) (8 * density);

        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundColor(getResources().getColor(R.color.pr_card));

        // Wiersz gorny: nazwa + "kategoria" (placeholder) + ikona NS
        android.widget.LinearLayout topRow = new android.widget.LinearLayout(this);
        topRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        TextView nameView = new TextView(this);
        String shortName = file.getName().replace("recording_", "").replaceAll("\\.(wav|mp3)$", "");
        nameView.setText(shortName);
        nameView.setTextColor(getResources().getColor(R.color.pr_accent));
        topRow.addView(nameView);

        TextView catBadge = new TextView(this);
        catBadge.setText(" bez opisu ");
        catBadge.setTextColor(getResources().getColor(R.color.pr_pause));
        topRow.addView(catBadge);

        TextView nsIcon = new TextView(this);
        nsIcon.setText(" ☁"); // zwykla, nieaktywna chmurka — brak integracji NS na tym etapie
        nsIcon.setTextColor(getResources().getColor(R.color.pr_muted));
        topRow.addView(nsIcon);

        card.addView(topRow);

        // Wiersz daty
        TextView dateView = new TextView(this);
        dateView.setText(fmt.format(new java.util.Date(file.lastModified())));
        dateView.setTextColor(getResources().getColor(R.color.pr_muted));
        card.addView(dateView);

        // Wiersz przyciskow — dokladnie 4, jak w PitchRec (nsB/open/dl/del)
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

        Button sendBtn = new Button(this);
        sendBtn.setText("📝 Opisz i wyślij");
        sendBtn.setTextColor(getResources().getColor(R.color.pr_pause));
        sendBtn.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        sendBtn.setOnClickListener(v -> Toast.makeText(this, "Wymaga zalogowania — logowanie w kolejnym etapie", Toast.LENGTH_LONG).show());
        btnRow.addView(sendBtn);

        Button openBtn = new Button(this);
        openBtn.setText("DAW");
        openBtn.setTextColor(getResources().getColor(R.color.pr_accent));
        openBtn.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        openBtn.setOnClickListener(v -> {
            loadAndDisplayFile(file);
            playFile(file.getAbsolutePath(), 0L);
        });
        btnRow.addView(openBtn);

        Button shareBtn = new Button(this);
        shareBtn.setText("📤 Udostępnij");
        shareBtn.setTextColor(getResources().getColor(R.color.pr_accent));
        shareBtn.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        shareBtn.setOnClickListener(v -> shareRecording(file));
        btnRow.addView(shareBtn);

        Button delBtn = new Button(this);
        delBtn.setText("Usuń");
        delBtn.setTextColor(getResources().getColor(R.color.pr_warn));
        delBtn.setBackgroundColor(getResources().getColor(R.color.pr_bg));
        delBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Usunąć nagranie?")
                .setMessage(file.getName())
                .setPositiveButton("Usuń", (d2, w2) -> {
                    file.delete();
                    Toast.makeText(this, "Usunięto", Toast.LENGTH_SHORT).show();
                    showRecordingsList();
                })
                .setNegativeButton("Anuluj", null)
                .show());
        btnRow.addView(delBtn);

        card.addView(btnRow);

        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = marginBottom;
        card.setLayoutParams(cardParams);

        return card;
    }

    // Import zewnetrznego pliku audio (jak "Wgraj plik" w PitchRec) — otwiera systemowy
    // wybornik plikow, kopiuje wybrany plik do folderu nagran.
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
                String ext = sourceUri.toString().toLowerCase(Locale.getDefault()).endsWith(".mp3") ? ".mp3" : ".wav";
                File destFile = new File(getFilesDir(), "recording_" + System.currentTimeMillis() + ext);
                try (java.io.InputStream in = getContentResolver().openInputStream(sourceUri);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while (in != null && (read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                Toast.makeText(this, "Zaimportowano: " + destFile.getName(), Toast.LENGTH_SHORT).show();
                showRecordingsList();
            } catch (Exception e) {
                Toast.makeText(this, "Błąd importu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
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
