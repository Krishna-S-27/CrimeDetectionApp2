package com.krishna.crimedetection.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for Login and Registration
 */
public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;
    
    @SerializedName("token_type")
    private String tokenType;
    
    @SerializedName("user")
    private UserResponse user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    public static class UserResponse {
        private int id;
        private String username;
        private String email;
        private String role;
        @SerializedName("phone_number")
        private String phoneNumber;
        @SerializedName("emergency_contact")
        private String emergencyContact;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getEmergencyContact() { return emergencyContact; }
    }
}