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
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.krishna.crimedetection.activities.DashboardActivity;
import com.krishna.crimedetection.activities.EmergencyActivity;
import com.krishna.crimedetection.activities.MainActivity;
import com.krishna.crimedetection.databinding.ActivityProfileBinding;
import com.krishna.crimedetection.utils.PreferenceUtils;

import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isEditing = false;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupImagePicker();
        setupUI();
        loadUserData();
        getCurrentLocation();
        setupNavigation();
        loadProfileImage();

        binding.btnEditPhoto.setOnClickListener(v -> openImagePicker());
        binding.btnEdit.setOnClickListener(v -> toggleEditMode());
        binding.btnLogout.setOnClickListener(v -> {
            PreferenceUtils.logout(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadProfileImage() {
        String imageUri = PreferenceUtils.getProfileImageUri(this);
        if (!imageUri.isEmpty()) {
            binding.ivProfileLarge.setImageURI(Uri.parse(imageUri));
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        binding.ivProfileLarge.setImageURI(imageUri);
                        if (imageUri != null) {
                            PreferenceUtils.setProfileImageUri(this, imageUri.toString());
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
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
        binding.tvProfileName.setText(name.isEmpty() ? "User" : name);
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
        String name = binding.etName.getText().toString();
        String email = binding.etEmail.getText().toString();
        String phone = binding.etPhone.getText().toString();
        String emergency = binding.etEmergency.getText().toString();

        PreferenceUtils.saveFullProfile(this, name, email, phone, emergency);
        binding.tvProfileName.setText(name);
        Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
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