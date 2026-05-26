package com.krishna.crimedetection.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.auth.ProfileActivity;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.WhatsAppManager;

import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity {

    private TextInputEditText etFamilyNum;
    private FusedLocationProviderClient fusedLocationClient;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.title_emergency);
        }

        etFamilyNum = findViewById(R.id.etFamilyNum);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        setupNavigation();
        
        etFamilyNum.setText(PreferenceUtils.getEmergencyNumber(this));

        etFamilyNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                PreferenceUtils.setEmergencyNumber(EmergencyActivity.this, s.toString());
            }
        });

        findViewById(R.id.btnCallFamily).setOnClickListener(v -> makeCall(PreferenceUtils.getEmergencyNumber(this)));
        findViewById(R.id.btnShareWhatsApp).setOnClickListener(v -> shareOnWhatsApp());
        
        findViewById(R.id.cardPolice).setOnClickListener(v -> makeCall(getString(R.string.num_police)));
        findViewById(R.id.cardAmbulance).setOnClickListener(v -> makeCall(getString(R.string.num_ambulance)));
        findViewById(R.id.cardWomen).setOnClickListener(v -> makeCall(getString(R.string.num_women_helpline)));
        findViewById(R.id.cardFire).setOnClickListener(v -> makeCall(getString(R.string.num_fire_brigade)));
        findViewById(R.id.cardChild).setOnClickListener(v -> makeCall(getString(R.string.num_child_helpline)));
        findViewById(R.id.cardAntiRagging).setOnClickListener(v -> makeCall(getString(R.string.num_anti_ragging)));

        findViewById(R.id.btnSearchPolice).setOnClickListener(v -> searchNearbyPolice());
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_emergency);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_emergency) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void searchNearbyPolice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2001);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String uri = String.format(Locale.US, "geo:%f,%f?q=police+station", location.getLatitude(), location.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    String webUri = String.format(Locale.US, "https://www.google.com/maps/search/police+station/@%f,%f,15z", location.getLatitude(), location.getLongitude());
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
                }
            } else {
                Toast.makeText(this, "Location unavailable. Please enable GPS.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeCall(String number) {
        if (number == null || number.isEmpty()) {
            Toast.makeText(this, "No number provided", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void shareOnWhatsApp() {
        String number = PreferenceUtils.getEmergencyNumber(this);
        if (number == null || number.isEmpty()) {
            Toast.makeText(this, "Please set an emergency number first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2002);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            String message = "EMERGENCY! I need help. ";
            if (location != null) {
                message += "My current location: https://www.google.com/maps/search/?api=1&query=" + 
                        location.getLatitude() + "," + location.getLongitude();
            } else {
                message += "Unable to fetch location details.";
            }

            WhatsAppManager.sendWhatsAppMessage(this, number, message, new WhatsAppManager.Callback() {
                @Override
                public void onSuccess(String phoneNumber) {
                    Toast.makeText(EmergencyActivity.this, "WhatsApp message initiated", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String phoneNumber, String error) {
                    Toast.makeText(EmergencyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNotInstalled() {
                    Toast.makeText(EmergencyActivity.this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            searchNearbyPolice();
        } else if (requestCode == 2002 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            shareOnWhatsApp();
        }
    }
}