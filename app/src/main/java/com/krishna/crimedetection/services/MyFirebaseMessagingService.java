package com.krishna.crimedetection.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.krishna.crimedetection.activities.IncidentDetailsActivity;
import com.krishna.crimedetection.utils.NotificationHandler;
import com.krishna.crimedetection.utils.NotificationPreferences;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token locally
        NotificationPreferences prefs = new NotificationPreferences(this);
        prefs.setFcmToken(token);
        
        // Send to backend
        NotificationHandler.sendFcmTokenToBackend(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(TAG, "From: " + message.getFrom());

        // Check if message contains a notification payload.
        if (message.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + message.getNotification().getBody());
            showNotification(message);
        }
    }

    private void showNotification(RemoteMessage message) {
        String title = message.getNotification().getTitle();
        String body = message.getNotification().getBody();
        
        // Handle data payload for deep linking
        Intent intent;
        if (message.getData().containsKey("incidentId")) {
            intent = new Intent(this, IncidentDetailsActivity.class);
            intent.putExtra("incident_id", Integer.parseInt(message.getData().get("incidentId")));
        } else {
            intent = new Intent(this, com.krishna.crimedetection.activities.MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        NotificationHandler.showLocalNotification(this, title, body, intent);
    }
}
