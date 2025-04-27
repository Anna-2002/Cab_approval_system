package com.example.cab_approval_system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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

public class ApprovedByFunctionalHeadFragment extends Fragment {

    private static final String ARG_TEAM = "team", ARG_EMAIL ="email";
    private String requesterEmail,requesterTeam;
    private History_adapter adapter;
    private final List<RequestModel> approvedRequestsList = new ArrayList<>();
    private final HashMap<String, String> employeeTeamMap = new HashMap<>();

    public static ApprovedByFunctionalHeadFragment newInstance(String email, String team) {
        ApprovedByFunctionalHeadFragment fragment = new ApprovedByFunctionalHeadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_TEAM, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterEmail = getArguments().getString(ARG_EMAIL);
            requesterTeam = getArguments().getString(ARG_TEAM);
            Log.d("ApprovedFH", "Team: " + requesterTeam);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_history, container, false);

        EditText searchBar = view.findViewById(R.id.search_bar);

        RecyclerView recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), approvedRequestsList);
        recyclerView.setAdapter(adapter);

        fetchApprovedRequests();
        Log.d("ApprovedFH", "Fetching pending requests...");
        fetchPendingRequests();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRequests(s.toString()); // Call filter function
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }
    private void filterRequests(String query) {
        new Thread(() -> { // Run filtering in the background
            List<RequestModel> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim(); // Precompute

            if (lowerCaseQuery.isEmpty()) {
                filteredList.addAll(approvedRequestsList); // Restore full list if query is empty
            } else {
                for (RequestModel request : approvedRequestsList) {
                    if (request.getEmpName() != null && request.getEmpId() != null) {
                        String empNameLower = request.getEmpName().toLowerCase();
                        String empIdLower = request.getEmpId().toLowerCase();

                        if (empNameLower.contains(lowerCaseQuery) || empIdLower.contains(lowerCaseQuery)) {
                            filteredList.add(request);
                        }
                    }
                }
            }

            // Update UI on the main thread
            new Handler(Looper.getMainLooper()).post(() -> adapter.updateSearchList(filteredList));
        }).start();
    }


    private void fetchApprovedRequests() {
        DatabaseReference sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        sheetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String empEmail = dataSnapshot.child("Official Email ID").getValue(String.class);
                    String empTeam = dataSnapshot.child("Team").getValue(String.class);

                    if (empEmail != null && empTeam != null) {
                        employeeTeamMap.put(empEmail, empTeam);
                    }
                }

                // Now fetch approved requests and filter based on team
                fetchFilteredApprovedRequests();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching employee teams", error.toException());
            }
        });
    }
    private void fetchFilteredApprovedRequests() {
        DatabaseReference approvedRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_by_FH");

        approvedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //approvedRequestsList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Boolean isApproved = dataSnapshot.child("approvedByFH").getValue(Boolean.class);
                    String empEmail = dataSnapshot.child("Emp_email").getValue(String.class);
                    Log.d("empEmail"," "+empEmail);

                    if (Boolean.TRUE.equals(isApproved) && empEmail != null) {
                        String empTeam = employeeTeamMap.get(empEmail); // Get team from employee data
                        Log.d("empTeam"," "+empTeam);
                        Log.d("requester team"," "+requesterTeam);

                        if (empTeam != null && empTeam.equals(requesterTeam)) {
                            RequestModel request = dataSnapshot.getValue(RequestModel.class);
                            if (request != null && !approvedRequestsList.contains(request)) {
                                approvedRequestsList.add(request);
                            }
                        }
                    }
                }
                //adapter.updateList(approvedRequestsList);
                Log.d("before pending", String.valueOf(approvedRequestsList.size()));

                Log.d("ApprovedFH", "Total Approved Requests for Team " + requesterTeam + ": " + approvedRequestsList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching approved requests", error.toException());
            }
        });
    }
    private void fetchPendingRequests() {
        if (requesterEmail == null) {
            Log.e("FetchPendingRequests", "Requester email is null, cannot fetch pending requests.");
            return;
        }

        DatabaseReference notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");

        Log.d("FetchPendingRequests", "Querying for requester_email: " + requesterEmail);

        notificationRef.orderByChild("requester_email").equalTo(requesterEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("FetchPendingRequests", "Snapshot exists: " + snapshot.exists());
                        approvedRequestsList.clear(); // Clear existing list to avoid duplicates

                        for (DataSnapshot data : snapshot.getChildren()) {
                            RequestModel request = data.getValue(RequestModel.class);

                            if (request != null && "pending".equals(request.getStatus())) {
                                String requestEmpEmail = data.child("requester_email").getValue(String.class);

                                // Ensure the pending request belongs to the same team
                                if (requestEmpEmail != null && employeeTeamMap.containsKey(requestEmpEmail)) {
                                    String requestEmpTeam = employeeTeamMap.get(requestEmpEmail);

                                    if (requestEmpTeam != null && requestEmpTeam.equals(requesterTeam)) {
                                        fetchRequestDetails(request);
                                    } else {
                                        Log.d("FetchPendingRequests", "Skipping request, different team: " + requestEmpTeam);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FetchPendingRequests", "Failed to fetch pending requests", error.toException());
                    }
                });
    }
    private void fetchRequestDetails(RequestModel request) {
        DatabaseReference requestDetailsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Request_details");

        Log.d("pending",request.getRequestId());
        requestDetailsRef.orderByChild("id").equalTo(Integer.parseInt(request.getRequestId()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Log.d("pending",data.toString());
                                // Extract additional fields
                                String emailId = data.child("email").getValue(String.class);
                                Log.d("pending",emailId);
                                String source = data.child("pickupLocation").getValue(String.class);
                                String destination = data.child("dropoffLocation").getValue(String.class);
                                String date = data.child("date").getValue(String.class);
                                String time = data.child("time").getValue(String.class);
                                String purpose = data.child("purpose").getValue(String.class);
                                String project =  data.child("project_type").getValue(String.class);

                                // Set fields in RequestModel
                                request.setEmpEmail(emailId);
                                request.setPickupLocation(source);
                                request.setDropoffLocation(destination);
                                request.setDate(date);
                                request.setTime(time);
                                request.setPurpose(purpose);
                                request.setProjectType(project);

                                Log.d("project_type"," "+request.getProjectType());

                            }
                            fetchEmployeeDetails(request);
                        }
                        else
                            Log.d("pending","snapshot doesn't exist");

                        // Add updated request to the list and refresh UI
                        //approvedRequestsList.add(request);
                        //adapter.notifyDataSetChanged();

                        Log.d("FetchRequestDetails", "Updated request details for ID: " + request.getRequestId());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FetchRequestDetails", "Failed to fetch request details", error.toException());
                    }
                });
    }
    private void fetchEmployeeDetails(RequestModel request) {
        DatabaseReference employeeRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        String emailId = request.getEmpEmail(); // Use the extracted email from Request_details

        Log.d("FetchEmployeeDetails", "Querying Sheet1 for email: " + emailId);

        employeeRef.orderByChild("Official Email ID").equalTo(emailId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                String empName = data.child("Employee Name").getValue(String.class);
                                String empId = String.valueOf(data.child("Emp ID").getValue(Long.class));

                                Log.d("FetchEmployeeDetails", "Found Employee: " + empName + " (ID: " + empId + ")");

                                // Set employee details in RequestModel
                                request.setEmpName(empName);
                                request.setEmpId(empId);
                            }
                        } else {
                            Log.d("FetchEmployeeDetails", "No employee found for email: " + emailId);
                        }

                        // Update the list and refresh UI
                        if (!approvedRequestsList.contains(request)) {
                            approvedRequestsList.add(request);
                            adapter.notifyDataSetChanged();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FetchEmployeeDetails", "Failed to fetch employee details", error.toException());
                    }
                });
    }
}