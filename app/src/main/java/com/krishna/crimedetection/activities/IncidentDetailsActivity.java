package com.krishna.crimedetection.activities;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.databinding.ActivityIncidentDetailsBinding;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.models.IncidentResponse;
import com.krishna.crimedetection.utils.PreferenceUtils;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "IncidentDetails";
    public static final String EXTRA_INCIDENT_ID = "extra_incident_id";

    private ActivityIncidentDetailsBinding binding;
    private ApiService apiService;
    private ExoPlayer player;
    private int incidentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncidentDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        incidentId = getIntent().getIntExtra(EXTRA_INCIDENT_ID, -1);
        if (incidentId == -1) {
            Toast.makeText(this, R.string.error_invalid_incident_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService(this);
        setupUI();
        loadIncidentDetails();
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_incident_history);
        }

        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnShare.setOnClickListener(v -> Toast.makeText(this, R.string.msg_sharing, Toast.LENGTH_SHORT).show());
        binding.btnDownload.setOnClickListener(v -> Toast.makeText(this, R.string.msg_downloading, Toast.LENGTH_SHORT).show());
    }

    private void loadIncidentDetails() {
        apiService.getIncidentDetails(incidentId).enqueue(new Callback<IncidentResponse>() {
            @Override
            public void onResponse(Call<IncidentResponse> call, Response<IncidentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayIncident(response.body());
                } else {
                    Toast.makeText(IncidentDetailsActivity.this, R.string.error_load_details_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<IncidentResponse> call, Throwable t) {
                Toast.makeText(IncidentDetailsActivity.this, getString(R.string.msg_network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayIncident(IncidentResponse incident) {
        binding.tvVideoName.setText(getVideoFileName(incident.getVideoPath()));
        
        String prediction = incident.getPrediction().toUpperCase();
        binding.tvPrediction.setText(prediction);
        boolean isViolent = "VIOLENT".equalsIgnoreCase(prediction) || "CRIME".equalsIgnoreCase(prediction);
        binding.tvPrediction.setBackgroundTintList(ColorStateList.valueOf(isViolent ? 0xFFEF4444 : 0xFF10B981));

        binding.tvConfidence.setText(String.format(Locale.getDefault(), "%.1f%%", incident.getConfidence() * 100));
        binding.tvTimestamp.setText(incident.getTimestamp());
        binding.chipType.setText(incident.getDetectionType() != null ? incident.getDetectionType().toUpperCase() : "UNKNOWN");
        
        binding.tvCoordinates.setText(getString(R.string.label_coordinates_format, 
                incident.getLatitude(), incident.getLongitude()));

        setupVideoPlayer(incident.getVideoPath());
    }

    private String getVideoFileName(String path) {
        if (path == null) return getString(R.string.label_unknown_video);
        int lastSlash = path.lastIndexOf("/");
        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
    }

    private void setupVideoPlayer(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            binding.videoProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.error_no_video_available, Toast.LENGTH_SHORT).show();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);

        // Assume the video path is relative to the server URL if it doesn't start with http
        String fullUrl = videoPath;
        if (!videoPath.startsWith("http")) {
            String baseUrl = PreferenceUtils.getServerURL(this);
            if (!baseUrl.endsWith("/") && !videoPath.startsWith("/")) {
                baseUrl += "/";
            }
            fullUrl = baseUrl + videoPath;
        }

        Log.d(TAG, "Playing video from: " + fullUrl);
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(fullUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_incident_title)
                .setMessage(R.string.delete_incident_msg)
                .setPositiveButton(R.string.btn_delete_confirm, (dialog, which) -> {
                    // API call to delete (placeholder)
                    Toast.makeText(this, R.string.msg_incident_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.clear, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
