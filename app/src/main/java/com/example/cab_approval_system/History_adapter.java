package com.example.cab_approval_system;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class History_adapter extends RecyclerView.Adapter<History_adapter.HistoryViewHolder> {

    private Context context;
    private List<RequestModel> approvedRequestList;
    private List<RequestModel> displayedList;

    public History_adapter(Context context, List<RequestModel> approvedRequestList) {
        this.context = context;
        this.approvedRequestList = approvedRequestList;
        this.displayedList = approvedRequestList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_history_adapter, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (displayedList == null || displayedList.isEmpty()) {
            Log.e("HistoryAdapter", "approvedRequestList is null or empty!");
            return;
        }

        RequestModel request = displayedList.get(position);
        if (request == null) {
            Log.e("HistoryAdapter", "RequestModel is null at position: " + position);
            return;
        }

        Log.d("HistoryAdapter", "Binding data for position: " + position);

        holder.nameTextView.setText(request.getEmpName() != null ? request.getEmpName() : "N/A");
        holder.empIdTextView.setText(request.getEmpId() != null ? String.valueOf(request.getEmpId()) : "N/A");
        holder.empEmailTextView.setText(request.getEmpEmail() != null ? request.getEmpEmail() : "N/A");
        holder.pickupTextView.setText(request.getPickupLocation() != null ? request.getPickupLocation() : "N/A");
        holder.dropoffTextView.setText(request.getDropoffLocation() != null ? request.getDropoffLocation() : "N/A");
        holder.dateTextView.setText(request.getDate() != null ? request.getDate() : "N/A");
        holder.timeTextView.setText(request.getTime() != null ? request.getTime() : "N/A");
        holder.purposeTextView.setText(request.getPurpose() != null ? request.getPurpose() : "N/A");
        holder.statusTextView.setText(request.getStatus() != null ? request.getStatus() : "N/A");

        if (request.getPendingStatus() != null && !request.getPendingStatus().equalsIgnoreCase("N/A")) {
            holder.pendingStatus.setText(request.getPendingStatus());
            holder.pendingStatus.setVisibility(View.VISIBLE);
            holder.pendingStatusLabel.setVisibility(View.VISIBLE);
        } else {
            holder.pendingStatus.setVisibility(View.GONE);
            holder.pendingStatusLabel.setVisibility(View.GONE);
        }

        holder.HR_approver_name.setText(request.getHR_approver_name()!= null ? request.getHR_approver_name(): "N/A");
        if(holder.HR_approver_name.getText()== "N/A")
            holder.HR_name_layout.setVisibility(View.GONE);
        holder.HRApprovedTime.setText(request.getHRApprovedTime() != null ? request.getHRApprovedTime() : "N/A");
        if(holder.HRApprovedTime.getText()== "N/A")
        {
            holder.HRApprovedTime.setVisibility(View.GONE);
            holder.HRApprovedTime_label.setVisibility(View.GONE);
        }
        holder.FH_approver_name_textview.setText(request.getApprovedFHName() != null ? request.getApprovedFHName() : "N/A");
        if(holder.FH_approver_name_textview.getText() == "N/A")
            holder.FH_approved_name_layout.setVisibility(View.GONE);
        holder.FH_approved_time_textview.setText(request.getFH_Approved_Time() != null ? request.getFH_Approved_Time() : "N/A");
        if(holder.FH_approved_time_textview.getText() == "N/A")
            holder.FH_approved_time_layout.setVisibility(View.GONE);
        holder.FH_approver_email_textview.setText(request.getApprovedFHEmail() != null ? request.getApprovedFHEmail(): "N/A");
        if(holder.FH_approver_email_textview.getText() == "N/A")
            holder.FH_approved_email_layout.setVisibility(View.GONE);

        holder.drop_down_button.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.VISIBLE) {
                holder.detailsLayout.setVisibility(View.GONE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_down_24); // Use collapse icon
            } else {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_up_24); // Use expand icon
            }
        });
    }


    @Override
    public int getItemCount() {
        return (displayedList != null) ? displayedList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView,
                dropoffTextView, dateTextView, timeTextView,
                purposeTextView, FH_approver_name_textview,
                FH_approver_email_textview, FH_approved_time_textview,
                statusTextView, pendingStatus, pendingStatusLabel,
                HR_approver_name,HRApprovedTime_label,HRApprovedTime;
        ImageButton drop_down_button;
        LinearLayout detailsLayout,HR_name_layout, HR_approved_time_layout,
                FH_approved_name_layout,FH_approved_email_layout,FH_approved_time_layout;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_textview);
            empIdTextView = itemView.findViewById(R.id.emp_id_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_emailid_textview);
            pickupTextView = itemView.findViewById(R.id.source_textview);
            dropoffTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            purposeTextView = itemView.findViewById(R.id.purpose_textview);
            FH_approver_name_textview = itemView.findViewById(R.id.FH_approver_name_textview);
            FH_approved_time_textview = itemView.findViewById(R.id.FH_approved_time_textview);
            FH_approver_email_textview = itemView.findViewById(R.id.FH_approver_email_textview);
            statusTextView = itemView.findViewById(R.id.status_textview);
            pendingStatusLabel = itemView.findViewById(R.id.pending_label);
            pendingStatus = itemView.findViewById(R.id.pending_status_textview);
            HR_approver_name = itemView.findViewById(R.id.HR_approver_name_textview);
            HRApprovedTime = itemView.findViewById(R.id.approved_time_textview);
            drop_down_button =  itemView.findViewById(R.id.drop_down_button);
            detailsLayout = itemView.findViewById(R.id.details_layout_history);
            HR_name_layout = itemView.findViewById(R.id.layout12);
            HRApprovedTime_label= itemView.findViewById(R.id.approved_time_label);
            HR_approved_time_layout = itemView.findViewById(R.id.layout15);
            FH_approved_name_layout = itemView.findViewById(R.id.layout9);
            FH_approved_email_layout = itemView.findViewById(R.id.layout10);
            FH_approved_time_layout = itemView.findViewById(R.id.layout11);
        }
    }

    private void sortRequestsByApprovedTime() {
        if (displayedList != null && !displayedList.isEmpty()) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            displayedList.sort((r1, r2) -> {
                try {
                    Date date1 = getLatestApprovalTime(r1, dateTimeFormat);
                    Date date2 = getLatestApprovalTime(r2, dateTimeFormat);

                    // If both dates are null, maintain order
                    if (date1 == null && date2 == null) return 0;
                    if (date1 == null) return 1; // Keep null at the end
                    if (date2 == null) return -1;

                    // Compare in descending order (latest first)
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // If parsing fails, keep original order
                }
            });
        }
    }

    private Date getLatestApprovalTime(RequestModel request, SimpleDateFormat dateTimeFormat) throws ParseException {
        if (request.getHRApprovedTime() != null && !request.getHRApprovedTime().isEmpty()) {
            return dateTimeFormat.parse(request.getHRApprovedTime());
        } else if (request.getFH_Approved_Time() != null && !request.getFH_Approved_Time().isEmpty()) {
            return dateTimeFormat.parse(request.getFH_Approved_Time());
        } else if (request.getRequestSubmissionTime() != null && !request.getRequestSubmissionTime().isEmpty()) { // Fallback to request submission time
            return dateTimeFormat.parse(request.getRequestSubmissionTime());
        }
        return null;
    }

    public void updateList(List<RequestModel> newList) {
        if (newList == null) return;

        //this.approvedRequestList = new ArrayList<>(newList); // Update approved list
        this.displayedList = new ArrayList<>(newList); // Ensure displayed list is also updated
        sortRequestsByApprovedTime();  // Sort before notifying
        notifyDataSetChanged();        // Refresh RecyclerView
    }
    public void updateSearchList(List<RequestModel> newList) {
        displayedList = new ArrayList<>(newList); // Create a new list instead of modifying the original
        notifyDataSetChanged();
    }


}