package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krishna.crimedetection.models.CrimeRecord;
import com.krishna.crimedetection.repository.CrimeRepository;
import com.krishna.crimedetection.utils.PdfReportGenerator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportViewModel extends AndroidViewModel {
    private final CrimeRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<CrimeRecord>> incidents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);
    private final MutableLiveData<File> reportFile = new MutableLiveData<>();
    private final MutableLiveData<String> reportSize = new MutableLiveData<>();
    private final MutableLiveData<String> generatedTime = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private final MutableLiveData<Integer> violentCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> nonViolentCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> averageConfidence = new MutableLiveData<>(0.0);

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new CrimeRepository(application);
    }

    public LiveData<List<CrimeRecord>> getIncidents() { return incidents; }
    public LiveData<Boolean> getIsGenerating() { return isGenerating; }
    public LiveData<File> getReportFile() { return reportFile; }
    public LiveData<String> getReportSize() { return reportSize; }
    public LiveData<String> getGeneratedTime() { return generatedTime; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getViolentCount() { return violentCount; }
    public LiveData<Integer> getNonViolentCount() { return nonViolentCount; }
    public LiveData<Double> getAverageConfidence() { return averageConfidence; }

    public void generateDailyReport() {
        long end = System.currentTimeMillis();
        long start = getStartOfDay(end);
        generateReportInternal(start, end, "Daily");
    }

    public void generateWeeklyReport() {
        long end = System.currentTimeMillis();
        long start = end - (7 * 24 * 60 * 60 * 1000L);
        generateReportInternal(start, end, "Weekly");
    }

    public void generateMonthlyReport() {
        long end = System.currentTimeMillis();
        long start = end - (30 * 24 * 60 * 60 * 1000L);
        generateReportInternal(start, end, "Monthly");
    }

    public void generateCustomReport(long fromDate, long toDate) {
        generateReportInternal(fromDate, toDate, "Custom");
    }

    private void generateReportInternal(long start, long end, String type) {
        isGenerating.postValue(true);
        executor.execute(() -> {
            try {
                List<CrimeRecord> records = repository.getIncidentsForDateRange(start, end);
                if (records == null || records.isEmpty()) {
                    errorMessage.postValue("No incidents found for the selected period.");
                    isGenerating.postValue(false);
                    return;
                }

                // Calculate Statistics
                int vCount = 0;
                double totalConf = 0;
                for (CrimeRecord r : records) {
                    if (r.isViolent()) vCount++;
                    totalConf += r.getConfidence();
                }

                int nvCount = records.size() - vCount;
                double avgConf = totalConf / records.size();

                // Post results to UI
                incidents.postValue(records);
                violentCount.postValue(vCount);
                nonViolentCount.postValue(nvCount);
                averageConfidence.postValue(avgConf);

                // Generate PDF
                String dateRangeStr = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(start)) + 
                        " - " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(end));
                
                File file = PdfReportGenerator.generateCustomReport(getApplication(), records, type, dateRangeStr);

                if (file != null && file.exists()) {
                    reportFile.postValue(file);
                    reportSize.postValue(String.format(Locale.getDefault(), "%.1f KB", file.length() / 1024.0));
                    generatedTime.postValue(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
                } else {
                    errorMessage.postValue("Failed to generate PDF report.");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error: " + e.getMessage());
            } finally {
                isGenerating.postValue(false);
            }
        });
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
}
