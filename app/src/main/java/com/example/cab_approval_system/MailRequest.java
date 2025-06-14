package com.example.cab_approval_system;

public class MailRequest {
    private String requesterEmail;
    private String approverEmail;
    private String requesterName;

    public MailRequest(String requesterEmail, String approverEmail, String requesterName) {
        this.requesterEmail = requesterEmail;
        this.approverEmail = approverEmail;
        this.requesterName = requesterName;
    }

    // Getters and setters (optional if using Gson)
}