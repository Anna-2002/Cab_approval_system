package com.example.cab_approval_system;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class
Profile_page extends AppCompatActivity {

    private TextView passwordTextView;
    private EditText passwordEditText;
    private ImageView passwordToggle;
    private boolean isPasswordVisible = false;
    private Button resetSaveButton;

    private DatabaseReference passwordReference;
    private String userEmail,user_role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        Intent intentProfile = getIntent();
        userEmail = intentProfile.getStringExtra("email");
        user_role = getIntent().getStringExtra("userRole");
        Home_Screen.setupBottomNavigation(this,userEmail,user_role);

        // Initialize Firebase reference
        passwordReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        fetchUserDetails();
        fetchVendorDetails();
        // Initialize views
        passwordTextView = findViewById(R.id.employee_password_text);
        resetSaveButton = findViewById(R.id.reset_password_button);
        passwordEditText = findViewById(R.id.employee_password_editText);
        passwordToggle = findViewById(R.id.password_toggle);

        passwordTextView.setTransformationMethod(new PasswordTransformationMethod());

        passwordToggle.setOnClickListener(view -> {
            if (isPasswordVisible) {
                // Hide password
                passwordTextView.setTransformationMethod(new PasswordTransformationMethod());
                passwordEditText.setTransformationMethod(new PasswordTransformationMethod());
                passwordToggle.setImageResource(R.drawable.closed_eye);
            } else {
                // Show password
                passwordTextView.setTransformationMethod(null);
                passwordEditText.setTransformationMethod(null);
                passwordToggle.setImageResource(R.drawable.opened_eye);
            }
            isPasswordVisible = !isPasswordVisible;

            // Move cursor to end after toggling
            passwordEditText.setSelection(passwordEditText.length());
        });


        // Set up the button's onClickListener
        resetSaveButton.setOnClickListener(new View.OnClickListener() {
            private boolean isEditing = false;

            @Override
            public void onClick(View view) {
                if (!isEditing) {
                    // Switch to EditText for editing the password
                    passwordEditText.setText(passwordTextView.getText().toString());
                    passwordTextView.setVisibility(View.GONE);
                    passwordEditText.setVisibility(View.VISIBLE);
                    resetSaveButton.setText("Save");
                    isEditing = true;
                } else {
                    // Save the password to Firebase
                    String newPassword = passwordEditText.getText().toString().trim();
                    if (newPassword.isEmpty()) {
                        Toast.makeText(Profile_page.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update password in Firebase

                    String modifiedEmail = userEmail.replace(".", ","); // Convert email to match Firebase key
                    Toast.makeText(Profile_page.this," "+userEmail,Toast.LENGTH_SHORT).show();
                    passwordReference.child(modifiedEmail).child("password").setValue(newPassword)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Log.d("DEBUG", "Password updated successfully for " + modifiedEmail);
                                    Toast.makeText(Profile_page.this, "Password updated successfully", Toast.LENGTH_SHORT).show();

                                    // Switch back to TextView
                                    passwordTextView.setText(newPassword);
                                    passwordTextView.setVisibility(View.VISIBLE);
                                    passwordEditText.setVisibility(View.GONE);
                                    resetSaveButton.setText("Reset Password");
                                    isEditing = false;
                                } else {
                                    Log.d("DEBUG", "Failed to update password for " + modifiedEmail);
                                    Toast.makeText(Profile_page.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                                }
                            });

                }
            }
        });
    }
    private void fetchUserDetails() {
        DatabaseReference sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        // Convert email to match Firebase keys (replace '.' with ',')
        String modifiedEmail = userEmail.replace(".", ",");

        // Fetch Password from Registration_data
        passwordReference.child(modifiedEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String password = task.getResult().child("password").getValue(String.class);

                if (password != null) {
                    passwordTextView.setText(password); // Display fetched password
                }
            } else {
                Log.d("DEBUG", "Password not found in Registration_data.");
                Toast.makeText(Profile_page.this, "Password not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch Other Details from Sheet1
        sheetRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String email = snapshot.child("Official Email ID").getValue(String.class);

                    if (email != null && email.equals(userEmail)) {
                        String empId = String.valueOf(snapshot.child("Emp ID").getValue(Long.class));
                        String name = snapshot.child("Employee Name").getValue(String.class);
                        String team = snapshot.child("Team").getValue(String.class);
                        String mobile = String.valueOf(snapshot.child("MobileNo").getValue(Long.class));

                        updateUI(empId, name, email, team, mobile);
                        return;
                    }
                }
            }
            Log.d("DEBUG", "User not found in Sheet1.");
            Toast.makeText(Profile_page.this, "User not found", Toast.LENGTH_SHORT).show();
        });
    }


    private void updateUI(String empId, String name, String email, String team, String mobile) {
        // Assuming you have TextViews in your layout
        TextView empIdTextView = findViewById(R.id.employee_id_profile);
        TextView nameTextView = findViewById(R.id.employee_name_profile);
        TextView emailTextView = findViewById(R.id.employee_email_profile);
        TextView teamTextView = findViewById(R.id.employee_team_profile);
        TextView mobileTextView = findViewById(R.id.employee_mobile_profile);

        empIdTextView.setText(empId);
        nameTextView.setText(name);
        emailTextView.setText(email);
        teamTextView.setText(team);
        mobileTextView.setText(mobile);
    }
    private void fetchVendorDetails() {
        DatabaseReference vendorRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Vendor_details");

        // Fetch Vendor details
        vendorRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String phoneNumber = String.valueOf(snapshot.child("Phone_Number").getValue(Long.class));
                    String designation = snapshot.child("designation").getValue(String.class);
                    String email = snapshot.child("email_id").getValue(String.class);
                    String empId = String.valueOf(snapshot.child("employeeId").getValue(Long.class));
                    String name = snapshot.child("name").getValue(String.class);

                    updateVendorUI(phoneNumber, designation, email, name);
                    return; // Stop after fetching the first vendor (if needed)
                }
            } else {
                Log.d("DEBUG", "Vendor not found in Vendor_details.");
                Toast.makeText(Profile_page.this, "Vendor details not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to update UI with vendor details
    private void updateVendorUI(String phone, String designation, String email,String name) {

        TextView empIdTextView = findViewById(R.id.employee_id_profile);
        TextView nameTextView = findViewById(R.id.employee_name_profile);
        TextView emailTextView = findViewById(R.id.employee_email_profile);
        TextView designationTextView = findViewById(R.id.employee_team_label);
        TextView teamTextView = findViewById(R.id.employee_team_profile);
        TextView mobileTextView = findViewById(R.id.employee_mobile_profile);

        empIdTextView.setText("0");
        mobileTextView.setText(phone);
        designationTextView.setText("Designation");
        teamTextView.setText(designation);
        emailTextView.setText(email);
        nameTextView.setText(name);
    }


}