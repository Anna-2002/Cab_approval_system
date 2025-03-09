package com.example.cab_approval_system;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Recycler_adapter extends RecyclerView.Adapter<Recycler_adapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private String approverEmail, userRole;
    private Map<String, RequestModel> requestMap;
    private DatabaseReference notificationRef, sheet1Ref, approvedByFHRef, approvedRequestsRef,rejectedRequestsRef;
    private ImageView notificationDot;

    public Recycler_adapter(Context context, List<RequestModel> requestList, String approverEmail, String userRole, ImageView notificationDot) {
        this.context = context;
        this.requestList = requestList;
        this.approverEmail = approverEmail;
        this.userRole = userRole;
        this.notificationDot =notificationDot;

        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        approvedByFHRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_by_FH");
        approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");
        sheet1Ref = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
        rejectedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Rejected_requests");


        requestMap = new HashMap<>();

        Log.d("Role"," "+userRole);

        if (userRole.equals("HR Head")) {
            fetchPendingHRApprovals();  // HR now fetches from Approved_by_FH
            checkForHRNotifications();  // Check HR notifications correctly
        }
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_view_layout_activity, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        holder.nameTextView.setText(request.getEmpName());
        holder.empIdTextView.setText(request.getEmpId());
        holder.empEmailTextView.setText(request.getEmpEmail());
        holder.pickupTextView.setText(request.getPickupLocation());
        holder.dropoffTextView.setText(request.getDropoffLocation());
        holder.dateTextView.setText(request.getDate());
        holder.timeTextView.setText(request.getTime());
        holder.purposeTextView.setText(request.getPurpose());
        holder.statusTextView.setText(request.getStatus());
        holder.passengerCountTextView.setText(request.getNoOfPassengers());
        holder.approveFHName.setText(request.getApprovedFHName());
        holder.approvedFHEmail.setText(request.getApprovedFHEmail());
        holder.FHApprovedTime.setText(request.getFH_Approved_Time());

        // Reset Visibility Before Setting Conditions
        holder.rejectReason_Text.setVisibility(View.GONE);
        holder.rejectReasonTextview.setVisibility(View.GONE);
        holder.rejectReasonEdittext.setVisibility(View.GONE);
        holder.approve_button.setVisibility(View.VISIBLE);
        holder.reject_button.setVisibility(View.VISIBLE);
        holder.approved_display_textView.setVisibility(View.GONE);
        holder.reject_display_textView.setVisibility(View.GONE);

        // Ensure Passenger Details Are Always Reset
        holder.passengerLayout.removeAllViews();
        if (request.getPassengerMap() != null) {
            for (String passengerName : request.getPassengerMap().values()) {
                TextView passengerTextView = new TextView(context);
                passengerTextView.setText(passengerName);
                passengerTextView.setTextSize(12);
                passengerTextView.setPadding(0, 8, 0, 8);
                holder.passengerLayout.addView(passengerTextView);
            }
        }

        // HR Approval - Show Details Only If Approved By FH
        if (isHRUser() && "Approved by FH".equals(request.getStatus()) && request.isApprovedByFH()) {
            holder.pendingLayout.setVisibility(View.VISIBLE);
            holder.approvedFHemailLayout.setVisibility(View.VISIBLE);
            holder.approvedFHnameLayout.setVisibility(View.VISIBLE);
            holder.FHapprovedTimeLayout.setVisibility(View.VISIBLE);
        } else {
            holder.pendingLayout.setVisibility(View.GONE);
            holder.approvedFHemailLayout.setVisibility(View.GONE);
            holder.approvedFHnameLayout.setVisibility(View.GONE);
            holder.FHapprovedTimeLayout.setVisibility(View.GONE);
        }

        holder.detailsLayout.setVisibility(View.GONE);

        // Hide Approve/Reject Buttons for Approved Requests
        if ("Ride approved successfully".equals(request.getStatus())) {
            holder.approve_button.setVisibility(View.GONE);
            holder.reject_button.setVisibility(View.GONE);
            holder.approved_display_textView.setVisibility(View.VISIBLE);
        }

        holder.drop_down_button.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.VISIBLE) {
                holder.detailsLayout.setVisibility(View.GONE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_up_24);
            }
        });

        // Approve Button Click Listener
        holder.approve_button.setOnClickListener(v -> {
            Log.d("ApproveClick", "Approve button clicked for request: " + request.getRequestId());
            approveRequest(request, holder.statusTextView, holder.approve_button,holder.reject_button, holder.approved_display_textView, holder.reject_display_textView);
        });

        holder.reject_button.setOnClickListener(view -> {
            rejectRequest(request, holder.statusTextView, holder.approve_button, holder.reject_button,
                    holder.reject_display_textView, holder.reject_save_button, holder.rejectReasonEdittext, holder.rejectReasonTextview, holder.rejectReason_Text);
        });

    }

    private void approveRequest(RequestModel request, TextView statusTextView, ImageButton approve_button, ImageButton reject_button, TextView approved_display_textView, TextView reject_display_textView) {
        sheet1Ref.orderByChild("Official Email ID").equalTo(approverEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String approverName = "Unknown";
                        String approverRole = "Unknown";

                        for (DataSnapshot data : snapshot.getChildren()) {
                            approverName = data.child("Employee Name").getValue(String.class);
                            approverRole = data.child("Approval Matrix").getValue(String.class);
                            break;
                        }

                        if (approverName == null) {
                            Log.e("ApprovalProcess", "Approver details missing " + approverEmail);
                            Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String finalApproverName = approverName;
                        final String finalApproverRole = approverRole;

                        // Use a boolean array to modify inside inner class
                        final boolean[] isRequesterFH = {false};

                        sheet1Ref.orderByChild("Official Email ID").equalTo(request.getEmpEmail())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot requesterSnapshot) {
                                        for (DataSnapshot data : requesterSnapshot.getChildren()) {
                                            String requesterRole = data.child("Approval Matrix").getValue(String.class);
                                            if ("FH".equals(requesterRole)) {
                                                isRequesterFH[0] = true; // Modify array element instead of local variable
                                                break;
                                            }
                                        }
                                        DatabaseReference sourceRef;
                                        DatabaseReference destinationRef;

                                        if (isRequesterFH[0]) {
                                            // FH requests → Directly to HR
                                            sourceRef = notificationRef;
                                            destinationRef = approvedRequestsRef;
                                        } else {
                                            // Normal Employee Flow: Employee → FH → HR
                                            sourceRef = (finalApproverRole.equals("FH") ? notificationRef : approvedByFHRef);
                                            destinationRef = (finalApproverRole.equals("FH") ? approvedByFHRef : approvedRequestsRef);
                                        }

                                        long requestIdLong = Long.parseLong(request.getRequestId());

                                        sourceRef.orderByChild("request_id")
                                                .equalTo(requestIdLong)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                                        if (!requestSnapshot.exists()) {
                                                            Toast.makeText(context, "Request not found in the database.", Toast.LENGTH_SHORT).show();
                                                            return;
                                                        }

                                                        for (DataSnapshot snapshot : requestSnapshot.getChildren()) {
                                                            String key = snapshot.getKey();


                                                            String FHapprovedTime = snapshot.child("FH_Approved_Time").exists()
                                                                    ? snapshot.child("FH_Approved_Time").getValue(String.class)
                                                                    : new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                                                            Map<String, Object> approvedData = new HashMap<>();
                                                            approvedData.put("FH_Approved_Time",FHapprovedTime);
                                                            approvedData.put("Date", request.getDate());
                                                            approvedData.put("Destination", request.getDropoffLocation());
                                                            approvedData.put("Emp_ID", request.getEmpId());
                                                            approvedData.put("Emp_name", request.getEmpName());
                                                            approvedData.put("Emp_email", request.getEmpEmail());
                                                            approvedData.put("Source", request.getPickupLocation());
                                                            approvedData.put("Purpose", request.getPurpose());
                                                            approvedData.put("Time", request.getTime());
                                                            approvedData.put("no_of_passengers", request.getNoOfPassengers());
                                                            approvedData.put("request_id", requestIdLong);

                                                            if (request.getPassengerMap() != null) {
                                                                approvedData.put("passengerNames", request.getPassengerMap());
                                                            }

                                                            if (isRequesterFH[0]) {
                                                                // FH → HR Direct Approval
                                                                approvedData.put("HR_Approved_Time",request.getHRApprovedTime());
                                                                approvedData.put("Status", "Ride approved successfully");
                                                                approvedData.put("Approved_HR_name", finalApproverName);
                                                                approvedData.put("Approved_HR_email", approverEmail);
                                                                approvedData.put("approvedByFH", false);

                                                            } else if (finalApproverRole.equals("FH")) {
                                                                // Normal Flow: FH Approval First
                                                                approvedData.put("Status", "Approved by FH");
                                                                approvedData.put("Pending", "HR approval pending");
                                                                approvedData.put("Approved_FH_name", finalApproverName);
                                                                approvedData.put("Approved_FH_email", approverEmail);
                                                                approvedData.put("approvedByFH", true);
                                                            } else {
                                                                // HR Final Approval
                                                                approvedData.put("HR_Approved_Time",new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
                                                                approvedData.put("Status", "Ride approved successfully");
                                                                approvedData.put("approvedByFH", false);
                                                                approvedData.put("Approved_FH_name", request.getApprovedFHName());
                                                                approvedData.put("Approved_FH_email", request.getApprovedFHEmail());
                                                                approvedData.put("Approved_HR_name", finalApproverName);
                                                                approvedData.put("Approved_HR_email", approverEmail);
                                                            }

                                                            destinationRef.child(request.getRequestId()).setValue(approvedData)
                                                                    .addOnCompleteListener(task -> {
                                                                        if (task.isSuccessful()) {
                                                                            sourceRef.child(key).removeValue()
                                                                                    .addOnCompleteListener(deleteTask -> {
                                                                                        if (deleteTask.isSuccessful()) {
                                                                                            Toast.makeText(context, "Request approved", Toast.LENGTH_SHORT).show();
                                                                                            statusTextView.setText(approvedData.get("Status").toString());
                                                                                            approve_button.setVisibility(View.GONE);
                                                                                            reject_button.setVisibility(View.GONE);
                                                                                            approved_display_textView.setVisibility(View.VISIBLE);
                                                                                        } else {
                                                                                            Toast.makeText(context, "Failed to remove old request.", Toast.LENGTH_SHORT).show();
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            Toast.makeText(context, "Approval failed.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Toast.makeText(context, "Database error.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(context, "Failed to fetch requester details.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Failed to fetch approver details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectRequest(RequestModel request, TextView statusTextView,
                               ImageButton approve_button, ImageButton reject_button,
                               TextView reject_display_textView, Button reject_save_button,
                               EditText reject_reason_editText, TextView reject_reason_textview, TextView rejectReason_Text) {

        // Show reason input field and save button, hide reject button
        rejectReason_Text.setVisibility(View.GONE);
        reject_reason_editText.setVisibility(View.VISIBLE);
        reject_save_button.setVisibility(View.VISIBLE);
        reject_reason_textview.setVisibility(View.VISIBLE);
        reject_button.setVisibility(View.GONE);
        approve_button.setVisibility(View.GONE);

        reject_save_button.setOnClickListener(view -> {
            String rejectionReason = reject_reason_editText.getText().toString().trim();
            if (rejectionReason.isEmpty()) {
                Toast.makeText(context, "Please enter a reason for rejection.", Toast.LENGTH_SHORT).show();
                return;
            }

            request.setRejectionReason(rejectionReason);

            sheet1Ref.orderByChild("Official Email ID").equalTo(approverEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String approverName = "Unknown";
                            String approverRole = "Unknown";

                            for (DataSnapshot data : snapshot.getChildren()) {
                                approverName = data.child("Employee Name").getValue(String.class);
                                approverRole = data.child("Approval Matrix").getValue(String.class);
                                break;
                            }

                            if (approverName == null) {
                                Log.e("ApprovalProcess", "Approver details missing " + approverEmail);
                                Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final String finalApproverName = approverName;
                            final String finalApproverRole = approverRole;

                            long requestIdLong = Long.parseLong(request.getRequestId());

                            DatabaseReference sourceRef = notificationRef; // Default
                            if (request.isApprovedByFH()) {
                                sourceRef = approvedByFHRef;
                            }

                            // Fetch request from source table
                            sourceRef.orderByChild("request_id").equalTo(requestIdLong)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                            if (!requestSnapshot.exists()) {
                                                Toast.makeText(context, "Request not found in the database.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            Map<String, Object> rejectedData = new HashMap<>();
                                            rejectedData.put("request_id", requestIdLong);
                                            rejectedData.put("Date", request.getDate());
                                            rejectedData.put("Emp_ID", request.getEmpId());
                                            rejectedData.put("Emp_name", request.getEmpName());
                                            rejectedData.put("Emp_email", request.getEmpEmail());
                                            rejectedData.put("Source", request.getPickupLocation());
                                            rejectedData.put("Destination", request.getDropoffLocation());
                                            rejectedData.put("Purpose", request.getPurpose());
                                            rejectedData.put("Time", request.getTime());
                                            rejectedData.put("no_of_passengers", request.getNoOfPassengers());
                                            rejectedData.put("Reason", request.getRejectionReason());
                                            rejectedData.put("rejected_by", finalApproverName + "," + finalApproverRole);
                                            rejectedData.put("Status", "Request rejected");

                                            rejectedRequestsRef.child(request.getRequestId()).setValue(rejectedData)
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            // Remove the rejected request from the original table
                                                            for (DataSnapshot childSnapshot : requestSnapshot.getChildren()) {
                                                                childSnapshot.getRef().removeValue();
                                                            }

                                                            // Update UI
                                                            statusTextView.setText("Request rejected");
                                                            rejectReason_Text.setText(request.getRejectionReason());
                                                            rejectReason_Text.setVisibility(View.VISIBLE);
                                                            reject_reason_editText.setVisibility(View.GONE);
                                                            reject_display_textView.setVisibility(View.VISIBLE);
                                                            reject_display_textView.setVisibility(View.VISIBLE);
                                                            reject_save_button.setVisibility(View.GONE);

                                                            Toast.makeText(context, "Request rejected successfully.", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(context, "Rejection failed.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(context, "Database error.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Failed to fetch approver details.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }


    private void fetchPendingHRApprovals() {
        approvedByFHRef.orderByChild("Pending").equalTo("HR approval pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            RequestModel request = data.getValue(RequestModel.class);  // Fetch full object directly

                            if (request != null) {
                                request.setRequestId(String.valueOf(data.child("request_id").getValue(Long.class)));
                                request.setApprovedByFH(Boolean.TRUE.equals(data.child("approvedByFH").getValue(Boolean.class)));
                                requestList.add(request);
                            }
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HR_APPROVALS", "Error: " + error.getMessage());
                    }
                });
    }


    private void checkForHRNotifications() {
        if (!isHRUser()) {
            if (notificationDot != null) {
                notificationDot.setVisibility(View.GONE); // Ensure employees don't see it
            }
            return; // Only HR should proceed further
        }
        approvedByFHRef.orderByChild("Pending").equalTo("HR approval pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasPendingHRApprovals = snapshot.exists();  // This is the key check
                        if (notificationDot != null) {
                            notificationDot.setVisibility(hasPendingHRApprovals ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HR_NOTIFICATIONS", "Error checking notifications: " + error.getMessage());
                    }
                });
    }

    private boolean isHRUser() {
        return "HR Head".equals(userRole);
    }


    @Override
    public int getItemCount() {
        return requestList.size();
    }
    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView, rejectReasonTextview,rejectReason_Text,
                dropoffTextView, dateTextView, timeTextView, pendingTextView,approved_display_textView,reject_display_textView,
                purposeTextView, statusTextView, passengerCountTextView,passenger_details_title,approveFHName,approvedFHEmail,FHApprovedTime;
        Button reject_save_button;
        EditText rejectReasonEdittext;
        ImageButton drop_down_button,approve_button,reject_button;
        LinearLayout detailsLayout, passengerLayout, pendingLayout,approvedFHnameLayout,approvedFHemailLayout,FHapprovedTimeLayout;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_textview);
            empIdTextView = itemView.findViewById(R.id.emp_id_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_emailid_textview);
            pickupTextView = itemView.findViewById(R.id.source_textview);
            dropoffTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            purposeTextView = itemView.findViewById(R.id.purpose_textview);
            statusTextView = itemView.findViewById(R.id.status_textview);
            drop_down_button =  itemView.findViewById(R.id.drop_down_button);
            detailsLayout = itemView.findViewById(R.id.outer_layout);
            passengerCountTextView = itemView.findViewById(R.id.no_of_passengers_textview);
            passengerLayout = itemView.findViewById(R.id.passenger_container);
            passenger_details_title =itemView.findViewById(R.id.passenger_details_title);
            pendingTextView =  itemView.findViewById(R.id.pending_textview);
            pendingLayout =  itemView.findViewById(R.id.layout15);
            approvedFHEmail = itemView.findViewById(R.id.approvedFHemail_textview);
            approveFHName = itemView.findViewById(R.id.approvedFHname_textview);
            FHApprovedTime = itemView.findViewById(R.id.FHApprovedTime_textview);
            approvedFHnameLayout = itemView.findViewById(R.id.layout12);
            approvedFHemailLayout = itemView.findViewById(R.id.layout13);
            FHapprovedTimeLayout = itemView.findViewById(R.id.layout14);
            approve_button = itemView.findViewById(R.id.approve_button);
            reject_button = itemView.findViewById(R.id.reject_button);
            approved_display_textView = itemView.findViewById(R.id.approved_display);
            reject_display_textView = itemView.findViewById(R.id.reject_display);
            reject_save_button = itemView.findViewById(R.id.reject_reason_save_button);
            rejectReasonEdittext = itemView.findViewById(R.id.reject_reason_edittext);
            rejectReasonTextview = itemView.findViewById(R.id.reject_reason_textview);
            rejectReason_Text = itemView.findViewById(R.id.reject_reason);

        }

    }
}
