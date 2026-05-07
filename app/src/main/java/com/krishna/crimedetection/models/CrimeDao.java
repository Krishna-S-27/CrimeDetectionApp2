package com.krishna.crimedetection.models;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CrimeDao {
    @Insert
    void insert(CrimeRecord record);

    @Query("SELECT * FROM crime_history ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getAllRecords();
}