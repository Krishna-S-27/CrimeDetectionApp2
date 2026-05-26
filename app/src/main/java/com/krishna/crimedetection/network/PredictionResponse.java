package com.krishna.crimedetection.network;

import com.google.gson.annotations.SerializedName;
import java.util.Locale;

public class PredictionResponse {
    @SerializedName("success")
    private Boolean success;

    @SerializedName("prediction")
    private String prediction;

    @SerializedName("confidence")
    private Double confidence;

    @SerializedName("inference_time_ms")
    private Double inferenceTimeMs;

    @SerializedName("preprocessing_time_ms")
    private Double preprocessingTimeMs;

    @SerializedName("total_time_ms")
    private Double totalTimeMs;

    @SerializedName("cached")
    private Boolean cached;

    @SerializedName("filename")
    private String filename;

    @SerializedName("error")
    private String error;

    @SerializedName("alert_message")
    private String alertMessage;

    @SerializedName("share_whatsapp_video")
    private Boolean shareWhatsappVideo;

    @SerializedName("local_save_folder")
    private String localSaveFolder;

    @SerializedName("frame_count")
    private Integer frameCount;

    @SerializedName("fps")
    private Double fps;

    @SerializedName("buffer_count")
    private Integer bufferCount;

    @SerializedName("detection_status")
    private String detectionStatus;

    public Boolean getSuccess() { return success; }
    public String getPrediction() { return prediction; }
    public Double getConfidence() { return confidence; }
    public Double getInferenceTimeMs() { return inferenceTimeMs; }
    public Double getPreprocessingTimeMs() { return preprocessingTimeMs; }
    public Double getTotalTimeMs() { return totalTimeMs; }
    public Boolean getCached() { return cached; }
    public String getFilename() { return filename; }
    public String getError() { return error; }
    public String getAlertMessage() { return alertMessage; }
    public Boolean getShareWhatsappVideo() { return shareWhatsappVideo; }
    public String getLocalSaveFolder() { return localSaveFolder; }
    public Integer getFrameCount() { return frameCount; }
    public Double getFps() { return fps; }
    public Integer getBufferCount() { return bufferCount; }
    public String getDetectionStatus() { return detectionStatus; }

    public void setSuccess(Boolean success) { this.success = success; }
    public void setPrediction(String prediction) { this.prediction = prediction; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public void setInferenceTimeMs(Double inferenceTimeMs) { this.inferenceTimeMs = inferenceTimeMs; }
    public void setPreprocessingTimeMs(Double preprocessingTimeMs) { this.preprocessingTimeMs = preprocessingTimeMs; }
    public void setTotalTimeMs(Double totalTimeMs) { this.totalTimeMs = totalTimeMs; }
    public void setCached(Boolean cached) { this.cached = cached; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setError(String error) { this.error = error; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }
    public void setShareWhatsappVideo(Boolean shareWhatsappVideo) { this.shareWhatsappVideo = shareWhatsappVideo; }
    public void setLocalSaveFolder(String localSaveFolder) { this.localSaveFolder = localSaveFolder; }
    public void setFrameCount(Integer frameCount) { this.frameCount = frameCount; }
    public void setFps(Double fps) { this.fps = fps; }
    public void setBufferCount(Integer bufferCount) { this.bufferCount = bufferCount; }
    public void setDetectionStatus(String detectionStatus) { this.detectionStatus = detectionStatus; }

    public boolean isViolent() {
        return prediction != null && prediction.equalsIgnoreCase("VIOLENT");
    }

    public String getConfidencePercent() {
        if (confidence == null) return "0%";
        return String.format(Locale.getDefault(), "%.1f%%", confidence * 100);
    }

    public String getTimingSummary() {
        return String.format(Locale.getDefault(), "Preprocessing: %.0fms | Inference: %.0fms | Total: %.0fms",
                preprocessingTimeMs != null ? preprocessingTimeMs : 0,
                inferenceTimeMs != null ? inferenceTimeMs : 0,
                totalTimeMs != null ? totalTimeMs : 0);
    }
}