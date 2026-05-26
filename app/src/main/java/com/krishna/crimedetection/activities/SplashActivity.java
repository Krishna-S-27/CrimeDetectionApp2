package com.krishna.crimedetection.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.krishna.crimedetection.auth.LoginActivity;
import com.krishna.crimedetection.databinding.ActivitySplashBinding;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.HealthCheckResponse;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Splash screen that checks for authentication and server availability.
 */
@SuppressLint("CustomSplash")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private TokenManager tokenManager;
    private static final int SPLASH_DELAY = 2000;
    private boolean isHealthChecked = false;
    private boolean isTimerExpired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        // Start 2-second timer
        new Handler().postDelayed(() -> {
            isTimerExpired = true;
            checkReadyToNavigate();
        }, SPLASH_DELAY);

        // Check backend health
        checkServerHealth();
    }

    private void checkServerHealth() {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.healthCheck().enqueue(new Callback<HealthCheckResponse>() {
            @Override
            public void onResponse(@NonNull Call<HealthCheckResponse> call, @NonNull Response<HealthCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isHealthChecked = true;
                    binding.tvStatus.setText("Server connected");
                    checkReadyToNavigate();
                } else {
                    handleServerError("Server is not responding correctly.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<HealthCheckResponse> call, @NonNull Throwable t) {
                handleServerError("Cannot connect to server. Please check your connection.");
                Log.e("SplashActivity", "Health check failed", t);
            }
        });
    }

    private void handleServerError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setText("Error");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // Even if server fails, we might want to let the user proceed to login/main 
        // if they are already logged in, or we might want to block them.
        // For now, let's wait 3 seconds then proceed anyway or show a retry.
        new Handler().postDelayed(() -> {
            isHealthChecked = true; 
            checkReadyToNavigate();
        }, 3000);
    }

    private void checkReadyToNavigate() {
        if (isTimerExpired && isHealthChecked) {
            navigateToNext();
        }
    }

    private void navigateToNext() {
        Intent intent;
        if (tokenManager.isLoggedIn()) {
            intent = new Intent(SplashActivity.this, PermissionsActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}