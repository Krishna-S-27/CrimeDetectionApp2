package com.krishna.crimedetection.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private final File file;
    private final UploadCallbacks listener;
    private final MediaType contentType;

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    public interface UploadCallbacks {
        void onProgressUpdate(int percentage, long bytesUploaded, long totalBytes);
        void onError();
        void onFinish();
    }

    public ProgressRequestBody(final File file, String contentType, final UploadCallbacks listener) {
        this.file = file;
        this.contentType = MediaType.parse(contentType);
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() throws IOException {
        return file.length();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long uploaded = 0;

        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                // Update progress on main thread
                final long currentUploaded = uploaded;
                handler.post(() -> listener.onProgressUpdate((int) (100 * currentUploaded / fileLength), currentUploaded, fileLength));

                uploaded += read;
                sink.write(buffer, 0, read);
            }
        }
    }
}
