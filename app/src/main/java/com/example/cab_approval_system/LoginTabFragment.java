package com.example.cab_approval_system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginTabFragment extends Fragment {

    private DatabaseReference databaseReference, employeeReference;

    private SharedPreferences shared_userRole;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

        EditText emailField = view.findViewById(R.id.login_email);
        EditText passwordField = view.findViewById(R.id.login_password);
        Button loginButton = view.findViewById(R.id.login_button);
        Button registrationButton = view.findViewById(R.id.registration_button);
        ImageView login_image = view.findViewById(R.id.login_image);
        ImageView passwordToggle = view.findViewById(R.id.password_toggle);


        // Initialize password visibility state
        boolean[] isPasswordVisible = {false};  // Using an array since lambdas require effectively final variables

        passwordField.setTransformationMethod(new PasswordTransformationMethod());
        passwordToggle.setOnClickListener(v -> {  // Change 'view' to 'v'
            if (isPasswordVisible[0]) {
                passwordField.setTransformationMethod(new PasswordTransformationMethod()); // Hide password
                passwordToggle.setImageResource(R.drawable.closed_eye);
            } else {
                passwordField.setTransformationMethod(null); // Show password
                passwordToggle.setImageResource(R.drawable.opened_eye);
            }
            isPasswordVisible[0] = !isPasswordVisible[0];
        });



        View[] blurView = {emailField, passwordField, loginButton, registrationButton, login_image};

        for (View v : blurView) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Enable software rendering

            // Apply blur only to the background, not text
            if (v.getBackground() != null) {
                v.getBackground().mutate().setFilterBitmap(true);
                v.getBackground().setAlpha(254);  // Optional: Adjust transparency for better effect
            }
        }


        shared_userRole = requireActivity().getSharedPreferences("UserRole", Context.MODE_PRIVATE);

        loginButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // When button is pressed
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                    v.setAlpha(0.8f); // Slight transparency for a glossy effect
                    break;
                case MotionEvent.ACTION_UP:   // When button is released
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    v.setAlpha(1.0f); // Restore original transparency
                    break;
            }
            return false;
        });

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Input validation
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }
            // Authenticate with Firebase
            loginUser(email, password);
        });

        // Registration button click listener
        registrationButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Navigate to Registration fragment
                ViewPager2 viewPager2 = getActivity().findViewById(R.id.view_pager);
                if (viewPager2 != null) {
                    viewPager2.setCurrentItem(1); // Assuming Registration is at position 1
                }
            }
        });

        return view;
    }

    private void loginUser(String email, String password) {
        // Reference to Registration Data table
        DatabaseReference registrationReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Registration_data");

        registrationReference.orderByChild("email").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String dbPassword = snapshot.child("password").getValue(String.class);
                                if (password.equals(dbPassword)) {
                                    // Password matches, now fetch user role
                                    fetchUserRoleFromSheet(email);
                                    return;
                                }
                            }
                            // Invalid email or password
                            Toast.makeText(getContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Email not registered", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error connecting to Registration details", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleFromSheet(String email) {
        // Reference to Sheet1 (or intern) table
        DatabaseReference sheet1Reference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Sheet1");  // Change to intern table if needed

        // Query the Sheet1 table for the user's role
        sheet1Reference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String userRole = snapshot.child("Approval Matrix").getValue(String.class);
                                if (userRole != null) {
                                    // Successfully fetched the role, navigate to home page
                                    Toast.makeText(getContext(), "Login successful as " + userRole, Toast.LENGTH_SHORT).show();

                                    saveUserSession(email, userRole);
                                    navigateToHome(email, userRole);
                                    return;
                                }
                            }
                            // If role is not found in Sheet1, check Vendor_details
                            Toast.makeText(getContext(), "Role not found in Sheet1, checking Vendor table...", Toast.LENGTH_SHORT).show();
                        }
                    }
                    // If email not found in Sheet1, check Vendor_details
                    checkVendorTable(email);
                });
    }

    private void checkVendorTable(String email) {
        DatabaseReference vendorReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Vendor_details");

        vendorReference.orderByChild("email_id").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String userRole = snapshot.child("designation").getValue(String.class);
                                if (userRole != null) {
                                    saveUserSession(email, userRole);
                                    navigateToHome(email, userRole);
                                    return;
                                }
                            }
                        }
                    }
                    Toast.makeText(getContext(), "User role not found in Sheet1 or Vendor table", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserSession(String email, String userRole) {
        SharedPreferences.Editor editor = shared_userRole.edit();
        editor.putString("email", email);
        editor.putString("userRole", userRole);
        editor.apply();
    }

    private void navigateToHome(String email, String userRole) {
        Toast.makeText(getContext(), "Login successful as " + userRole, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), Home_page.class);
        intent.putExtra("email", email);
        intent.putExtra("userRole", userRole);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

}