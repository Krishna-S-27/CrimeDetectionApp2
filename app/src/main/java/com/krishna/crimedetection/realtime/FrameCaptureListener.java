package com.krishna.crimedetection.realtime;

import android.graphics.Bitmap;

/**
 * Listener interface for frame capture events
 * Used by CameraX ImageAnalysis to notify when frames are ready
 */
public interface FrameCaptureListener {

    /**
     * Called when a new frame is captured from camera
     *
     * @param frame Bitmap frame from camera
     * @param timestamp Frame timestamp in milliseconds
     * @param fps Current processing FPS
     */
    void onFrameCaptured(Bitmap frame, long timestamp, float fps);

    /**
     * Called when buffer is full and ready for prediction
     *
     * @param frames Array of 16 frames
     */
    void onBufferFull(Bitmap[] frames);

    /**
     * Called on capture error
     *
     * @param error Error message
     */
    void onCaptureError(String error);
}