package com.example.qurannexus.features.onboard;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.example.qurannexus.R;
import com.example.qurannexus.features.auth.AuthActivity;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Timer timer =new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent i = new Intent(new Intent(SplashActivity.this, AuthActivity.class));
                startActivity(i);
                finish();
            }
        },2000);


    }
}
