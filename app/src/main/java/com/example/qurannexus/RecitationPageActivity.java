package com.example.qurannexus;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.Fragments.ByAyatRecitationFragment;
import com.example.qurannexus.Fragments.ByPageRecitationFragment;
import com.example.qurannexus.Models.AyatModel;
import com.example.qurannexus.Models.SurahListAdapter;
import com.example.qurannexus.Models.SurahModel;
import com.example.qurannexus.Services.DatabaseService;

import org.bson.Document;

import java.util.ArrayList;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class RecitationPageActivity extends AppCompatActivity {
    String appID = "application-0-plbqdoy";
    SurahModel surahModel;
    String layoutType = "verseByVerse";
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    ArrayList<AyatModel> ayatModels = new ArrayList<>();
    DatabaseService databaseService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recitation_page);

        databaseService = new DatabaseService("application-0-plbqdoy");
        surahModel = getIntent().getParcelableExtra("surahModel");
        layoutType = getIntent().getStringExtra("fragmentType");

        if (surahModel != null) {
            displayFragment(layoutType);
        }

        ToggleButton toggleLayoutButton = findViewById(R.id.layoutToggleButton);
        toggleLayoutButton.setChecked("pageByPage".equals(layoutType));
        toggleLayoutButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutType = isChecked ? "pageByPage" : "verseByVerse";
            displayFragment(layoutType);
        });

        ImageView bookmarkIcon = findViewById(R.id.bookmarkIcon);
        bookmarkIcon.setOnClickListener(v -> {
            // Toggle the bookmark status
            toggleBookmarkStatus();
        });
    }
    private void displayFragment(String fragmentType) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if ("verseByVerse".equals(fragmentType)) {
            ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahModel);
            transaction.replace(R.id.recitationFragmentContainerView, fragment);
        } else if ("pageByPage".equals(fragmentType)) {
            ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(surahModel);
            transaction.replace(R.id.recitationFragmentContainerView, fragment);
        }
        transaction.commit();
    }
    private void toggleBookmarkStatus() {
        surahModel.setBookmarked(!surahModel.isBookmarked());
        updateBookmarkStatusInDatabase(surahModel.getSurahNumber(), surahModel.isBookmarked());
    }

    private void updateBookmarkStatusInDatabase(String surahNumber, boolean isBookmarked) {
        // Assuming you have a method in DatabaseService to update the bookmark status
        databaseService.updateBookmarkStatus(surahNumber, isBookmarked, new DatabaseService.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    // Optionally, update UI or provide feedback to user
                    Toast.makeText(RecitationPageActivity.this, "Bookmark updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RecitationPageActivity.this, "Failed to update bookmark", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}