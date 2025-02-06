package com.example.qurannexus.core.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qurannexus.R;
import com.example.qurannexus.features.graphs.GraphSurfaceView;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        GraphSurfaceView graphSurfaceView = new GraphSurfaceView(this);
//        setContentView(graphSurfaceView);
        setContentView(R.layout.activity_test);


    }

}