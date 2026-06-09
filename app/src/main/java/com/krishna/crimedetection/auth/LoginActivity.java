package com.krishna.crimedetection.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.krishna.crimedetection.R;
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
            Toast.makeText(this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show();
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
                        Log.d("LoginActivity", "User login successful: " + auth.getUser().getUsername());
                        tokenManager.saveUserInfo(
                                auth.getUser().getId(),
                                auth.getUser().getUsername(),
                                auth.getUser().getEmail(),
                                auth.getUser().getRole()
                        );
                        // Save complete profile to PreferenceUtils for display
                        String phone = auth.getUser().getPhoneNumber() != null ? auth.getUser().getPhoneNumber() : "";
                        String emergency = auth.getUser().getEmergencyContact() != null ? auth.getUser().getEmergencyContact() : "";
                        Log.d("LoginActivity", "Saving profile - Name: " + auth.getUser().getUsername() + 
                              ", Email: " + auth.getUser().getEmail() + ", Phone: " + phone + ", Emergency: " + emergency);
                        PreferenceUtils.saveFullProfile(LoginActivity.this,
                                auth.getUser().getUsername(),
                                auth.getUser().getEmail(),
                                phone,
                                emergency);
                    }
                    
                    Toast.makeText(LoginActivity.this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, PermissionsActivity.class));
                    finish();
                } else {
                    String error = getString(R.string.error_invalid_credentials);
                    if (response.code() == 401) {
                        error = getString(R.string.error_incorrect_credentials);
                    }
                    Log.e("LoginActivity", "Login failed with code: " + response.code());
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e("LoginActivity", "Login failed", t);
                Toast.makeText(LoginActivity.this, getString(R.string.msg_network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.btnLogin.setEnabled(!show);
        // You could add a progress bar to activity_login.xml if needed
        if (show) {
            binding.btnLogin.setText(R.string.label_logging_in);
        } else {
            binding.btnLogin.setText(R.string.btn_signin);
        }
    }
}
