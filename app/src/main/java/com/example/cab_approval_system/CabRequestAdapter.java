package com.example.cab_approval_system;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CabRequestAdapter extends RecyclerView.Adapter<CabRequestAdapter.ViewHolder> {
    private Context context;
    private List<CabRequestModel> requestList;

    public CabRequestAdapter(Context context, List<CabRequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cab_request_recycler_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CabRequestModel request = requestList.get(position);

        holder.empNameTextView.setText(request.getEmpName());
        holder.empEmailTextView.setText(request.getEmpEmail());
        holder.sourceTextView.setText(request.getSource());
        holder.destinationTextView.setText(request.getDestination());
        holder.dateTextView.setText(request.getDate());
        holder.timeTextView.setText(request.getTime());
        holder.numberOfPassengerTextView.setText(request.getNumberOfPassengers());
        holder.empMobileTextView.setText(request.getEmpMobile());

        holder.driver_name_stored.setVisibility(View.GONE);
        holder.driver_mobileNum_stored.setVisibility(View.GONE);
        holder.cab_num_stored.setVisibility(View.GONE);

        holder.detailsLayout.setVisibility(View.GONE);
        holder.addDriverDetailsButton.setVisibility(View.GONE);

        holder.dropDownButton.setOnClickListener(v -> {
            boolean isExpanded = holder.detailsLayout.getVisibility() == View.VISIBLE;
            holder.detailsLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.addDriverDetailsButton.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.dropDownButton.setImageResource(isExpanded ? R.drawable.baseline_arrow_drop_down_24 : R.drawable.baseline_arrow_drop_up_24);
        });

        holder.addDriverDetailsButton.setOnClickListener(v -> {
            holder.driverDetailsLayout.setVisibility(View.VISIBLE);
            holder.driver_details_title.setVisibility(View.VISIBLE);
            holder.addDriverDetailsButton.setVisibility(View.GONE);
        });

        // Save button logic
        holder.saveButton.setOnClickListener(v -> {
            String driverName = holder.driverNameEditText.getText().toString().trim();
            String driverMobile = holder.driverMobileEditText.getText().toString().trim();
            String cabNumber = holder.cabNumberEditText.getText().toString().trim();

            if (!driverName.isEmpty() && !driverMobile.isEmpty() && !cabNumber.isEmpty()) {
                request.setDriverName(driverName);
                request.setDriverMobile(driverMobile);
                request.setCabNumber(cabNumber);
                //storing the entered driver details to textview
                holder.driver_name_stored.setText(driverName);
                holder.driver_mobileNum_stored.setText(driverMobile);
                holder.cab_num_stored.setText(cabNumber);

                holder.driverNameEditText.setVisibility(View.GONE);
                holder.driverMobileEditText.setVisibility(View.GONE);
                holder.cabNumberEditText.setVisibility(View.GONE);

                holder.saveButton.setVisibility(View.GONE);
                holder.driver_name_stored.setVisibility(View.VISIBLE);
                holder.driver_mobileNum_stored.setVisibility(View.VISIBLE);
                holder.cab_num_stored.setVisibility(View.VISIBLE);


                holder.driverDetailsLayout.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show();
            }
        });

        // Send button logic (store in Assigned_rides)
        holder.sendButton.setOnClickListener(v -> {
            DatabaseReference assignedRideRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Assigned_rides");

            String requestId = request.getRequestId();

            assignedRideRef.child(requestId).setValue(request)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Ride Assigned Successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return (requestList != null) ? requestList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView empNameTextView, empEmailTextView, empMobileTextView, sourceTextView,
                destinationTextView, dateTextView, timeTextView, numberOfPassengerTextView,
                driver_name_stored, driver_mobileNum_stored, cab_num_stored, driver_details_title;
        ImageButton dropDownButton;
        LinearLayout detailsLayout, driverDetailsLayout;
        Button addDriverDetailsButton, saveButton, sendButton;
        EditText driverNameEditText, driverMobileEditText, cabNumberEditText;
        TextView driverNameTextView, driverMobileTextView, cabNumberTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            empNameTextView = itemView.findViewById(R.id.name_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_emailid_textview);
            empMobileTextView = itemView.findViewById(R.id.mobile_num_textview);
            sourceTextView = itemView.findViewById(R.id.source_textview);
            destinationTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            numberOfPassengerTextView = itemView.findViewById(R.id.no_of_passengers_textview);
            dropDownButton = itemView.findViewById(R.id.drop_down_button);
            detailsLayout = itemView.findViewById(R.id.outer_layout);
            driverDetailsLayout = itemView.findViewById(R.id.driver_details_layout);
            addDriverDetailsButton = itemView.findViewById(R.id.add_driver_details_button);
            saveButton = itemView.findViewById(R.id.driverDetails_save_button);
            sendButton = itemView.findViewById(R.id.driverDetails_send_button);
            driverNameEditText = itemView.findViewById(R.id.driver_name_edit_text);
            driverMobileEditText = itemView.findViewById(R.id.driver_mobileNum_edit_text);
            cabNumberEditText = itemView.findViewById(R.id.cab_num_edit_text);
            driverNameTextView = itemView.findViewById(R.id.driver_name_textview);
            driverMobileTextView = itemView.findViewById(R.id.driver_mobileNum_textview);
            cabNumberTextView = itemView.findViewById(R.id.cab_num_textview);
            driver_name_stored = itemView.findViewById(R.id.driver_name_stored);
            driver_mobileNum_stored = itemView.findViewById(R.id.driver_num_stored);
            cab_num_stored = itemView.findViewById(R.id.cab_num_stored);
            driver_details_title =  itemView.findViewById(R.id.driver_details_title_textview);

        }
    }
}