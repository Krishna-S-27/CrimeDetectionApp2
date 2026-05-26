package com.krishna.crimedetection.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for a single Crime Incident
 */
public class IncidentResponse {
    private int id;
    private String prediction;
    private double confidence;
    private String timestamp;
    @SerializedName("video_path")
    private String videoPath;
    private double latitude;
    private double longitude;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("detection_type")
    private String detectionType;

    public int getId() { return id; }
    public String getPrediction() { return prediction; }
    public double getConfidence() { return confidence; }
    public String getTimestamp() { return timestamp; }
    public String getVideoPath() { return videoPath; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getUserId() { return userId; }
    public String getDetectionType() { return detectionType; }
}