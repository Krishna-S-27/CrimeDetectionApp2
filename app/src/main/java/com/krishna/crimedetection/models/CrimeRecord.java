package com.krishna.crimedetection.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity for storing crime/violence detection records
 *
 * Stores:
 * - Prediction (VIOLENT/NONVIOLENT)
 * - Confidence score
 * - Timestamp
 * - Location coordinates
 * - Timing information
 * - Video path
 */
@Entity(tableName = "crime_history")
public class CrimeRecord {

    @PrimaryKey(autoGenerate = true)
    private int id;

    // ===================== PREDICTION DATA =====================

    private String prediction;          // "VIOLENT" or "NONVIOLENT"
    private double confidence;          // 0.0 - 1.0

    // ===================== TIMESTAMP & LOCATION =====================

    private long timestamp;             // When prediction occurred (milliseconds)
    private double latitude;            // GPS latitude
    private double longitude;           // GPS longitude

    // ===================== VIDEO & FILE =====================

    private String videoPath;           // Path to video file (or "realtime_stream")

    // ===================== TIMING INFO =====================

    private double inferenceTimeMs;     // Time taken by model
    private double preprocessingTimeMs; // Time to prepare frames

    // ===================== DETECTION TYPE =====================

    private String detectionType;       // "batch" or "realtime"

    // ===================== EMERGENCY RESPONSE =====================
    private boolean emergencyContactsNotified;
    private String googleDriveVideoId;
    private String googleDriveShareLink;
    private String addressAtTime;
    private boolean isEmergencyResponse;

    // ===================== CONSTRUCTORS =====================

    /**
     * Full constructor with all fields
     */
    @Ignore
    public CrimeRecord(String prediction, double confidence, long timestamp,
                       String videoPath, double latitude, double longitude) {
        this.prediction = prediction;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.videoPath = videoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.inferenceTimeMs = 0;
        this.preprocessingTimeMs = 0;
        this.detectionType = "batch";  // Default
    }

    /**
     * Default constructor (required by Room)
     */
    public CrimeRecord() {
    }

    // ===================== GETTERS =====================

    public int getId() {
        return id;
    }

    public String getPrediction() {
        return prediction;
    }

    public double getConfidence() {
        return confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public double getPreprocessingTimeMs() {
        return preprocessingTimeMs;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public boolean isEmergencyContactsNotified() { return emergencyContactsNotified; }
    public String getGoogleDriveVideoId() { return googleDriveVideoId; }
    public String getGoogleDriveShareLink() { return googleDriveShareLink; }
    public String getAddressAtTime() { return addressAtTime; }
    public boolean isEmergencyResponse() { return isEmergencyResponse; }

    // ===================== SETTERS =====================

    public void setEmergencyContactsNotified(boolean notified) { this.emergencyContactsNotified = notified; }
    public void setGoogleDriveVideoId(String id) { this.googleDriveVideoId = id; }
    public void setGoogleDriveShareLink(String link) { this.googleDriveShareLink = link; }
    public void setAddressAtTime(String address) { this.addressAtTime = address; }
    public void setEmergencyResponse(boolean emergency) { this.isEmergencyResponse = emergency; }

    public void setId(int id) {
        this.id = id;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setInferenceTimeMs(double inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public void setPreprocessingTimeMs(double preprocessingTimeMs) {
        this.preprocessingTimeMs = preprocessingTimeMs;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Check if prediction is violent
     */
    public boolean isViolent() {
        return prediction != null && prediction.equalsIgnoreCase("VIOLENT");
    }

    /**
     * Get confidence as percentage string
     */
    public String getConfidencePercent() {
        return String.format("%.1f%%", confidence * 100);
    }

    /**
     * Get location as string
     */
    public String getLocationString() {
        return String.format("%.4f, %.4f", latitude, longitude);
    }

    @Override
    public String toString() {
        return "CrimeRecord{" +
                "id=" + id +
                ", prediction='" + prediction + '\'' +
                ", confidence=" + confidence +
                ", timestamp='" + timestamp + '\'' +
                ", detectionType='" + detectionType + '\'' +
                '}';
    }
}