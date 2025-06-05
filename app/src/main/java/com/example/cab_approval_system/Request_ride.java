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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class Request_ride extends AppCompatActivity {

    private ImageButton time_picker_button, date_picker_button, decrease_button, increase_button;
    private TextView time_selected, date_selected, people_count, num_of_riders_edit_text,
            passengers_details, passenger_name_display, request_heading;
    private View num_of_people_horizontal_layout;
    private ToggleButton select_toggle_button;
    private Button request_button,save_button;
    private EditText pickup, dropoff,purpose_of_ride,project,vehicle_type;
    private String email_id;
    private LinearLayout passenger_layout, main_passenger_layout, source_layout,
            destination_layout, time_picker_layout, date_picker_layout, purpose_layout,
            outer_num_of_passenger_layout, inner_num_of_passenger_layout, passengerdetails_layout,project_layout, vehicleType_layout;
    private int passenger_count;
    Map<String, String> passengerMap = new HashMap<>();
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final String FCM_SERVER_KEY = "f50cc0f02ff73c102676121cf85d8b66d9b0813f";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_ride);


        request_heading = findViewById(R.id.request_heading);
        source_layout =  findViewById(R.id.source_layout);
        destination_layout =  findViewById(R.id.destination_layout);
        time_picker_layout = findViewById(R.id.time_picker_layout);
        date_picker_layout = findViewById(R.id.date_picker_layout);
        purpose_layout = findViewById(R.id.purpose_layout);
        outer_num_of_passenger_layout =  findViewById(R.id.outer_num_of_passenger_layout);
        inner_num_of_passenger_layout =  findViewById(R.id.inner_num_of_passenger_layout);
        passengerdetails_layout = findViewById(R.id.passengerdetails_layout);
        project_layout = findViewById(R.id.project_layout);
        vehicleType_layout = findViewById(R.id.vehicleType_layout);

        initializeUI();

        // methods for specific functionalities in UI
        setupDateTimePickers();
        setupCountButtons();
        setupToggleButton();
        setupRequestButton();

        View[] blurView = {request_heading, passenger_layout, source_layout, destination_layout, time_picker_layout, date_picker_layout, purpose_layout,project_layout,outer_num_of_passenger_layout,
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
                        Log.d("FirebaseMessaging", "Subscribed to ride_requests topic");
                    }
                });
        FirebaseApp.initializeApp(this);
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
    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Cab Approval Notifications";
            String description = "Notifications for ride request approvals";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("cab_approval_notification", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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
        passenger_name_display =  findViewById(R.id.passenger_name_display);
        main_passenger_layout = findViewById(R.id.main_passenger_layout);
        project =  findViewById(R.id.project_edittext);
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

    //on click of select toggle button the number selected in edit text gets displayed in a text view and the same will be used to display edit texts to enter passenger names.
    //on click of change toggle button we can edit the number of passengers and again enter passenger names as per the number entered.
    // calling a function to dynamically generate edit texts to enter passenger names as per number selected.
    //managing the visibility of buttons and layouts as per the requirements.

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

        //on click of save button the names entered in the edit texts gets saved and is viewed in a plane text.
        // passenger names are stored in table under attribute name passengerNames as subfield.

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

//method to generate edit texts dynamically as per the number of passengers being selected.

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

//on click of request button the entered details are being stored to request_details table and then is shown in pending request of specific approver.

    private void setupRequestButton() {
        request_button.setOnClickListener(v -> {
            String pickup_location = pickup.getText().toString();
            String dropoff_location = dropoff.getText().toString();
            String time = time_selected.getText().toString();
            String date = date_selected.getText().toString();
            String purpose = purpose_of_ride.getText().toString();
            String project_type =  project.getText().toString();
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

    // saving the details to request_details on click of request button and auto increment of request_counter
    //approver email is being fetched and also using fcm token sending notification to that specific approver for approval.
    //the approver on logging in can see the pending approvals as a dot icon in their app.

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
                                                    if (approverToken != null) {
                                                        fetchDeviceIdByEmail(approverEmail, approverDeviceId -> {
                                                            fetchRequesterToken(email, requesterToken -> {
                                                                if (requesterToken != null) {
                                                                    fetchDeviceIdByEmail(email, requesterDeviceId -> {
                                                                        // ✅ All tokens and device IDs fetched, proceed
                                                                        Log.d("FCM", "Approver: Token=" + approverToken + ", DeviceID=" + approverDeviceId);
                                                                        Log.d("FCM", "Requester: Token=" + requesterToken + ", DeviceID=" + requesterDeviceId);

                                                                        saveNotificationData(finalNewId, approverEmail);
                                                                        new Handler().postDelayed(() -> {
                                                                                  // Finish the current activity to prevent going back to it
                                                                        sendFCMNotification(finalNewId, requesterToken, requesterDeviceId, approverToken, approverDeviceId);
                                                                        }, 1000);
                                                                    });
                                                                } else {
                                                                    Toast.makeText(Request_ride.this, "Requester FCM token not found", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
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

    private void fetchDeviceIdByEmail(String email, OnDeviceIdFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String deviceId = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    deviceId = snapshot.child("device_id").getValue(String.class); // Check exact field name
                }
                listener.onDeviceIdFetched(deviceId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch Device ID: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface OnDeviceIdFetchedListener {
        void onDeviceIdFetched(String deviceId);
    }



    private void fetchRequesterToken(String email,  OnTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");// Firebase doesn't allow dots in keys

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class);
                }
                listener.onTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch FCM token: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void fetchApproverToken(String approverEmail, OnTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");  // Assuming you have a 'Users' table where the FCM token is stored

        usersRef.orderByChild("email").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class);
                }
                listener.onTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch FCM token: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnTokenFetchedListener {
        void onTokenFetched(String token);
    }

    private void sendLocalNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Notification", "Permission not granted, skipping notification.");
                return; // Don't send notification if permission is not granted
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "cab_approval_notification")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void sendFCMNotification(int requestId, String requesterToken, String requesterDeviceId, String approverToken, String approverDeviceId) {
        String title = "Ride Request Update";
        String message = "Ride request ID " + requestId + " needs your attention.";

        List<String> tokens = new ArrayList<>();
        tokens.add(requesterToken);
        tokens.add(approverToken);

        sendNotificationToMultipleDevices(tokens, title, message);

        sendLocalNotification(title, message);  // Optional Local Notification
    }

    private void sendNotificationToMultipleDevices(List<String> tokens, String title, String message) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://cab-notification-service.onrender.com/") // ✅ your Render server URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NotificationAPI notificationAPI = retrofit.create(NotificationAPI.class);

        Map<String, String> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("body", message);


        NotificationBody body = new NotificationBody(tokens, notificationData);

        Call<Void> call = notificationAPI.sendNotification(body);

        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("RetrofitFCM", "Notification sent successfully");
                } else {
                    Log.e("RetrofitFCM", "Failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("RetrofitFCM", "Error sending notification: " + t.getMessage());
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
        notificationData.put("requester_email",email_id);

        Log.d("ARequest_id"," " + requestId);

        notificationRef.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> Log.d("Notification", "Notification saved successfully"))
                .addOnFailureListener(e -> Log.e("Notification", "Failed to save notification: " + e.getMessage()));
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
                listener.onApproverFetched(approverEmail);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch approver email: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
        num_of_riders_edit_text.setText("0");
        people_count.setText("0");
        select_toggle_button.setChecked(false);
        purpose_of_ride.setText("");
        project.setText("");
        vehicle_type.setText("");
    }
    public class RideRequest {
        private int id;
        private String pickupLocation;
        private String dropoffLocation;
        private String time;
        private String date;
        private String purpose;
        private String no_of_passengers ;
        private String email;
        private String vehicle_type;
        private Map<String, String> passengerNames;
        String project_type;

        public RideRequest(){
        }

        public RideRequest(int id, String pickupLocation, String dropoffLocation, String time, String date, String purpose, String no_of_passengers, String email, Map<String, String> passengerNames, String project_type, String vehicle_type) {
            this.id = id;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.time = time;
            this.date = date;
            this.purpose = purpose;
            this.no_of_passengers  = no_of_passengers;
            this.email = email;
            this.passengerNames = passengerNames;
            this.project_type = project_type;
            this.vehicle_type =  vehicle_type;
        }

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
        public String getDropoffLocation() { return dropoffLocation; }
        public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getNo_of_passengers() { return no_of_passengers ; }
        public void setNo_of_passengers(String no_of_passengers) { this.no_of_passengers  = no_of_passengers; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Map<String, String> getPassengerNames() { return passengerNames; }
        public void setPassengerNames(Map<String, String> passengerNames) { this.passengerNames = passengerNames; }
        public String getProject_type(){return project_type;};
        public void setProject_type(){this.project_type = project_type;}
        public String getVehicle_type(){return vehicle_type;};
        public void setVehicle_type(){this.vehicle_type = vehicle_type;}
    }

}