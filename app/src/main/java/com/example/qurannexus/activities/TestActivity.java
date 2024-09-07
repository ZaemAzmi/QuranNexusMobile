package com.example.qurannexus.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.qurannexus.R;
import com.example.qurannexus.interfaces.JokeApi;
import com.example.qurannexus.models.Joke;
import com.example.qurannexus.services.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestActivity extends AppCompatActivity {
    private TextView jokeTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        jokeTextView = findViewById(R.id.jokeTextView);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://official-joke-api.appspot.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JokeApi jokeApi = retrofit.create(JokeApi.class);

        jokeApi.getRandomJoke().enqueue(new Callback<Joke>() {
            @Override
            public void onResponse(Call<Joke> call, retrofit2.Response<Joke> response) {
                if (response.isSuccessful()) {
                    Joke joke = response.body();
                    jokeTextView.setText(joke.getSetup() + "\n" + joke.getPunchline());
                }
            }

            @Override
            public void onFailure(Call<Joke> call, Throwable t) {
                jokeTextView.setText("Failed to get a joke");
            }
        });
    }
}