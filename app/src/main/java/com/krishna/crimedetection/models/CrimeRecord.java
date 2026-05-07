package com.krishna.crimedetection.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crime_history")
public class CrimeRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String prediction;
    private double confidence;
    private String timestamp;
    private String videoPath;
    private double latitude;
    private double longitude;

    // NEW FIELDS FOR VIOLENCE DETECTION
    private double inferenceTimeMs;      // NEW: Time taken by model
    private double preprocessingTimeMs;  // NEW: Time to prepare video

    public CrimeRecord() {
        // Required for Room or other frameworks
    }

    public CrimeRecord(String prediction, double confidence, String timestamp, String videoPath, double latitude, double longitude) {
        this.prediction = prediction;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.videoPath = videoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.inferenceTimeMs = 0;      // Default
        this.preprocessingTimeMs = 0;  // Default
    }

    // Existing getters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // NEW GETTERS/SETTERS
    public double getInferenceTimeMs() { return inferenceTimeMs; }
    public void setInferenceTimeMs(double inferenceTimeMs) { this.inferenceTimeMs = inferenceTimeMs; }

    public double getPreprocessingTimeMs() { return preprocessingTimeMs; }
    public void setPreprocessingTimeMs(double preprocessingTimeMs) { this.preprocessingTimeMs = preprocessingTimeMs; }
}