package com.krishna.crimedetection.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.krishna.crimedetection.managers.EmergencyResponseManager;
import com.krishna.crimedetection.models.Prediction;
import com.krishna.crimedetection.realtime.RealtimeCameraManager;
import com.krishna.crimedetection.realtime.RealtimePredictionManager;
import com.krishna.crimedetection.utils.Constants;
import com.krishna.crimedetection.utils.LocationManager;
import com.krishna.crimedetection.utils.NotificationUtils;

import java.io.File;

/**
 * Foreground service that handles real-time crime detection even when the app is in background.
 */
public class RecordingForegroundService extends LifecycleService implements RealtimePredictionManager.RealtimePredictionListener {

    private static final String TAG = "RecordingService";
    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";

    private final IBinder binder = new LocalBinder();
    private RealtimeCameraManager cameraManager;
    private RealtimePredictionManager predictionManager;
    private ServiceListener listener;

    private Recording activeRecording;
    private boolean isRecordingVideo = false;

    public interface ServiceListener {
        void onPredictionUpdate(Prediction prediction, double inferenceTime);
        void onFrameProcessed(int bufferSize, int totalCaptured, float fps);
        void onError(String error);
    }

    public class LocalBinder extends Binder {
        public RecordingForegroundService getService() {
            return RecordingForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
        NotificationUtils.ensureChannels(this);
        
        predictionManager = new RealtimePredictionManager(this, this);
        // Camera manager starts without a preview initially
        cameraManager = new RealtimeCameraManager(this, this, null, predictionManager);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(1001, NotificationUtils.buildForegroundNotification(this, "Real-time detection active", false));
        cameraManager.startCamera();
        
        return START_STICKY;
    }

    public void setListener(ServiceListener listener) {
        this.listener = listener;
    }

    public void updatePreview(androidx.camera.view.PreviewView previewView) {
        if (cameraManager != null) {
            cameraManager.updatePreviewView(previewView);
        }
    }

    public void startVideoRecording() {
        if (isRecordingVideo || cameraManager == null || cameraManager.getVideoCapture() == null) return;

        String folderName = "CrimeDetection";
        File folder = new File(getExternalFilesDir(null), folderName);
        if (!folder.exists()) folder.mkdirs();

        File videoFile = new File(folder, "crime_realtime_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();

        try {
            activeRecording = cameraManager.getVideoCapture().getOutput()
                    .prepareRecording(this, options)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                        if (recordEvent instanceof VideoRecordEvent.Finalize) {
                            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                            isRecordingVideo = false;
                            if (!finalizeEvent.hasError()) {
                                lastSavedVideoPath = videoFile.getAbsolutePath();
                                Log.i(TAG, "Video recording finalized: " + videoFile.getAbsolutePath());
                            } else {
                                Log.e(TAG, "Video recording error: " + finalizeEvent.getError());
                            }
                        }
                    });
            isRecordingVideo = true;
            Log.i(TAG, "Video recording started");
        } catch (SecurityException e) {
            Log.e(TAG, "Audio permission missing for recording: " + e.getMessage());
        }
    }

    public void stopVideoRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
            isRecordingVideo = false;
            Log.i(TAG, "Video recording stopped");
        }
    }

    private String lastSavedVideoPath = null;

    public String getLastSavedVideoPath() {
        return lastSavedVideoPath;
    }

    public boolean isRecordingVideo() {
        return isRecordingVideo;
    }

    @Override
    public void onPredictionReceived(Prediction prediction, double inferenceTimeMs) {
        if (listener != null) {
            listener.onPredictionUpdate(prediction, inferenceTimeMs);
        }

        if (prediction.isViolent()) {
            // Update notification with latest status
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                String status = String.format(java.util.Locale.getDefault(), "🚨 ALERT: Violence detected! (%.0f%%)", prediction.getConfidence() * 100);
                nm.notify(1001, NotificationUtils.buildForegroundNotification(this, status, true));
            }
            
            if (prediction.getConfidence() > Constants.EMERGENCY_RESPONSE_THRESHOLD) {
                handleHighConfidenceDetection(prediction);
            }
        } else {
            // Safe status
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(1001, NotificationUtils.buildForegroundNotification(this, "Real-time detection active (Safe)", false));
            }
        }
    }

    @Override
    public void onFrameCaptured(int bufferSize, int totalCaptured, float fps) {
        if (listener != null) {
            listener.onFrameProcessed(bufferSize, totalCaptured, fps);
        }
    }

    private void handleHighConfidenceDetection(Prediction prediction) {
        NotificationUtils.showNotification(this, "🚨 VIOLENCE DETECTED", 
                prediction.alertMessage != null ? prediction.alertMessage : "High confidence: " + prediction.getConfidencePercent());

        new LocationManager(this).getLastKnownLocation(new LocationManager.LocationCallback() {
            @Override
            public void onLocationReceived(Location location) {
                EmergencyResponseManager.triggerEmergencyResponse(
                        RecordingForegroundService.this,
                        prediction,
                        location,
                        lastSavedVideoPath,
                        null
                );
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Location error: " + error);
                EmergencyResponseManager.triggerEmergencyResponse(
                        RecordingForegroundService.this,
                        prediction,
                        null,
                        lastSavedVideoPath,
                        null
                );
            }
        });
    }

    @Override
    public void onPredictionError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onDestroy() {
        stopVideoRecording();
        if (cameraManager != null) {
            cameraManager.stopCamera();
        }
        if (predictionManager != null) {
            predictionManager.stop();
        }
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
    }
}
