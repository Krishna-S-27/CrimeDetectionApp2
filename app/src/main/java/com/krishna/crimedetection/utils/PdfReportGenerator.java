package com.krishna.crimedetection.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import com.krishna.crimedetection.models.CrimeRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {
    private static final String TAG = "PdfReportGenerator";

    public static File generateDailyReport(Context context, List<CrimeRecord> incidents) {
        return generateCustomReport(context, incidents, "Daily Report", "Today");
    }

    public static File generateWeeklyReport(Context context, List<CrimeRecord> incidents) {
        return generateCustomReport(context, incidents, "Weekly Report", "Last 7 Days");
    }

    public static File generateMonthlyReport(Context context, List<CrimeRecord> incidents) {
        return generateCustomReport(context, incidents, "Monthly Report", "Last 30 Days");
    }

    public static File generateCustomReport(Context context, List<CrimeRecord> incidents, String type, String dateRange) {
        PdfDocument document = new PdfDocument();
        int pageHeight = 1120;
        int pageWidth = 792;
        int yPos = 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        
        // Header
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLACK);
        canvas.drawText("VIOLENCE DETECTION SYSTEM - " + type.toUpperCase(), 100, yPos, titlePaint);
        
        yPos += 30;
        paint.setTextSize(14);
        canvas.drawText("Date Range: " + dateRange, 100, yPos, paint);
        
        yPos += 20;
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), 100, yPos, paint);

        yPos += 40;
        // Statistics
        int total = incidents.size();
        int violent = 0;
        double totalConfidence = 0;
        for (CrimeRecord r : incidents) {
            if (r.isViolent()) violent++;
            totalConfidence += r.getConfidence();
        }
        int nonViolent = total - violent;
        double avgConf = total > 0 ? totalConfidence / total : 0;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Summary Statistics", 100, yPos, paint);
        yPos += 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Total Incidents: " + total, 100, yPos, paint);
        yPos += 20;
        canvas.drawText("Violent: " + violent, 100, yPos, paint);
        canvas.drawText("Non-Violent: " + nonViolent, 300, yPos, paint);
        yPos += 20;
        canvas.drawText(String.format(Locale.getDefault(), "Average Confidence: %.1f%%", avgConf * 100), 100, yPos, paint);

        yPos += 40;
        // Table Header
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ID", 50, yPos, paint);
        canvas.drawText("Type", 100, yPos, paint);
        canvas.drawText("Prediction", 200, yPos, paint);
        canvas.drawText("Conf %", 350, yPos, paint);
        canvas.drawText("Timestamp", 450, yPos, paint);

        yPos += 10;
        canvas.drawLine(50, yPos, pageWidth - 50, yPos, paint);
        yPos += 20;

        // Table Rows
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (CrimeRecord record : incidents) {
            if (yPos > pageHeight - 60) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                yPos = 40;
            }

            canvas.drawText(String.valueOf(record.getId()), 50, yPos, paint);
            canvas.drawText(record.getDetectionType() != null ? record.getDetectionType() : "batch", 100, yPos, paint);
            
            if (record.isViolent()) paint.setColor(Color.RED);
            canvas.drawText(record.getPrediction(), 200, yPos, paint);
            paint.setColor(Color.BLACK);
            
            canvas.drawText(record.getConfidencePercent(), 350, yPos, paint);
            canvas.drawText(TimeUtils.formatTimestamp(record.getTimestamp()), 450, yPos, paint);
            yPos += 20;
        }

        // Footer
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("Confidential Report - Generated by Crime Detection App", pageWidth / 2f - 150, pageHeight - 30, paint);

        document.finishPage(page);

        File reportsDir = new File(context.getExternalFilesDir(null), "Reports");
        if (!reportsDir.exists()) reportsDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(reportsDir, "Report_" + type.replace(" ", "_") + "_" + timeStamp + ".pdf");

        try {
            document.writeTo(new FileOutputStream(file));
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF: " + e.getMessage());
            return null;
        } finally {
            document.close();
        }

        return file;
    }
}
