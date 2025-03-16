package com.example.cab_approval_system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class PersonalHistoryFragment extends Fragment {

    private static final String ARG_EMAIL = "email";
    private String requesterEmail;

    private RecyclerView recyclerView;
    private History_adapter adapter;
    private List<RequestModel> approvedRequestList = new ArrayList<>();

    private DatabaseReference databaseReference;

    public static PersonalHistoryFragment newInstance(String email) {
        PersonalHistoryFragment fragment = new PersonalHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterEmail = getArguments().getString(ARG_EMAIL);
            Log.d("PersonalHistory", "Requester Email: " + requesterEmail);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_history, container, false);

        EditText searchBar = view.findViewById(R.id.search_bar);
        searchBar.setVisibility(View.GONE);
        recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), approvedRequestList);
        recyclerView.setAdapter(adapter);

        fetchData();

        return view;
    }

    private void fetchData() {
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Approved_requests");
        databaseReference.orderByChild("Emp_email").equalTo(requesterEmail.trim().toLowerCase())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        approvedRequestList.clear();
                        Log.d("PersonalHistory", "Total Requests Found: " + snapshot.getChildrenCount());

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            // Each dataSnapshot represents a request ID (12, 13, 14, etc.)
                            Log.d("PersonalHistory", "Request ID: " + dataSnapshot.getKey());

                            // Now extract the actual request details
                            RequestModel request = dataSnapshot.getValue(RequestModel.class);

                            if (request != null) {
                                approvedRequestList.add(request);
                            }else {
                                Log.e("PersonalHistory", "RequestModel is null for key: " + dataSnapshot.getKey());
                            }
                        }

                        Log.d("PersonalHistory", "Final List Size: " + approvedRequestList.size());

                        // Notify adapter after data is updated
                        adapter.notifyDataSetChanged();
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error fetching data", error.toException());
                    }
                });
    }

}
