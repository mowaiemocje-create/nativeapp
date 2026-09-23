package com.pitchrec.nativetest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Button;
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

// Minimalna aktywność testowa — Faza 1: sprawdzenie, czy w pełni natywna architektura
// (bez WebView/Capacitor w procesie) przetrwa nagrywanie z zablokowanym ekranem, przy
// targetSdkVersion 36. Po zatrzymaniu, nagranie jest automatycznie odtwarzane, żeby od
// razu było słychać czy dźwięk jest ciągły, czy milknie w jakimś momencie.
public class MainActivity extends AppCompatActivity implements RecordingResultHolder.Listener {

    private Button recordButton;
    private TextView statusText;
    private boolean isRecording = false;
    private static final int REQUEST_MIC_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);

        RecordingResultHolder.setListener(this);

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecordingFlow();
            } else {
                stopRecordingFlow();
            }
        });
    }

    private void startRecordingFlow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            return;
        }
        // UWAGA: prośba o wyjątek od optymalizacji baterii USUNIĘTA z tego miejsca — wywołanie
        // startActivity() (ekran ustawień) w tym samym momencie co startForegroundService()
        // (bez czekania aż pierwsze się ustabilizuje) prawdopodobnie powodowało, że system
        // (Samsung One UI) zabijał cały proces (signal 9), zanim nagrywanie nawet się zaczęło.
        // Nie jest to kluczowe dla testu — jeśli okaże się potrzebne, dodamy to jako osobny,
        // wcześniejszy krok (np. przy starcie aplikacji, nie w momencie startu nagrywania).

        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isRecording = true;
        recordButton.setText("Zatrzymaj");
        statusText.setText("Nagrywanie… (możesz zablokować ekran)");
    }

    private void stopRecordingFlow() {
        Intent intent = new Intent(this, BackgroundRecorderService.class);
        intent.setAction(BackgroundRecorderService.ACTION_STOP);
        startService(intent);
        isRecording = false;
        recordButton.setText("Nagraj");
        statusText.setText("Przetwarzanie…");
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

    // ── RecordingResultHolder.Listener ──

    @Override
    public void onSuccess(String base64, long durationMs, String mimeType) {
        runOnUiThread(() -> {
            statusText.setText("Nagrano " + (durationMs / 1000) + "s — odtwarzanie do sprawdzenia…");
            playBackRecording(base64);
        });
    }

    @Override
    public void onError(String code, String message) {
        runOnUiThread(() -> statusText.setText("Błąd: " + code + " — " + message));
    }

    private void playBackRecording(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            File tempFile = new File(getCacheDir(), "playback_test.m4a");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(bytes);
            }
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(tempFile.getAbsolutePath());
            player.setOnCompletionListener(MediaPlayer::release);
            player.prepare();
            player.start();
        } catch (IOException e) {
            statusText.setText("Błąd odtwarzania: " + e.getMessage());
        }
    }
}
