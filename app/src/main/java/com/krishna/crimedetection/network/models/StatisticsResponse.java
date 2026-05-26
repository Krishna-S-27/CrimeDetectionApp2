package com.krishna.crimedetection.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Response model for User Statistics
 */
public class StatisticsResponse {
    @SerializedName("total_incidents")
    private int totalIncidents;
    
    @SerializedName("violent_count")
    private int violentCount;
    
    @SerializedName("non_violent_count")
    private int nonViolentCount;
    
    @SerializedName("avg_confidence")
    private double avgConfidence;
    
    @SerializedName("incidents_by_day")
    private Map<String, Integer> incidentsByDay;

    public int getTotalIncidents() { return totalIncidents; }
    public int getViolentCount() { return violentCount; }
    public int getNonViolentCount() { return nonViolentCount; }
    public double getAvgConfidence() { return avgConfidence; }
    public Map<String, Integer> getIncidentsByDay() { return incidentsByDay; }
}