package com.krishna.crimedetection.realtime;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Convert Bitmap frames to model input format
 *
 * Processing steps:
 * 1. Resize to 224x224
 * 2. Convert BGR (Bitmap) to RGB
 * 3. Normalize: pixel / 255.0
 * 4. Stack to shape (1, 16, 224, 224, 3)
 * 5. Return as float array for model
 */
public class FramePreprocessor {

    private static final String TAG = "FramePreprocessor";
    private static final int FRAME_SIZE = 224;
    private static final float NORMALIZATION = 255.0f;
    private static final int FRAMES_REQUIRED = 16;

    /**
     * Convert frames to model input format
     *
     * Input: Array of Bitmap frames (any size)
     * Output: float[1][16][224][224][3] for model
     *
     * @param frames Array of 16 Bitmap frames
     * @return float array ready for model, or null if error
     */
    public static float[][][][][] preprocessFrames(Bitmap[] frames) {
        Log.d(TAG, "Preprocessing " + frames.length + " frames...");

        if (frames == null || frames.length != FRAMES_REQUIRED) {
            Log.e(TAG, "Invalid frame count: " + (frames != null ? frames.length : 0));
            return null;
        }

        try {
            // Initialize output array: (1, 16, 224, 224, 3)
            float[][][][][] output = new float[1][FRAMES_REQUIRED][FRAME_SIZE][FRAME_SIZE][3];

            // Process each frame
            for (int frameIdx = 0; frameIdx < FRAMES_REQUIRED; frameIdx++) {
                Bitmap frame = frames[frameIdx];

                if (frame == null) {
                    Log.e(TAG, "Null frame at index " + frameIdx);
                    return null;
                }

                // Resize frame to 224x224
                Bitmap resized = Bitmap.createScaledBitmap(frame, FRAME_SIZE, FRAME_SIZE, true);

                // Convert to RGB and normalize
                float[][][] frameArray = bitmapToArray(resized);

                if (frameArray == null) {
                    Log.e(TAG, "Failed to convert frame " + frameIdx);
                    return null;
                }

                // Store in output
                output[0][frameIdx] = frameArray;

                // Recycle resized bitmap if different from original
                if (resized != frame) {
                    resized.recycle();
                }
            }

            Log.d(TAG, "✓ Preprocessing complete. Output shape: (1, 16, 224, 224, 3)");
            return output;

        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing frames: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert single Bitmap to float array
     *
     * Steps:
     * 1. Extract pixel data
     * 2. Convert BGR to RGB
     * 3. Normalize: pixel / 255.0
     *
     * @param bitmap Bitmap frame (224x224)
     * @return float[224][224][3] (RGB, normalized)
     */
    private static float[][][] bitmapToArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width != FRAME_SIZE || height != FRAME_SIZE) {
            Log.w(TAG, "Frame size mismatch: " + width + "x" + height);
            return null;
        }

        float[][][] array = new float[FRAME_SIZE][FRAME_SIZE][3];

        try {
            // Extract pixel data
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            // Convert each pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixelIndex = y * width + x;
                    int pixel = pixels[pixelIndex];

                    // Extract ARGB components
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    // Bitmap is ARGB, convert to RGB and normalize
                    array[y][x][0] = r / NORMALIZATION;  // R
                    array[y][x][1] = g / NORMALIZATION;  // G
                    array[y][x][2] = b / NORMALIZATION;  // B
                }
            }

            return array;

        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify output shape
     * @param output Preprocessed frames
     * @return true if shape is correct (1, 16, 224, 224, 3)
     */
    public static boolean verifyShape(float[][][][][] output) {
        if (output == null) return false;
        return output.length == 1 &&
                output[0].length == 16 &&
                output[0][0].length == 224 &&
                output[0][0][0].length == 224 &&
                output[0][0][0][0].length == 3;
    }
}