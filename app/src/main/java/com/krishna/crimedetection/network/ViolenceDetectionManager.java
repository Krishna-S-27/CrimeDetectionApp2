package com.krishna.crimedetection.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manager class for violence detection video uploads and predictions
 * Handles:
 * - Video file conversion from URI to File
 * - Video upload to FastAPI backend
 * - Server health checks
 * - Server statistics retrieval
 * - Error handling and callbacks
 */
public class ViolenceDetectionManager {

    private static final String TAG = "ViolenceDetectionManager";
    private static final int MAX_VIDEO_SIZE_MB = 500;
    private static final long MAX_VIDEO_SIZE_BYTES = MAX_VIDEO_SIZE_MB * 1024L * 1024L;

    private Context context;
    private String serverURL;
    private ApiService apiService;

    // ===================== CONSTRUCTORS =====================

    /**
     * Initialize ViolenceDetectionManager with server URL
     *
     * @param context Android context
     * @param serverURL Server base URL (e.g., "http://192.168.1.100:8000")
     */
    public ViolenceDetectionManager(Context context, String serverURL) {
        this.context = context;
        this.serverURL = serverURL;
        // Use RetrofitClient to include Authentication headers
        this.apiService = RetrofitClient.getApiService(context);

        Log.i(TAG, "ViolenceDetectionManager initialized with authenticated client");
    }

    // ===================== PUBLIC METHODS =====================

    /**
     * Check if server is healthy and model is loaded
     * Makes a request to /health endpoint
     *
     * @param callback Callback with health check result
     */
    public void checkServerHealth(HealthCheckCallback callback) {
        Log.i(TAG, "🏥 Checking server health...");

        apiService.healthCheck().enqueue(new Callback<HealthCheckResponse>() {
            @Override
            public void onResponse(Call<HealthCheckResponse> call, Response<HealthCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HealthCheckResponse healthCheck = response.body();
                    Log.i(TAG, "✅ Server health: " + healthCheck.getStatusMessage());
                    callback.onHealthy(healthCheck);
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "❌ Server error: " + error);
                    callback.onUnhealthy(error);
                }
            }

            @Override
            public void onFailure(Call<HealthCheckResponse> call, Throwable t) {
                Log.e(TAG, "❌ Connection failed: " + t.getMessage());
                callback.onUnhealthy("Connection failed: " + t.getMessage());
            }
        });
    }

    /**
     * Get server statistics (cache size, model type, etc.)
     * Makes a request to /stats endpoint
     *
     * @param callback Callback with statistics
     */
    public void getServerStats(StatsCallback callback) {
        Log.i(TAG, "📊 Fetching server statistics...");

        apiService.getStats().enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatsResponse stats = response.body();
                    Log.i(TAG, "✅ Stats received: " + stats.getCacheInfo());
                    callback.onStatsReceived(stats);
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "❌ Stats error: " + error);
                    callback.onStatsError(error);
                }
            }

            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {
                Log.e(TAG, "❌ Stats fetch failed: " + t.getMessage());
                callback.onStatsError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Upload video and get violence detection prediction
     * Core method for video analysis
     *
     * @param videoUri URI of video file from storage/camera
     * @param callback Callback with prediction result
     */
    public void predictViolence(Uri videoUri, PredictionCallback callback) {
        Log.i(TAG, "🎬 Starting violence prediction...");
        Log.i(TAG, "Video URI: " + videoUri.toString());

        try {
            // Step 1: Convert URI to File
            File videoFile = getFileFromUri(videoUri);
            if (videoFile == null) {
                Log.e(TAG, "❌ Could not access video file");
                callback.onError("Could not access video file");
                return;
            }

            // Step 2: Validate file size
            long fileSizeBytes = videoFile.length();
            long fileSizeMB = fileSizeBytes / (1024 * 1024);

            Log.i(TAG, "📁 Video file: " + videoFile.getName());
            Log.i(TAG, "📊 File size: " + fileSizeMB + "MB");

            if (fileSizeBytes > MAX_VIDEO_SIZE_BYTES) {
                String error = String.format("File too large: %dMB (max: %dMB)",
                        fileSizeMB, MAX_VIDEO_SIZE_MB);
                Log.e(TAG, "❌ " + error);
                callback.onError(error);
                return;
            }

            // Step 3: Create multipart request
            RequestBody requestBody = RequestBody.create(videoFile, MediaType.parse("video/mp4"));
            MultipartBody.Part videoFilePart = MultipartBody.Part.createFormData(
                    "file",
                    videoFile.getName(),
                    requestBody
            );

            // Step 4: Upload video
            Log.i(TAG, "📤 Uploading video...");
            callback.onUploadProgress(10);

            apiService.uploadVideo(videoFilePart).enqueue(new Callback<PredictionResponse>() {
                @Override
                public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                    callback.onUploadProgress(100);

                    if (response.isSuccessful() && response.body() != null) {
                        PredictionResponse prediction = response.body();

                        if (prediction.getSuccess() != null && prediction.getSuccess()) {
                            Log.i(TAG, "✅ Prediction received");
                            Log.i(TAG, "🎯 Result: " + prediction.getPrediction());
                            Log.i(TAG, "📈 Confidence: " + prediction.getConfidencePercent());
                            Log.i(TAG, "⏱️  Timing: " + prediction.getTimingSummary());

                            callback.onSuccess(prediction);
                        } else {
                            String error = prediction.getError() != null ?
                                    prediction.getError() : "Unknown error";
                            Log.e(TAG, "❌ Server error: " + error);
                            callback.onError(error);
                        }
                    } else {
                        String error = "HTTP " + response.code();
                        Log.e(TAG, "❌ API error: " + error);
                        callback.onError(error);
                    }

                    // Cleanup temp file
                    cleanupTempFile(videoFile);
                }

                @Override
                public void onFailure(Call<PredictionResponse> call, Throwable t) {
                    callback.onUploadProgress(0);
                    Log.e(TAG, "❌ Upload failed: " + t.getMessage());
                    callback.onError("Network error: " + t.getMessage());

                    // Cleanup temp file
                    cleanupTempFile(videoFile);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage());
            e.printStackTrace();
            callback.onError("Error: " + e.getMessage());
        }
    }

    // ===================== PRIVATE HELPER METHODS =====================

    /**
     * Convert content URI to File
     * Handles both gallery videos and camera recordings
     *
     * @param uri Content URI
     * @return File object or null if conversion failed
     */
    private File getFileFromUri(Uri uri) {
        try {
            Log.d(TAG, "Converting URI to File: " + uri.toString());

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream from URI");
                return null;
            }

            // Create temp file in cache directory
            File tempDir = context.getCacheDir();
            File tempFile = new File(tempDir, "video_" + System.currentTimeMillis() + ".mp4");

            // Copy from URI to temp file
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "✓ File created: " + tempFile.getAbsolutePath());
            return tempFile;

        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to File: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete temporary video file
     * Called after upload is complete
     *
     * @param file File to delete
     */
    private void cleanupTempFile(File file) {
        try {
            if (file != null && file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "✓ Temp file deleted: " + file.getName());
                } else {
                    Log.w(TAG, "Could not delete temp file: " + file.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error deleting temp file: " + e.getMessage());
        }
    }

    // ===================== CALLBACKS =====================

    /**
     * Callback for health check results
     */
    public interface HealthCheckCallback {
        /**
         * Server is healthy and model is loaded
         */
        void onHealthy(HealthCheckResponse response);

        /**
         * Server is not healthy or unreachable
         */
        void onUnhealthy(String reason);
    }

    /**
     * Callback for server statistics
     */
    public interface StatsCallback {
        /**
         * Statistics received successfully
         */
        void onStatsReceived(StatsResponse stats);

        /**
         * Error retrieving statistics
         */
        void onStatsError(String error);
    }

    /**
     * Callback for prediction results
     */
    public interface PredictionCallback {
        /**
         * Prediction successful
         *
         * @param response Prediction result with confidence and timing
         */
        void onSuccess(PredictionResponse response);

        /**
         * Prediction failed
         *
         * @param error Error message
         */
        void onError(String error);

        /**
         * Upload progress update
         *
         * @param progress Progress percentage (0-100)
         */
        void onUploadProgress(int progress);
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Update server URL (e.g., when user changes settings)
     * Resets the API client with new URL
     *
     * @param newServerURL New server URL
     */
    public void updateServerURL(String newServerURL) {
        Log.i(TAG, "Updating server URL to " + newServerURL);
        this.serverURL = newServerURL;
        // RetrofitClient will pick up the new URL from PreferenceUtils
        this.apiService = RetrofitClient.getApiService(context);
    }

    /**
     * Get current server URL
     *
     * @return Server URL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * Check if prediction indicates violence
     * Utility method for checking result
     *
     * @param prediction Prediction string
     * @return true if VIOLENT, false if NONVIOLENT
     */
    public static boolean isViolent(String prediction) {
        return prediction != null && prediction.equalsIgnoreCase("VIOLENT");
    }

    /**
     * Get confidence as percentage string
     * Utility method for displaying confidence
     *
     * @param confidence Confidence value (0.0-1.0)
     * @return Formatted percentage string
     */
    public static String formatConfidence(Double confidence) {
        if (confidence == null) return "0%";
        return String.format("%.1f%%", confidence * 100);
    }
}