package com.example.cab_approval_system;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignupTabFragment extends Fragment {

    private DatabaseReference databaseReference, employeeReference, internReference, vendorReference;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize database references
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");
        employeeReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
        internReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Interns_data");
        vendorReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Vendor_details");

        // Initialize views
        EditText employeeIdField = view.findViewById(R.id.signup_emp_id);
        EditText nameField = view.findViewById(R.id.signup_name);
        EditText emailField = view.findViewById(R.id.signup_email);
        EditText passwordField = view.findViewById(R.id.signup_password);
        EditText confirmPasswordField = view.findViewById(R.id.signup_confirm);
        Button submitButton = view.findViewById(R.id.signup_button);
        TextView loginPageLink = view.findViewById(R.id.loginpage_link);
        ImageView passwordToggle = view.findViewById(R.id.password_toggle);
        ImageView confirmPasswordToggle = view.findViewById(R.id.confirm_password_toggle);

        // Initialize password visibility state
        boolean[] isPasswordVisible = {false};  // Using an array since lambdas require effectively final variables
        boolean[] isConfirmPasswordVisible = {false};

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

        // Set confirm password toggle listener
        confirmPasswordField.setTransformationMethod(new PasswordTransformationMethod());
        confirmPasswordToggle.setOnClickListener(v -> {  // Change 'view' to 'v'
            if (isConfirmPasswordVisible[0]) {
                confirmPasswordField.setTransformationMethod(new PasswordTransformationMethod()); // Hide password
                confirmPasswordToggle.setImageResource(R.drawable.closed_eye);
            } else {
                confirmPasswordField.setTransformationMethod(null); // Show password
                confirmPasswordToggle.setImageResource(R.drawable.opened_eye);
            }
            isConfirmPasswordVisible[0] = !isConfirmPasswordVisible[0];
        });

        // Setup Spinner
        Spinner designationSpinner = view.findViewById(R.id.designation_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.designation_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        designationSpinner.setAdapter(adapter);


        View[] blurView = {emailField, passwordField, employeeIdField, nameField, passwordField, confirmPasswordField, submitButton, loginPageLink, designationSpinner };

        for (View v : blurView) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Enable software rendering

            // Apply blur only to the background, not text
            if (v.getBackground() != null) {
                v.getBackground().mutate().setFilterBitmap(true);
                v.getBackground().setAlpha(254);  // Optional: Adjust transparency for better effect
            }
        }

        // setting the emp id field disbaled or enable based on designation
        designationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDesignation = parent.getItemAtPosition(position).toString();

                if (selectedDesignation.equals("Vendor")) {
                    employeeIdField.setEnabled(false);
                    employeeIdField.setText(String.valueOf(0));
                } else {
                    employeeIdField.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set initial visibility of the login link
        loginPageLink.setVisibility(View.GONE);

        submitButton.setOnClickListener(v -> {
            String designation = designationSpinner.getSelectedItem().toString();
            String employeeId = employeeIdField.getText().toString();
            String name = nameField.getText().toString();
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            String confirmPassword = confirmPasswordField.getText().toString();

            if (!validateInputFields(employeeId, name, email, password, confirmPassword)) {
                return;
            }

            if (designation.equals("Employee")) {
                validateAndRegisterEmployee(employeeId, name, email, password, loginPageLink);
            } else if (designation.equals("Intern")) {
                validateAndRegisterIntern(employeeId, name, email, password, loginPageLink);
            } else if (designation.equals("Vendor")) {
                validateAndRegisterVendor(vendorReference, "email_id", "0", email, name, password, loginPageLink);
            }
        });

        // Navigate to Login fragment when clicking the login link
        loginPageLink.setOnClickListener(v -> {
            if (getActivity() != null) {
                ViewPager2 viewPager2 = getActivity().findViewById(R.id.view_pager);
                if (viewPager2 != null) {
                    viewPager2.setCurrentItem(0);
                }
            }
        });
    }

    private boolean validateInputFields(String employeeId, String name, String email, String password, String confirmPassword) {
        if (employeeId.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!email.matches("^[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]+$")) {
            Toast.makeText(getContext(), "Invalid email format!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 8 || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            Toast.makeText(getContext(), "Password must be at least 8 characters long and include letters and numbers.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void validateAndRegisterEmployee(String employeeId, String name, String email, String password, TextView loginPageLink) {
        employeeReference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        boolean isValid = false;
                        String dbEmpId = "";
                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            if (snapshot.child("Emp ID").exists()) {
                                Long empIdLong = snapshot.child("Emp ID").getValue(Long.class);
                                if (empIdLong != null) {
                                    dbEmpId = empIdLong.toString();
                                }
                            }
                            if (dbEmpId.equals(employeeId)) {
                                isValid = true;
                                break;
                            }
                        }
                        if (isValid) {
                            saveRegistrationData(employeeId, name, email, password, loginPageLink);
                        } else {
                            Toast.makeText(getContext(), "Employee ID mismatch!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Employee not found in the system!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void validateAndRegisterIntern(String employeeId, String name, String email, String password, TextView loginPageLink) {
        internReference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    Log.d("Email id"," "+email);
                    if (task.isSuccessful() && task.getResult().exists()) {
                        boolean isValid = false;
                        String dbEmpId = "";
                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            if (snapshot.child("Emp ID").exists()) {
                                Long empIdLong = snapshot.child("Emp ID").getValue(Long.class);
                                if (empIdLong != null) {
                                    dbEmpId = empIdLong.toString();
                                }
                            }
                            if (dbEmpId.equals(employeeId)) {
                                isValid = true;
                                break;
                            }
                        }
                        if (isValid) {
                            saveRegistrationData(employeeId, name, email, password, loginPageLink);
                        } else {
                            Toast.makeText(getContext(), "Intern ID mismatch!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Intern not found in the system!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void validateAndRegisterVendor(DatabaseReference reference, String idKey, String employeeId, String email, String name, String password, TextView loginPageLink) {
        reference.orderByChild(idKey).equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DataSnapshot snapshot = task.getResult();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                            }
                            saveRegistrationData(employeeId, name, email, password, loginPageLink);
                        } else {
                        }
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }



    private void saveRegistrationData(String employeeId, String name, String email, String password, TextView loginPageLink) {
        // Check if the user is already registered in the Registration_data node
        databaseReference.child(email.replace(".", ","))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // User already exists
                        Toast.makeText(getContext(), "User already registered!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Fetch FCM token and register new user
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (tokenTask.isSuccessful()) {
                                        String token = tokenTask.getResult();
                                        Map<String, Object> employee = new HashMap<>();
                                        employee.put("employeeId", Integer.parseInt(employeeId));
                                        employee.put("name", name);
                                        employee.put("email", email);
                                        employee.put("password", password);
                                        employee.put("fcm_token", token);

                                        // Save the new registration
                                        databaseReference.child(email.replace(".", ","))
                                                .setValue(employee)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(getContext(), "Registration saved!", Toast.LENGTH_SHORT).show();
                                                    clearInputFields();
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(() -> loginPageLink.setVisibility(View.VISIBLE));
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getContext(), "Failed to register user!", Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Toast.makeText(getContext(), "Failed to fetch FCM token!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }


    private void clearInputFields() {
        EditText employeeIdField = requireView().findViewById(R.id.signup_emp_id);
        EditText nameField = requireView().findViewById(R.id.signup_name);
        EditText emailField = requireView().findViewById(R.id.signup_email);
        EditText passwordField = requireView().findViewById(R.id.signup_password);
        EditText confirmPasswordField = requireView().findViewById(R.id.signup_confirm);

        employeeIdField.setText("");
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }
}