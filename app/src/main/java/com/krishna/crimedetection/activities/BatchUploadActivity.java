package com.krishna.crimedetection.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.models.UploadStatus;
import com.krishna.crimedetection.network.PredictionResponse;
import com.krishna.crimedetection.utils.FileUtils;
import com.krishna.crimedetection.viewmodel.VideoUploadViewModel;

import java.util.Locale;

public class BatchUploadActivity extends AppCompatActivity {

    private VideoUploadViewModel viewModel;
    private ActivityResultLauncher<String> videoPickerLauncher;

    // UI Components - Selection
    private View selectionContainer;
    private View videoPickerCard;
    private View videoInfoCard;
    private ImageView thumbnailPreview;
    private TextView fileNameText, fileSizeText, durationText;
    private MaterialButton analyzeBtn, clearSelectionBtn;

    // UI Components - Upload
    private View uploadContainer;
    private TextView uploadStatusTitle, uploadPercentageText, uploadSpeedText, timeRemainingText;
    private LinearProgressIndicator uploadProgressBar;
    private MaterialButton cancelUploadBtn;

    // UI Components - Result
    private View resultContainer;
    private TextView predictionBadge, confidenceScore, inferenceTimeText, totalTimeText;
    private LinearProgressIndicator confidenceProgress;
    private MaterialButton viewInHistoryBtn, uploadAnotherBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_upload);

        viewModel = new ViewModelProvider(this).get(VideoUploadViewModel.class);
        initViews();
        setupPickers();
        observeViewModel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewModel.getUploadStatus().getValue() == UploadStatus.UPLOADING ||
                        viewModel.getUploadStatus().getValue() == UploadStatus.PROCESSING) {
                    showCancelConfirmation();
                } else {
                    finish();
                }
            }
        });
    }

    private void initViews() {
        // Toolbar
        findViewById(R.id.toolbar).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Containers
        selectionContainer = findViewById(R.id.selectionContainer);
        uploadContainer = findViewById(R.id.uploadContainer);
        resultContainer = findViewById(R.id.resultContainer);

        // Selection
        videoPickerCard = findViewById(R.id.videoPickerCard);
        videoInfoCard = findViewById(R.id.videoInfoCard);
        thumbnailPreview = findViewById(R.id.thumbnailPreview);
        fileNameText = findViewById(R.id.fileNameText);
        fileSizeText = findViewById(R.id.fileSizeText);
        durationText = findViewById(R.id.durationText);
        analyzeBtn = findViewById(R.id.analyzeBtn);
        clearSelectionBtn = findViewById(R.id.clearSelectionBtn);

        // Upload
        uploadStatusTitle = findViewById(R.id.uploadStatusTitle);
        uploadPercentageText = findViewById(R.id.uploadPercentageText);
        uploadSpeedText = findViewById(R.id.uploadSpeedText);
        timeRemainingText = findViewById(R.id.timeRemainingText);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        cancelUploadBtn = findViewById(R.id.cancelUploadBtn);

        // Result
        predictionBadge = findViewById(R.id.predictionBadge);
        confidenceScore = findViewById(R.id.confidenceScore);
        confidenceProgress = findViewById(R.id.confidenceProgress);
        inferenceTimeText = findViewById(R.id.inferenceTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        viewInHistoryBtn = findViewById(R.id.viewInHistoryBtn);
        uploadAnotherBtn = findViewById(R.id.uploadAnotherBtn);

        // Listeners
        videoPickerCard.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));
        clearSelectionBtn.setOnClickListener(v -> viewModel.clearSelection());
        analyzeBtn.setOnClickListener(v -> viewModel.uploadVideo());
        cancelUploadBtn.setOnClickListener(v -> showCancelConfirmation());
        uploadAnotherBtn.setOnClickListener(v -> viewModel.clearSelection());
        viewInHistoryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, IncidentHistoryActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupPickers() {
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        if (FileUtils.isValidVideoFormat(FileUtils.getFileNameFromUri(this, uri))) {
                            long size = FileUtils.getFileSize(this, uri);
                            if (size > 500 * 1024 * 1024) { // 500 MB
                                Toast.makeText(this, "Video exceeds 500MB limit", Toast.LENGTH_LONG).show();
                            } else {
                                viewModel.selectVideo(uri);
                            }
                        } else {
                            Toast.makeText(this, "Unsupported video format", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void observeViewModel() {
        viewModel.getSelectedVideoUri().observe(this, uri -> {
            if (uri != null) {
                videoPickerCard.setVisibility(View.GONE);
                videoInfoCard.setVisibility(View.VISIBLE);
                analyzeBtn.setEnabled(true);

                fileNameText.setText(FileUtils.getFileNameFromUri(this, uri));
                fileSizeText.setText(FileUtils.formatFileSize(FileUtils.getFileSize(this, uri)));
                durationText.setText(FileUtils.formatDuration(FileUtils.getVideoDuration(this, uri)));
                thumbnailPreview.setImageBitmap(FileUtils.getVideoThumbnail(this, uri));
            } else {
                videoPickerCard.setVisibility(View.VISIBLE);
                videoInfoCard.setVisibility(View.GONE);
                analyzeBtn.setEnabled(false);
            }
        });

        viewModel.getUploadStatus().observe(this, this::handleStatusChange);
        viewModel.getUploadProgress().observe(this, progress -> {
            uploadProgressBar.setProgress(progress);
            uploadPercentageText.setText(String.format(Locale.getDefault(), "%d%%", progress));
        });

        viewModel.getUploadSpeed().observe(this, speed -> uploadSpeedText.setText(speed));
        viewModel.getTimeRemaining().observe(this, time -> timeRemainingText.setText(getString(R.string.upload_remaining_format, time)));

        viewModel.getPredictionResult().observe(this, this::displayResult);

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleStatusChange(UploadStatus status) {
        selectionContainer.setVisibility(View.GONE);
        uploadContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);

        switch (status) {
            case IDLE:
            case SELECTING:
                selectionContainer.setVisibility(View.VISIBLE);
                break;
            case UPLOADING:
                uploadContainer.setVisibility(View.VISIBLE);
                uploadStatusTitle.setText(R.string.uploading);
                uploadProgressBar.setIndeterminate(false);
                break;
            case PROCESSING:
                uploadContainer.setVisibility(View.VISIBLE);
                uploadStatusTitle.setText(R.string.server_processing);
                uploadProgressBar.setIndeterminate(true);
                cancelUploadBtn.setVisibility(View.GONE);
                break;
            case DONE:
                resultContainer.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                selectionContainer.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void displayResult(PredictionResponse result) {
        if (result == null) return;

        boolean isViolent = result.isViolent();
        predictionBadge.setText(result.getPrediction());
        predictionBadge.setBackgroundResource(isViolent ? R.drawable.badge_violent : R.drawable.badge_nonviolent);
        
        confidenceScore.setText(result.getConfidencePercent());
        confidenceProgress.setProgress((int) (result.getConfidence() * 100));
        
        inferenceTimeText.setText(String.format(Locale.getDefault(), "%.1fs", result.getInferenceTimeMs() / 1000.0));
        totalTimeText.setText(String.format(Locale.getDefault(), "%.1fs", result.getTotalTimeMs() / 1000.0));
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Upload")
                .setMessage("Are you sure you want to cancel the video upload?")
                .setPositiveButton("Yes", (dialog, which) -> viewModel.cancelUpload())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        getOnBackPressedDispatcher().onBackPressed();
    }
}
