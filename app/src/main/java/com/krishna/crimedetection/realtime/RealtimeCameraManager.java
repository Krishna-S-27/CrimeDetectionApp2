package com.krishna.crimedetection.realtime;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Manages CameraX setup for real-time frame capture.
 * Can be bound to an Activity or a LifecycleService.
 */
public class RealtimeCameraManager {

    private static final String TAG = "RealtimeCameraManager";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    private final FrameCaptureListener frameListener;

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private VideoCapture<Recorder> videoCapture;

    public RealtimeCameraManager(Context context, LifecycleOwner lifecycleOwner,
                                 PreviewView previewView, FrameCaptureListener frameListener) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.frameListener = frameListener;
    }

    /**
     * Updates the PreviewView dynamically (useful for switching between background and foreground)
     */
    public void updatePreviewView(PreviewView newPreviewView) {
        this.previewView = newPreviewView;
        if (cameraProvider != null) {
            setupCamera();
        }
    }

    /**
     * Start camera and begin frame capture
     */
    public void startCamera() {
        if (cameraProvider != null) {
            setupCamera();
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                setupCamera();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization error: " + e.getMessage());
                if (frameListener != null) {
                    frameListener.onCaptureError("Camera error: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Setup camera preview, frame analysis and video capture
     */
    private void setupCamera() {
        if (cameraProvider == null) return;

        // Initialize Image Analysis if not already done
        if (imageAnalysis == null) {
            imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    new FrameAnalyzer(frameListener)
            );
        }

        // Initialize Video Capture if not already done
        if (videoCapture == null) {
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build();
            videoCapture = VideoCapture.withOutput(recorder);
        }

        // Setup Preview if view is available
        if (previewView != null) {
            preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } else {
            preview = null;
        }

        // Select back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        // Bind to lifecycle
        try {
            cameraProvider.unbindAll();

            List<UseCase> useCases = new ArrayList<>();
            useCases.add(imageAnalysis);
            useCases.add(videoCapture);
            if (preview != null) {
                useCases.add(preview);
            }

            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    useCases.toArray(new UseCase[0])
            );

            String mode = preview != null ? "Preview, Analysis, Video" : "Analysis, Video";
            Log.i(TAG, "✓ Camera bound with: " + mode);

        } catch (Exception e) {
            Log.e(TAG, "Error binding camera: " + e.getMessage());
            if (frameListener != null) {
                frameListener.onCaptureError("Binding error: " + e.getMessage());
            }
        }
    }

    public VideoCapture<Recorder> getVideoCapture() {
        return videoCapture;
    }

    /**
     * Stop camera capture
     */
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
