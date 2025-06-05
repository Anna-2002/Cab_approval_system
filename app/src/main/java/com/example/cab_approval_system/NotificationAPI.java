package com.example.cab_approval_system;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface NotificationAPI {
    @Headers({
            "Authorization: key=d9a830681c0c5c2c7702133d97273aa5424e5d64",  // 🔐 Your server key
            "Content-Type: application/json"
    })
    @POST("send-notification")
    Call<Void> sendNotification(@Body NotificationBody body);
}