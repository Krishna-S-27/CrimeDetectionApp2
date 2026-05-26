package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krishna.crimedetection.models.CrimeRecord;
import com.krishna.crimedetection.models.Prediction;
import com.krishna.crimedetection.repository.CrimeRepository;
import com.krishna.crimedetection.utils.TimeUtils;

public class RealtimeViewModel extends AndroidViewModel {

    private final CrimeRepository repository;

    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentPrediction = new MutableLiveData<>("INITIALIZING");
    private final MutableLiveData<Float> currentConfidence = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> totalFrames = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> violentDetections = new MutableLiveData<>(0);
    private final MutableLiveData<Long> sessionDuration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> alertVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Float> backendFps = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> backendBufferCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> detectionStatus = new MutableLiveData<>("IDLE");
    private final MutableLiveData<Boolean> isServerConnected = new MutableLiveData<>(true);

    private Prediction lastViolentPrediction;

    private long startTime;

    public RealtimeViewModel(@NonNull Application application) {
        super(application);
        repository = new CrimeRepository(application);
    }

    public LiveData<Boolean> getIsRecording() { return isRecording; }
    public LiveData<String> getCurrentPrediction() { return currentPrediction; }
    public LiveData<Float> getCurrentConfidence() { return currentConfidence; }
    public LiveData<Integer> getTotalFrames() { return totalFrames; }
    public LiveData<Integer> getViolentDetections() { return violentDetections; }
    public LiveData<Long> getSessionDuration() { return sessionDuration; }
    public LiveData<Boolean> getAlertVisible() { return alertVisible; }
    public LiveData<Float> getBackendFps() { return backendFps; }
    public LiveData<Integer> getBackendBufferCount() { return backendBufferCount; }
    public LiveData<String> getDetectionStatus() { return detectionStatus; }
    public LiveData<Boolean> getIsServerConnected() { return isServerConnected; }

    public void startSession() {
        startTime = SystemClock.elapsedRealtime();
        setRecording(true);
        totalFrames.setValue(0);
        violentDetections.setValue(0);
        updateDuration();
    }

    public void setRecording(boolean recording) {
        isRecording.setValue(recording);
    }

    public void stopSession() {
        setRecording(false);
    }

    public void updateFrameCount(int count) {
        totalFrames.postValue(count);
    }

    public void updatePrediction(Prediction prediction, double inferenceTimeMs) {
        currentPrediction.postValue(prediction.prediction);
        currentConfidence.postValue(prediction.violenceConfidence);
        
        if (prediction.frameCount != null) {
            totalFrames.postValue(prediction.frameCount);
        }
        if (prediction.fps != null) {
            backendFps.postValue(prediction.fps.floatValue());
        }
        if (prediction.bufferCount != null) {
            backendBufferCount.postValue(prediction.bufferCount);
        }
        if (prediction.detectionStatus != null) {
            detectionStatus.postValue(prediction.detectionStatus);
        }

        if (prediction.isViolent() && prediction.violenceConfidence > 0.8) {
            lastViolentPrediction = prediction;
            violentDetections.postValue((violentDetections.getValue() != null ? violentDetections.getValue() : 0) + 1);
            alertVisible.postValue(true);
            saveIncident(prediction.prediction, prediction.violenceConfidence, inferenceTimeMs, 0);
        }
        
        updateDuration();
    }

    public Prediction getLastViolentPrediction() {
        return lastViolentPrediction;
    }

    private void updateDuration() {
        if (startTime > 0) {
            sessionDuration.postValue(SystemClock.elapsedRealtime() - startTime);
        }
    }

    public void dismissAlert() {
        alertVisible.setValue(false);
    }

    public void saveIncident(String prediction, float confidence, double inferenceTimeMs, double preprocessingTimeMs) {
        long timestamp = System.currentTimeMillis();
        CrimeRecord record = new CrimeRecord(
                prediction,
                confidence,
                timestamp,
                "realtime_stream",
                0.0,
                0.0
        );
        record.setDetectionType("realtime");
        record.setInferenceTimeMs(inferenceTimeMs);
        record.setPreprocessingTimeMs(preprocessingTimeMs);
        repository.insertCrimeRecord(record);
    }

    public void setServerConnected(boolean connected) {
        isServerConnected.postValue(connected);
    }
}
