package com.krishna.crimedetection.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.krishna.crimedetection.R;
import com.krishna.crimedetection.activities.MainActivity;
import com.krishna.crimedetection.activities.PermissionsActivity;
import com.krishna.crimedetection.databinding.ActivityRegisterBinding;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.EmergencyContact;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.models.AuthResponse;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.TokenManager;

import com.krishna.crimedetection.network.models.MessageResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private TokenManager tokenManager;
    private ApiService apiService;
    private boolean isPhoneVerified = false;
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService(this);

        binding.btnVerifyPhone.setOnClickListener(v -> sendOtp(binding.etUserPhone.getText().toString(), "phone"));
        binding.btnSubmitOtp.setOnClickListener(v -> verifyOtp(binding.etUserPhone.getText().toString(), binding.etOtp.getText().toString(), "phone"));
        
        binding.btnVerifyEmail.setOnClickListener(v -> sendOtp(binding.etEmail.getText().toString(), "email"));
        binding.btnSubmitEmailOtp.setOnClickListener(v -> verifyOtp(binding.etEmail.getText().toString(), binding.etEmailOtp.getText().toString(), "email"));

        binding.btnRegister.setOnClickListener(v -> register());

        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void sendOtp(String target, String type) {
        if (target.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_target, type), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("target", target);
        data.put("type", type);

        apiService.sendOtp(data).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().getMessage();
                    Toast.makeText(RegisterActivity.this, getString(R.string.msg_otp_requested, target), Toast.LENGTH_SHORT).show();
                    
                    if (type.equals("phone")) {
                        binding.otpContainer.setVisibility(View.VISIBLE);
                        // Auto-fill shortcut for developers
                        if (msg != null && msg.contains("DEV_MODE_OTP:")) {
                            binding.etOtp.setText(msg.split(":")[1]);
                            Toast.makeText(RegisterActivity.this, getString(R.string.msg_dev_otp_autofill), Toast.LENGTH_SHORT).show();
                        }
                    } else if (type.equals("email")) {
                        binding.otpEmailContainer.setVisibility(View.VISIBLE);
                        // Auto-fill shortcut for developers
                        if (msg != null && msg.contains("DEV_MODE_OTP:")) {
                            binding.etEmailOtp.setText(msg.split(":")[1]);
                            Toast.makeText(RegisterActivity.this, getString(R.string.msg_dev_otp_autofill), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, getString(R.string.error_otp_send_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Toast.makeText(RegisterActivity.this, getString(R.string.msg_error_format, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String target, String otp, String type) {
        if (otp.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_otp_required), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("target", target);
        data.put("otp", otp);
        data.put("type", type);

        apiService.verifyOtp(data).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.msg_verification_success), Toast.LENGTH_SHORT).show();
                    if (type.equals("phone")) {
                        isPhoneVerified = true;
                        binding.otpContainer.setVisibility(View.GONE);
                        binding.btnVerifyPhone.setText(R.string.label_verified);
                        binding.btnVerifyPhone.setEnabled(false);
                        binding.etUserPhone.setEnabled(false);
                    } else {
                        isEmailVerified = true;
                        binding.btnVerifyEmail.setText(R.string.label_verified);
                        binding.btnVerifyEmail.setEnabled(false);
                        binding.etEmail.setEnabled(false);
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, getString(R.string.error_invalid_otp), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Toast.makeText(RegisterActivity.this, getString(R.string.msg_error_format, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyField(String field, String value) {
        if (value.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_target, field), Toast.LENGTH_SHORT).show();
            return;
        }
        // Simulated OTP/Verification
        Toast.makeText(this, "OTP sent to " + value + ". (Simulated Verification)", Toast.LENGTH_SHORT).show();
    }

    private void register() {
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String userPhone = binding.etUserPhone.getText().toString().trim();
        String emergencyPhone = binding.etFamilyNum.getText().toString().trim();
        String emergencyEmail = binding.etEmergencyEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (firstName.isEmpty()) {
            binding.etFirstName.setError(getString(R.string.error_first_name_required));
            binding.etFirstName.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            binding.etLastName.setError(getString(R.string.error_last_name_required));
            binding.etLastName.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            binding.etUsername.setError(getString(R.string.error_username_required));
            binding.etUsername.requestFocus();
            return;
        }

        if (userPhone.isEmpty()) {
            binding.etUserPhone.setError(getString(R.string.error_phone_required));
            binding.etUserPhone.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            binding.etEmail.setError(getString(R.string.error_email_required));
            binding.etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.error_invalid_email));
            binding.etEmail.requestFocus();
            return;
        }

        if (emergencyPhone.isEmpty()) {
            binding.etFamilyNum.setError(getString(R.string.error_emergency_phone_required));
            binding.etFamilyNum.requestFocus();
            return;
        }

        if (userPhone.equals(emergencyPhone)) {
            binding.etFamilyNum.setError(getString(R.string.error_emergency_phone_same));
            binding.etFamilyNum.requestFocus();
            return;
        }

        if (emergencyEmail.isEmpty()) {
            binding.etEmergencyEmail.setError(getString(R.string.error_emergency_email_required));
            binding.etEmergencyEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.etPassword.setError(getString(R.string.error_password_required));
            binding.etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this, getString(R.string.error_password_too_short), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPhoneVerified) {
            Toast.makeText(this, getString(R.string.error_verify_phone_first), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEmailVerified) {
            Toast.makeText(this, getString(R.string.error_verify_email_first), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Map<String, String> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("password", password);
        data.put("phone", userPhone);
        data.put("emergency_phone", emergencyPhone);
        data.put("emergency_email", emergencyEmail);
        data.put("first_name", firstName);
        data.put("last_name", lastName);

        apiService.register(data).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    tokenManager.saveToken(auth.getAccessToken());
                    
                    int currentUserId = -1;
                    if (auth.getUser() != null) {
                        currentUserId = auth.getUser().getId();
                        tokenManager.saveUserInfo(
                                currentUserId,
                                auth.getUser().getUsername(),
                                auth.getUser().getEmail(),
                                auth.getUser().getRole()
                        );
                    }
                    
                    // Save initial emergency contact to local DB
                    if (currentUserId != -1 && !emergencyPhone.isEmpty()) {
                        final int userId = currentUserId;
                        new Thread(() -> {
                            EmergencyContact contact = new EmergencyContact();
                            contact.setUserId(userId);
                            contact.setContactName("Family");
                            contact.setPhoneNumber(emergencyPhone);
                            contact.setRelationship("Family");
                            contact.setPrimary(true);
                            contact.setActive(true);
                            AppDatabase.getInstance(getApplicationContext())
                                    .emergencyContactDao().insertContact(contact);
                        }).start();
                    }
                    
                    Toast.makeText(RegisterActivity.this, getString(R.string.msg_registration_success), Toast.LENGTH_SHORT).show();
                    
                    // Also save to PreferenceUtils for immediate UI updates in other activities
                    PreferenceUtils.saveFullProfile(RegisterActivity.this, 
                            auth.getUser().getUsername(), 
                            auth.getUser().getEmail(), 
                            userPhone, 
                            emergencyPhone);
                    
                    // Save emergency email
                    PreferenceUtils.setEmergencyEmail(RegisterActivity.this, emergencyEmail);

                    Intent intent = new Intent(RegisterActivity.this, PermissionsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = getString(R.string.msg_registration_failed);
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                            if (jsonObject.has("detail")) {
                                errorMessage = jsonObject.getString("detail");
                            } else {
                                errorMessage = getString(R.string.msg_registration_failed) + ": " + response.code();
                            }
                        }
                    } catch (Exception e) {
                        errorMessage = getString(R.string.msg_registration_failed) + ": " + response.message();
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e("RegisterActivity", "Registration failed", t);
                Toast.makeText(RegisterActivity.this, getString(R.string.msg_network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        binding.btnRegister.setEnabled(!show);
        if (show) {
            binding.btnRegister.setText(R.string.label_registering);
        } else {
            binding.btnRegister.setText(R.string.btn_register);
        }
    }
}
