package com.example.cab_approval_system;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationBody {
    private List<String> registration_ids;
    private Map<String, String> data;
    private Map<String, String> notification;  // Added notification field which might be needed

    public NotificationBody(List<String> registration_ids, Map<String, String> data) {
        this.registration_ids = registration_ids;
        this.data = data;

        // Create notification field with same content for proper notification display
        this.notification = new HashMap<>();
        this.notification.put("title", data.get("title"));
        this.notification.put("body", data.get("body"));

    }

    // Getters and setters
    public List<String> getRegistration_ids() {
        return registration_ids;
    }

    public void setRegistration_ids(List<String> registration_ids) {
        this.registration_ids = registration_ids;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public Map<String, String> getNotification() {
        return notification;
    }

    public void setNotification(Map<String, String> notification) {
        this.notification = notification;
    }
}