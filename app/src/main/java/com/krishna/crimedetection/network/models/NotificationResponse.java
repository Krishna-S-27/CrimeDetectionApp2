package com.krishna.crimedetection.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for a single Notification
 */
public class NotificationResponse {
    private int id;
    private String title;
    private String content;
    private String timestamp;
    @SerializedName("is_read")
    private boolean isRead;
    @SerializedName("incident_id")
    private Integer incidentId;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public Integer getIncidentId() { return incidentId; }
}
