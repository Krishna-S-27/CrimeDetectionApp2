package com.krishna.crimedetection.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.krishna.crimedetection.utils.NotificationUtils;

public class RecordingForegroundService extends Service {
    public static final int NOTIF_ID = 1001;
    public static final String ACTION_CRIME_DETECTED = "com.krishna.crimedetection.CRIME_DETECTED";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationUtils.ensureChannels(this);
    }

    private Notification buildNotification(String text, boolean isAlert) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationUtils.SERVICE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setContentTitle("Crime Detection")
                .setContentText(text)
                .setOngoing(true)
                .setPriority(isAlert ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_LOW);

        if (isAlert) {
            builder.setColor(Color.RED)
                   .setColorized(true)
                   .setSmallIcon(android.R.drawable.ic_dialog_alert);
        }

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CRIME_DETECTED.equals(intent.getAction())) {
            updateNotification("🚨 CRIME DETECTED! Checking details...", true);
        } else {
            startForeground(NOTIF_ID, buildNotification("Recording in progress…", false));
        }
        return START_STICKY;
    }

    private void updateNotification(String text, boolean isAlert) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification(text, isAlert));
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
