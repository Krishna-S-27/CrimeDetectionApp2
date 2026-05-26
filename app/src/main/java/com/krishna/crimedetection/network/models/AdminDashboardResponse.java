package com.krishna.crimedetection.network.models;

import java.util.List;

public class AdminDashboardResponse {
    private int totalUsers;
    private int totalIncidents;
    private int pendingReviews;
    private int incidentsThisWeek;
    private int criticalAlerts;
    private double violenceRate;
    private double avgConfidence;
    private List<ChartData> dailyTrends;
    private List<UserActivity> recentActivity;

    public int getTotalUsers() { return totalUsers; }
    public int getTotalIncidents() { return totalIncidents; }
    public int getPendingReviews() { return pendingReviews; }
    public int getIncidentsThisWeek() { return incidentsThisWeek; }
    public int getCriticalAlerts() { return criticalAlerts; }
    public double getViolenceRate() { return violenceRate; }
    public double getAvgConfidence() { return avgConfidence; }
    public List<ChartData> getDailyTrends() { return dailyTrends; }
    public List<UserActivity> getRecentActivity() { return recentActivity; }

    public static class ChartData {
        private String label;
        private float value;
        public String getLabel() { return label; }
        public float getValue() { return value; }
    }

    public static class UserActivity {
        private int userId;
        private String userName;
        private String userEmail;
        private String action;
        private String incidentId;
        private long timestamp;

        public int getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getUserEmail() { return userEmail; }
        public String getAction() { return action; }
        public String getIncidentId() { return incidentId; }
        public long getTimestamp() { return timestamp; }
    }
}
