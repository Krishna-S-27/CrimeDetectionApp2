package com.krishna.crimedetection.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages JWT tokens and user session data.
 */
public class TokenManager {
    private static final String PREF_NAME = "CrimeDetectionAuthPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUserInfo(int id, String username, String email, String role) {
        prefs.edit()
                .putInt(KEY_USER_ID, id)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public boolean isAdmin() {
        String role = prefs.getString(KEY_ROLE, "USER");
        return "ADMIN".equalsIgnoreCase(role);
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
}