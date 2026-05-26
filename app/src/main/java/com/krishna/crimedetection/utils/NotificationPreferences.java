package com.krishna.crimedetection.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPreferences {
    private static final String PREF_NAME = "notification_prefs";
    private static final String FCM_TOKEN_KEY = "fcm_token";
    private static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String SOUND_ENABLED = "sound_enabled";
    private static final String VIBRATION_ENABLED = "vibration_enabled";
    private static final String VIOLENCE_ALERTS = "violence_alerts";
    private static final String ADMIN_ALERTS = "admin_alerts";
    
    private SharedPreferences prefs;
    
    public NotificationPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void setFcmToken(String token) {
        prefs.edit().putString(FCM_TOKEN_KEY, token).apply();
    }
    
    public String getFcmToken() {
        return prefs.getString(FCM_TOKEN_KEY, null);
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply();
    }
    
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(NOTIFICATIONS_ENABLED, true);
    }
    
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(SOUND_ENABLED, enabled).apply();
    }
    
    public boolean isSoundEnabled() {
        return prefs.getBoolean(SOUND_ENABLED, true);
    }
    
    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(VIBRATION_ENABLED, enabled).apply();
    }
    
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(VIBRATION_ENABLED, true);
    }
    
    public void setViolenceAlerts(boolean enabled) {
        prefs.edit().putBoolean(VIOLENCE_ALERTS, enabled).apply();
    }
    
    public boolean isViolenceAlertsEnabled() {
        return prefs.getBoolean(VIOLENCE_ALERTS, true);
    }
    
    public void setAdminAlerts(boolean enabled) {
        prefs.edit().putBoolean(ADMIN_ALERTS, enabled).apply();
    }
    
    public boolean isAdminAlertsEnabled() {
        return prefs.getBoolean(ADMIN_ALERTS, true);
    }
}
