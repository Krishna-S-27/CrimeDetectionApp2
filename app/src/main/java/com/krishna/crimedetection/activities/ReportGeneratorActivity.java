package com.krishna.crimedetection.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.krishna.crimedetection.databinding.ActivityReportGeneratorBinding;
import com.krishna.crimedetection.viewmodel.ReportViewModel;

import java.io.File;

public class ReportGeneratorActivity extends AppCompatActivity {

    private ActivityReportGeneratorBinding binding;
    private ReportViewModel viewModel;
    private File currentReportFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportGeneratorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        setupToolbar();
        setupListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Generate Report");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnDailyReport.setOnClickListener(v -> viewModel.generateDailyReport());
        binding.btnWeeklyReport.setOnClickListener(v -> viewModel.generateWeeklyReport());
        binding.btnMonthlyReport.setOnClickListener(v -> viewModel.generateMonthlyReport());

        binding.btnCustomReport.setOnClickListener(v -> {
            MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select Date Range")
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection.first != null && selection.second != null) {
                    viewModel.generateCustomReport(selection.first, selection.second);
                }
            });
            picker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        binding.btnOpenPdf.setOnClickListener(v -> openPdf(currentReportFile));
        binding.btnSaveToDevice.setOnClickListener(v -> Toast.makeText(this, "Report saved to: " + currentReportFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
        binding.btnShareEmail.setOnClickListener(v -> sharePdf(currentReportFile, "com.google.android.gm"));
        binding.btnShareWhatsApp.setOnClickListener(v -> sharePdf(currentReportFile, "com.whatsapp"));
        binding.btnUploadDrive.setOnClickListener(v -> sharePdf(currentReportFile, "com.google.android.apps.docs"));
        
        binding.fabRefresh.setOnClickListener(v -> {
            binding.previewCard.setVisibility(View.GONE);
            binding.shareCard.setVisibility(View.GONE);
        });
    }

    private void observeViewModel() {
        viewModel.getIsGenerating().observe(this, isGenerating -> {
            binding.loadingLayout.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
            setButtonsEnabled(!isGenerating);
        });

        viewModel.getReportFile().observe(this, file -> {
            if (file != null) {
                currentReportFile = file;
                binding.previewCard.setVisibility(View.VISIBLE);
                binding.shareCard.setVisibility(View.VISIBLE);
                binding.tvFileName.setText("File Name: " + file.getName());
            }
        });

        viewModel.getReportSize().observe(this, size -> binding.tvFileSize.setText("File Size: " + size));
        viewModel.getGeneratedTime().observe(this, time -> binding.tvGeneratedTime.setText("Generated: " + time));

        viewModel.getViolentCount().observe(this, count -> binding.tvStatViolent.setText(String.valueOf(count)));
        viewModel.getNonViolentCount().observe(this, count -> binding.tvStatNonViolent.setText(String.valueOf(count)));
        viewModel.getAverageConfidence().observe(this, avg -> 
                binding.tvStatConfidence.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", avg * 100)));

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnDailyReport.setEnabled(enabled);
        binding.btnWeeklyReport.setEnabled(enabled);
        binding.btnMonthlyReport.setEnabled(enabled);
        binding.btnCustomReport.setEnabled(enabled);
    }

    private void openPdf(File file) {
        if (file == null || !file.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open PDF"));
    }

    private void sharePdf(File file, String packageName) {
        if (file == null || !file.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        if (packageName != null) {
            intent.setPackage(packageName);
        }

        try {
            startActivity(Intent.createChooser(intent, "Share Report"));
        } catch (Exception e) {
            Toast.makeText(this, "Application not found", Toast.LENGTH_SHORT).show();
        }
    }
}
