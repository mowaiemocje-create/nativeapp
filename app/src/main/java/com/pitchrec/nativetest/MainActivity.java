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

// Faza 1B: nagrywanie + na żywo rysowany wykres fali i pitch (żółta linia), bez logowania.
// Menu i logowanie NS to kolejny, osobny etap.
public class MainActivity extends AppCompatActivity implements RecordingResultHolder.Listener {

    private Button recordButton;
    private Button playButton;
    private TextView statusText;
    private PitchWaveView pitchWaveView;
    private boolean isRecording = false;
    private String lastSavedFilePath = null;
    private static final int REQUEST_MIC_PERMISSION = 100;

    private final Handler redrawHandler = new Handler(Looper.getMainLooper());
    private final Runnable redrawLoop = new Runnable() {
        @Override
        public void run() {
            pitchWaveView.invalidate();
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
        statusText = findViewById(R.id.statusText);
        pitchWaveView = findViewById(R.id.pitchWaveView);

        RecordingResultHolder.setListener(this);

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecordingFlow();
            } else {
                stopRecordingFlow();
            }
        });

        playButton.setOnClickListener(v -> playLastRecording());
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
        recordButton.setText("Zatrzymaj");
        playButton.setEnabled(false);
        statusText.setText("Nagrywanie… (możesz zablokować ekran)");
        redrawHandler.post(redrawLoop);
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
