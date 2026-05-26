package com.krishna.crimedetection.models;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VideoUploadDao {
    @Insert
    long insertUpload(VideoUploadRecord record);

    @Update
    void updateUpload(VideoUploadRecord record);

    @Query("DELETE FROM video_uploads WHERE id = :id")
    void deleteUpload(int id);

    @Query("SELECT * FROM video_uploads WHERE id = :id")
    VideoUploadRecord getUploadById(int id);

    @Query("SELECT * FROM video_uploads ORDER BY createdAt DESC")
    LiveData<List<VideoUploadRecord>> getAllUploads();

    @Query("SELECT * FROM video_uploads WHERE uploadStatus = 'PENDING' OR uploadStatus = 'FAILED' ORDER BY createdAt ASC")
    List<VideoUploadRecord> getPendingUploads();

    @Query("DELETE FROM video_uploads WHERE updatedAt < :olderThanMs AND uploadStatus = 'COMPLETED'")
    void deleteOldUploads(long olderThanMs);
}
