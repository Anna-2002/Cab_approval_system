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
import java.util.List;

public class AllApprovedRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private History_adapter adapter;
    private List<RequestModel> approvedRequestList = new ArrayList<>();
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_history, container, false);

        EditText searchBar = view.findViewById(R.id.search_bar);

        recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), approvedRequestList);
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
        new Thread(() -> {
            List<RequestModel> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            if (lowerCaseQuery.isEmpty()) {
                // Restore full list WITHOUT modifying `approvedRequestList`
                new Handler(Looper.getMainLooper()).post(() -> {
                    adapter.updateSearchList(approvedRequestList);
                });
                return;
            }

            // Filter without modifying original list
            for (RequestModel request : approvedRequestList) {
                if (request.getEmpName() != null && request.getEmpId() != null) {
                    String empNameLower = request.getEmpName().toLowerCase();
                    String empIdLower = request.getEmpId().toLowerCase();

                    if (empNameLower.contains(lowerCaseQuery) || empIdLower.contains(lowerCaseQuery)) {
                        filteredList.add(request);
                    }
                }
            }

            // Update UI with filtered list
            new Handler(Looper.getMainLooper()).post(() -> adapter.updateSearchList(filteredList));
        }).start();
    }






    private void fetchData() {
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                approvedRequestList.clear();
                Log.d("AllApprovedRequests", "Total Requests Found: " + snapshot.getChildrenCount());

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    RequestModel request = dataSnapshot.getValue(RequestModel.class);
                    if (request != null) {
                        approvedRequestList.add(request);
                    } else {
                        Log.e("AllApprovedRequests", "RequestModel is null for key: " + dataSnapshot.getKey());
                    }
                }
                //desc order
                approvedRequestList.sort((r1, r2) -> Integer.compare(Integer.parseInt(r2.getRequestId()), Integer.parseInt(r1.getRequestId())));
                Log.d("AllApprovedRequests", "Final List Size: " + approvedRequestList.size());

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }
}