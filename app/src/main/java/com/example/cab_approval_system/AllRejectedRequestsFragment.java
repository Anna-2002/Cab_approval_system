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

public class AllRejectedRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private Rejected_history_adapter adapter;
    private List<RequestModel> rejectedRequestList = new ArrayList<>();
    private DatabaseReference databaseReference;

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
        new Thread(() -> { // Run filtering in the background
            List<RequestModel> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim(); // Precompute

            if (lowerCaseQuery.isEmpty()) {
                filteredList.addAll(rejectedRequestList); // Restore full list if query is empty
            } else {
                for (RequestModel request : rejectedRequestList) {
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



    private void fetchData() {
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Rejected_requests");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rejectedRequestList.clear();
                Log.d("AllRejectedRequests", "Total Requests Found: " + snapshot.getChildrenCount());

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    RequestModel request = dataSnapshot.getValue(RequestModel.class);
                    if (request != null) {
                        rejectedRequestList.add(request);
                    } else {
                        Log.e("AllRejectedRequests", "RequestModel is null for key: " + dataSnapshot.getKey());
                    }
                }
                // Sort in descending order by requestId
                rejectedRequestList.sort((r1, r2) -> Integer.compare(Integer.parseInt(r2.getRequestId()), Integer.parseInt(r1.getRequestId())));
                Log.d("AllRejectedRequests", "Final List Size: " + rejectedRequestList.size());

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }
}