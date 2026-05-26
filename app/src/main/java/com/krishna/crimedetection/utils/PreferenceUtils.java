package com.krishna.crimedetection.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for SharedPreferences management.
 */
public final class PreferenceUtils {
    private static final String PREF_NAME = "CrimeDetectionPrefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_LAST_PREDICTION = "last_prediction";
    private static final String KEY_LAST_CONFIDENCE = "last_confidence";
    private static final String KEY_LAST_TIMESTAMP = "last_timestamp";
    private static final String KEY_EMERGENCY_NUM = "emergency_num";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_LOCATION = "user_location";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";
    private static final String KEY_PROFILE_AVATAR_RES = "profile_avatar_res";
    private static final String KEY_DRIVE_FOLDER_ID = "drive_folder_id";
    private static final String KEY_EMERGENCY_EMAIL = "emergency_email";
    
    private static final String DEFAULT_URL = "http://10.20.51.215:8000/";

    private PreferenceUtils() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setLoggedIn(Context context, boolean loggedIn) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void saveUser(Context context, String name, String email, String familyNum) {
        getPrefs(context).edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_EMERGENCY_NUM, familyNum)
                .apply();
    }

    public static void saveFullProfile(Context context, String name, String email, String phone, String familyNum) {
        getPrefs(context).edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_PHONE, phone)
                .putString(KEY_EMERGENCY_NUM, familyNum)
                .apply();
    }

    public static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "");
    }

    public static String getUserEmail(Context context) {
        return getPrefs(context).getString(KEY_USER_EMAIL, "");
    }

    public static String getUserPhone(Context context) {
        return getPrefs(context).getString(KEY_USER_PHONE, "");
    }

    public static void setLastLocation(Context context, String location) {
        getPrefs(context).edit().putString(KEY_USER_LOCATION, location).apply();
    }

    public static String getLastLocation(Context context) {
        return getPrefs(context).getString(KEY_USER_LOCATION, "Unknown Location");
    }

    public static void logout(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static String getBaseUrl(Context context) {
        String url = getPrefs(context).getString(KEY_BASE_URL, DEFAULT_URL);
        if (url == null || url.isEmpty()) return DEFAULT_URL;
        return url;
    }

    public static void setBaseUrl(Context context, String url) {
        if (url != null && !url.isEmpty() && !url.endsWith("/")) {
            url += "/";
        }
        getPrefs(context).edit().putString(KEY_BASE_URL, url).apply();
    }

    public static void saveLastResult(Context context, String prediction, double confidence, String timestamp) {
        getPrefs(context).edit()
                .putString(KEY_LAST_PREDICTION, prediction)
                .putFloat(KEY_LAST_CONFIDENCE, (float) confidence)
                .putString(KEY_LAST_TIMESTAMP, timestamp)
                .apply();
    }

    public static String getLastPrediction(Context context) {
        return getPrefs(context).getString(KEY_LAST_PREDICTION, "No data");
    }

    public static float getLastConfidence(Context context) {
        return getPrefs(context).getFloat(KEY_LAST_CONFIDENCE, 0.0f);
    }

    public static String getLastTimestamp(Context context) {
        return getPrefs(context).getString(KEY_LAST_TIMESTAMP, "--:--");
    }

    public static void setEmergencyNumber(Context context, String num) {
        getPrefs(context).edit().putString(KEY_EMERGENCY_NUM, num).apply();
    }

    public static String getEmergencyNumber(Context context) {
        return getPrefs(context).getString(KEY_EMERGENCY_NUM, "");
    }

    public static void setProfileImageUri(Context context, String uri) {
        getPrefs(context).edit().putString(KEY_PROFILE_IMAGE_URI, uri).putInt(KEY_PROFILE_AVATAR_RES, 0).apply();
    }

    public static String getProfileImageUri(Context context) {
        return getPrefs(context).getString(KEY_PROFILE_IMAGE_URI, "");
    }
    // In PreferenceUtils.java, add these methods:

    /**
     * Save server URL to preferences
     */
    public static void saveServerURL(Context context, String url) {
        setBaseUrl(context, url);
    }

    /**
     * Get server URL from preferences
     */
    public static String getServerURL(Context context) {
        return getBaseUrl(context);
    }

    public static void setProfileAvatarRes(Context context, int resId) {
        getPrefs(context).edit().putInt(KEY_PROFILE_AVATAR_RES, resId).putString(KEY_PROFILE_IMAGE_URI, "").apply();
    }

    public static int getProfileAvatarRes(Context context) {
        return getPrefs(context).getInt(KEY_PROFILE_AVATAR_RES, 0);
    }

    public static void setDriveFolderId(Context context, String folderId) {
        getPrefs(context).edit().putString(KEY_DRIVE_FOLDER_ID, folderId).apply();
    }

    public static String getDriveFolderId(Context context) {
        return getPrefs(context).getString(KEY_DRIVE_FOLDER_ID, "");
    }

    public static void setEmergencyEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMERGENCY_EMAIL, email).apply();
    }

    public static String getEmergencyEmail(Context context) {
        return getPrefs(context).getString(KEY_EMERGENCY_EMAIL, "");
    }
}
