package com.krishna.crimedetection.network;

import com.krishna.crimedetection.network.models.AuthResponse;
import com.krishna.crimedetection.network.models.IncidentListResponse;
import com.krishna.crimedetection.network.models.IncidentResponse;
import com.krishna.crimedetection.network.models.MessageResponse;
import com.krishna.crimedetection.network.models.ProfileResponse;
import com.krishna.crimedetection.network.models.StatisticsResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit API Service interface for Crime Detection Backend.
 */
public interface ApiService {

    // ===================== AUTH ENDPOINTS =====================

    /**
     * Login with username and password
     */
    @POST("auth/login")
    Call<AuthResponse> login(@Body Map<String, String> credentials);

    /**
     * Register a new user
     */
    @POST("auth/register")
    Call<AuthResponse> register(@Body Map<String, String> userData);

    /**
     * Get current user profile
     */
    @GET("auth/profile")
    Call<ProfileResponse> getProfile();

    /**
     * Delete current user profile
     */
    @retrofit2.http.DELETE("auth/profile")
    Call<MessageResponse> deleteProfile();

    /**
     * Update user profile
     */
    @POST("auth/profile/update")
    Call<MessageResponse> updateProfile(@Body Map<String, String> profileData);

    /**
     * Save FCM token for the user
     */
    @POST("api/v1/user/fcm-token")
    Call<MessageResponse> saveFcmToken(@Body com.krishna.crimedetection.network.models.FcmTokenRequest request);

    // ===================== DETECTION ENDPOINTS =====================

    /**
     * Upload video for violence/crime detection (Batch)
     */
    @Multipart
    @POST("predict")
    Call<PredictionResponse> uploadVideo(@Part MultipartBody.Part video);

    /**
     * Upload preprocessed frames for real-time violence detection
     */
    @Multipart
    @POST("predict-realtime")
    Call<PredictionResponse> predictRealtime(
            @Part MultipartBody.Part file,
            @Part("fps") RequestBody fps,
            @Part("buffer_count") RequestBody bufferCount
    );

    // ===================== INCIDENT ENDPOINTS =====================

    /**
     * Get list of incidents for the current user with filtering and pagination
     */
    @GET("api/v1/incidents/list")
    Call<IncidentListResponse> getIncidents(
            @retrofit2.http.Query("skip") int skip,
            @retrofit2.http.Query("limit") int limit,
            @retrofit2.http.Query("prediction") String prediction,
            @retrofit2.http.Query("search") String search,
            @retrofit2.http.Query("date_from") String dateFrom,
            @retrofit2.http.Query("date_to") String dateTo,
            @retrofit2.http.Query("sort_by") String sortBy
    );

    /**
     * Get details of a specific incident
     */
    @GET("api/v1/incidents/{id}")
    Call<IncidentResponse> getIncidentDetails(@Path("id") int id);

    /**
     * Get user statistics
     */
    @GET("api/v1/incidents/user/statistics")
    Call<StatisticsResponse> getStatistics();

    // ===================== NOTIFICATION ENDPOINTS =====================

    /**
     * Get list of notifications for the current user
     */
    @GET("api/v1/notifications/list")
    Call<com.krishna.crimedetection.network.models.NotificationListResponse> getNotifications(
            @retrofit2.http.Query("skip") int skip,
            @retrofit2.http.Query("limit") int limit
    );

    /**
     * Mark notification as read
     */
    @POST("api/v1/notifications/{id}/read")
    Call<MessageResponse> markNotificationRead(@Path("id") int id);

    // ===================== ADMIN ENDPOINTS =====================

    /**
     * Get admin dashboard data
     */
    @GET("api/v1/admin/dashboard")
    Call<com.krishna.crimedetection.network.models.AdminDashboardResponse> getAdminDashboard();

    /**
     * Get incidents for admin review
     */
    @GET("api/v1/admin/incidents")
    Call<IncidentListResponse> getAdminIncidents(
            @retrofit2.http.Query("skip") int skip,
            @retrofit2.http.Query("limit") int limit,
            @retrofit2.http.Query("status") String status,
            @retrofit2.http.Query("sort_by") String sortBy
    );

    /**
     * Review an incident
     */
    @POST("api/v1/admin/incidents/{incident_id}/review")
    Call<MessageResponse> reviewIncident(
            @Path("incident_id") int incidentId,
            @Body com.krishna.crimedetection.network.models.AdminReviewRequest request
    );

    /**
     * Get all users (admin only)
     */
    @GET("api/v1/admin/users")
    Call<com.krishna.crimedetection.network.models.UserListResponse> getAllUsers(
            @retrofit2.http.Query("skip") int skip,
            @retrofit2.http.Query("limit") int limit,
            @retrofit2.http.Query("role") String role
    );

    /**
     * Suspend user
     */
    @POST("api/v1/admin/users/{user_id}/suspend")
    Call<MessageResponse> suspendUser(@Path("user_id") int userId);

    /**
     * Unsuspend user
     */
    @POST("api/v1/admin/users/{user_id}/unsuspend")
    Call<MessageResponse> unsuspendUser(@Path("user_id") int userId);

    /**
     * Promote user to admin
     */
    @POST("api/v1/admin/users/{user_id}/promote")
    Call<MessageResponse> promoteUserToAdmin(@Path("user_id") int userId);

    /**
     * Delete user
     */
    @retrofit2.http.DELETE("api/v1/admin/users/{user_id}")
    Call<MessageResponse> deleteUser(@Path("user_id") int userId);

    /**
     * Generate report
     */
    @GET("api/v1/admin/reports/{type}")
    Call<com.krishna.crimedetection.network.models.ReportResponse> generateReport(
            @Path("type") String type,
            @retrofit2.http.Query("date_from") String dateFrom,
            @retrofit2.http.Query("date_to") String dateTo
    );

    // ===================== SERVER STATUS =====================

    /**
     * Check server health
     */
    @POST("auth/send-otp")
    Call<MessageResponse> sendOtp(@Body Map<String, String> data);

    @POST("auth/verify-otp")
    Call<MessageResponse> verifyOtp(@Body Map<String, String> data);

    @GET("health")
    Call<HealthCheckResponse> healthCheck();

    /**
     * Get server stats
     */
    @GET("stats")
    Call<StatsResponse> getStats();
}