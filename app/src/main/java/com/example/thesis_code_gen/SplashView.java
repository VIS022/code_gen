package com.example.thesis_code_gen;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashView extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // Splash screen duration in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_view);
        // Delayed navigation to the Home screen after SPLASH_DELAY milliseconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashView.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the current activity to prevent users from navigating back to the splash screen
            }
        }, SPLASH_DELAY);
    }


}