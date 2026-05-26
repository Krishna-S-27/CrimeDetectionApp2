package com.krishna.crimedetection.models;

import java.io.Serializable;

public class FilterState implements Serializable {
    private String prediction; // "VIOLENT", "NONVIOLENT", or null for "All"
    private String dateFrom;   // ISO format string or null
    private String dateTo;     // ISO format string or null
    private String searchQuery;
    private String sortBy;     // "newest", "oldest", "confidence_high", "confidence_low"

    public FilterState() {
        this.prediction = null;
        this.dateFrom = null;
        this.dateTo = null;
        this.searchQuery = "";
        this.sortBy = "newest";
    }

    // Getters and Setters
    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }

    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public void reset() {
        prediction = null;
        dateFrom = null;
        dateTo = null;
        searchQuery = "";
        sortBy = "newest";
    }
}
