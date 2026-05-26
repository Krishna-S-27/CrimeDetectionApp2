package com.krishna.crimedetection.realtime;

import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.krishna.crimedetection.R;

/**
 * Manages alerts for violent detections
 *
 * Features:
 * - Cooldown to prevent alert spam (5 seconds)
 * - Save to database
 * - Notification
 * - Callback to UI
 */
public class AlertManager {

    private static final String TAG = "AlertManager";
    private static final long ALERT_COOLDOWN_MS = 5000;  // 5 second cooldown

    private Context context;
    private long lastAlertTime = 0;
    private AlertListener listener;
    private MediaPlayer mediaPlayer;

    /**
     * Initialize alert manager
     */
    public AlertManager(Context context, AlertListener listener) {
        this.context = context;
        this.listener = listener;
        this.mediaPlayer = MediaPlayer.create(context, R.raw.siren); // Assuming siren.mp3 exists in res/raw
        Log.i(TAG, "AlertManager initialized with 5s cooldown");
    }

    /**
     * Handle violent prediction
     * Checks cooldown before triggering alert
     */
    public void handleViolentPrediction(float confidence) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastAlert = currentTime - lastAlertTime;

        if (timeSinceLastAlert < ALERT_COOLDOWN_MS) {
            Log.d(TAG, "Alert in cooldown. Time remaining: " +
                    (ALERT_COOLDOWN_MS - timeSinceLastAlert) + "ms");
            return;
        }

        // Update last alert time
        lastAlertTime = currentTime;

        Log.w(TAG, "🚨 VIOLENT DETECTED: " + String.format("%.2f%%", confidence * 100));

        // Feedback
        playSiren();
        triggerIntenseVibration();

        // Show notification
        showNotification(confidence);

        // Callback to UI
        if (listener != null) {
            listener.onViolentDetected(confidence);
        }
    }

    private void playSiren() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                try {
                    mediaPlayer.prepare();
                } catch (Exception e) {
                    Log.e(TAG, "Error preparing MediaPlayer: " + e.getMessage());
                }
            }
            mediaPlayer.start();
        }
    }

    private void triggerIntenseVibration() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 500, 200, 500, 200, 500};
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(1000);
            }
        }
    }

    /**
     * Show notification
     */
    private void showNotification(float confidence) {
        String title = "⚠️ Violence Detected!";
        String message = String.format("Confidence: %.1f%%", confidence * 100);
        com.krishna.crimedetection.utils.NotificationUtils.showNotification(context, title, message);
    }

    /**
     * Alert listener interface
     */
    public interface AlertListener {
        void onViolentDetected(float confidence);
    }
}