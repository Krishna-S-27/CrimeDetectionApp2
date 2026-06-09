package com.krishna.crimedetection.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.krishna.crimedetection.activities.BatchUploadActivity;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.VideoUploadDao;
import com.krishna.crimedetection.models.VideoUploadRecord;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.network.ProgressRequestBody;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.utils.NotificationUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;

public class VideoUploadService extends Service {
    private static final String TAG = "VideoUploadService";
    public static final String ACTION_START_UPLOAD = "ACTION_START_UPLOAD";
    public static final String ACTION_CANCEL_UPLOAD = "ACTION_CANCEL_UPLOAD";
    public static final String ACTION_PAUSE_UPLOAD = "ACTION_PAUSE_UPLOAD";
    public static final String ACTION_RESUME_UPLOAD = "ACTION_RESUME_UPLOAD";
    public static final String EXTRA_UPLOAD_ID = "EXTRA_UPLOAD_ID";

    public static final String BROADCAST_PROGRESS = "com.krishna.crimedetection.UPLOAD_PROGRESS";
    public static final String BROADCAST_COMPLETE = "com.krishna.crimedetection.UPLOAD_COMPLETE";
    public static final String BROADCAST_ERROR = "com.krishna.crimedetection.UPLOAD_ERROR";

    private static final int NOTIFICATION_ID = 1001;

    private ExecutorService executorService;
    private VideoUploadDao uploadDao;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean isPaused = false;
    private volatile int currentUploadId = -1;
    private retrofit2.Call<PredictionResponse> currentCall;

    private long lastNotificationUpdate = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        uploadDao = AppDatabase.getInstance(this).videoUploadDao();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CrimeDetection:UploadWakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            int uploadId = intent.getIntExtra(EXTRA_UPLOAD_ID, -1);

            if (ACTION_START_UPLOAD.equals(action)) {
                startForeground(NOTIFICATION_ID, createNotification("Starting upload...", 0));
                processQueue();
            } else if (ACTION_CANCEL_UPLOAD.equals(action)) {
                cancelUpload(uploadId);
            } else if (ACTION_PAUSE_UPLOAD.equals(action)) {
                pauseUpload(uploadId);
            } else if (ACTION_RESUME_UPLOAD.equals(action)) {
                resumeUpload(uploadId);
            }
        }
        return START_STICKY;
    }

    private void processQueue() {
        executorService.execute(() -> {
            List<VideoUploadRecord> pending = uploadDao.getPendingUploads();
            if (pending.isEmpty()) {
                stopSelf();
                return;
            }

            for (VideoUploadRecord record : pending) {
                if (isPaused) break;
                performUpload(record);
            }
            
            // Re-check queue if not paused
            if (!isPaused) {
                processQueue();
            }
        });
    }

    private void performUpload(VideoUploadRecord record) {
        currentUploadId = record.id;
        record.uploadStatus = "UPLOADING";
        uploadDao.updateUpload(record);

        if (!wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

        try {
            File file = getFileFromUri(Uri.parse(record.videoUri));
            if (file == null || !file.exists()) {
                handleError(record, "File not found");
                return;
            }

            long startTime = System.currentTimeMillis();
            
            ProgressRequestBody requestBody = new ProgressRequestBody(file, "video/*", new ProgressRequestBody.UploadCallbacks() {
                @Override
                public void onProgressUpdate(int percentage, long bytesUploaded, long totalBytes) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastNotificationUpdate > 500) {
                        long duration = currentTime - startTime;
                        double speed = bytesUploaded / (duration / 1000.0) / (1024 * 1024); // MB/s
                        long remainingBytes = totalBytes - bytesUploaded;
                        long timeRemaining = speed > 0 ? (long) (remainingBytes / (speed * 1024 * 1024) * 1000) : 0;

                        record.progress = percentage;
                        record.speed = String.format(java.util.Locale.getDefault(), "%.2f MB/s", speed);
                        record.timeRemaining = timeRemaining;
                        uploadDao.updateUpload(record);

                        updateNotification("Uploading " + record.fileName, percentage);
                        sendProgressBroadcast(record);
                        lastNotificationUpdate = currentTime;
                    }
                }

                @Override
                public void onError() {
                    handleError(record, "Stream error");
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Upload stream finished");
                }
            });

            MultipartBody.Part body = MultipartBody.Part.createFormData("video", record.fileName, requestBody);
            ApiService apiService = RetrofitClient.getApiService(this);
            retrofit2.Call<PredictionResponse> call = apiService.uploadVideo(body);
            currentCall = call;
            
            retrofit2.Response<PredictionResponse> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                PredictionResponse result = response.body();
                record.uploadStatus = "COMPLETED";
                record.progress = 100;
                // record.incidentId = result.getIncidentId(); // Assuming prediction response might link to an incident
                uploadDao.updateUpload(record);
                sendCompleteBroadcast(record);
                updateNotification("Upload Complete: " + record.fileName, 100);
            } else {
                handleError(record, "Server error: " + response.code());
            }

        } catch (IOException e) {
            if (currentCall != null && currentCall.isCanceled()) {
                Log.d(TAG, "Upload canceled");
            } else {
                handleError(record, "Network error: " + e.getMessage());
            }
        } finally {
            if (wakeLock.isHeld()) wakeLock.release();
            currentCall = null;
            currentUploadId = -1;
        }
    }

    private void handleError(VideoUploadRecord record, String error) {
        Log.e(TAG, "Upload failed: " + error);
        record.uploadStatus = "FAILED";
        record.errorMessage = error;
        record.retryCount++;
        uploadDao.updateUpload(record);
        sendErrorBroadcast(record, error);
        updateNotification("Upload Failed: " + record.fileName, 0);
    }

    private void cancelUpload(int uploadId) {
        if (currentUploadId == uploadId && currentCall != null) {
            currentCall.cancel();
        }
        executorService.execute(() -> {
            VideoUploadRecord record = uploadDao.getUploadById(uploadId);
            if (record != null) {
                record.uploadStatus = "PENDING"; // Or create a CANCELED state
                uploadDao.updateUpload(record);
            }
        });
    }

    private void pauseUpload(int uploadId) {
        isPaused = true;
        if (currentUploadId == uploadId && currentCall != null) {
            currentCall.cancel();
        }
    }

    private void resumeUpload(int uploadId) {
        isPaused = false;
        processQueue();
    }

    private File getFileFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            File tempFile = new File(getCacheDir(), "upload_temp_video");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
            return tempFile;
        } catch (SecurityException e) {
            Log.e(TAG, "Security error (permission denied) reading URI: " + uri, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error reading URI: " + uri, e);
            return null;
        }
    }

    private void updateNotification(String text, int progress) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, createNotification(text, progress));
    }

    private Notification createNotification(String text, int progress) {
        NotificationUtils.ensureChannels(this);
        
        Intent intent = new Intent(this, BatchUploadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NotificationUtils.VIDEO_UPLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Video Upload")
                .setContentText(text)
                .setProgress(100, progress, progress == 0)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void sendProgressBroadcast(VideoUploadRecord record) {
        Intent intent = new Intent(BROADCAST_PROGRESS);
        intent.putExtra("uploadId", record.id);
        intent.putExtra("progress", record.progress);
        intent.putExtra("speed", record.speed);
        intent.putExtra("timeRemaining", record.timeRemaining);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCompleteBroadcast(VideoUploadRecord record) {
        Intent intent = new Intent(BROADCAST_COMPLETE);
        intent.putExtra("uploadId", record.id);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendErrorBroadcast(VideoUploadRecord record, String error) {
        Intent intent = new Intent(BROADCAST_ERROR);
        intent.putExtra("uploadId", record.id);
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentCall != null) currentCall.cancel();
        executorService.shutdownNow();
        if (wakeLock.isHeld()) wakeLock.release();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Ensure service stops when app is swiped away
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
