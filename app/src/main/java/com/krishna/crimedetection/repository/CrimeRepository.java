package com.krishna.crimedetection.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.CrimeDao;
import com.krishna.crimedetection.models.CrimeRecord;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for handling local database operations.
 *
 * Handles:
 * - Inserting crime records
 * - Querying records
 * - Deleting records
 * - Real-time and batch predictions
 */
public class CrimeRepository {

    private static final String TAG = "CrimeRepository";

    private final CrimeDao crimeDao;
    private final ExecutorService executorService;
    private final Context context;

    /**
     * Initialize repository with application context
     * Creates thread pool for background database operations
     */
    public CrimeRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        crimeDao = db.crimeDao();
        executorService = Executors.newFixedThreadPool(2);
        context = application.getApplicationContext();

        Log.i(TAG, "CrimeRepository initialized");
    }

    /**
     * Insert a single crime record
     * Runs asynchronously on background thread
     *
     * @param record Crime record to insert
     */
    public void insertCrimeRecord(CrimeRecord record) {
        executorService.execute(() -> {
            try {
                long id = crimeDao.insert(record);
                Log.i(TAG, "✓ Crime record inserted: ID=" + id + ", Prediction=" + record.getPrediction());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting crime record: " + e.getMessage());
            }
        });
    }

    /**
     * Insert multiple crime records (batch)
     * Useful for batch operations
     *
     * @param records List of crime records
     */
    public void insertCrimeRecords(List<CrimeRecord> records) {
        executorService.execute(() -> {
            try {
                for (CrimeRecord record : records) {
                    crimeDao.insert(record);
                }
                Log.i(TAG, "✓ " + records.size() + " records inserted");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting records: " + e.getMessage());
            }
        });
    }

    /**
     * Get all crime records from database
     * Returns LiveData for real-time updates
     *
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getAllCrimeRecords() {
        Log.d(TAG, "Fetching all crime records");
        return crimeDao.getAllRecords();
    }

    /**
     * Get crime records by prediction type
     *
     * @param prediction "VIOLENT" or "NONVIOLENT"
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getCrimeRecordsByPrediction(String prediction) {
        Log.d(TAG, "Fetching records by prediction: " + prediction);
        return crimeDao.getRecordsByPrediction(prediction);
    }

    /**
     * Get only VIOLENT records (for alerts)
     *
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getViolentRecords() {
        Log.d(TAG, "Fetching violent records");
        return crimeDao.getRecordsByPrediction("VIOLENT");
    }

    /**
     * Get records from a specific date range (Synchronous for reports)
     *
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return List<CrimeRecord>
     */
    public List<CrimeRecord> getRecordsByDateRangeSync(long startTime, long endTime) {
        Log.d(TAG, "Fetching records from " + startTime + " to " + endTime + " synchronously");
        return crimeDao.getRecordsByDateRangeSync(startTime, endTime);
    }

    public List<CrimeRecord> getIncidentsForDateRange(long fromDate, long toDate) {
        return crimeDao.getIncidentsInRange(fromDate, toDate);
    }

    public List<CrimeRecord> getIncidentsForToday() {
        long start = getStartOfDay(System.currentTimeMillis());
        long end = System.currentTimeMillis();
        return getIncidentsForDateRange(start, end);
    }

    public List<CrimeRecord> getIncidentsForWeek() {
        long end = System.currentTimeMillis();
        long start = end - (7 * 24 * 60 * 60 * 1000L);
        return getIncidentsForDateRange(start, end);
    }

    public List<CrimeRecord> getIncidentsForMonth() {
        long end = System.currentTimeMillis();
        long start = end - (30 * 24 * 60 * 60 * 1000L);
        return getIncidentsForDateRange(start, end);
    }

    private long getStartOfDay(long time) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public int getViolentIncidentCount(List<CrimeRecord> incidents) {
        int count = 0;
        for (CrimeRecord record : incidents) {
            if (record.isViolent()) count++;
        }
        return count;
    }

    public int getNonViolentIncidentCount(List<CrimeRecord> incidents) {
        int count = 0;
        for (CrimeRecord record : incidents) {
            if (!record.isViolent()) count++;
        }
        return count;
    }

    public double getAverageConfidence(List<CrimeRecord> incidents) {
        if (incidents.isEmpty()) return 0;
        double sum = 0;
        for (CrimeRecord record : incidents) {
            sum += record.getConfidence();
        }
        return sum / incidents.size();
    }

    public double getDetectionAccuracyRate(List<CrimeRecord> incidents) {
        if (incidents.isEmpty()) return 0;
        int highConfidenceCount = 0;
        for (CrimeRecord record : incidents) {
            if (record.getConfidence() >= 0.7) highConfidenceCount++;
        }
        return (double) highConfidenceCount / incidents.size();
    }

    /**
     * Get records from last N hours
     *
     * @param hours Number of hours
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getRecordsFromLastHours(int hours) {
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (hours * 60 * 60 * 1000L);

        Log.d(TAG, "Fetching records from last " + hours + " hours");
        return crimeDao.getRecordsByDateRange(startTime, currentTime);
    }

    /**
     * Get high confidence predictions (>= 0.7)
     *
     * @param minConfidence Minimum confidence threshold
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getHighConfidencePredictions(double minConfidence) {
        Log.d(TAG, "Fetching records with confidence >= " + minConfidence);
        return crimeDao.getRecordsByMinConfidence(minConfidence);
    }

    /**
     * Get records with location data
     * Used for mapping dangerous areas
     *
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getRecordsWithLocation() {
        Log.d(TAG, "Fetching records with location");
        return crimeDao.getRecordsWithLocation();
    }

    /**
     * Get records by detection type (batch or realtime)
     *
     * @param detectionType "batch" or "realtime"
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getCrimeRecordsByType(String detectionType) {
        Log.d(TAG, "Fetching records by type: " + detectionType);
        return crimeDao.getRecordsByType(detectionType);
    }

    /**
     * Delete a single crime record
     *
     * @param record Record to delete
     */
    public void deleteCrimeRecord(CrimeRecord record) {
        executorService.execute(() -> {
            try {
                crimeDao.delete(record);
                Log.i(TAG, "✓ Record deleted: ID=" + record.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error deleting record: " + e.getMessage());
            }
        });
    }

    /**
     * Delete record by ID
     *
     * @param recordId ID of record to delete
     */
    public void deleteCrimeRecordById(int recordId) {
        executorService.execute(() -> {
            try {
                crimeDao.deleteById(recordId);
                Log.i(TAG, "✓ Record deleted: ID=" + recordId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting record: " + e.getMessage());
            }
        });
    }

    /**
     * Delete records older than specified days
     * Used for cleanup/retention policy
     *
     * @param days Number of days to keep
     */
    public void deleteOldRecords(int days) {
        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);

        executorService.execute(() -> {
            try {
                int count = crimeDao.deleteOlderThan(cutoffTime);
                Log.i(TAG, "✓ Deleted " + count + " records older than " + days + " days");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old records: " + e.getMessage());
            }
        });
    }

    /**
     * Clear all records
     * Use with caution!
     */
    public void clearAllCrimeRecords() {
        executorService.execute(() -> {
            try {
                int count = crimeDao.deleteAll();
                Log.w(TAG, "⚠️ Cleared " + count + " records from database");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing records: " + e.getMessage());
            }
        });
    }

    /**
     * Get total count of records
     * Synchronous - use sparingly
     *
     * @return Total record count
     */
    public int getRecordCount() {
        try {
            return crimeDao.getCount();
        } catch (Exception e) {
            Log.e(TAG, "Error getting count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get statistics for dashboard
     *
     * @return Statistics object
     */
    public LiveData<Integer> getViolentCount() {
        return crimeDao.getViolentCount();
    }

    /**
     * Get average confidence score
     *
     * @return Average confidence
     */
    public LiveData<Double> getAverageConfidence() {
        return crimeDao.getAverageConfidence();
    }

    /**
     * Shutdown executor service
     * Call on app shutdown
     */
    public void shutdown() {
        executorService.shutdown();
        Log.i(TAG, "CrimeRepository shutdown");
    }
}