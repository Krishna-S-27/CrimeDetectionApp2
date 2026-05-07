package com.krishna.crimedetection.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.auth.ProfileActivity;
import com.krishna.crimedetection.databinding.ActivityMainBinding;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.CrimeRecord;
import com.krishna.crimedetection.services.RecordingForegroundService;
import com.krishna.crimedetection.utils.NotificationUtils;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.TimeUtils;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResult;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.viewmodel.CrimeViewModel;
import com.krishna.crimedetection.viewmodel.CrimeViewModelFactory;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Main Activity for Crime/Violence Detection Application
 *
 * Features:
 * - Live camera recording via CameraX
 * - Video upload from gallery
 * - Violence detection via FastAPI backend
 * - Result history in Room database
 * - Real-time UI updates with LiveData
 * - Server connectivity monitoring
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION} :
            new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION};

    // ===================== UI BINDINGS =====================

    private ActivityMainBinding binding;

    // ===================== CAMERA & RECORDING =====================

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private File videoFile;

    // ===================== VIEWMODEL & LOCATION =====================

    private CrimeViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    // ===================== STATE VARIABLES =====================

    private boolean isServerConnected = false;
    private boolean isNetworkAvailable = false;

    // ===================== ACTIVITY RESULT LAUNCHERS =====================

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            android.net.Uri selectedVideoUri = result.getData().getData();
                            if (selectedVideoUri != null) {
                                handleSelectedVideo(selectedVideoUri);
                            }
                        }
                    });

    // ===================== LIFECYCLE METHODS =====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in
        if (!PreferenceUtils.isLoggedIn(this)) {
            startActivity(new Intent(this, com.krishna.crimedetection.auth.LoginActivity.class));
            finish();
            return;
        }

        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup ViewModel (observes predictions and server status)
        setupViewModel();

        // Setup bottom navigation
        setupNavigation();

        // Request permissions if not granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Setup button click listeners
        binding.fabAction.setOnClickListener(v -> toggleRecording());
        binding.btnUploadMedia.setOnClickListener(v -> openVideoPicker());

        // Display last result on app startup
        updateLastResultUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check server connection when activity resumes
        viewModel.checkServerConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showServerSettingsDialog();
            return true;
        } else if (id == R.id.action_stealth) {
            Toast.makeText(this, "Stealth Mode: Coming Soon!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===================== INITIALIZATION METHODS =====================

    /**
     * Setup ViewModel and observe LiveData
     * Connects backend predictions to UI
     */
    private void setupViewModel() {
        CrimeViewModelFactory factory = new CrimeViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(CrimeViewModel.class);

        // ===== UPLOAD STATUS MESSAGE =====
        viewModel.getUploadStatusMessage().observe(this, status -> {
            TransitionManager.beginDelayedTransition(binding.root);
            binding.tvStatus.setText(status);

            if (status.contains("Uploading") || status.contains("Preparing")) {
                binding.progress.setVisibility(View.VISIBLE);
            } else {
                binding.progress.setVisibility(View.GONE);
            }
        });

        // ===== PREDICTION RESULT =====
        viewModel.getPredictionResult().observe(this, response -> {
            if (response != null) {
                // Extract prediction and confidence
                String prediction = response.getPrediction();
                Double confidence = response.getConfidence();

                // Get location and save result
                processDetectionResult(prediction, confidence);
            }
        });

        // ===== PREDICTION ERROR =====
        viewModel.getPredictionError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.tvStatus.setText(R.string.status_failed);
                Toast.makeText(this, "❌ Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // ===== SERVER CONNECTION STATUS =====
        viewModel.getIsServerConnected().observe(this, isConnected -> {
            this.isServerConnected = isConnected;
            if (isConnected) {
                binding.tvStatus.setText("✅ Server Connected");
            } else {
                binding.tvStatus.setText("❌ Server Disconnected");
            }
        });

        // ===== SERVER STATUS MESSAGE =====
        viewModel.getServerStatusMessage().observe(this, statusMsg -> {
            if (statusMsg != null && !statusMsg.isEmpty()) {
                binding.tvStatus.setText(statusMsg);
            }
        });

        // ===== NETWORK STATUS =====
        viewModel.getIsNetworkAvailable().observe(this, isAvailable -> {
            this.isNetworkAvailable = isAvailable;
        });

        // ===== IS PREDICTING (LOADING STATE) =====
        viewModel.getIsPredicting().observe(this, isPredicting -> {
            if (isPredicting) {
                binding.fabAction.setEnabled(false);
                binding.btnUploadMedia.setEnabled(false);
            } else {
                binding.fabAction.setEnabled(true);
                binding.btnUploadMedia.setEnabled(true);
            }
        });
    }

    /**
     * Setup bottom navigation bar
     */
    private void setupNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    /**
     * Show server settings dialog
     * Allows user to configure backend IP and port
     */
    private void showServerSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        EditText etUrl = dialogView.findViewById(R.id.editServerUrl);

        String currentURL = PreferenceUtils.getServerURL(this);
        etUrl.setText(currentURL);

        new AlertDialog.Builder(this)
                .setTitle("Server Settings")
                .setMessage("Enter server URL (e.g., http://192.168.1.100:8000)")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUrl = etUrl.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        // Save and update
                        PreferenceUtils.saveServerURL(this, newUrl);
                        viewModel.updateServerURL(newUrl);
                        Toast.makeText(this, "Server URL updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ===================== CAMERA & RECORDING METHODS =====================

    /**
     * Start camera preview
     * Uses CameraX for modern camera implementation
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                if (binding != null && binding.viewFinder != null) {
                    preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
                } else {
                    return;
                }

                // Video recorder
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                // Camera selector (back camera)
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Toggle recording on/off
     */
    private void toggleRecording() {
        if (recording != null) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    /**
     * Start recording video
     */
    private void startRecording() {
        if (videoCapture == null) return;

        binding.tvStatus.setText(R.string.status_recording);
        binding.fabAction.setText(R.string.btn_stop);
        binding.fabAction.setIconResource(android.R.drawable.ic_media_pause);
        binding.statusOverlay.setVisibility(View.VISIBLE);

        // Start foreground service
        Intent serviceIntent = new Intent(this, RecordingForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Create video file
        videoFile = new File(getExternalFilesDir(null), "crime_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(videoFile).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission required for recording.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start recording
        recording = videoCapture.getOutput()
                .prepareRecording(this, fileOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                        if (!finalizeEvent.hasError()) {
                            // Recording finished successfully, upload it
                            uploadVideo();
                        } else {
                            handleRecordingError();
                        }
                    }
                });
    }

    /**
     * Stop recording video
     */
    private void stopRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        binding.fabAction.setText(R.string.btn_start);
        binding.fabAction.setIconResource(android.R.drawable.ic_media_play);
        binding.statusOverlay.setVisibility(View.GONE);
        stopService(new Intent(this, RecordingForegroundService.class));
    }

    /**
     * Handle recording error
     */
    private void handleRecordingError() {
        binding.tvStatus.setText(R.string.status_failed);
        binding.fabAction.setText(R.string.btn_start);
        binding.fabAction.setIconResource(android.R.drawable.ic_media_play);
        binding.statusOverlay.setVisibility(View.GONE);
        stopService(new Intent(this, RecordingForegroundService.class));
        Toast.makeText(this, "Recording error", Toast.LENGTH_SHORT).show();
    }

    // ===================== VIDEO UPLOAD METHODS =====================

    /**
     * Open video picker from gallery
     */
    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
        videoPickerLauncher.launch(intent);
    }

    /**
     * Handle video selected from gallery
     */
    private void handleSelectedVideo(android.net.Uri uri) {
        viewModel.predictViolenceFromVideo(uri);
    }

    /**
     * Upload recorded video to backend for analysis
     */
    private void uploadVideo() {
        if (videoFile == null || !videoFile.exists()) return;
        viewModel.predictViolenceFromVideo(android.net.Uri.fromFile(videoFile));
    }

    // ===================== RESULT PROCESSING METHODS =====================

    /**
     * Process detection result
     * Gets location and saves result to database
     */
    private void processDetectionResult(String prediction, double confidence) {
        String timestamp = TimeUtils.getCurrentTimestamp();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                double lat = location != null ? location.getLatitude() : 0.0;
                double lon = location != null ? location.getLongitude() : 0.0;
                updateUIWithResult(prediction, confidence, timestamp, lat, lon);
                saveResult(prediction, confidence, timestamp, lat, lon);
            });
        } else {
            updateUIWithResult(prediction, confidence, timestamp, 0, 0);
            saveResult(prediction, confidence, timestamp, 0, 0);
        }
    }

    /**
     * Update UI with prediction result
     */
    private void updateUIWithResult(String prediction, double confidence, String timestamp, double lat, double lon) {
        TransitionManager.beginDelayedTransition(binding.root);
        binding.resultCard.setVisibility(View.VISIBLE);
        binding.tvPrediction.setText("Prediction: " + prediction.toUpperCase());
        binding.tvConfidence.setText(String.format("Confidence: %.2f%%", confidence * 100));
        binding.tvTimestamp.setText("Time: " + timestamp);
        binding.tvLocation.setText(String.format("Location: %.4f, %.4f", lat, lon));

        if ("VIOLENT".equalsIgnoreCase(prediction)) {
            binding.resultCard.setStrokeWidth(2);
            binding.resultCard.setStrokeColor(Color.RED);
            triggerHapticFeedback();
            NotificationUtils.showNotification(this, "🚨 CRIME DETECTED",
                    "Confidence: " + String.format("%.0f%%", confidence * 100));
        } else {
            binding.resultCard.setStrokeWidth(2);
            binding.resultCard.setStrokeColor(ContextCompat.getColor(this, R.color.crime_safe));
        }
        binding.tvStatus.setText(R.string.status_ready);
    }

    /**
     * Save result to Room database
     */
    private void saveResult(String prediction, double confidence, String timestamp, double lat, double lon) {
        PreferenceUtils.saveLastResult(this, prediction, confidence, timestamp);

        CrimeRecord record = new CrimeRecord(prediction, confidence, timestamp,
                videoFile != null ? videoFile.getAbsolutePath() : "unknown", lat, lon);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).crimeDao().insert(record);
        });
    }

    /**
     * Update UI with last saved result
     */
    private void updateLastResultUI() {
        String lastPred = PreferenceUtils.getLastPrediction(this);
        if (!"No data".equals(lastPred)) {
            binding.resultCard.setVisibility(View.VISIBLE);
            binding.tvPrediction.setText("Prediction: " + lastPred.toUpperCase());
            binding.tvConfidence.setText(String.format("Confidence: %.2f%%", PreferenceUtils.getLastConfidence(this) * 100));
            binding.tvTimestamp.setText("Time: " + PreferenceUtils.getLastTimestamp(this));
        }
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Trigger haptic feedback (vibration)
     */
    private void triggerHapticFeedback() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0, 200, 100, 200}, -1));
            } else {
                v.vibrate(500);
            }
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}