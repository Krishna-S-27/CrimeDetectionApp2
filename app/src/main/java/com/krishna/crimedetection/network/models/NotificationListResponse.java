package com.krishna.crimedetection.network.models;

import java.util.List;

/**
 * Response model for list of notifications
 */
public class NotificationListResponse {
    private List<NotificationResponse> notifications;
    private int total;
    private int skip;
    private int limit;

    public List<NotificationResponse> getNotifications() { return notifications; }
    public int getTotal() { return total; }
    public int getSkip() { return skip; }
    public int getLimit() { return limit; }
}
