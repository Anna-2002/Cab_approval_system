package com.example.cab_approval_system;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class History_page extends AppCompatActivity {

    private String user_emial;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);
        user_emial = getIntent().getStringExtra("email");
        Home_Screen.setupBottomNavigation(this,user_emial);
    }
}