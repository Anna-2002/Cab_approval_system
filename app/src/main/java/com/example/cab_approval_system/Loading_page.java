package com.example.cab_approval_system;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Loading_page extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading_page);

        ImageView appIcon = findViewById(R.id.app_icon);
        TextView appName = findViewById(R.id.app_title);

        // Load animations from res/anim/
        Animation zoomInAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation dropEffectAnim = AnimationUtils.loadAnimation(this, R.anim.drop_effect);

        // Start animations
        appIcon.startAnimation(zoomInAnim);
        appName.startAnimation(dropEffectAnim);

        // Delay for 5 seconds before navigating to the login page
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Loading_page.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish(); // Finish the current activity to prevent going back to it
        }, 700); // 5000 milliseconds = 5 seconds
    }
}