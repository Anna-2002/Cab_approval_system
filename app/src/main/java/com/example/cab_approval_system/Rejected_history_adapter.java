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

import java.util.ArrayList;
import java.util.List;

public class Rejected_history_adapter extends RecyclerView.Adapter<Rejected_history_adapter.RejectedViewHolder> {

    private Context context;
    private List<RequestModel> rejectedRequestList,displayedList;

    public Rejected_history_adapter(Context context, List<RequestModel> rejectedRequestList) {
        this.context = context;
        this.rejectedRequestList = rejectedRequestList;
        this.displayedList = rejectedRequestList;
    }

    @NonNull
    @Override
    public RejectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_rejected_history_adapter, parent, false);
        return new RejectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RejectedViewHolder holder, int position) {
        if (displayedList == null || displayedList.isEmpty()) {
            Log.e("RejectedAdapter", "rejectedRequestList is null or empty!");
            return;
        }

        RequestModel request = displayedList.get(position);
        if (request == null) {
            Log.e("RejectedAdapter", "RequestModel is null at position: " + position);
            return;
        }

        Log.d("RejectedAdapter", "Binding data for position: " + position);

        holder.nameTextView.setText(request.getEmpName() != null ? request.getEmpName() : "N/A");
        holder.empIdTextView.setText(request.getEmpId() != null ? String.valueOf(request.getEmpId()) : "N/A");
        holder.empEmailTextView.setText(request.getEmpEmail() != null ? request.getEmpEmail() : "N/A");
        holder.pickupTextView.setText(request.getPickupLocation() != null ? request.getPickupLocation() : "N/A");
        holder.dropoffTextView.setText(request.getDropoffLocation() != null ? request.getDropoffLocation() : "N/A");
        holder.dateTextView.setText(request.getDate() != null ? request.getDate() : "N/A");
        holder.timeTextView.setText(request.getTime() != null ? request.getTime() : "N/A");
        holder.num_of_passengers.setText(request.getNoOfPassengers()!=null? request.getNoOfPassengers() : "N/A");
        holder.purposeTextView.setText(request.getPurpose() != null ? request.getPurpose() : "N/A");
        holder.statusTextView.setText(request.getStatus() != null ? request.getStatus() : "N/A");
        holder.rejectedByTextView.setText(request.getRejectedBy()!=null? request.getRejectedBy(): "N/A");
        holder.rejectionReasonTextView.setText(request.getRejectionReason() != null ? request.getRejectionReason() : "N/A");

        holder.dropDownButton.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.VISIBLE) {
                holder.detailsLayout.setVisibility(View.GONE);
                holder.dropDownButton.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.dropDownButton.setImageResource(R.drawable.baseline_arrow_drop_up_24);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (displayedList != null) ?  displayedList.size() : 0;
    }

    public static class RejectedViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView,
                dropoffTextView, dateTextView, timeTextView,num_of_passengers,
                purposeTextView, statusTextView, rejectedByTextView,rejectionReasonTextView;
        ImageButton dropDownButton;
        LinearLayout detailsLayout;

        public RejectedViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.emp_name_textview);
            empIdTextView = itemView.findViewById(R.id.emp_id_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_email_textview);
            pickupTextView = itemView.findViewById(R.id.source_textview);
            dropoffTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            purposeTextView = itemView.findViewById(R.id.purpose_textview);
            num_of_passengers = itemView.findViewById(R.id.num_of_passengers_textview);
            statusTextView = itemView.findViewById(R.id.status_textview);
            rejectedByTextView = itemView.findViewById(R.id.rejected_by_textview);
            rejectionReasonTextView = itemView.findViewById(R.id.rejected_reason_textview);
            dropDownButton = itemView.findViewById(R.id.drop_down_button);
            detailsLayout = itemView.findViewById(R.id.outer_layout);
        }
    }

    public void updateList(List<RequestModel> newList) {
        if (newList == null) return;
        this.displayedList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
    public void updateSearchList(List<RequestModel> newList) {
        displayedList = new ArrayList<>(newList); // Create a new list instead of modifying the original
        notifyDataSetChanged();
    }
}