package com.pitchrec.nativetest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pitchrec.backgroundrecorder.BackgroundRecorderService;
import com.pitchrec.backgroundrecorder.RecordingResultHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

// Ekran DAW — odtworzony uklad z PitchRec: suwaki MIC/ZOOM, miernik poziomu, przyciski
// PLAY/REC/RESET, wyswietlacz czasu. Bez logowania na razie (Faza 1) — menu NS to kolejny etap.
public class MainActivity extends AppCompatActivity implements RecordingResultHolder.Listener {

    private Button recordButton;
    private Button playButton;
    private Button resetButton;
    private TextView statusText;
    private TextView timeText;
    private TextView gainValueText;
    private TextView zoomValueText;
    private PitchWaveView pitchWaveView;
    private SliderView gainSlider;
    private SliderView zoomSlider;
    private ProgressBar levelMeter;

    private boolean isRecording = false;
    private long recordingStartedAtMs = 0L;
    private String lastSavedFilePath = null;
    private static final int REQUEST_MIC_PERMISSION = 100;

    private final Handler redrawHandler = new Handler(Looper.getMainLooper());
    private final Runnable redrawLoop = new Runnable() {
        @Override
        public void run() {
            pitchWaveView.invalidate();
            updateTimeDisplay();
            updateLevelMeter();
            if (isRecording) {
                redrawHandler.postDelayed(this, 50);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);
        resetButton = findViewById(R.id.resetButton);
        statusText = findViewById(R.id.statusText);
        timeText = findViewById(R.id.timeText);
        gainValueText = findViewById(R.id.gainValueText);
        zoomValueText = findViewById(R.id.zoomValueText);
        pitchWaveView = findViewById(R.id.pitchWaveView);
        gainSlider = findViewById(R.id.gainSlider);
        zoomSlider = findViewById(R.id.zoomSlider);
        levelMeter = findViewById(R.id.levelMeter);

        RecordingResultHolder.setListener(this);

        gainSlider.setValue(0.3f); // odpowiednik domyslnego gainLvl w PitchRec
        gainSlider.setOnValueChangeListener(v -> {
            float gain = 0.5f + v * 14.5f; // zakres podobny do gainLvl (0.5 - 15.0) w JS
            gainValueText.setText(String.format(Locale.getDefault(), "%.1fx", gain));
        });
        gainValueText.setText("1.0x");

        zoomSlider.setValue(0f); // 0 = ALL (caly zakres), jak w PitchRec
        zoomSlider.setOnValueChangeListener(v -> {
            if (v < 0.05f) {
                zoomValueText.setText("ALL");
                pitchWaveView.setZoomSeconds(0);
            } else {
                float seconds = 1f + v * 59f; // 1s - 60s zakres, podobnie do ZOOMS w JS
                zoomValueText.setText(String.format(Locale.getDefault(), "%.0fs", seconds));
                pitchWaveView.setZoomSeconds(seconds);
            }
        });

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecordingFlow();
            } else {
                stopRecordingFlow();
            }
        });

        playButton.setOnClickListener(v -> playLastRecording());
        resetButton.setOnClickListener(v -> resetRecording());
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isRecording = true;
        recordingStartedAtMs = System.currentTimeMillis();
        recordButton.setText("■ STOP");
        playButton.setEnabled(false);
        statusText.setText("Nagrywanie… (możesz zablokować ekran)");
        redrawHandler.post(redrawLoop);
    }

    private void stopRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_STOP);
        startService(intent);
        isRecording = false;
        recordButton.setText("● REC");
        statusText.setText("Przetwarzanie…");
    }

    private void resetRecording() {
        if (isRecording) stopRecordingFlow();
        LiveAudioData.reset();
        pitchWaveView.invalidate();
        timeText.setText("00:00:00");
        statusText.setText("Gotowy");
    }

    private void updateTimeDisplay() {
        long elapsedMs = System.currentTimeMillis() - recordingStartedAtMs;
        long totalSec = elapsedMs / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        timeText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
    }

    private void updateLevelMeter() {
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
            pitchWaveView.invalidate();
            saveRecordingForPlayback(base64);
            playButton.setEnabled(true);
        });
    }

    @Override
    public void onError(String code, String message) {
        runOnUiThread(() -> statusText.setText("Błąd: " + code + " — " + message));
    }

    private void saveRecordingForPlayback(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            File savedFile = new File(getFilesDir(), "recording_" + System.currentTimeMillis() + ".wav");
            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                fos.write(bytes);
            }
            lastSavedFilePath = savedFile.getAbsolutePath();
        } catch (IOException e) {
            statusText.setText("Błąd zapisu: " + e.getMessage());
        }
    }

    private void playLastRecording() {
        if (lastSavedFilePath == null) return;
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(lastSavedFilePath);
            player.setOnCompletionListener(MediaPlayer::release);
            player.prepare();
            player.start();
            statusText.setText("Odtwarzanie…");
        } catch (IOException e) {
            statusText.setText("Błąd odtwarzania: " + e.getMessage());
        }
    }
}
