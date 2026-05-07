package com.krishna.crimedetection.network;

import com.google.gson.annotations.SerializedName;

/**
 * Response model from FastAPI backend /health endpoint
 * Used to verify server is running and model is loaded
 */
public class HealthCheckResponse {

    @SerializedName("status")
    private String status;  // "healthy" or error

    @SerializedName("model_loaded")
    private Boolean modelLoaded;  // Is the ML model loaded?

    @SerializedName("model_type")
    private String modelType;  // "h5", "tflite", "savedmodel"

    @SerializedName("api_version")
    private String apiVersion;  // Backend version

    @SerializedName("cache_size")
    private Integer cacheSize;  // Current cache entries

    @SerializedName("cache_max")
    private Integer cacheMax;  // Max cache size

    // ===================== GETTERS =====================

    public String getStatus() {
        return status;
    }

    public Boolean getModelLoaded() {
        return modelLoaded;
    }

    public String getModelType() {
        return modelType;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public Integer getCacheMax() {
        return cacheMax;
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Check if server is healthy
     */
    public boolean isHealthy() {
        return status != null && status.equalsIgnoreCase("healthy") && modelLoaded != null && modelLoaded;
    }

    /**
     * Get status message for UI
     */
    public String getStatusMessage() {
        if (isHealthy()) {
            return "✅ Server Healthy | Model: " + modelType + " | v" + apiVersion;
        } else {
            return "❌ Server Unhealthy | Model Loaded: " + (modelLoaded != null ? modelLoaded : false);
        }
    }
}