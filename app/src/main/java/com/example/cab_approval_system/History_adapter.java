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

    public History_adapter(Context context, List<RequestModel> approvedRequestList) {
        this.context = context;
        this.approvedRequestList = approvedRequestList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_history_adapter, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (approvedRequestList == null || approvedRequestList.isEmpty()) {
            Log.e("HistoryAdapter", "approvedRequestList is null or empty!");
            return;
        }

        RequestModel request = approvedRequestList.get(position);
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
        holder.HRApprovedTime.setText(request.getHRApprovedTime() != null ? request.getHRApprovedTime() : "N/A");
        holder.FH_approver_name_textview.setText(request.getApprovedFHName() != null ? request.getApprovedFHName() : "N/A");
        holder.FH_approved_time_textview.setText(request.getFH_Approved_Time() != null ? request.getFH_Approved_Time() : "N/A");
        holder.FH_approver_email_textview.setText(request.getApprovedFHEmail() != null ? request.getApprovedFHEmail(): "N/A");

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
        return (approvedRequestList != null) ? approvedRequestList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView,
                dropoffTextView, dateTextView, timeTextView,
                purposeTextView, FH_approver_name_textview,
                FH_approver_email_textview, FH_approved_time_textview,
                statusTextView, pendingStatus, pendingStatusLabel, HR_approver_name,HRApprovedTime;
        ImageButton drop_down_button;
        LinearLayout detailsLayout;

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
        }
    }

    private void sortRequestsByApprovedTime() {
        if (approvedRequestList != null && !approvedRequestList.isEmpty()) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            approvedRequestList.sort((r1, r2) -> {
                try {
                    Date date1,date2;
                    if(r1.getHRApprovedTime()!=null && r2.getHRApprovedTime()!=null)
                    {
                        date1 = dateTimeFormat.parse(r1.getHRApprovedTime());
                        date2 = dateTimeFormat.parse(r2.getHRApprovedTime());
                    }
                    else
                    {
                        date1 = dateTimeFormat.parse(r1.getFH_Approved_Time());
                        date2 = dateTimeFormat.parse(r2.getFH_Approved_Time());
                    }
                    // Compare in descending order (latest first)
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // If parsing fails, keep the original order
                }
            });
        }
    }

    public void updateList(List<RequestModel> newList) {
        if (newList == null) return;

        this.approvedRequestList = new ArrayList<>(newList); // Create a new reference
        sortRequestsByApprovedTime();  // Sort before notifying
        notifyDataSetChanged();        // Refresh RecyclerView
    }
}