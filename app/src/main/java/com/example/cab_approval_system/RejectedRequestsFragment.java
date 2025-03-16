package com.example.cab_approval_system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import java.util.List;
import java.util.Objects;

public class RejectedRequestsFragment extends Fragment {

    private static final String ARG_EMAIL = "email";
    private static final String ARG_TEAM = "team";
    private String requesterEmail;
    private String requesterTeam;

    private RecyclerView recyclerView;
    private Rejected_history_adapter adapter;
    private List<RequestModel> rejectedRequestList = new ArrayList<>();

    private DatabaseReference databaseReference;

    public static RejectedRequestsFragment newInstance(String email, String team) {
        RejectedRequestsFragment fragment = new RejectedRequestsFragment();
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
        }
        Log.d("RejectedRequests", "Requester Email: " + requesterEmail);
        Log.d("RejectedRequests", "Requester Team: " + requesterTeam);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_rejected_requests_fragment, container, false);

        EditText searchBar = view.findViewById(R.id.search_bar);

        recyclerView = view.findViewById(R.id.item_rejected_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Rejected_history_adapter(getContext(), rejectedRequestList);
        recyclerView.setAdapter(adapter);

        fetchData();

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
        List<RequestModel> filteredList = new ArrayList<>();

        for (RequestModel request : rejectedRequestList) {
            if (request.getEmpName() != null && request.getEmpId() != null) {
                if (request.getEmpName().toLowerCase().contains(query.toLowerCase()) ||
                        request.getEmpId().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(request);
                }
            }
        }

        adapter.updateSearchList(filteredList); // Update RecyclerView
    }

    private void fetchData() {
        if (requesterEmail == null) {
            Log.e("RejectedRequests", "Error: Requester Email is NULL!");
            return;
        }

        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Rejected_requests");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rejectedRequestList.clear();

                isTeamMember(requesterEmail, (isSameTeam, isFH, requesterTeam) -> {
                    for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                        Log.d("FirebaseData", "Snapshot: " + requestSnapshot.getValue());
                        RequestModel request = requestSnapshot.getValue(RequestModel.class);
                        if (request == null || request.getEmpEmail() == null) {
                            Log.e("RejectedRequests", "Skipping null request or email.");
                            continue;
                        }

                        if (request.getEmpEmail().equalsIgnoreCase(requesterEmail)) {
                            // Employee should see only their own requests
                            rejectedRequestList.add(request);
                        } else if (isFH) {
                            // FH should only see requests from their department
                            isTeamMember(request.getEmpEmail(), (requestIsSameTeam, r, requestTeam) -> {
                                if (requestIsSameTeam && requestTeam.equals(requesterTeam)) {
                                    rejectedRequestList.add(request);
                                }
                                adapter.updateList(rejectedRequestList);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    }
                    // Update adapter once after processing all requests
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching rejected requests", error.toException());
            }
        });
    }



    private void isTeamMember(String email, OnTeamCheckListener listener) {
        DatabaseReference sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        sheetRef.orderByChild("Official Email ID").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            listener.onCheck(false, false, null);  // No matching email found
                            return;
                        }

                        String userTeam = null;
                        boolean isFH = false;

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            userTeam = dataSnapshot.child("Team").getValue(String.class);
                            String role = dataSnapshot.child("Approval Matrix").getValue(String.class);
                            isFH = role != null && role.equalsIgnoreCase("FH");
                            break; // Only need the first match
                        }

                        if (userTeam == null) {
                            listener.onCheck(false, false, null);
                            return;
                        }

                        // Check if the user is in the requester's team
                        listener.onCheck(userTeam.equals(requesterTeam), isFH, userTeam);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Error fetching user team", error.toException());
                        listener.onCheck(false, false, null);
                    }
                });
    }

    // Updated Interface
    public interface OnTeamCheckListener {
        void onCheck(boolean isSameTeam, boolean isFH, String userTeam);
    }

}