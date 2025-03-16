package com.example.cab_approval_system;

import android.os.Bundle;
import android.util.Log;
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
import java.util.List;

public class Cab_request extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CabRequestAdapter adapter;
    private List<CabRequestModel> requestList;
    private DatabaseReference approvedRequestsRef, sheet1Ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cab_request);

        recyclerView = findViewById(R.id.cabRequest_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestList = new ArrayList<>();
        adapter = new CabRequestAdapter(this, requestList);
        recyclerView.setAdapter(adapter);

        approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");
        sheet1Ref = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        fetchApprovedRequests();
    }

    private void fetchApprovedRequests() {
        approvedRequestsRef.orderByChild("rideAssigned").equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String requestId = requestSnapshot.getKey();

                            if (requestId == null || requestId.isEmpty()) {
                                continue;
                            }

                            String empName = requestSnapshot.child("Emp_name").getValue(String.class);
                            String empEmail = requestSnapshot.child("Emp_email").getValue(String.class);
                            String source = requestSnapshot.child("Source").getValue(String.class);
                            String destination = requestSnapshot.child("Destination").getValue(String.class);
                            String rideDate = requestSnapshot.child("Date").getValue(String.class);
                            String rideTime = requestSnapshot.child("Time").getValue(String.class);
                            String numberOfPassengers = requestSnapshot.child("no_of_passengers").getValue(String.class);

                            if (empName == null) empName = "N/A";
                            if (empEmail == null) empEmail = "N/A";
                            if (source == null) source = "Unknown";
                            if (destination == null) destination = "Unknown";
                            if (rideDate == null) rideDate = "Not Set";
                            if (rideTime == null) rideTime = "Not Set";
                            if (numberOfPassengers == null) numberOfPassengers = "1"; // Default to 1


                            if (empName != null && empEmail != null) {
                                fetchMobileNumber(requestId, empName, empEmail, source, destination, rideDate, rideTime, numberOfPassengers);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Cab_request.this, "Failed to fetch requests: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchMobileNumber(String requestId, String empName, String empEmail, String source, String destination, String rideDate, String rideTime, String numberOfPassengers) {
        String normalizedEmail = empEmail.trim().toLowerCase();
        Log.d("Emp email", "Searching for: " + normalizedEmail);

        sheet1Ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mobileNumber = "Not Found";
                boolean matchFound = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String storedEmail = userSnapshot.child("Official Email ID").getValue(String.class);

                    if (storedEmail != null) {
                        String cleanedStoredEmail = storedEmail.trim().toLowerCase();

                        if (cleanedStoredEmail.equals(normalizedEmail)) {
                            Long mobileLong = userSnapshot.child("MobileNo").getValue(Long.class);
                            mobileNumber = (mobileLong != null) ? String.valueOf(mobileLong) : "Not Found";
                            matchFound = true;
                            break;
                        }
                    }
                }

                if (!matchFound) {
                    Log.d("MATCH_FAILED", "No exact match for: " + normalizedEmail);
                }

                // Store fetched data in the model (Driver details are initially empty)
                CabRequestModel request = new CabRequestModel(requestId, empName, empEmail, mobileNumber, source, destination, rideDate, rideTime, numberOfPassengers, "", "", "");
                requestList.add(request);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching data: " + error.getMessage());
            }
        });
    }
}