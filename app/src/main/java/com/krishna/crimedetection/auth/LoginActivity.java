package com.krishna.crimedetection.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.krishna.crimedetection.activities.PermissionsActivity;
import com.krishna.crimedetection.databinding.ActivityLoginBinding;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.models.AuthResponse;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        tokenManager = new TokenManager(this);
        if (tokenManager.isLoggedIn()) {
            startActivity(new Intent(this, PermissionsActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        apiService = RetrofitClient.getApiService(this);

        binding.btnLogin.setOnClickListener(v -> login());

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void login() {
        String usernameOrEmail = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", usernameOrEmail);
        credentials.put("password", password);

        apiService.login(credentials).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    tokenManager.saveToken(auth.getAccessToken());
                    
                    if (auth.getUser() != null) {
                        tokenManager.saveUserInfo(
                                auth.getUser().getId(),
                                auth.getUser().getUsername(),
                                auth.getUser().getEmail(),
                                auth.getUser().getRole()
                        );
                        // Also save to PreferenceUtils for consistency
                        PreferenceUtils.saveUser(LoginActivity.this, 
                                auth.getUser().getUsername(), 
                                auth.getUser().getEmail(), 
                                "");
                    }
                    
                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, PermissionsActivity.class));
                    finish();
                } else {
                    String error = "Invalid credentials";
                    if (response.code() == 401) {
                        error = "Incorrect email or password";
                    }
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e("LoginActivity", "Login failed", t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.btnLogin.setEnabled(!show);
        // You could add a progress bar to activity_login.xml if needed
        if (show) {
            binding.btnLogin.setText("Logging in...");
        } else {
            binding.btnLogin.setText("Login");
        }
    }
}
