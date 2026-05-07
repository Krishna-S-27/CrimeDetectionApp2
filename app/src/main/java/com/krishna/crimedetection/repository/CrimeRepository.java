package com.krishna.crimedetection.repository;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.CrimeDao;
import com.krishna.crimedetection.models.CrimeRecord;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for handling local database operations.
 * Networking is handled by ViolenceDetectionManager via ViewModel.
 */
public class CrimeRepository {
    private final CrimeDao crimeDao;
    private final ExecutorService executorService;

    public CrimeRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        crimeDao = db.crimeDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertCrimeRecord(CrimeRecord record) {
        executorService.execute(() -> crimeDao.insert(record));
    }

    public LiveData<List<CrimeRecord>> getAllCrimeRecords() {
        return crimeDao.getAllRecords();
    }

    public LiveData<List<CrimeRecord>> getCrimeRecordsByType(String type) {
        // Fallback or implementation if needed, for now returning all
        return crimeDao.getAllRecords();
    }

    public void deleteCrimeRecord(CrimeRecord record) {
        // Implementation if needed
    }

    public void clearAllCrimeRecords() {
        // Implementation if needed
    }
}
