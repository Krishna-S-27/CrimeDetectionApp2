package com.krishna.crimedetection.network;

import com.google.gson.annotations.SerializedName;

/**
 * Response model from FastAPI backend /stats endpoint
 * Get server statistics and performance info
 */
public class StatsResponse {

    @SerializedName("cache_entries")
    private Integer cacheEntries;  // Current cached videos

    @SerializedName("cache_max_size")
    private Integer cacheMaxSize;  // Maximum cache size

    @SerializedName("model_type")
    private String modelType;  // "h5", "tflite", etc.

    @SerializedName("inference_queue_size")
    private Integer inferenceQueueSize;  // Max concurrent inferences

    @SerializedName("max_video_size_mb")
    private Integer maxVideoSizeMb;  // Max video size allowed

    // ===================== GETTERS =====================

    public Integer getCacheEntries() {
        return cacheEntries;
    }

    public Integer getCacheMaxSize() {
        return cacheMaxSize;
    }

    public String getModelType() {
        return modelType;
    }

    public Integer getInferenceQueueSize() {
        return inferenceQueueSize;
    }

    public Integer getMaxVideoSizeMb() {
        return maxVideoSizeMb;
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Get cache usage percentage
     */
    public int getCacheUsagePercent() {
        if (cacheMaxSize == null || cacheMaxSize == 0) return 0;
        return (cacheEntries != null ? cacheEntries : 0) * 100 / cacheMaxSize;
    }

    /**
     * Get cache info string
     */
    public String getCacheInfo() {
        return String.format("Cache: %d/%d (%d%%)",
                cacheEntries != null ? cacheEntries : 0,
                cacheMaxSize != null ? cacheMaxSize : 0,
                getCacheUsagePercent());
    }
}