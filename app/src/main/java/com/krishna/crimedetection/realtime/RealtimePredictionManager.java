package com.krishna.crimedetection.realtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.krishna.crimedetection.models.Prediction;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.network.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles ML inference for real-time violence detection by sending frames 
 * to a backend server hosting an .h5 model.
 */
public class RealtimePredictionManager implements FrameCaptureListener {
    private static final String TAG = "RealtimePredictionManager";
    private static final int INPUT_SIZE = 224;

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final RealtimePredictionListener listener;
    private final ApiService apiService;
    private final Context context;
    
    // Internal FrameBuffer to store frames for the model
    private final FrameBuffer frameBuffer;
    private int framesSinceLastPrediction = 0;
    private static final int PREDICTION_INTERVAL = 12; // Frequency of sending batches

    public interface RealtimePredictionListener {
        void onPredictionReceived(Prediction prediction, double inferenceTimeMs);
        void onFrameCaptured(int bufferSize, int totalCaptured, float fps);
        void onPredictionError(String error);
    }

    public RealtimePredictionManager(Context context, RealtimePredictionListener listener) {
        this.context = context;
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.frameBuffer = new FrameBuffer();
        this.apiService = RetrofitClient.getApiService(context);
        
        Log.i(TAG, "Initialized RealtimePredictionManager (Remote .h5 Mode)");
    }

    public void onFrameCaptured(Bitmap frame, long timestamp, float fps) {
        // Resize immediately to save memory and processing time during serialization
        Bitmap resized = Bitmap.createScaledBitmap(frame, INPUT_SIZE, INPUT_SIZE, true);
        frameBuffer.addFrame(resized);
        framesSinceLastPrediction++;

        // Notify listener that a frame was captured for UI feedback
        if (listener != null) {
            mainHandler.post(() -> listener.onFrameCaptured(frameBuffer.getSize(), frameBuffer.getFrameCount(), fps));
        }

        // Send to server when buffer is ready and interval is met
        if (framesSinceLastPrediction >= PREDICTION_INTERVAL && frameBuffer.isReady()) {
            framesSinceLastPrediction = 0;
            Bitmap[] frames = frameBuffer.getFrames();
            if (frames != null) {
                // Take a snapshot of the current frames to avoid concurrent modification issues
                Bitmap[] framesCopy = frames.clone();
                executorService.execute(() -> uploadFramesForInference(framesCopy, timestamp, fps));
            }
        }
    }

    private void uploadFramesForInference(Bitmap[] frames, long timestamp, float currentFps) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Starting inference request for " + frames.length + " frames");

        try {
            // 1. Serialize the batch of frames into a binary file
            File file = serializeFrames(frames);
            if (file == null) {
                Log.e(TAG, "Failed to serialize frames");
                return;
            }

            Log.d(TAG, "Serialized file size: " + file.length() + " bytes");

            // 2. Prepare Multipart request
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            RequestBody fpsBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentFps));
            RequestBody bufferCountBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(frameBuffer.getSize()));

            // 3. Call backend API
            apiService.predictRealtime(body, fpsBody, bufferCountBody).enqueue(new Callback<PredictionResponse>() {
                @Override
                public void onResponse(@NonNull Call<PredictionResponse> call, @NonNull Response<PredictionResponse> response) {
                    long totalTime = System.currentTimeMillis() - startTime;
                    if (response.isSuccessful() && response.body() != null) {
                        PredictionResponse res = response.body();
                        
                        String label = res.getPrediction();
                        double confidence = res.getConfidence() != null ? res.getConfidence() : 0.0;
                        
                        // Calculate confidence scores
                        float violentConf = (label != null && label.equalsIgnoreCase("VIOLENT")) ? (float)confidence : (float)(1 - confidence);
                        float nonViolentConf = 1.0f - violentConf;

                        Prediction prediction = new Prediction(violentConf, nonViolentConf, label, timestamp);
                        prediction.alertMessage = res.getAlertMessage();
                        prediction.shareWhatsappVideo = res.getShareWhatsappVideo();
                        prediction.localSaveFolder = res.getLocalSaveFolder();
                        prediction.frameCount = res.getFrameCount();
                        prediction.fps = res.getFps();
                        prediction.bufferCount = res.getBufferCount();
                        prediction.detectionStatus = res.getDetectionStatus();
                        mainHandler.post(() -> listener.onPredictionReceived(prediction, (double) totalTime));
                    } else {
                        Log.e(TAG, "Server error: " + response.code());
                        mainHandler.post(() -> listener.onPredictionError("Server error: " + response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<PredictionResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Network failure: " + t.getMessage());
                    mainHandler.post(() -> listener.onPredictionError("Network failure: " + t.getMessage()));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in prediction flow: " + e.getMessage());
        }
    }

    /**
     * Converts a batch of Bitmaps into a single binary file (RGB raw bytes).
     */
    private File serializeFrames(Bitmap[] frames) throws IOException {
        File tempFile = File.createTempFile("frames_batch_", ".dat", context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            for (Bitmap bitmap : frames) {
                // Bitmaps are already resized in onFrameCaptured
                int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
                bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
                
                byte[] rgbBytes = new byte[INPUT_SIZE * INPUT_SIZE * 3];
                for (int i = 0; i < pixels.length; i++) {
                    rgbBytes[i * 3] = (byte) ((pixels[i] >> 16) & 0xFF);     // R
                    rgbBytes[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF); // G
                    rgbBytes[i * 3 + 2] = (byte) (pixels[i] & 0xFF);        // B
                }
                fos.write(rgbBytes);
            }
        }
        return tempFile;
    }

    @Override
    public void onBufferFull(Bitmap[] frames) {}

    @Override
    public void onCaptureError(String error) {
        mainHandler.post(() -> listener.onPredictionError(error));
    }

    public void stop() {
        executorService.shutdown();
    }
}
