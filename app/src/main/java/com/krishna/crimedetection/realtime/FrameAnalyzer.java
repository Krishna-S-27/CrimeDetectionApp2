package com.krishna.crimedetection.realtime;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Analyzer for CameraX ImageAnalysis
 * Converts camera frames to Bitmap and notifies listener
 *
 * Runs on background thread continuously
 */
public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "FrameAnalyzer";

    private FrameCaptureListener listener;
    private long lastFrameTime = 0;
    private static final long FRAME_INTERVAL_MS = 1000 / 20;  // 20 fps target
    
    private int frameCount = 0;
    private long fpsStartTime = 0;
    private float currentFps = 0;

    /**
     * Initialize analyzer
     *
     * @param listener Callback for frame events
     */
    public FrameAnalyzer(FrameCaptureListener listener) {
        this.listener = listener;
        Log.i(TAG, "FrameAnalyzer initialized (Target 20 fps)");
    }

    @Override
    public void analyze(ImageProxy image) {
        try {
            long currentTime = System.currentTimeMillis();

            // Calculate FPS every second
            if (fpsStartTime == 0) fpsStartTime = currentTime;
            frameCount++;
            if (currentTime - fpsStartTime >= 1000) {
                currentFps = (frameCount * 1000f) / (currentTime - fpsStartTime);
                frameCount = 0;
                fpsStartTime = currentTime;
            }

            // Rate limiting
            if (currentTime - lastFrameTime < FRAME_INTERVAL_MS) {
                image.close();
                return;
            }
            lastFrameTime = currentTime;

            // Convert to Bitmap
            Bitmap frame = imageToBitmap(image);

            if (frame != null && listener != null) {
                listener.onFrameCaptured(frame, currentTime, currentFps);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing frame: " + e.getMessage());
            if (listener != null) {
                listener.onCaptureError("Frame analysis error: " + e.getMessage());
            }
        } finally {
            image.close();
        }
    }

    /**
     * Convert ImageProxy to Bitmap
     *
     * @param image ImageProxy from camera
     * @return Bitmap frame
     */
    private Bitmap imageToBitmap(ImageProxy image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int format = image.getFormat();

            Log.d(TAG, "Frame: " + width + "x" + height + " format: " + format);

            if (format == ImageFormat.NV21) {
                // Convert NV21 to RGB
                byte[] nv21 = getDataFromImage(image);
                return nv21ToRgb(nv21, width, height);
            } else if (format == ImageFormat.YUV_420_888) {
                // Convert YUV to RGB
                return yuv420ToRgb(image, width, height);
            } else {
                Log.w(TAG, "Unsupported format: " + format);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error converting image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert NV21 to RGB Bitmap
     */
    private Bitmap nv21ToRgb(byte[] nv21, int width, int height) {
        int frameSize = width * height;
        int[] rgb = new int[frameSize];

        for (int i = 0; i < frameSize; i++) {
            int y = nv21[i] & 0xff;
            int u = nv21[frameSize + 2 * (i / 2)] & 0xff;
            int v = nv21[frameSize + 2 * (i / 2) + 1] & 0xff;

            u = u - 128;
            v = v - 128;

            int r = (int) (y + 1.402f * v);
            int g = (int) (y - 0.344f * u - 0.714f * v);
            int b = (int) (y + 1.772f * u);

            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            rgb[i] = 0xff000000 | (r << 16) | (g << 8) | b;
        }

        return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Convert YUV_420_888 to RGB Bitmap
     */
    private Bitmap yuv420ToRgb(ImageProxy image, int width, int height) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();

        int ySize = planes[0].getBuffer().remaining();
        int uvSize = planes[1].getBuffer().remaining();

        byte[] nv21 = new byte[ySize + uvSize];

        planes[0].getBuffer().get(nv21, 0, ySize);

        ByteBuffer buffer = planes[1].getBuffer();
        int pixelStride = planes[1].getPixelStride();

        if (pixelStride == 1) {
            buffer.get(nv21, ySize, uvSize);
        } else {
            byte[] uvData = new byte[uvSize];
            buffer.get(uvData);

            for (int i = 0; i < uvSize / 2; i++) {
                nv21[ySize + i * 2] = uvData[i * 2 + 1];
                nv21[ySize + i * 2 + 1] = uvData[i * 2];
            }
        }

        return nv21ToRgb(nv21, width, height);
    }

    /**
     * Extract byte array from ImageProxy
     */
    private byte[] getDataFromImage(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int ySize = planes[0].getBuffer().remaining();
        int uvSize = planes[1].getBuffer().remaining() + planes[2].getBuffer().remaining();

        byte[] data = new byte[ySize + uvSize];
        planes[0].getBuffer().get(data, 0, ySize);

        byte[] uvData = new byte[uvSize];
        planes[1].getBuffer().get(uvData, 0, planes[1].getBuffer().remaining());
        planes[2].getBuffer().get(uvData, planes[1].getBuffer().remaining(), planes[2].getBuffer().remaining());

        System.arraycopy(uvData, 0, data, ySize, uvSize);
        return data;
    }
}