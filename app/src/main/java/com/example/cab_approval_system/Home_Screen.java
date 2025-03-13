package com.example.cab_approval_system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class Home_Screen extends AppCompatActivity {
    private String user_email,user_role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        // Call the helper method to set up the bottom navigation buttons
        user_email = getIntent().getStringExtra("email");
        user_role = getIntent().getStringExtra("userRole");
        setupBottomNavigation(this,user_email,user_role);
    }

    // This method sets up the bottom navigation buttons and their click listeners
    public static void setupBottomNavigation(Context context, String user_email, String user_role) {
        // Initialize the ImageButtons with context
        CardView homeButton = ((AppCompatActivity) context).findViewById(R.id.home_cardview);
        CardView historyButton = ((AppCompatActivity) context).findViewById(R.id.history_cardview);
        CardView profileButton = ((AppCompatActivity) context).findViewById(R.id.profile_cardview);


        // Make sure CardView is clickable
        homeButton.setClickable(true);
        historyButton.setClickable(true);
        profileButton.setClickable(true);

        // Fix: Allow click events to work properly
        homeButton.setFocusable(true);
        historyButton.setFocusable(true);
        profileButton.setFocusable(true);

        // Set up onClick listeners for navigation
        homeButton.setOnClickListener(v -> {
            // Navigate to Home page
            Intent intent = new Intent(context, Home_page.class);
            intent.putExtra("email", user_email);
            intent.putExtra("userRole", user_role);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Prevent new instances
            context.startActivity(intent);
        });

        historyButton.setOnClickListener(v -> {
            // Navigate to History page
            Intent intent = new Intent(context, History_page.class);
            intent.putExtra("email",user_email);
            context.startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            // Navigate to Profile page
            Intent intent = new Intent(context, Profile_page.class);
            intent.putExtra("email",user_email);
            context.startActivity(intent);
        });
    }
}