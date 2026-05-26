package com.krishna.crimedetection.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crime_scene_evidence")
public class CrimeSceneEvidence {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int incidentId;
    private double latitude;
    private double longitude;
    private String address;
    private long timestamp;
    private String weather;
    private String lighting;
    private String crowdSize;
    private String additionalNotes;
    private String photoPaths; // JSON array
    private String videoPaths; // JSON array
    private long createdAt;

    public CrimeSceneEvidence() {
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIncidentId() { return incidentId; }
    public void setIncidentId(int incidentId) { this.incidentId = incidentId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }
    public String getLighting() { return lighting; }
    public void setLighting(String lighting) { this.lighting = lighting; }
    public String getCrowdSize() { return crowdSize; }
    public void setCrowdSize(String crowdSize) { this.crowdSize = crowdSize; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public String getPhotoPaths() { return photoPaths; }
    public void setPhotoPaths(String photoPaths) { this.photoPaths = photoPaths; }
    public String getVideoPaths() { return videoPaths; }
    public void setVideoPaths(String videoPaths) { this.videoPaths = videoPaths; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
