package com.krishna.crimedetection.viewmodel;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.VideoUploadDao;
import com.krishna.crimedetection.models.VideoUploadRecord;
import com.krishna.crimedetection.services.VideoUploadService;

import java.util.List;

public class UploadServiceViewModel extends AndroidViewModel {
    private final VideoUploadDao uploadDao;
    private final LiveData<List<VideoUploadRecord>> allUploads;
    
    private final MutableLiveData<Integer> currentProgress = new MutableLiveData<>();
    private final MutableLiveData<String> currentSpeed = new MutableLiveData<>();
    private final MutableLiveData<Long> currentTimeRemaining = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeUploadId = new MutableLiveData<>();

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (VideoUploadService.BROADCAST_PROGRESS.equals(intent.getAction())) {
                activeUploadId.setValue(intent.getIntExtra("uploadId", -1));
                currentProgress.setValue(intent.getIntExtra("progress", 0));
                currentSpeed.setValue(intent.getStringExtra("speed"));
                currentTimeRemaining.setValue(intent.getLongExtra("timeRemaining", 0));
            }
        }
    };

    public UploadServiceViewModel(@NonNull Application application) {
        super(application);
        uploadDao = AppDatabase.getInstance(application).videoUploadDao();
        allUploads = uploadDao.getAllUploads();
        
        IntentFilter filter = new IntentFilter(VideoUploadService.BROADCAST_PROGRESS);
        LocalBroadcastManager.getInstance(application).registerReceiver(progressReceiver, filter);
    }

    public LiveData<List<VideoUploadRecord>> getAllUploads() {
        return allUploads;
    }

    public LiveData<Integer> getCurrentProgress() {
        return currentProgress;
    }

    public LiveData<String> getCurrentSpeed() {
        return currentSpeed;
    }

    public LiveData<Long> getCurrentTimeRemaining() {
        return currentTimeRemaining;
    }

    public LiveData<Integer> getActiveUploadId() {
        return activeUploadId;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(progressReceiver);
    }
}
