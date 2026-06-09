package com.krishna.crimedetection.auth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.activities.AdminDashboardActivity;
import com.krishna.crimedetection.activities.DashboardActivity;
import com.krishna.crimedetection.activities.EmergencyActivity;
import com.krishna.crimedetection.activities.MainActivity;
import com.krishna.crimedetection.databinding.ActivityProfileBinding;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.models.MessageResponse;
import com.krishna.crimedetection.network.models.ProfileResponse;
import com.krishna.crimedetection.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isEditing = false;

    private TokenManager tokenManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupUI();
        loadUserData();
        fetchProfileFromServer();
        getCurrentLocation();
        setupNavigation();

        binding.btnEdit.setOnClickListener(v -> toggleEditMode());
        binding.btnAdminDashboard.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboardActivity.class)));

        if (tokenManager.isAdmin()) {
            binding.btnAdminDashboard.setVisibility(View.VISIBLE);
        }

        binding.btnLogout.setOnClickListener(v -> {
            tokenManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void fetchProfileFromServer() {
        apiService.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();
                    Log.d("ProfileActivity", "Server profile fetched: " + profile.getUsername());
                    
                    // Get existing local data first
                    String localName = PreferenceUtils.getUserName(ProfileActivity.this);
                    String localEmail = PreferenceUtils.getUserEmail(ProfileActivity.this);
                    String localPhone = PreferenceUtils.getUserPhone(ProfileActivity.this);
                    String localEmergency = PreferenceUtils.getEmergencyNumber(ProfileActivity.this);

                    // Only use server data if it's not null/empty, otherwise keep local
                    String name = (profile.getUsername() != null && !profile.getUsername().trim().isEmpty()) ? profile.getUsername() : localName;
                    String email = (profile.getEmail() != null && !profile.getEmail().trim().isEmpty()) ? profile.getEmail() : localEmail;
                    String phone = (profile.getPhoneNumber() != null && !profile.getPhoneNumber().trim().isEmpty()) ? profile.getPhoneNumber() : localPhone;
                    String emergency = (profile.getEmergencyContact() != null && !profile.getEmergencyContact().trim().isEmpty()) ? profile.getEmergencyContact() : localEmergency;

                    // Update UI
                    binding.etName.setText(name);
                    binding.tvProfileName.setText(name.isEmpty() ? getString(R.string.label_user_placeholder) : name);
                    binding.etEmail.setText(email);
                    binding.etPhone.setText(phone);
                    binding.etEmergency.setText(emergency);

                    // Sync local storage with the merged data
                    PreferenceUtils.saveFullProfile(ProfileActivity.this, name, email, phone, emergency);
                } else {
                    Log.w("ProfileActivity", "Server profile fetch failed, staying with local data");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                Log.e("ProfileActivity", "Failed to fetch profile from server, using local data", t);
                Log.d("ProfileActivity", "Local profile data will be retained: " + 
                      PreferenceUtils.getUserName(ProfileActivity.this) + " / " + 
                      PreferenceUtils.getUserEmail(ProfileActivity.this));
            }
        });
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupNavigation() {
        binding.bottomNavigation.setSelectedItemId(com.krishna.crimedetection.R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.krishna.crimedetection.R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == com.krishna.crimedetection.R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == com.krishna.crimedetection.R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == com.krishna.crimedetection.R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        String name = PreferenceUtils.getUserName(this);
        binding.etName.setText(name);
        binding.tvProfileName.setText(name.isEmpty() ? getString(R.string.label_user_placeholder) : name);
        binding.etEmail.setText(PreferenceUtils.getUserEmail(this));
        binding.etPhone.setText(PreferenceUtils.getUserPhone(this));
        binding.etEmergency.setText(PreferenceUtils.getEmergencyNumber(this));
        binding.tvLocationStatus.setText(PreferenceUtils.getLastLocation(this));
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        binding.etName.setEnabled(isEditing);
        binding.etEmail.setEnabled(isEditing);
        binding.etPhone.setEnabled(isEditing);
        binding.etEmergency.setEnabled(isEditing);

        if (isEditing) {
            binding.btnEdit.setText(getString(com.krishna.crimedetection.R.string.btn_save_profile));
            binding.etName.requestFocus();
        } else {
            saveProfile();
            binding.btnEdit.setText(getString(com.krishna.crimedetection.R.string.btn_edit_profile));
        }
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String emergency = binding.etEmergency.getText().toString().trim();

        // Update locally first for immediate feedback
        PreferenceUtils.saveFullProfile(this, name, email, phone, emergency);
        binding.tvProfileName.setText(name);

        // Update on backend
        Map<String, String> profileData = new HashMap<>();
        profileData.put("username", name);
        profileData.put("email", email);
        profileData.put("phone_number", phone);
        profileData.put("emergency_contact", emergency);

        apiService.updateProfile(profileData).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.msg_profile_synced), Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("ProfileActivity", "Failed to sync profile: " + response.code());
                    Toast.makeText(ProfileActivity.this, getString(R.string.msg_profile_local_only), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Log.e("ProfileActivity", "Error syncing profile", t);
                Toast.makeText(ProfileActivity.this, getString(R.string.msg_profile_local_only), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        binding.tvLocationStatus.setText(getString(com.krishna.crimedetection.R.string.msg_location_searching));
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        binding.tvLocationStatus.setText(address);
                        PreferenceUtils.setLastLocation(this, address);
                    }
                } catch (Exception e) {
                    binding.tvLocationStatus.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                }
            } else {
                binding.tvLocationStatus.setText(getString(com.krishna.crimedetection.R.string.msg_location_not_found));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}