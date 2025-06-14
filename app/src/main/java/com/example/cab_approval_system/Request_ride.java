package com.example.cab_approval_system;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Request_ride extends AppCompatActivity {

    private MailService mailService;
    private ImageButton time_picker_button, date_picker_button, decrease_button, increase_button;
    private TextView time_selected, date_selected, people_count, num_of_riders_edit_text,
            passengers_details, passenger_name_display, request_heading;
    private View num_of_people_horizontal_layout;
    private ToggleButton select_toggle_button;
    private Button request_button, save_button;
    private EditText pickup, dropoff, purpose_of_ride, project, vehicle_type;
    private String email_id;
    private LinearLayout passenger_layout, main_passenger_layout, source_layout,
            destination_layout, time_picker_layout, date_picker_layout, purpose_layout,
            outer_num_of_passenger_layout, inner_num_of_passenger_layout, passengerdetails_layout, project_layout, vehicleType_layout;
    private int passenger_count;
    Map<String, String> passengerMap = new HashMap<>();
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "cab_approval_notifications";
    private static final String TAG = "Request_ride"; // For logging

    // FCM Server URL, changed to proper endpoint
    private static final String FCM_API_URL = "https://fcm.googleapis.com/";
    private static final String FCM_SERVER_KEY = "key=6574b46cb1c850a522bc8244ea110cb68ecde71b";

    // Retrofit interface for FCM
    private interface FCMService {
        @Headers({
                "Content-Type: application/json"
        })
        @POST("send-notification") // This should match your backend endpoint
        Call<FCMResponse> sendNotification(@Body FCMRequest fcmRequest);
    }

    // FCM Request model
    public class FCMRequest {
        private String token;
        private String title;
        private String body;

        public FCMRequest(String token, String title, String message) {
            this.token = token;
            this.title = title;
            this.body = message;
        }
    }

    //initializing Retrofit to send mail to requester and approver
    private void initializeMailRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://your-node-api.onrender.com/") // Replace with your actual Render URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        mailService = retrofit.create(MailService.class);
    }


    // FCM Response model
    public class FCMResponse {
        private boolean success;
        private String messageId;
        private int successCount;
        private int failureCount;
        private List<Object> failures;
    }
    // Retrofit instance for FCM
    private FCMService fcmService;

    private void initializeRetrofit() {
        // Create OkHttpClient with increased timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Use your Render service URL, not FCM directly
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://cab-notification-service.onrender.com/") // Your Render URL
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        fcmService = retrofit.create(FCMService.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_ride);

        request_heading = findViewById(R.id.request_heading);
        source_layout = findViewById(R.id.source_layout);
        destination_layout = findViewById(R.id.destination_layout);
        time_picker_layout = findViewById(R.id.time_picker_layout);
        date_picker_layout = findViewById(R.id.date_picker_layout);
        purpose_layout = findViewById(R.id.purpose_layout);
        outer_num_of_passenger_layout = findViewById(R.id.outer_num_of_passenger_layout);
        inner_num_of_passenger_layout = findViewById(R.id.inner_num_of_passenger_layout);
        passengerdetails_layout = findViewById(R.id.passengerdetails_layout);
        project_layout = findViewById(R.id.project_layout);
        vehicleType_layout = findViewById(R.id.vehicleType_layout);

        initializeUI();

        // methods for specific functionalities in UI
        setupDateTimePickers();
        setupCountButtons();
        setupToggleButton();
        setupRequestButton();

        View[] blurView = {request_heading, passenger_layout, source_layout, destination_layout, time_picker_layout, date_picker_layout, purpose_layout, project_layout, outer_num_of_passenger_layout,
                inner_num_of_passenger_layout, main_passenger_layout, passengerdetails_layout, vehicleType_layout};

        for (View v : blurView) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Enable software rendering

            // Apply blur only to the background, not text
            if (v.getBackground() != null) {
                v.getBackground().mutate().setFilterBitmap(true);
                v.getBackground().setAlpha(200);  // Optional: Adjust transparency for better effect
            }
        }

        // Subscribe to Firebase topic for notifications
        FirebaseMessaging.getInstance().subscribeToTopic("ride_requests")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to ride_requests topic");
                    }
                });
        FirebaseApp.initializeApp(this);
        initializeRetrofit();
        checkNotificationPermission(); // Ensure permission is checked
        createNotificationChannel();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //creating notification channel for push notification
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Cab Approval Notifications";
            String description = "Notifications for ride request approvals";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initializeUI() {
        time_picker_button = findViewById(R.id.time_picker_button);
        time_selected = findViewById(R.id.time_edit_text);
        date_picker_button = findViewById(R.id.date_picker_button);
        date_selected = findViewById(R.id.date_edit_text);
        increase_button = findViewById(R.id.num_increase_button);
        decrease_button = findViewById(R.id.num_decrease_button);
        people_count = findViewById(R.id.num_of_rides_edit_text);
        num_of_riders_edit_text = findViewById(R.id.people_count);
        num_of_people_horizontal_layout = findViewById(R.id.inner_num_of_passenger_layout);
        select_toggle_button = findViewById(R.id.select_toggleButton);
        request_button = findViewById(R.id.request_btn);
        pickup = findViewById(R.id.source_edit_text);
        dropoff = findViewById(R.id.destination_edit_text);
        purpose_of_ride = findViewById(R.id.purpose_edittext);
        passengers_details = findViewById(R.id.passenger_details);
        passenger_layout = findViewById(R.id.passengerdetails_layout);
        save_button = findViewById(R.id.save_button);
        passenger_name_display = findViewById(R.id.passenger_name_display);
        main_passenger_layout = findViewById(R.id.main_passenger_layout);
        project = findViewById(R.id.project_edittext);
        vehicle_type = findViewById(R.id.vehicle_type_edittext);
    }

    // method to fetch time
    private void setupDateTimePickers() {
        time_picker_button.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(Request_ride.this,
                    (view, hourOfDay, minute) -> {
                        String amPm = hourOfDay >= 12 ? "PM" : "AM";
                        hourOfDay = hourOfDay % 12;
                        hourOfDay = hourOfDay == 0 ? 12 : hourOfDay;
                        String time = String.format("%02d:%02d %s", hourOfDay, minute, amPm);
                        time_selected.setText(time);
                    }, hour, min, false);
            timePickerDialog.show();
        });

        //method to fetch date
        date_picker_button.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(Request_ride.this,
                    (view, year1, month1, dayOfMonth) -> date_selected.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1),
                    year, month, day);
            datePickerDialog.show();
        });
    }

    // A horizontal layout where increase and decrease button with a edit text in center to enter number of people for a particular ride. Functionalities of increase and decrease button is specified here.
    private void setupCountButtons() {
        increase_button.setOnClickListener(v -> {
            int currentCount = Integer.parseInt(people_count.getText().toString());
            if (currentCount < 100) {
                currentCount++;
                people_count.setText(String.valueOf(currentCount));
            }
        });

        decrease_button.setOnClickListener(v -> {
            int currentCount = Integer.parseInt(people_count.getText().toString());
            if (currentCount > 0) {
                currentCount--;
                people_count.setText(String.valueOf(currentCount));
            }
        });
    }

    private void setupToggleButton() {
        select_toggle_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int passengerCount = Integer.parseInt(people_count.getText().toString());

            if (passengerCount > 1) {
                passengers_details.setVisibility(View.VISIBLE);
                save_button.setVisibility(View.VISIBLE);
            } else {
                passengers_details.setVisibility(View.GONE);
                save_button.setVisibility(View.GONE);
            }

            if (isChecked) {
                main_passenger_layout.setVisibility(View.VISIBLE);
                num_of_riders_edit_text.setText(String.valueOf(passengerCount));
                num_of_people_horizontal_layout.setVisibility(View.GONE);
                num_of_riders_edit_text.setVisibility(View.VISIBLE);
                passenger_name_display.setText(""); // Clear previous saved text
                passenger_name_display.setVisibility(View.GONE);

                generateEditTexts(passengerCount - 1); // Generate new EditTexts
            } else {
                main_passenger_layout.setVisibility(View.GONE);
                people_count.setText(num_of_riders_edit_text.getText().toString());
                num_of_people_horizontal_layout.setVisibility(View.VISIBLE);
                num_of_riders_edit_text.setVisibility(View.GONE);
                passenger_layout.removeAllViews(); // Clear everything
                passengers_details.setVisibility(View.GONE);
                save_button.setVisibility(View.GONE);
            }
        });

        save_button.setOnClickListener(v -> {
            StringBuilder passengerNames = new StringBuilder();
            boolean allFieldsFilled = true;
            int passengerIndex = 1; // To track correct numbering

            for (int i = 0; i < passenger_layout.getChildCount(); i++) {
                View childView = passenger_layout.getChildAt(i);
                if (childView instanceof EditText) {
                    String name = ((EditText) childView).getText().toString().trim();
                    if (!name.isEmpty()) {
                        String key = "Passenger " + passengerIndex;
                        passengerMap.put(key, name);
                        passengerNames.append("Passenger ").append(passengerIndex).append(": ").append(name).append("\n");
                        passengerIndex++; // Increment only for valid passengers
                    } else {
                        allFieldsFilled = false;
                        break;
                    }
                }
            }
            if (allFieldsFilled) {
                passenger_name_display.setText(passengerNames.toString());
                passenger_name_display.setVisibility(View.VISIBLE);
                passenger_layout.removeAllViews(); // Clear EditTexts after saving
                save_button.setVisibility(View.GONE);

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Rides");
                databaseReference.child("passengerNames").setValue(passengerMap)
                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "Passenger names saved successfully"))
                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to save passenger names", e));

            } else {
                Toast.makeText(Request_ride.this, "Enter all passenger names", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateEditTexts(int numFields) {
        passenger_layout.removeAllViews(); // Ensure no leftover views

        if (numFields <= 0) return;

        for (int i = 1; i <= numFields; i++) {
            TextView textView = new TextView(this);
            textView.setText("Passenger " + i + ":");
            textView.setTextSize(16);
            textView.setPadding(10, 10, 10, 5);
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_color));
            passenger_layout.addView(textView);

            EditText editText = new EditText(this);
            editText.setHint("Enter Passenger Name");
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setPadding(10, 10, 10, 10);
            editText.setId(View.generateViewId());
            editText.setBackgroundResource(R.drawable.edittext_bkg);
            editText.setTextColor(ContextCompat.getColor(this, R.color.text_color));
            passenger_layout.addView(editText);
        }
    }

    private void setupRequestButton() {
        request_button.setOnClickListener(v -> {
            String pickup_location = pickup.getText().toString();
            String dropoff_location = dropoff.getText().toString();
            String time = time_selected.getText().toString();
            String date = date_selected.getText().toString();
            String purpose = purpose_of_ride.getText().toString();
            String project_type = project.getText().toString();
            String vehicleType = vehicle_type.getText().toString();
            String no_of_passengers = num_of_riders_edit_text.getText().toString();
            email_id = getIntent().getStringExtra("email");

            if (time.isEmpty() || date.isEmpty() || purpose.isEmpty() || no_of_passengers.isEmpty()) {
                Toast.makeText(Request_ride.this, "All fields need to be filled", Toast.LENGTH_SHORT).show();
            } else {
                saveDetails(pickup_location, dropoff_location, time, date, purpose, no_of_passengers, email_id, passengerMap, project_type, vehicleType);
            }
        });
    }

    private void fetchRequesterName(String email, OnNameFetchedListener listener) {
        DatabaseReference ref = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        ref.orderByChild("Official Email ID").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("Employee Name").getValue(String.class);
                            listener.onNameFetched(name);
                            return;
                        }
                        listener.onNameFetched(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onNameFetched(null);
                    }
                });
    }

    public interface OnNameFetchedListener {
        void onNameFetched(String name);
    }

    //Add this method to trigger email using Retrofit
    private void sendEmailNotification(String requesterEmail, String approverEmail, String requesterName) {
        MailRequest request = new MailRequest(requesterEmail, approverEmail, requesterName);
        mailService.sendMail(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MailService", "Emails sent successfully");
                } else {
                    Log.e("MailService", "Failed with status: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("MailService", "Error sending email", t);
            }
        });
    }

    private void saveDetails(String pickupLocation, String dropoffLocation, String time, String date, String purpose, String no_of_passengers, String email, Map<String, String> passengerNames, String project, String vehicle_type) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference requestDetailsRef = database.getReference("Request_details");
        DatabaseReference lastIdRef = database.getReference("Request_Counter");

        lastIdRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Integer lastId = task.getResult().getValue(Integer.class);
                if (lastId == null) lastId = 1;
                int finalNewId = lastId + 1;

                // Create a new request with updated ID
                RideRequest request = new RideRequest(finalNewId, pickupLocation, dropoffLocation, time, date, purpose, no_of_passengers, email, passengerNames, project, vehicle_type);
                requestDetailsRef.child(String.valueOf(finalNewId)).setValue(request)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Request_ride.this, "Ride Requested", Toast.LENGTH_SHORT).show();
                            clearFields();

                            // Update the counter in Firebase after saving
                            lastIdRef.setValue(finalNewId)
                                    .addOnSuccessListener(unused -> {
                                        fetchApproverEmail(email, approverEmail -> {
                                            if (approverEmail != null) {
                                                fetchApproverToken(approverEmail, approverToken -> {
                                                    if (approverEmail != null) {
                                                        fetchRequesterName(email, requesterName -> {
                                                            if (requesterName != null) {
                                                                sendEmailNotification(email, approverEmail, requesterName);
                                                            }
                                                        });
                                                    } else {
                                                        Toast.makeText(Request_ride.this, "Approver FCM token not found", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(Request_ride.this, "Approver email not found", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(Request_ride.this, "Failed to update request counter: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(Request_ride.this, "Failed to save request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(Request_ride.this, "Error retrieving request_counter", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Modified to fetch only approver token
    private void fetchApproverToken(String approverEmail, OnTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        usersRef.orderByChild("email").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class);
                    if (token != null) {
                        Log.d(TAG, "Found approver token: " + token);
                    }
                }
                if (token == null) {
                    Log.e(TAG, "No token found for approver email: " + approverEmail);
                }
                listener.onTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch approver token: " + databaseError.getMessage());
                listener.onTokenFetched(null);
            }
        });
    }

    // Modified to fetch only requester token
    private void fetchRequesterToken(String email, OnTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class);
                    if (token != null) {
                        Log.d(TAG, "Found requester token: " + token);
                    }
                }
                if (token == null) {
                    Log.e(TAG, "No token found for requester email: " + email);
                }
                listener.onTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch requester token: " + databaseError.getMessage());
                listener.onTokenFetched(null);
            }
        });
    }

    public interface OnTokenFetchedListener {
        void onTokenFetched(String token);
    }

    private void sendLocalNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not granted, skipping notification.");
                return; // Don't send notification if permission is not granted
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // New method to send FCM notifications using Retrofit
    private void sendFCMNotifications(int requestId, String requesterToken, String approverToken) {
        // Notification messages
        String titleRequester = "Ride Requested";
        String messageRequester = "Your ride request with ID " + requestId + " has been submitted.";
        String titleApprover = "Request Approval Pending";
        String messageApprover = "New ride request (ID: " + requestId + ") is pending your approval.";

        // Send to requester
        sendNotificationWithRetrofit(requesterToken, titleRequester, messageRequester);

        // Send to approver
        sendNotificationWithRetrofit(approverToken, titleApprover, messageApprover);

        // Local fallback notifications (optional)
        sendLocalNotification(titleRequester, messageRequester);
    }

    private void sendNotificationWithRetrofit(String token, String title, String message) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot send notification: token is null or empty");
            return;
        }

        // Create the request body with the correct format for your backend
        FCMRequest request = new FCMRequest(token, title, message);

        // Log the request for debugging
        Log.d(TAG, "Sending FCM notification to token: " + token);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Message: " + message);

        // Make the API call with retry logic
        sendNotificationWithRetry(request, 0);
    }

    // 6. Add this retry method to handle timeouts
    private void sendNotificationWithRetry(FCMRequest request, final int retryCount) {
        final int MAX_RETRIES = 3;

        fcmService.sendNotification(request).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification sent successfully: " + response.code());
                    if (response.body() != null) {
                        Log.d(TAG, "Success: " + response.body().success);
                    }
                } else {
                    Log.e(TAG, "Failed to send notification: " + response.code());
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    // Retry on server errors if we haven't exceeded max retries
                    if (retryCount < MAX_RETRIES && (response.code() >= 500 || response.code() == 429)) {
                        int nextRetryDelay = 1000 * (retryCount + 1); // Exponential backoff
                        new Handler().postDelayed(() -> {
                            Log.d(TAG, "Retrying notification sending, attempt " + (retryCount + 1));
                            sendNotificationWithRetry(request, retryCount + 1);
                        }, nextRetryDelay);
                    }
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                Log.e(TAG, "Error sending notification", t);

                // Retry on network failures if we haven't exceeded max retries
                if (retryCount < MAX_RETRIES) {
                    int nextRetryDelay = 1000 * (retryCount + 1); // Exponential backoff
                    new Handler().postDelayed(() -> {
                        Log.d(TAG, "Retrying notification sending after failure, attempt " + (retryCount + 1));
                        sendNotificationWithRetry(request, retryCount + 1);
                    }, nextRetryDelay);
                }
            }
        });
    }


    private void saveNotificationData(int requestId, String approverEmail) {
        DatabaseReference notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");

        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new Date(currentTimeMillis));

        String message = "A new ride request has been submitted with ID: " + requestId;
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("approver_email", approverEmail);
        notificationData.put("message", message);
        notificationData.put("request_id", requestId);
        notificationData.put("status", "pending");
        notificationData.put("timestamp", formattedTime);
        notificationData.put("title", "Ride Request Pending");
        notificationData.put("requester_email", email_id);

        Log.d(TAG, "Saving notification data for request ID: " + requestId);

        notificationRef.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification: " + e.getMessage()));
    }

    private void fetchApproverEmail(String employeeEmail, OnApproverFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        usersRef.orderByChild("Official Email ID").equalTo(employeeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String approverEmail = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    approverEmail = snapshot.child("Email ID of Approver").getValue(String.class);
                }
                if (approverEmail != null) {
                    Log.d(TAG, "Found approver email: " + approverEmail);
                } else {
                    Log.e(TAG, "No approver email found for: " + employeeEmail);
                }
                listener.onApproverFetched(approverEmail);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch approver email: " + databaseError.getMessage());
                listener.onApproverFetched(null);
            }
        });
    }

    public interface OnApproverFetchedListener {
        void onApproverFetched(String approverEmail);
    }

    private void clearFields() {
        pickup.setText("");
        dropoff.setText("");
        time_selected.setText("");
        date_selected.setText("");
        purpose_of_ride.setText("");
        project.setText("");
        vehicle_type.setText("");
        people_count.setText("0");
        num_of_riders_edit_text.setText("0");
        passenger_name_display.setText("");
        passenger_name_display.setVisibility(View.GONE);

        // Reset the toggle button state
        select_toggle_button.setChecked(false);

        // Clear passenger map
        passengerMap.clear();

        // Hide passenger layout
        main_passenger_layout.setVisibility(View.GONE);
        passengers_details.setVisibility(View.GONE);

        // Show default count layout
        num_of_people_horizontal_layout.setVisibility(View.VISIBLE);
        num_of_riders_edit_text.setVisibility(View.GONE);

        // Clear any generated passenger fields
        passenger_layout.removeAllViews();
    }

    // Class to represent a ride request
    public static class RideRequest {
        private int id;
        private String pickupLocation;
        private String dropoffLocation;
        private String time;
        private String date;
        private String purpose;
        private String numberOfPassengers;
        private String requesterEmail;
        private Map<String, String> passengerNames;
        private String project;
        private String vehicleType;
        private String status;
        private long timestamp;

        // Default constructor required for Firebase
        public RideRequest() {
        }

        public RideRequest(int id, String pickupLocation, String dropoffLocation, String time,
                           String date, String purpose, String numberOfPassengers,
                           String requesterEmail, Map<String, String> passengerNames,
                           String project, String vehicleType) {
            this.id = id;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.time = time;
            this.date = date;
            this.purpose = purpose;
            this.numberOfPassengers = numberOfPassengers;
            this.requesterEmail = requesterEmail;
            this.passengerNames = passengerNames;
            this.project = project;
            this.vehicleType = vehicleType;
            this.status = "pending";
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getPickupLocation() {
            return pickupLocation;
        }

        public void setPickupLocation(String pickupLocation) {
            this.pickupLocation = pickupLocation;
        }

        public String getDropoffLocation() {
            return dropoffLocation;
        }

        public void setDropoffLocation(String dropoffLocation) {
            this.dropoffLocation = dropoffLocation;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public String getNumberOfPassengers() {
            return numberOfPassengers;
        }

        public void setNumberOfPassengers(String numberOfPassengers) {
            this.numberOfPassengers = numberOfPassengers;
        }

        public String getRequesterEmail() {
            return requesterEmail;
        }

        public void setRequesterEmail(String requesterEmail) {
            this.requesterEmail = requesterEmail;
        }

        public Map<String, String> getPassengerNames() {
            return passengerNames;
        }

        public void setPassengerNames(Map<String, String> passengerNames) {
            this.passengerNames = passengerNames;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getVehicleType() {
            return vehicleType;
        }

        public void setVehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    // Handle back button press to navigate to previous activity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        String email = getIntent().getStringExtra("email");
        Intent intent = new Intent(Request_ride.this, Request_ride.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    // Method to check if we need to resubscribe to FCM topic when app resumes
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        // Update token in Firebase for the current user if needed
                        if (email_id != null && !email_id.isEmpty()) {
                            updateUserFCMToken(email_id, token);
                        }
                    }
                });
    }

    private void updateUserFCMToken(String email, String token) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().child("fcm_token").setValue(token)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token", e));
                    break; // Update only the first match
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error updating FCM token: " + databaseError.getMessage());
            }
        });
    }
}