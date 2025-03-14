package com.example.cab_approval_system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String ARG_TEAM = "team";
    private String requesterTeam;
    private History_adapter adapter;
    private final List<RequestModel> approvedRequestsList = new ArrayList<>();
    private final HashMap<String, String> employeeTeamMap = new HashMap<>();

    public static ApprovedByFunctionalHeadFragment newInstance(String team) {
        ApprovedByFunctionalHeadFragment fragment = new ApprovedByFunctionalHeadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterTeam = getArguments().getString(ARG_TEAM);
            Log.d("ApprovedFH", "Team: " + requesterTeam);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_department_history, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), approvedRequestsList);
        recyclerView.setAdapter(adapter);

        fetchApprovedRequests();

        return view;
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
                approvedRequestsList.clear();
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
                            if (request != null) {
                                approvedRequestsList.add(request);
                                adapter.updateList(approvedRequestsList);
                            }
                        }
                    }
                }

                Log.d("ApprovedFH", "Total Approved Requests for Team " + requesterTeam + ": " + approvedRequestsList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching approved requests", error.toException());
            }
        });
    }

}