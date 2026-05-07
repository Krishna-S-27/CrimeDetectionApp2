package com.krishna.crimedetection.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    /**
     * Upload video for violence/crime detection
     * Connects to: POST /predict in FastAPI backend
     *
     * @param video Video file as multipart
     * @return Prediction response with confidence and timing
     */
    @Multipart
    @POST("predict")
    Call<PredictionResponse> uploadVideo(@Part MultipartBody.Part video);

    /**
     * Check if server is healthy and reachable
     * Connects to: GET /health in FastAPI backend
     *
     * @return Health check response
     */
    @GET("health")
    Call<HealthCheckResponse> healthCheck();

    /**
     * Get server statistics (cache size, etc.)
     * Connects to: GET /stats in FastAPI backend
     *
     * @return Statistics response
     */
    @GET("stats")
    Call<StatsResponse> getStats();
}