package com.krishna.crimedetection.network.models;

import java.util.List;

public class ReportResponse {
    private String reportType;
    private String generatedAt;
    private int totalIncidents;
    private int violenceCount;
    private double violenceRate;
    private List<DataPoint> trends;

    public String getReportType() { return reportType; }
    public String getGeneratedAt() { return generatedAt; }
    public int getTotalIncidents() { return totalIncidents; }
    public int getViolenceCount() { return violenceCount; }
    public double getViolenceRate() { return violenceRate; }
    public List<DataPoint> getTrends() { return trends; }

    public static class DataPoint {
        private String date;
        private int count;
        public String getDate() { return date; }
        public int getCount() { return count; }
    }
}
