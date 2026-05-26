package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krishna.crimedetection.models.UploadStatus;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.network.ProgressRequestBody;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.utils.FileUtils;

import java.io.File;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoUploadViewModel extends AndroidViewModel {

    private final MutableLiveData<Uri> selectedVideoUri = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>(0);
    private final MutableLiveData<String> uploadSpeed = new MutableLiveData<>("");
    private final MutableLiveData<String> timeRemaining = new MutableLiveData<>("");
    private final MutableLiveData<PredictionResponse> predictionResult = new MutableLiveData<>();
    private final MutableLiveData<UploadStatus> uploadStatus = new MutableLiveData<>(UploadStatus.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long startTime;
    private Call<PredictionResponse> currentCall;

    public VideoUploadViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Uri> getSelectedVideoUri() { return selectedVideoUri; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<String> getUploadSpeed() { return uploadSpeed; }
    public LiveData<String> getTimeRemaining() { return timeRemaining; }
    public LiveData<PredictionResponse> getPredictionResult() { return predictionResult; }
    public LiveData<UploadStatus> getUploadStatus() { return uploadStatus; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void selectVideo(Uri uri) {
        selectedVideoUri.setValue(uri);
        uploadStatus.setValue(UploadStatus.SELECTING);
    }

    public void clearSelection() {
        selectedVideoUri.setValue(null);
        uploadStatus.setValue(UploadStatus.IDLE);
        predictionResult.setValue(null);
    }

    public void uploadVideo() {
        Uri uri = selectedVideoUri.getValue();
        if (uri == null) {
            errorMessage.setValue("Please select a video file");
            return;
        }

        uploadStatus.setValue(UploadStatus.UPLOADING);
        startTime = SystemClock.elapsedRealtime();

        try {
            File file = FileUtils.getFileFromUri(getApplication(), uri);
            ProgressRequestBody requestBody = new ProgressRequestBody(file, "video/*", new ProgressRequestBody.UploadCallbacks() {
                @Override
                public void onProgressUpdate(int percentage, long bytesUploaded, long totalBytes) {
                    uploadProgress.postValue(percentage);
                    
                    long currentTime = SystemClock.elapsedRealtime();
                    long timeDiff = currentTime - startTime;
                    if (timeDiff > 0) {
                        double speed = (double) bytesUploaded / (timeDiff / 1000.0);
                        uploadSpeed.postValue(FileUtils.formatSpeed(speed));
                        
                        if (speed > 0) {
                            long remainingBytes = totalBytes - bytesUploaded;
                            long remainingTimeMs = (long) (remainingBytes / speed * 1000);
                            timeRemaining.postValue(FileUtils.formatTimeRemaining(remainingTimeMs));
                        }
                    }

                    if (percentage >= 100) {
                        uploadStatus.postValue(UploadStatus.PROCESSING);
                    }
                }

                @Override
                public void onError() {
                    uploadStatus.postValue(UploadStatus.ERROR);
                    errorMessage.postValue("Error preparing upload");
                }

                @Override
                public void onFinish() {
                    // Handled by Retrofit callback
                }
            });

            MultipartBody.Part body = MultipartBody.Part.createFormData("video", file.getName(), requestBody);
            ApiService apiService = RetrofitClient.getApiService(getApplication());
            currentCall = apiService.uploadVideo(body);
            
            currentCall.enqueue(new Callback<PredictionResponse>() {
                @Override
                public void onResponse(@NonNull Call<PredictionResponse> call, @NonNull Response<PredictionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        predictionResult.setValue(response.body());
                        uploadStatus.setValue(UploadStatus.DONE);
                    } else {
                        uploadStatus.setValue(UploadStatus.ERROR);
                        errorMessage.setValue("Server error: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PredictionResponse> call, @NonNull Throwable t) {
                    if (!call.isCanceled()) {
                        uploadStatus.setValue(UploadStatus.ERROR);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            uploadStatus.setValue(UploadStatus.ERROR);
            errorMessage.setValue("File error: " + e.getMessage());
        }
    }

    public void cancelUpload() {
        if (currentCall != null) {
            currentCall.cancel();
        }
        uploadStatus.setValue(UploadStatus.SELECTING);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentCall != null) {
            currentCall.cancel();
        }
    }
}
