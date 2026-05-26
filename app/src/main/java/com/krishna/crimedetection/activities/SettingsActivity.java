package com.krishna.crimedetection.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.krishna.crimedetection.databinding.ActivitySettingsBinding;
import com.krishna.crimedetection.utils.NotificationPreferences;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private NotificationPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        prefs = new NotificationPreferences(this);
        setupSwitches();
    }

    private void setupSwitches() {
        binding.switchNotifications.setChecked(prefs.isNotificationsEnabled());
        binding.switchSound.setChecked(prefs.isSoundEnabled());
        binding.switchVibration.setChecked(prefs.isVibrationEnabled());
        binding.switchViolenceAlerts.setChecked(prefs.isViolenceAlertsEnabled());
        binding.switchAdminAlerts.setChecked(prefs.isAdminAlertsEnabled());

        binding.switchNotifications.setOnCheckedChangeListener((v, checked) -> prefs.setNotificationsEnabled(checked));
        binding.switchSound.setOnCheckedChangeListener((v, checked) -> prefs.setSoundEnabled(checked));
        binding.switchVibration.setOnCheckedChangeListener((v, checked) -> prefs.setVibrationEnabled(checked));
        binding.switchViolenceAlerts.setOnCheckedChangeListener((v, checked) -> prefs.setViolenceAlerts(checked));
        binding.switchAdminAlerts.setOnCheckedChangeListener((v, checked) -> prefs.setAdminAlerts(checked));
    }
}
