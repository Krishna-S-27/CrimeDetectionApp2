package com.krishna.crimedetection.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_uploads")
public class VideoUploadRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String videoUri;
    public String fileName;
    public long fileSize;
    public String uploadStatus; // PENDING, UPLOADING, COMPLETED, FAILED, PAUSED
    public int progress;
    public String speed;
    public long timeRemaining;
    public long createdAt;
    public long updatedAt;
    public String errorMessage;
    public int retryCount;
    public Integer incidentId;

    public VideoUploadRecord() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.uploadStatus = "PENDING";
        this.progress = 0;
        this.retryCount = 0;
    }
}
