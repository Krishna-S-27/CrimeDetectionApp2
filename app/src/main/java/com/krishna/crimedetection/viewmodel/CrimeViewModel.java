package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krishna.crimedetection.models.CrimeRecord;
import com.krishna.crimedetection.network.HealthCheckResponse;
import com.krishna.crimedetection.network.NetworkStateManager;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.network.StatsResponse;
import com.krishna.crimedetection.network.ViolenceDetectionManager;
import com.krishna.crimedetection.repository.CrimeRepository;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.TimeUtils;

import java.util.List;
import java.util.Locale;

/**
 * ViewModel for managing crime/violence detection UI state
 *
 * Responsibilities:
 * - Manage LiveData for UI updates
 * - Handle video upload and prediction
 * - Check server health
 * - Store predictions in database
 * - Handle errors gracefully
 */
public class CrimeViewModel extends AndroidViewModel {

    private static final String TAG = "CrimeViewModel";

    // ===================== PRIVATE FIELDS =====================

    private Context context;
    private CrimeRepository crimeRepository;
    private ViolenceDetectionManager detectionManager;
    private NetworkStateManager networkStateManager;

    // ===================== LIVEDATA - UI STATE =====================

    // Server connectivity
    private MutableLiveData<Boolean> isServerConnected;
    private MutableLiveData<String> serverStatusMessage;
    private MutableLiveData<HealthCheckResponse> serverHealth;

    // Video upload progress
    private MutableLiveData<Integer> uploadProgress;
    private MutableLiveData<String> uploadStatusMessage;

    // Prediction results
    private MutableLiveData<PredictionResponse> predictionResult;
    private MutableLiveData<String> predictionError;
    private MutableLiveData<Boolean> isPredicting;

    // Server statistics
    private MutableLiveData<StatsResponse> serverStats;

    // Network status
    private MutableLiveData<Boolean> isNetworkAvailable;
    private MutableLiveData<NetworkStateManager.NetworkType> networkType;

    // ===================== CONSTRUCTOR =====================

    /**
     * Initialize CrimeViewModel with application context
     *
     * @param application Application context
     */
    public CrimeViewModel(@NonNull Application application) {
        super(application);
        this.context = application.getApplicationContext();

        // Initialize managers and repository
        this.crimeRepository = new CrimeRepository(application);
        this.networkStateManager = new NetworkStateManager(context);

        // Initialize LiveData
        this.isServerConnected = new MutableLiveData<>(false);
        this.serverStatusMessage = new MutableLiveData<>("Not connected");
        this.serverHealth = new MutableLiveData<>();
        this.uploadProgress = new MutableLiveData<>(0);
        this.uploadStatusMessage = new MutableLiveData<>("Ready");
        this.predictionResult = new MutableLiveData<>();
        this.predictionError = new MutableLiveData<>();
        this.isPredicting = new MutableLiveData<>(false);
        this.serverStats = new MutableLiveData<>();
        this.isNetworkAvailable = new MutableLiveData<>(false);
        this.networkType = new MutableLiveData<>(NetworkStateManager.NetworkType.NONE);

        Log.i(TAG, "CrimeViewModel initialized");

        // Initialize detection manager with saved server URL
        initializeDetectionManager();

        // Update network status
        updateNetworkStatus();
    }

    // ===================== INITIALIZATION =====================

    /**
     * Initialize ViolenceDetectionManager with server URL from preferences
     * Creates new manager or reuses existing one based on URL
     */
    private void initializeDetectionManager() {
        String serverURL = PreferenceUtils.getServerURL(context);
        Log.i(TAG, "Initializing detection manager with URL: " + serverURL);

        if (detectionManager == null) {
            detectionManager = new ViolenceDetectionManager(context, serverURL);
        } else {
            detectionManager.updateServerURL(serverURL);
        }
    }

    /**
     * Update network connectivity status
     * Checks if device is online and what type of connection
     */
    private void updateNetworkStatus() {
        boolean isConnected = networkStateManager.isNetworkConnected();
        NetworkStateManager.NetworkType type = networkStateManager.getNetworkType();

        isNetworkAvailable.postValue(isConnected);
        networkType.postValue(type);

        Log.d(TAG, "Network status: " + (isConnected ? "Connected (" + type.displayName + ")" : "Disconnected"));
    }

    // ===================== PUBLIC METHODS - SERVER CONNECTIVITY =====================

    /**
     * Check if server is reachable and model is loaded
     * Updates serverHealth and isServerConnected LiveData
     */
    public void checkServerConnection() {
        updateNetworkStatus();

        if (!networkStateManager.isNetworkConnected()) {
            serverStatusMessage.postValue("❌ No network connection");
            isServerConnected.postValue(false);
            Log.w(TAG, "Cannot check server: no network");
            return;
        }

        serverStatusMessage.postValue("🔄 Checking server...");
        Log.i(TAG, "Checking server connection...");

        detectionManager.checkServerHealth(new ViolenceDetectionManager.HealthCheckCallback() {
            @Override
            public void onHealthy(HealthCheckResponse response) {
                Log.i(TAG, "✅ Server is healthy: " + response.getStatusMessage());
                serverHealth.postValue(response);
                serverStatusMessage.postValue("✅ " + response.getStatusMessage());
                isServerConnected.postValue(true);

                // Fetch stats when server is healthy
                fetchServerStats();
            }

            @Override
            public void onUnhealthy(String reason) {
                Log.e(TAG, "❌ Server is unhealthy: " + reason);
                serverStatusMessage.postValue("❌ " + reason);
                isServerConnected.postValue(false);
                predictionError.postValue(reason);
            }
        });
    }

    /**
     * Fetch server statistics
     * Updates serverStats LiveData with cache info, model type, etc.
     */
    public void fetchServerStats() {
        if (isServerConnected.getValue() == null || !isServerConnected.getValue()) {
            Log.w(TAG, "Cannot fetch stats: server not connected");
            return;
        }

        Log.i(TAG, "Fetching server statistics...");
        detectionManager.getServerStats(new ViolenceDetectionManager.StatsCallback() {
            @Override
            public void onStatsReceived(StatsResponse stats) {
                Log.i(TAG, "✅ Stats received: " + stats.getCacheInfo());
                serverStats.postValue(stats);
            }

            @Override
            public void onStatsError(String error) {
                Log.e(TAG, "❌ Stats error: " + error);
                predictionError.postValue("Could not fetch stats: " + error);
            }
        });
    }

    /**
     * Update server URL (e.g., when user changes settings)
     * Reinitializes detection manager with new URL
     *
     * @param newServerURL New server URL
     */
    public void updateServerURL(String newServerURL) {
        Log.i(TAG, "Updating server URL to: " + newServerURL);

        // Save to preferences
        PreferenceUtils.saveServerURL(context, newServerURL);

        // Reinitialize manager
        initializeDetectionManager();

        // Check connection with new URL
        checkServerConnection();
    }

    // ===================== PUBLIC METHODS - PREDICTIONS =====================

    /**
     * Upload video and predict violence
     * Core method for video analysis
     *
     * Updates UI with progress and results via LiveData
     *
     * @param videoUri URI of video file
     */
    public void predictViolenceFromVideo(Uri videoUri) {
        Log.i(TAG, "🎬 Starting violence prediction from video: " + videoUri);

        // Check network
        updateNetworkStatus();
        if (!networkStateManager.isNetworkConnected()) {
            predictionError.postValue("No network connection");
            Log.w(TAG, "Cannot predict: no network");
            return;
        }

        // Check server
        if (isServerConnected.getValue() == null || !isServerConnected.getValue()) {
            predictionError.postValue("Server not connected. Please check server connection first.");
            Log.w(TAG, "Cannot predict: server not connected");
            return;
        }

        // Start prediction
        isPredicting.postValue(true);
        uploadProgress.postValue(0);
        uploadStatusMessage.postValue("Preparing video...");
        predictionError.postValue(null);

        detectionManager.predictViolence(videoUri, new ViolenceDetectionManager.PredictionCallback() {
            @Override
            public void onSuccess(PredictionResponse response) {
                Log.i(TAG, "✅ Prediction received: " + response.getPrediction());

                isPredicting.postValue(false);
                uploadProgress.postValue(100);
                uploadStatusMessage.postValue("Analysis complete");
                predictionResult.postValue(response);
                predictionError.postValue(null);

                // Save to database
                savePredictionToDatabase(response);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Prediction error: " + error);

                isPredicting.postValue(false);
                uploadProgress.postValue(0);
                uploadStatusMessage.postValue("Error");
                predictionError.postValue(error);
            }

            @Override
            public void onUploadProgress(int progress) {
                Log.d(TAG, "Upload progress: " + progress + "%");
                uploadProgress.postValue(progress);
                uploadStatusMessage.postValue("Uploading: " + progress + "%");
            }
        });
    }

    /**
     * Save prediction result to local database
     * Called after successful prediction
     *
     * @param response Prediction response from server
     */
    private void savePredictionToDatabase(PredictionResponse response) {
        try {
            Log.i(TAG, "Saving prediction to database...");

            long timestamp = System.currentTimeMillis();

            // Create crime record with all available data
            CrimeRecord crimeRecord = new CrimeRecord(
                    response.getPrediction(),
                    response.getConfidence() != null ? response.getConfidence() : 0.0,
                    timestamp,
                    response.getFilename() != null ? response.getFilename() : "unknown",
                    0.0, // Latitude (will be set by MainActivity if available)
                    0.0  // Longitude (will be set by MainActivity if available)
            );

            // Set timing information
            if (response.getInferenceTimeMs() != null) {
                crimeRecord.setInferenceTimeMs(response.getInferenceTimeMs());
            }
            if (response.getPreprocessingTimeMs() != null) {
                crimeRecord.setPreprocessingTimeMs(response.getPreprocessingTimeMs());
            }

            // Insert into database asynchronously
            crimeRepository.insertCrimeRecord(crimeRecord);

            Log.i(TAG, "✓ Prediction saved to database");
        } catch (Exception e) {
            Log.e(TAG, "Error saving prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear prediction result
     * Called when user dismisses result UI
     */
    public void clearPredictionResult() {
        Log.d(TAG, "Clearing prediction result");
        predictionResult.postValue(null);
        uploadProgress.postValue(0);
        uploadStatusMessage.postValue("Ready");
    }

    /**
     * Process prediction response and prepare for UI update
     * This is a helper method that can be called from MainActivity
     *
     * @param response PredictionResponse from backend
     * @return Formatted string for UI display
     */
    public String processPredictionResponse(PredictionResponse response) {
        if (response == null) {
            return "No response";
        }

        // Save to database
        savePredictionToDatabase(response);

        // Return formatted string for UI
        return String.format(Locale.getDefault(), "Prediction: %s | Confidence: %.1f%% | Time: %.0fms",
                response.getPrediction(),
                response.getConfidence() != null ? response.getConfidence() * 100 : 0,
                response.getTotalTimeMs() != null ? response.getTotalTimeMs() : 0);
    }

    // ===================== LIVEDATA GETTERS =====================

    /**
     * @return LiveData for server connection status
     */
    public LiveData<Boolean> getIsServerConnected() {
        return isServerConnected;
    }

    /**
     * @return LiveData for server status message
     */
    public LiveData<String> getServerStatusMessage() {
        return serverStatusMessage;
    }

    /**
     * @return LiveData for server health response
     */
    public LiveData<HealthCheckResponse> getServerHealth() {
        return serverHealth;
    }

    /**
     * @return LiveData for upload progress (0-100)
     */
    public LiveData<Integer> getUploadProgress() {
        return uploadProgress;
    }

    /**
     * @return LiveData for upload status message
     */
    public LiveData<String> getUploadStatusMessage() {
        return uploadStatusMessage;
    }

    /**
     * @return LiveData for prediction result
     */
    public LiveData<PredictionResponse> getPredictionResult() {
        return predictionResult;
    }

    /**
     * @return LiveData for prediction error
     */
    public LiveData<String> getPredictionError() {
        return predictionError;
    }

    /**
     * @return LiveData for is predicting state (loading)
     */
    public LiveData<Boolean> getIsPredicting() {
        return isPredicting;
    }

    /**
     * @return LiveData for server statistics
     */
    public LiveData<StatsResponse> getServerStats() {
        return serverStats;
    }

    /**
     * @return LiveData for network availability
     */
    public LiveData<Boolean> getIsNetworkAvailable() {
        return isNetworkAvailable;
    }

    /**
     * @return LiveData for network type
     */
    public LiveData<NetworkStateManager.NetworkType> getNetworkType() {
        return networkType;
    }

    // ===================== DATABASE OPERATIONS =====================

    /**
     * Get all crime records from database
     *
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getAllCrimeRecords() {
        return crimeRepository.getAllCrimeRecords();
    }

    /**
     * Get crime records by type (VIOLENCE, THEFT, etc.)
     *
     * @param detectionType Type of detection
     * @return LiveData<List<CrimeRecord>>
     */
    public LiveData<List<CrimeRecord>> getCrimeRecordsByType(String detectionType) {
        return crimeRepository.getCrimeRecordsByType(detectionType);
    }

    /**
     * Insert a crime record
     *
     * @param crimeRecord Record to insert
     */
    public void insertCrimeRecord(CrimeRecord crimeRecord) {
        crimeRepository.insertCrimeRecord(crimeRecord);
    }

    /**
     * Delete a crime record
     *
     * @param crimeRecord Record to delete
     */
    public void deleteCrimeRecord(CrimeRecord crimeRecord) {
        crimeRepository.deleteCrimeRecord(crimeRecord);
    }

    /**
     * Clear all crime records
     */
    public void clearAllCrimeRecords() {
        crimeRepository.clearAllCrimeRecords();
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Check if currently predicting
     *
     * @return true if prediction in progress
     */
    public boolean isPredicting() {
        return isPredicting.getValue() != null && isPredicting.getValue();
    }

    /**
     * Check if server is connected
     *
     * @return true if server is reachable
     */
    public boolean isServerConnected() {
        return isServerConnected.getValue() != null && isServerConnected.getValue();
    }

    /**
     * Check if network is available
     *
     * @return true if device has network connection
     */
    public boolean isNetworkAvailable() {
        return isNetworkAvailable.getValue() != null && isNetworkAvailable.getValue();
    }
}