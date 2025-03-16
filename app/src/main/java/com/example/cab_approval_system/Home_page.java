package com.example.cab_approval_system;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Home_page extends AppCompatActivity {

    private TextView emp_Name, empID, empTeam, request_ride_text,pending_approvals_text, cab_request_text;
    private DatabaseReference databaseReference, vendorReference;
    private static ImageView notificationDot; // Notification dot
    private String user_email, user_role;
    private LinearLayout emp_name_layout, emp_id_layout, team_Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);


        // Get user details from intent
        user_email = getIntent().getStringExtra("email");
        user_role = getIntent().getStringExtra("userRole");

        Home_Screen.setupBottomNavigation(this, user_email,user_role);

        // Initialize UI elements
        emp_Name = findViewById(R.id.emp_name_fetch);
        empID = findViewById(R.id.emp_id_edit_text);
        empTeam = findViewById(R.id.emp_team_edit_text);
        notificationDot = findViewById(R.id.notification_dot);
        emp_name_layout = findViewById(R.id.emp_name_layout);
        emp_id_layout =  findViewById(R.id.emp_id_layout);
        team_Layout =  findViewById(R.id.team_Layout);
        request_ride_text = findViewById(R.id.request_ride_text);
        pending_approvals_text = findViewById(R.id.pending_approvals_text);
        cab_request_text = findViewById(R.id.cab_request_text);

        // Initialize buttons

        CardView request_ride = findViewById(R.id.request_ride_card);
        CardView pending_approvals = findViewById(R.id.pending_approvals_card);
        CardView cab_request = findViewById(R.id.cab_request_card);

        // Ensure user_role is assigned before checking visibility
        updateButtonVisibility(user_role, request_ride,pending_approvals, cab_request, emp_id_layout, team_Layout);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
        vendorReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Vendor_details");

        // Fetch user data & notifications
        if (user_email != null) {
            fetchUserData(user_email);
            checkForUnreadNotifications(user_email);
        } else {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
        }

        // Button click listeners
        request_ride.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Request_ride.class);
            i.putExtra("email", user_email);
            startActivity(i);
        });

        pending_approvals.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Pending_approvals.class);
            i.putExtra("email", user_email);
            i.putExtra("userRole", user_role);
            startActivity(i);
        });

        cab_request.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Cab_request.class);
            i.putExtra("email", user_email);
            startActivity(i);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetchUserRoleAndUpdateUI(); // Fetch updated role and set visibility
    }

    public static ImageView getNotificationDot() {
        return notificationDot;
    }

    private void fetchUserRoleAndUpdateUI() {
        DatabaseReference userRoleRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        String modifiedEmail = user_email.replace(".", ",");

        userRoleRef.child(modifiedEmail).child("userRole").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                user_role = task.getResult().getValue(String.class);

                // Now update the button visibility with the latest userRole
                updateButtonVisibility(user_role, findViewById(R.id.request_ride),findViewById(R.id.pending_approvals), findViewById(R.id.cab_request), findViewById(R.id.emp_id_layout), findViewById(R.id.team_Layout));
            }
        });
    }


    private void updateButtonVisibility(String userRole, CardView request_ride,CardView pending_approvals, CardView cab_request, LinearLayout emp_id_layout, LinearLayout team_Layout) {
        if (userRole == null) return; // Prevent null exceptions

        // Handle visibility based on user role
        if ("Employee".equals(userRole)) {
            pending_approvals.setVisibility(View.GONE);
            pending_approvals_text.setVisibility(View.GONE);
            cab_request.setVisibility(View.GONE);
            cab_request_text.setVisibility(View.GONE);
        } else if ("HR Head".equals(userRole) || "FH".equals(userRole)) {
            pending_approvals.setVisibility(View.VISIBLE);
            pending_approvals_text.setVisibility(View.VISIBLE);
            cab_request_text.setVisibility(View.GONE);
            cab_request.setVisibility(View.GONE);
        }else if("Vendor".equals(userRole)){
            request_ride.setVisibility(View.GONE);
            request_ride_text.setVisibility(View.GONE);
            pending_approvals.setVisibility(View.GONE);
            pending_approvals_text.setVisibility(View.GONE);
            cab_request.setVisibility(View.VISIBLE);
            cab_request_text.setVisibility(View.VISIBLE);
            emp_id_layout.setVisibility(View.GONE);
            team_Layout.setVisibility(View.GONE);

        }
    }

    private void fetchUserData(String email) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
            return;
        }


        if ("Vendor".equals(user_role)) {
            // Fetch from Vendor table if the user is a vendor
            vendorReference.orderByChild("email_id").equalTo(email)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                                String vendorName = snapshot.child("name").getValue(String.class);
                                if (vendorName != null) {
                                    emp_Name.setText(vendorName);
                                } else {
                                    emp_Name.setText("Vendor");
                                }
                            }
                        } else {
                            Toast.makeText(Home_page.this, "Vendor details not found!", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Fetch from Sheet1 for regular employees
            databaseReference.orderByChild("Official Email ID").equalTo(email)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                                String empId = String.valueOf(snapshot.child("Emp ID").getValue());
                                String empName = String.valueOf(snapshot.child("Employee Name").getValue());
                                String team = String.valueOf(snapshot.child("Team").getValue());

                                empID.setText(empId);
                                emp_Name.setText(empName);
                                empTeam.setText(team);
                            }
                        } else {
                            Toast.makeText(Home_page.this, "User details not found!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkForUnreadNotifications(String approverEmail) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");

        notificationsRef.orderByChild("approver_email").equalTo(approverEmail)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean hasUnreadNotifications = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String status = snapshot.child("status").getValue(String.class);
                            if ("pending".equals(status)) {
                                hasUnreadNotifications = true;
                                break;
                            }
                        }
                        if (notificationDot != null) {
                            notificationDot.setVisibility(hasUnreadNotifications ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        Toast.makeText(Home_page.this, "Failed to check notifications: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}