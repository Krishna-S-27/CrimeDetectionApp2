package com.krishna.crimedetection.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Helper class for video file operations
 * Provides utility methods for video validation, sizing, etc.
 */
public class VideoUploadHelper {

    private static final String TAG = "VideoUploadHelper";
    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024;  // 500MB
    private static final long MIN_FILE_SIZE_BYTES = 100 * 1024;          // 100KB

    /**
     * Get file size in human-readable format
     *
     * @param bytes File size in bytes
     * @return Formatted size string (e.g., "5.2 MB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = new String[] {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        return new DecimalFormat("#,##0.#")
                .format(bytes / Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }

    /**
     * Validate video file before upload
     * Checks size, format, existence
     *
     * @param file Video file to validate
     * @return ValidationResult with status and error message if any
     */
    public static ValidationResult validateVideoFile(File file) {
        Log.d(TAG, "Validating video file: " + file.getName());

        // Check if file exists
        if (!file.exists()) {
            String error = "File does not exist: " + file.getAbsolutePath();
            Log.e(TAG, error);
            return new ValidationResult(false, error);
        }

        // Check if file is readable
        if (!file.canRead()) {
            String error = "Cannot read file: " + file.getAbsolutePath();
            Log.e(TAG, error);
            return new ValidationResult(false, error);
        }

        // Check file size
        long fileSize = file.length();
        if (fileSize < MIN_FILE_SIZE_BYTES) {
            String error = "File too small: " + formatFileSize(fileSize) +
                    " (minimum: " + formatFileSize(MIN_FILE_SIZE_BYTES) + ")";
            Log.e(TAG, error);
            return new ValidationResult(false, error);
        }

        if (fileSize > MAX_FILE_SIZE_BYTES) {
            String error = "File too large: " + formatFileSize(fileSize) +
                    " (maximum: " + formatFileSize(MAX_FILE_SIZE_BYTES) + ")";
            Log.e(TAG, error);
            return new ValidationResult(false, error);
        }

        // Check file extension
        String filename = file.getName().toLowerCase();
        String[] validExtensions = {".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".webm", ".3gp"};
        boolean hasValidExtension = false;

        for (String ext : validExtensions) {
            if (filename.endsWith(ext)) {
                hasValidExtension = true;
                break;
            }
        }

        if (!hasValidExtension) {
            String error = "Invalid file format: " + file.getName() +
                    "\nSupported: mp4, avi, mov, mkv, flv, wmv, webm, 3gp";
            Log.e(TAG, error);
            return new ValidationResult(false, error);
        }

        Log.d(TAG, "✓ File validation passed");
        return new ValidationResult(true, null);
    }

    /**
     * Get estimated upload time based on file size and network speed
     *
     * @param fileSizeBytes File size in bytes
     * @param networkSpeedMbps Network speed in Mbps
     * @return Estimated time in seconds
     */
    public static long estimateUploadTime(long fileSizeBytes, double networkSpeedMbps) {
        if (networkSpeedMbps <= 0) return 0;

        // Convert bytes to megabits: (bytes * 8) / 1,000,000
        double fileSizeMegabits = (fileSizeBytes * 8.0) / 1_000_000;

        // Calculate time: size / speed
        return Math.round(fileSizeMegabits / networkSpeedMbps);
    }

    /**
     * Get file size in megabytes
     *
     * @param file File object
     * @return Size in MB
     */
    public static double getFileSizeMB(File file) {
        return file.length() / (1024.0 * 1024.0);
    }

    /**
     * Result of file validation
     */
    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
}