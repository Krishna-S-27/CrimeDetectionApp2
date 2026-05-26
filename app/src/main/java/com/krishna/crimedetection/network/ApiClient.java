package com.krishna.crimedetection.network;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Retrofit client singleton for connecting to FastAPI backend
 * Supports dynamic base URL from PreferenceUtils
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    /**
     * Get Retrofit instance. Used by classes that need direct access to Retrofit.
     * Defaults to 10.0.2.2 for emulator.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            getService("http://10.0.2.2:8000/");
        }
        return retrofit;
    }

    public static Retrofit getClient(android.content.Context context) {
        String baseUrl = com.krishna.crimedetection.utils.PreferenceUtils.getBaseUrl(context);
        getService(baseUrl);
        return retrofit;
    }

    /**
     * Get ApiService instance with specified base URL
     * Creates new Retrofit instance if URL changed
     *
     * @param baseUrl Server URL (e.g., "http://192.168.1.15:8000")
     * @return ApiService interface for making calls
     */
    public static ApiService getService(String baseUrl) {
        // Ensure baseUrl is not null or empty
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://10.0.2.2:8000/"; // Default fallback
        }

        // Add scheme if missing
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }

        // Ensure base URL ends with /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        // Create new retrofit if URL changed or first time
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            Log.i(TAG, "Creating new Retrofit instance for URL: " + baseUrl);
            currentBaseUrl = baseUrl;
            retrofit = createRetrofit(baseUrl);
        }

        return retrofit.create(ApiService.class);
    }

    /**
     * Create Retrofit instance with OkHttp configuration
     *
     * @param baseUrl Server base URL
     * @return Configured Retrofit instance
     */
    private static Retrofit createRetrofit(String baseUrl) {

        // OkHttp client with timeouts for video uploads
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)     // Connection timeout
                .readTimeout(120, TimeUnit.SECONDS)       // Read timeout (videos take time)
                .writeTimeout(120, TimeUnit.SECONDS)      // Write timeout (upload time)
                .addInterceptor(getLoggingInterceptor())  // Log all requests/responses
                .build();

        // Gson configuration
        Gson gson = new GsonBuilder()
                .setLenient()  // More forgiving parsing
                .create();

        // Build Retrofit instance
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
    }

    /**
     * Get HTTP logging interceptor for debugging
     * Logs all request/response bodies
     *
     * @return Configured HttpLoggingInterceptor
     */
    private static HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG, message)
        );
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    /**
     * Reset Retrofit instance (useful when changing servers)
     * Call this if user changes server IP in settings
     */
    public static void reset() {
        Log.i(TAG, "Resetting Retrofit client");
        retrofit = null;
        currentBaseUrl = null;
    }

    /**
     * Get current base URL being used
     *
     * @return Current base URL or null if not initialized
     */
    public static String getCurrentBaseUrl() {
        return currentBaseUrl;
    }
}