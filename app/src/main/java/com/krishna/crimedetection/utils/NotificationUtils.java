package com.krishna.crimedetection.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

public final class NotificationUtils {
    public static final String CRIME_CHANNEL_ID = "crime_alerts";
    public static final String SERVICE_CHANNEL_ID = "recording_status";
    private static final int CRIME_NOTIF_ID = 2001;

    private NotificationUtils() {}

    public static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel alertChannel = new NotificationChannel(
                    CRIME_CHANNEL_ID,
                    "Crime Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Notifications when crime is detected.");
            nm.createNotificationChannel(alertChannel);

            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Recording Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Shows when video recording is active.");
            nm.createNotificationChannel(serviceChannel);
        }
    }

    public static void showNotification(Context c, String title, String text) {
        ensureChannels(c);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CRIME_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(sound)
                .setAutoCancel(true);

        vibrate(c);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(CRIME_NOTIF_ID, b.build());
    }

    private static void vibrate(Context c) {
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(600);
        }
    }
}