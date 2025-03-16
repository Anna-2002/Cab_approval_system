package com.example.cab_approval_system;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Arrays;
import java.util.List;

public class History_ViewPager extends FragmentStateAdapter {

    private final boolean isFH;
    private final boolean isHR;
    private final String requesterEmail;
    private final String requesterTeam;

    // List of all departments for HR view
    protected static final List<String> DEPARTMENTS = Arrays.asList(
            "All Approved requests",
            "All Rejected requests",
            "My Requests",
            "Mechanical Cable Systems",
            "Business Development",
            "Mechanical Systems",
            "Electronics",
            "PLM",
            "Accounts",
            "HR",
            "EMA"
    );

    public History_ViewPager (@NonNull FragmentActivity fragmentActivity, boolean isFH, boolean isHR, String requesterEmail, String requesterTeam) {
        super(fragmentActivity);
        this.isFH = isFH;
        this.isHR = isHR;
        this.requesterEmail = requesterEmail;
        this.requesterTeam = requesterTeam;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (isHR) {
            // HR should see "All Approved Requests", "My Requests", and department-wise approved requests
            if (position == 0) {
                return new AllApprovedRequestsFragment();  // New tab for all approved requests
            } else if (position ==1)
                return new AllRejectedRequestsFragment();
            else if (position == 2) {
                return PersonalHistoryFragment.newInstance(requesterEmail);  // "My Requests" tab
            } else {
                return DepartmentHistoryFragment.newInstance(DEPARTMENTS.get(position), requesterEmail); // Shift index for department tabs
            }
        } else if (isFH) {
            // Functional Heads get four tabs: Personal, Department, Awaiting HR approval, and Rejected Requests
            if (position == 0) {
                return PersonalHistoryFragment.newInstance(requesterEmail);
            } else if (position == 1) {
                return DepartmentHistoryFragment.newInstance(requesterTeam, requesterEmail);
            } else if (position == 2) {
                return ApprovedByFunctionalHeadFragment.newInstance(requesterEmail,requesterTeam);  // Partial Approvals tab
            } else {
                return RejectedRequestsFragment.newInstance(requesterEmail,requesterTeam);
            }
        } else {
            // Normal users get Personal History + Partial Approvals + Rejected Requests
            if (position == 0) {
                return PersonalHistoryFragment.newInstance(requesterEmail);
            } else if (position == 1) {
                return ApprovedByFunctionalHeadFragment.newInstance(requesterEmail, requesterTeam);
            } else {
                return RejectedRequestsFragment.newInstance(requesterEmail,requesterTeam);
            }
        }
    }



    @Override
    public int getItemCount() {
        if (isHR) {
            return DEPARTMENTS.size(); // 8 tabs for HR
        } else if (isFH) {
            return 4; // FH gets Personal + Department history + Partial approvals + Rejected
        } else {
            return 3; // Others get Personal history + Partial approvals + Rejected
        }
    }
}