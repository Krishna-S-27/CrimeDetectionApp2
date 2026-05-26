package com.krishna.crimedetection.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for User Profile
 */
public class ProfileResponse {
    private int id;
    private String username;
    private String email;
    @SerializedName("phone_number")
    private String phoneNumber;
    @SerializedName("emergency_contact")
    private String emergencyContact;
    private String role;
    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }

    public void setUsername(String username) { this.username = username; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
}