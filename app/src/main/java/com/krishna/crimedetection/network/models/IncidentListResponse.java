package com.krishna.crimedetection.network.models;

import java.util.List;

/**
 * Response model for list of incidents
 */
public class IncidentListResponse {
    private List<IncidentResponse> incidents;
    private int total;
    private int skip;
    private int limit;

    public List<IncidentResponse> getIncidents() { return incidents; }
    public int getTotal() { return total; }
    public int getSkip() { return skip; }
    public int getLimit() { return limit; }
}