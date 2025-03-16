package com.example.cab_approval_system;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.*;

import java.util.*;

public class History_page extends AppCompatActivity {

    private DatabaseReference employeeRef;
    private String requesterEmail,user_role,user_email;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String requesterDesignation, requesterTeam,userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);

        tabLayout = findViewById(R.id.tabLayoutHistory);
        viewPager = findViewById(R.id.viewPagerHistory);

        user_email =  getIntent().getStringExtra("email");
        user_role = getIntent().getStringExtra("userRole");
        Home_Screen.setupBottomNavigation(this,user_email,user_role);

        requesterEmail = user_email;

        if (requesterEmail == null) {
            requesterEmail = "";
        }
        Log.d("email", "Requester email: " + requesterEmail);

        employeeRef =FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Sheet1");

        loadEmployeeData();
    }

    private void loadEmployeeData() {
        employeeRef.orderByChild("Official Email ID").equalTo(requesterEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                        requesterTeam = empSnapshot.child("Team").getValue(String.class);
                        requesterDesignation = empSnapshot.child("Approval Matrix").getValue(String.class);
                        Log.d("Designation", "User is a: " + requesterDesignation);
                        Log.d("Team", "User team: " + requesterTeam);
                        break; // Only need the first match
                    }
                } else {
                    Log.e("FirebaseError", "User not found in database!");
                }

                if (requesterDesignation == null) {
                    Log.e("FirebaseError", "User designation not found!");
                    return;
                }


                setupViewPager();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching employee data", error.toException());
            }
        });
    }

    private void setupViewPager() {
        boolean isFH = "FH".equals(requesterDesignation);
        boolean isHR = "HR Head".equals(requesterDesignation);
        History_ViewPager adapter = new History_ViewPager(this, isFH,isHR, requesterEmail, requesterTeam);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (isHR) {
                tab.setText(History_ViewPager.DEPARTMENTS.get(position)); // Set department names as tab titles
            } else if (isFH) {
                if (position == 0) {
                    tab.setText("Personal History");
                } else if (position == 1) {
                    tab.setText("Approved Department Requests");
                } else if(position==2)
                    tab.setText("Pending Approvals"); // Correct name for new tab
                else
                    tab.setText("Rejected Requests");

            } else {
                if (position == 0) {
                    tab.setText("Approved Requests History");
                } else if(position == 1)
                    tab.setText("Pending Approvals"); // Employees also have this tab
                else
                    tab.setText("Rejected Requests");
            }
        }).attach();
    }
}
