package com.krishna.crimedetection.network;

import android.content.Context;

import com.krishna.crimedetection.utils.PreferenceUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    private static volatile Retrofit retrofit;
    private static volatile String lastBaseUrl;

    private RetrofitClient() {}

    public static ApiService getApiService(Context context) {
        String baseUrl = PreferenceUtils.getBaseUrl(context);

        if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null || !baseUrl.equals(lastBaseUrl)) {
                    HttpLoggingInterceptor log = new HttpLoggingInterceptor();
                    log.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient ok = new OkHttpClient.Builder()
                            .addInterceptor(log)
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(ok)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    lastBaseUrl = baseUrl;
                }
            }
        }
        return retrofit.create(ApiService.class);
    }
}