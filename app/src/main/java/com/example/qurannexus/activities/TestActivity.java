package com.example.qurannexus.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.example.qurannexus.R;
import com.example.qurannexus.fragments.HomeFragment;
import com.example.qurannexus.fragments.IrabFragment;
import com.example.qurannexus.fragments.SettingsFragment;
import com.example.qurannexus.fragments.TajweedFragment;
import com.example.qurannexus.interfaces.JokeApi;
import com.example.qurannexus.models.Joke;
import com.example.qurannexus.services.ApiService;
import com.example.qurannexus.services.UIService;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
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

    protected final int home=1;
    protected final int irab=2;
    protected final int tajweed=3;
    protected final int test=4;

//    private void setupMeow(){
//        MeowBottomNavigation meowBottomNavigation = findViewById(R.id.meowNavTest);
//        meowBottomNavigation.add(new MeowBottomNavigation.Model(home, R.drawable.ic_home));
//        meowBottomNavigation.add(new MeowBottomNavigation.Model(irab, R.drawable.ic_home));
//        meowBottomNavigation.add(new MeowBottomNavigation.Model(tajweed, R.drawable.ic_home));
//        meowBottomNavigation.add(new MeowBottomNavigation.Model(test, R.drawable.ic_home));
//
//        meowBottomNavigation.setOnClickMenuListener(new MeowBottomNavigation.ClickListener() {
//            @Override
//            public void onClickItem(MeowBottomNavigation.Model model) {
//                Toast.makeText(TestActivity.this, "Item click: " + model.getId(), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        meowBottomNavigation.setOnShowListener(new MeowBottomNavigation.ShowListener() {
//            Fragment selectedFragment = null;
//            @Override
//            public void onShowItem(MeowBottomNavigation.Model model) {
//                String name = "";
//                switch (model.getId()) {
//                    case home:
//                        selectedFragment = new HomeFragment();
//                        break;
//                    case irab:
//                        selectedFragment = new IrabFragment();
//                        break;
//                    case tajweed:
//                        selectedFragment = new TajweedFragment();
//                        break;
//                    case test:
//                        Intent i = new Intent(new Intent(TestActivity.this, TestActivity.class));
//                        startActivity(i);
//                        break;
//                }
//                loadFragment(selectedFragment);
//            }
//        });
//    }
//
//    private void loadFragment(Fragment fragment) {
//        if (fragment == null) {
//            Toast.makeText(this, "Error: Fragment is null", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.mainFragmentContainer, fragment)
//                .commit();
//    }

}