package com.krishna.crimedetection.realtime;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Circular buffer for maintaining last 16 frames
 * Automatically removes oldest frame when new one added
 *
 * Used for sliding window predictions:
 * Frame 1-16 → Predict
 * Frame 2-17 → Predict
 * Frame 3-18 → Predict
 */
public class FrameBuffer {

    private static final String TAG = "FrameBuffer";
    private static final int BUFFER_SIZE = 16;

    private Queue<Bitmap> frameQueue;
    private int frameCount;

    /**
     * Initialize frame buffer
     */
    public FrameBuffer() {
        this.frameQueue = new LinkedList<>();
        this.frameCount = 0;
        Log.i(TAG, "FrameBuffer initialized with capacity: " + BUFFER_SIZE);
    }

    /**
     * Add frame to buffer
     * Automatically removes oldest if buffer is full
     *
     * @param frame Bitmap frame to add
     */
    public synchronized void addFrame(Bitmap frame) {
        if (frame == null) {
            Log.w(TAG, "Null frame received, skipping");
            return;
        }

        // Add new frame
        frameQueue.offer(frame);
        frameCount++;

        // Remove oldest frame if buffer exceeds size
        if (frameQueue.size() > BUFFER_SIZE) {
            Bitmap removed = frameQueue.poll();
            if (removed != null) {
                removed.recycle();  // Free memory
            }
        }

        Log.d(TAG, "Frame added. Buffer size: " + frameQueue.size() + "/" + BUFFER_SIZE);
    }

    /**
     * Get current frames as array
     * Returns copy of current buffer
     *
     * @return Array of Bitmap frames (null if not full)
     */
    public synchronized Bitmap[] getFrames() {
        if (frameQueue.size() < BUFFER_SIZE) {
            return null;  // Buffer not full yet
        }

        return frameQueue.toArray(new Bitmap[0]);
    }

    /**
     * Check if buffer is full (16 frames)
     *
     * @return true if ready for prediction
     */
    public synchronized boolean isReady() {
        return frameQueue.size() >= BUFFER_SIZE;
    }

    /**
     * Get current buffer size
     *
     * @return Number of frames in buffer
     */
    public synchronized int getSize() {
        return frameQueue.size();
    }

    /**
     * Clear all frames from buffer
     * Used for reset
     */
    public synchronized void clear() {
        for (Bitmap frame : frameQueue) {
            if (frame != null) {
                frame.recycle();
            }
        }
        frameQueue.clear();
        frameCount = 0;
        Log.i(TAG, "FrameBuffer cleared");
    }

    /**
     * Get frame count since start
     *
     * @return Total frames processed
     */
    public synchronized int getFrameCount() {
        return frameCount;
    }
}