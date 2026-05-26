package com.krishna.crimedetection.models;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Room DAO (Data Access Object) for crime records
 *
 * Provides database query methods for:
 * - Insert, delete, update operations
 * - Query by prediction, date, confidence
 * - Statistics and aggregation
 */
@Dao
public interface CrimeDao {

    // ===================== INSERT =====================

    @Insert
    long insert(CrimeRecord record);

    // ===================== DELETE =====================

    @Delete
    void delete(CrimeRecord record);

    @Query("DELETE FROM crime_history WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM crime_history")
    int deleteAll();

    @Query("DELETE FROM crime_history WHERE timestamp < :cutoffTime")
    int deleteOlderThan(long cutoffTime);

    // ===================== SELECT ALL =====================

    @Query("SELECT * FROM crime_history ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getAllRecords();

    // ===================== SELECT BY PREDICTION =====================

    @Query("SELECT * FROM crime_history WHERE prediction = :prediction ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getRecordsByPrediction(String prediction);

    @Query("SELECT * FROM crime_history WHERE prediction = 'VIOLENT' ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getViolentRecords();

    // ===================== SELECT BY DATE =====================

    @Query("SELECT * FROM crime_history WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    List<CrimeRecord> getIncidentsInRange(long startDate, long endDate);

    @Query("SELECT * FROM crime_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY id DESC")
    List<CrimeRecord> getRecordsByDateRangeSync(long startTime, long endTime);

    @Query("SELECT * FROM crime_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getRecordsByDateRange(long startTime, long endTime);

    // ===================== SELECT BY CONFIDENCE =====================

    @Query("SELECT * FROM crime_history WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    LiveData<List<CrimeRecord>> getRecordsByMinConfidence(double minConfidence);

    // ===================== SELECT WITH LOCATION =====================

    @Query("SELECT * FROM crime_history WHERE latitude != 0 AND longitude != 0 ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getRecordsWithLocation();

    // ===================== SELECT BY DETECTION TYPE =====================

    @Query("SELECT * FROM crime_history WHERE detectionType = :type ORDER BY id DESC")
    LiveData<List<CrimeRecord>> getRecordsByType(String type);

    // ===================== COUNT =====================

    @Query("SELECT COUNT(*) FROM crime_history")
    int getCount();

    @Query("SELECT COUNT(*) FROM crime_history WHERE prediction = 'VIOLENT'")
    LiveData<Integer> getViolentCount();

    // ===================== STATISTICS =====================

    @Query("SELECT AVG(confidence) FROM crime_history")
    LiveData<Double> getAverageConfidence();

    @Query("SELECT AVG(confidence) FROM crime_history WHERE prediction = 'VIOLENT'")
    LiveData<Double> getAverageViolentConfidence();

    @Query("SELECT MAX(confidence) FROM crime_history")
    LiveData<Double> getMaxConfidence();

    // ===================== RECENT =====================

    @Query("SELECT * FROM crime_history ORDER BY id DESC LIMIT :limit")
    LiveData<List<CrimeRecord>> getRecentRecords(int limit);
}