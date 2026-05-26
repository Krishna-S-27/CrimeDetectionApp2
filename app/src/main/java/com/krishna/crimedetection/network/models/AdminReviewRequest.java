package com.krishna.crimedetection.network.models;

public class AdminReviewRequest {
    private String action; // approve, reject, request_info
    private String notes;
    private Boolean markedForPolice;
    private String caseNumber;

    public AdminReviewRequest(String action, String notes, Boolean markedForPolice, String caseNumber) {
        this.action = action;
        this.notes = notes;
        this.markedForPolice = markedForPolice;
        this.caseNumber = caseNumber;
    }
}
