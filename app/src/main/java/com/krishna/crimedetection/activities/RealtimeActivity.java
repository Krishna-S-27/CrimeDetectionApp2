package com.krishna.crimedetection.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.krishna.crimedetection.R;
import com.krishna.crimedetection.databinding.ActivityRealtimeBinding;
import com.krishna.crimedetection.models.Prediction;
import com.krishna.crimedetection.realtime.AlertManager;
import com.krishna.crimedetection.realtime.RealtimeAlertBottomSheet;
import com.krishna.crimedetection.services.RecordingForegroundService;
import com.krishna.crimedetection.utils.TimeUtils;
import com.krishna.crimedetection.viewmodel.RealtimeViewModel;

import java.util.Locale;

public class RealtimeActivity extends AppCompatActivity implements
        RecordingForegroundService.ServiceListener,
        AlertManager.AlertListener {

    private static final String TAG = "RealtimeActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
            } :
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private ActivityRealtimeBinding binding;
    private RealtimeViewModel viewModel;
    private AlertManager alertManager;
    
    private RecordingForegroundService recordingService;
    private boolean isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingForegroundService.LocalBinder binder = (RecordingForegroundService.LocalBinder) service;
            recordingService = binder.getService();
            isBound = true;
            recordingService.setListener(RealtimeActivity.this);
            
            // Sync UI state with service
            viewModel.setRecording(true);
            recordingService.updatePreview(binding.previewView);

            // Update record button state
            if (recordingService.isRecordingVideo()) {
                binding.btnRecord.setIconResource(android.R.drawable.ic_media_pause);
                binding.btnRecord.setText(R.string.btn_stop_recording);
            } else {
                binding.btnRecord.setIconResource(android.R.drawable.ic_menu_camera);
                binding.btnRecord.setText(R.string.btn_start_recording);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            recordingService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealtimeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RealtimeViewModel.class);
        alertManager = new AlertManager(this, this);

        setupToolbar();
        setupListeners();
        observeViewModel();

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_realtime);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnStartStop.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getIsRecording().getValue())) {
                stopDetection();
            } else {
                startDetection();
            }
        });

        binding.btnRecord.setOnClickListener(v -> {
            if (isBound && recordingService != null) {
                if (recordingService.isRecordingVideo()) {
                    recordingService.stopVideoRecording();
                    binding.btnRecord.setIconResource(android.R.drawable.ic_menu_camera);
                    binding.btnRecord.setText(R.string.btn_start_recording);
                } else {
                    recordingService.startVideoRecording();
                    binding.btnRecord.setIconResource(android.R.drawable.ic_media_pause);
                    binding.btnRecord.setText(R.string.btn_stop_recording);
                }
            }
        });

        binding.btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, IncidentHistoryActivity.class));
        });

        binding.btnDismissAlert.setOnClickListener(v -> viewModel.dismissAlert());
    }

    private void observeViewModel() {
        viewModel.getIsRecording().observe(this, isRecording -> {
            if (isRecording) {
                binding.btnStartStop.setText(R.string.btn_stop_detection);
                binding.btnStartStop.setIconResource(android.R.drawable.ic_media_pause);
                binding.tvStatus.setText("🔴 LIVE");
                binding.tvStatus.setBackgroundResource(R.drawable.status_badge_bg_red);
            } else {
                binding.btnStartStop.setText(R.string.btn_start_detection);
                binding.btnStartStop.setIconResource(android.R.drawable.ic_media_play);
                binding.tvStatus.setText("⏸️ READY");
                binding.tvStatus.setBackgroundResource(R.drawable.status_badge_bg);
            }
        });

        viewModel.getCurrentPrediction().observe(this, prediction -> {
            binding.tvPrediction.setText("Prediction: " + prediction);
            if ("VIOLENT".equalsIgnoreCase(prediction)) {
                binding.tvPrediction.setTextColor(getColor(R.color.red_alert));
                binding.progressBar.setIndicatorColor(getColor(R.color.red_alert));
            } else {
                binding.tvPrediction.setTextColor(getColor(R.color.crime_safe));
                binding.progressBar.setIndicatorColor(getColor(R.color.crime_safe));
            }
        });

        viewModel.getCurrentConfidence().observe(this, confidence -> {
            binding.tvConfidence.setText(String.format(Locale.getDefault(), "%.1f%%", confidence * 100));
            binding.progressBar.setProgress((int) (confidence * 100));
        });

        viewModel.getTotalFrames().observe(this, frames -> {
            binding.tvFrameCount.setText("Frames: " + frames);
            binding.tvLastUpdate.setText("Last: " + TimeUtils.getCurrentTimestamp());
        });

        viewModel.getViolentDetections().observe(this, count -> {
            binding.tvDetectionCount.setText("Violence Detections: " + count);
        });

        viewModel.getBackendFps().observe(this, fps -> {
            binding.tvFPS.setText(String.format(Locale.getDefault(), "%.1f FPS (S)", fps));
        });

        viewModel.getBackendBufferCount().observe(this, count -> {
            binding.tvBufferSize.setText("Server Buffer: " + count);
        });

        viewModel.getDetectionStatus().observe(this, status -> {
            if (Boolean.TRUE.equals(viewModel.getIsRecording().getValue())) {
                binding.tvStatus.setText("🔴 " + status);
            }
        });

        viewModel.getAlertVisible().observe(this, visible -> {
            binding.alertCard.setVisibility(visible ? View.VISIBLE : View.GONE);
            binding.statusOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                Prediction currentPred = viewModel.getLastViolentPrediction();
                if (currentPred != null && currentPred.alertMessage != null) {
                    binding.tvAlertMessage.setText(currentPred.alertMessage);
                } else {
                    binding.tvAlertMessage.setText("VIOLENCE DETECTED: Emergency contacts are being alerted.");
                }
                binding.statusOverlay.setBackgroundColor(android.graphics.Color.parseColor("#80FF0000"));
                triggerHapticFeedback();
                showAlertBottomSheet();
            }
        });
    }

    private void startDetection() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(this, RecordingForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        viewModel.startSession();
    }

    private void stopDetection() {
        if (recordingService != null && recordingService.isRecordingVideo()) {
            recordingService.stopVideoRecording();
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        Intent intent = new Intent(this, RecordingForegroundService.class);
        stopService(intent);
        viewModel.stopSession();
    }

    @Override
    public void onPredictionUpdate(Prediction prediction, double inferenceTime) {
        runOnUiThread(() -> {
            viewModel.updatePrediction(prediction, inferenceTime);
            
            // Log for debugging
            Log.d(TAG, "Prediction Update: " + prediction.prediction + " (" + prediction.getConfidencePercent() + ")");
            
            // Handle alert logic
            if (prediction.isViolent()) {
                if (prediction.getConfidence() > 0.6) {
                    alertManager.handleViolentPrediction(prediction.getConfidence());
                }
            } else {
                // If not violent, ensure the progress bar is showing safe color
                binding.progressBar.setIndicatorColor(getColor(R.color.crime_safe));
            }
        });
    }

    @Override
    public void onFrameProcessed(int bufferSize, int totalCaptured, float fps) {
        runOnUiThread(() -> {
            binding.tvBufferSize.setText("Buffer: " + bufferSize + "/16");
            binding.tvFrameCount.setText("Frames: " + totalCaptured);
            binding.tvFPS.setText(String.format(Locale.getDefault(), "%.1f FPS", fps));
            
            // Sync with ViewModel
            viewModel.updateFrameCount(totalCaptured);
            
            // Show that we are actively sending frames
            if (totalCaptured % 16 == 0) {
                binding.tvLastUpdate.setText("Processing Batch...");
            } else if (totalCaptured % 5 == 0) {
                binding.tvLastUpdate.setText("Syncing: " + TimeUtils.getCurrentTimestamp());
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onViolentDetected(float confidence) {
        // Handled via ViewModel in onPredictionUpdate
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, RecordingForegroundService.class);
        // Bind to existing service if it's already running
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            recordingService.updatePreview(null);
            unbindService(connection);
            isBound = false;
        }
    }

    private void showAlertBottomSheet() {
        RealtimeAlertBottomSheet bottomSheet = new RealtimeAlertBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), RealtimeAlertBottomSheet.TAG);
    }

    private void triggerHapticFeedback() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(500);
            }
        }
    }
}
