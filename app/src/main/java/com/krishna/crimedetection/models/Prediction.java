package com.krishna.crimedetection.models;

import java.util.Locale;

public class Prediction {
    public float violenceConfidence;
    public float nonViolenceConfidence;
    public String prediction;
    public long timestamp;
    public int frameIndex;
    public String alertMessage;
    public Boolean shareWhatsappVideo;
    public String localSaveFolder;
    public Integer frameCount;
    public Double fps;
    public Integer bufferCount;
    public String detectionStatus;

    public Prediction(float violenceConfidence, float nonViolenceConfidence, String prediction, long timestamp) {
        this.violenceConfidence = violenceConfidence;
        this.nonViolenceConfidence = nonViolenceConfidence;
        this.prediction = prediction;
        this.timestamp = timestamp;
    }

    public boolean isViolent() {
        return "VIOLENT".equalsIgnoreCase(prediction);
    }

    public String getConfidencePercent() {
        return String.format(Locale.getDefault(), "%.1f%%", violenceConfidence * 100);
    }

    public float getConfidence() {
        return violenceConfidence;
    }
}
