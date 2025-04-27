package com.example.cab_approval_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pending_approvals extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Recycler_adapter recyclerAdapter;
    private List<RequestModel> requestList;
    private Map<String, RequestModel> requestMap;
    private DatabaseReference requestRef, notificationRef, sheetRef;
    private String approverEmail,passedEmail,userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approvals);


        Intent intent = getIntent();
        String passedEmail = intent.getStringExtra("email");
        String userRole = intent.getStringExtra("userRole");
        ImageView notificationDot = Home_page.getNotificationDot();

        recyclerView = findViewById(R.id.pending_approvals_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get passed data (email and role)


        if ("HR Head".equals(userRole) || "FH".equals(userRole)) {
            approverEmail =passedEmail;
        }

        requestList = new ArrayList<>();
        requestMap = new HashMap<>();
        recyclerAdapter = new Recycler_adapter(this, requestList,approverEmail,userRole,notificationDot);
        recyclerView.setAdapter(recyclerAdapter);

        requestRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Request_details");
        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        fetchPendingRequests();
    }

    //in pending approvals we can see all the details along with employee details who requested the ride along with a approve chip on click of which the status gets changed from pending to approved.

    private void fetchPendingRequests() {
        notificationRef.orderByChild("approver_email").equalTo(approverEmail) // Filter by approver's email
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                String requestId = String.valueOf(requestSnapshot.child("request_id").getValue(Long.class));
                                String requesterEmail = requestSnapshot.child("requester_email").getValue(String.class);
                                if (requestId != null && requesterEmail != null && !requestMap.containsKey(requestId)) {
                                    fetchRequestDetails(requestId, requesterEmail);
                                }
                            }
                        } else {
                            Toast.makeText(Pending_approvals.this, "No pending requests found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Error fetching pending requests.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void fetchRequestDetails(String requestId, String requesterEmail) {
        requestRef.child(requestId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            RequestModel request = new RequestModel();
                            request.setRequestId(requestId);
                            request.setPickupLocation(snapshot.child("pickupLocation").getValue(String.class));
                            request.setDropoffLocation(snapshot.child("dropoffLocation").getValue(String.class));
                            request.setDate(snapshot.child("date").getValue(String.class));
                            request.setPurpose(snapshot.child("purpose").getValue(String.class));
                            request.setProjectType(snapshot.child("project_type").getValue(String.class));
                            request.setTime(snapshot.child("time").getValue(String.class));
                            request.setStatus(snapshot.child("status").getValue(String.class));

                            DataSnapshot passengerSnapshot = snapshot.child("passengerNames");
                            if (passengerSnapshot.exists()) {
                                Map<String, String> passengerMap = new HashMap<>();
                                for (DataSnapshot passenger : passengerSnapshot.getChildren()) {
                                    passengerMap.put(passenger.getKey(), passenger.getValue(String.class));
                                }
                                request.setPassengerMap(passengerMap);
                            }

                            String noOfPassengers = snapshot.child("no_of_passengers").getValue(String.class);
                            request.setNoOfPassengers(noOfPassengers != null ? noOfPassengers: null);

                            requestMap.put(requestId, request);
                            requestList.add(request);

                            // Correct method call passing the RequestModel
                            fetchEmployeeDetails(requesterEmail, request);

                            recyclerAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Failed to fetch request details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchEmployeeDetails(String email, RequestModel request) {
        sheetRef.orderByChild("Official Email ID").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                String employeeName = data.child("Employee Name").getValue(String.class);
                                Long emp_Id = data.child("Emp ID").getValue(Long.class);
                                String empId = emp_Id != null ? String.valueOf(emp_Id) : "N/A";

                                request.setEmpName(employeeName != null ? employeeName : "N/A");
                                request.setEmpId(empId);
                                request.setEmpEmail(email);

                                recyclerAdapter.notifyDataSetChanged();
                                break;
                            }
                        } else {
                            Toast.makeText(Pending_approvals.this, "Employee details not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Failed to fetch employee details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}