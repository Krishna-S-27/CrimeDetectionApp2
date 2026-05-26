package com.krishna.crimedetection.network;

import android.content.Context;
import androidx.annotation.NonNull;

import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.TokenManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Enhanced Retrofit Client with Auth Interceptors and proper timeouts.
 */
public final class RetrofitClient {
    private static volatile Retrofit retrofit;
    private static volatile String lastBaseUrl;

    private RetrofitClient() {}

    /**
     * Get API Service instance with auth interceptor
     */
    public static ApiService getApiService(Context context) {
        String baseUrl = PreferenceUtils.getBaseUrl(context);

        // Re-initialize if base URL changes or if it's the first time
        if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
                    
                    // 1. Logging Interceptor
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // 2. Auth Interceptor
                    Interceptor authInterceptor = new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            Request original = chain.request();
                            String path = original.url().encodedPath();

                            // Skip auth for public endpoints
                            if (path.contains("/auth/login") || 
                                path.contains("/auth/register") || 
                                path.contains("/health") ||
                                path.contains("/stats") ||
                                path.contains("/docs") ||
                                path.equals("/")) {
                                return chain.proceed(original);
                            }

                            TokenManager tokenManager = new TokenManager(context);
                            String token = tokenManager.getToken();

                            Request.Builder builder = original.newBuilder()
                                    .header("Accept", "application/json");

                            if (token != null) {
                                builder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(builder.build());
                        }
                    };

                    // 3. OkHttpClient with timeouts for large video files
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .addInterceptor(authInterceptor)
                            .connectTimeout(60, TimeUnit.SECONDS)
                            .readTimeout(120, TimeUnit.SECONDS)
                            .writeTimeout(120, TimeUnit.SECONDS)
                            .build();

                    // 4. Retrofit instance
                    retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    lastBaseUrl = baseUrl;
                }
            }
        }
        return retrofit.create(ApiService.class);
    }
}