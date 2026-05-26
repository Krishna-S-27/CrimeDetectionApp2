package com.krishna.crimedetection.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.VideoUploadDao;
import com.krishna.crimedetection.models.VideoUploadRecord;
import com.krishna.crimedetection.services.VideoUploadService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoUploadHelper {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void enqueueUpload(Context context, Uri uri, String fileName, long fileSize) {
        executor.execute(() -> {
            VideoUploadDao dao = AppDatabase.getInstance(context).videoUploadDao();
            VideoUploadRecord record = new VideoUploadRecord();
            record.videoUri = uri.toString();
            record.fileName = fileName;
            record.fileSize = fileSize;
            
            long id = dao.insertUpload(record);
            
            Intent intent = new Intent(context, VideoUploadService.class);
            intent.setAction(VideoUploadService.ACTION_START_UPLOAD);
            intent.putExtra(VideoUploadService.EXTRA_UPLOAD_ID, (int) id);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        });
    }

    public static void cancelUpload(Context context, int uploadId) {
        Intent intent = new Intent(context, VideoUploadService.class);
        intent.setAction(VideoUploadService.ACTION_CANCEL_UPLOAD);
        intent.putExtra(VideoUploadService.EXTRA_UPLOAD_ID, uploadId);
        context.startService(intent);
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
