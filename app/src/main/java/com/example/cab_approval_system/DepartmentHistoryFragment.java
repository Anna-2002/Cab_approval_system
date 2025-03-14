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
import java.util.List;

public class DepartmentHistoryFragment extends Fragment {

    private static final String ARG_TEAM = "team";
    //adding email because its needed in fetchData() function
    private static final String ARG_EMAIL = "email";
    private String requesterTeam;
    private String requesterEmail;

    private History_adapter adapter;
    private final List<RequestModel> departmentRequestList = new ArrayList<>();

    public static DepartmentHistoryFragment newInstance(String team,String email) {
        DepartmentHistoryFragment fragment = new DepartmentHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM, team);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterTeam = getArguments().getString(ARG_TEAM);
            requesterEmail = getArguments().getString(ARG_EMAIL);
            Log.d("DepartmentHistory", "Team: " + requesterTeam);
            Log.d("DepartmentHistory", "Requester Email: " + requesterEmail);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_department_history, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), departmentRequestList);
        recyclerView.setAdapter(adapter);  // Set adapter first


        fetchData();

        return view;
    }

    private void fetchData() {
        DatabaseReference sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        sheetRef.orderByChild("Team").equalTo(requesterTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> teamEmails = new ArrayList<>();
                        String deptHeadEmail = null;
                        boolean isHR = false;

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            String email = dataSnapshot.child("Official Email ID").getValue(String.class);
                            String designation = dataSnapshot.child("Approval Matrix").getValue(String.class);

                            if (email != null) {
                                teamEmails.add(email);
                            }
                            if (designation != null && designation.equals("FH")) {
                                deptHeadEmail = email;
                            }
                            Log.d("DeptHis","email: "+email);
                            Log.d("DeptHis","requester email: "+requesterEmail);
                            if (designation != null && designation.equals("HR Head") && requesterEmail != null) {
                                isHR = true;
                            }
                        }

                        Log.d("DepartmentHistory", "Team members count: " + teamEmails.size());
                        Log.d("DepartmentHistory", "Department Head Email: " + deptHeadEmail);
                        Log.d("DepartmentHistory", "Is HR: " + isHR);

                        // Now fetch Approved_requests filtered by these emails
                        fetchApprovedRequests(teamEmails,deptHeadEmail,isHR);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Error fetching team members", error.toException());
                    }
                });
    }

    private void fetchApprovedRequests(List<String> teamEmails, String deptHeadEmail, boolean isHR) {
        Log.d("DepartmentHistory", "isHR: " + isHR + ", DeptHeadEmail: " + deptHeadEmail);
        DatabaseReference approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");

        approvedRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentRequestList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String requesterEmail = dataSnapshot.child("Emp_email").getValue(String.class);

                    if (requesterEmail != null) {
                        if (teamEmails.contains(requesterEmail)) {
                            // Include FH's requests if the requester is HR
                            if (isHR || (deptHeadEmail == null || !requesterEmail.equals(deptHeadEmail))) {
                                RequestModel request = dataSnapshot.getValue(RequestModel.class);
                                if (request != null) {
                                    departmentRequestList.add(request);
                                    adapter.updateList(departmentRequestList);
                                }
                            }
                        }
                    }
                }

                Log.d("DepartmentHistory", "Final Approved Requests Size: " + departmentRequestList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching approved requests", error.toException());
            }
        });
    }



}