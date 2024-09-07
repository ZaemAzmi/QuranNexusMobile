package com.example.qurannexus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.models.SurahListAdapter;
import com.example.qurannexus.models.SurahModel;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import org.bson.Document;
import java.util.ArrayList;
import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class MainActivity extends AppCompatActivity {
    String appID = "application-0-plbqdoy";
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    ArrayList<SurahModel> surahModels = new ArrayList<>();
    ArrayList<SurahModel> filteredSurahModels = new ArrayList<>();
    String layoutType = "verseByVerse";
    SurahListAdapter surahListAdapter;
    TabLayout tabLayout;
    SearchView searchView;
    int currentTab = 0;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm.init(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupNavigationDrawer();

        tabLayout = findViewById(R.id.tabLayout);
        searchView = findViewById(R.id.searchSurahView);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    updateUI(surahModels);
                } else if (currentTab == 2) {
                    // Saved Surah Tab
                    ArrayList<SurahModel> savedSurahs = new ArrayList<>();
                    for (SurahModel surah : surahModels) {
                        if (surah.isBookmarked()) {
                            savedSurahs.add(surah);
                        }
                    }
                    updateUI(savedSurahs);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSurahList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSurahList(newText);
                return true;
            }
        });
        setUpSurahListModels();
    }
    private void filterSurahList(String query) {
        query = query.toLowerCase();
        filteredSurahModels.clear();
        ArrayList<SurahModel> currentList = new ArrayList<>();

        // Determine the current list based on the selected tab
        if (currentTab == 0) {
            currentList = surahModels;
        } else if (currentTab == 2) {
            for (SurahModel surah : surahModels) {
                if (surah.isBookmarked()) {
                    currentList.add(surah);
                }
            }
        }

        // Apply the search filter on the determined list
        for (SurahModel surah : currentList) {
            if (surah.getSurahName().toLowerCase().contains(query)) {
                filteredSurahModels.add(surah);
            }
        }
        updateUI(filteredSurahModels);
    }
    private void setUpSurahListModels(){
        App app = new App(new AppConfiguration.Builder(appID).build());
        app.loginAsync(Credentials.anonymous(), new App.Callback<User>(){
            @Override
            public void onResult(App.Result<User> result){
                if(result.isSuccess()){
                    Log.v("User","Logged in");
                    User user = app.currentUser();
                    mongoClient = user.getMongoClient("mongodb-atlas");
                    mongoDatabase = mongoClient.getDatabase("quran_database");
                    MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("surahs");
//                    Document queryFilter = new Document().append("title","The Great Train Robbery");
                    RealmResultTask<MongoCursor<Document>> findTask = mongoCollection.find().iterator();

                    findTask.getAsync(task ->{
                        if(task.isSuccess()){
                            MongoCursor<Document> results = task.get();
                            if(!results.hasNext()){
                                Log.v("result","cant find");
                            }
                            while(results.hasNext()){
                                Document currentDoc = results.next();
                                String surahNumber ="";
                                String arabicSurahName ="";
                                String surahName ="";
                                String surahMeaning ="";
                                String ayatNumber ="";
                                boolean isBookmarked = false;
                                if(currentDoc.getInteger("index")!= null){
                                    surahNumber = String.valueOf(currentDoc.getInteger("index"));
                                }if(currentDoc.getString("name")!= null){
                                    arabicSurahName = currentDoc.getString("name");
                                }if(currentDoc.getString("tname")!= null){
                                    surahName = currentDoc.getString("tname");
                                }if(currentDoc.getString("ename")!= null){
                                    surahMeaning = currentDoc.getString("ename");
                                }if(currentDoc.getInteger("ayas")!= null){
                                    ayatNumber = String.valueOf(currentDoc.getInteger("ayas"));
                                }if(currentDoc.getBoolean("bookmarked") != null){
                                    isBookmarked = currentDoc.getBoolean("bookmarked");
                                }
                                surahModels.add(new SurahModel(
                                        surahName,
                                        arabicSurahName,
                                        surahNumber,
                                        surahMeaning,
                                        ayatNumber,
                                        isBookmarked
                                ));
                            }
                            updateUI(surahModels);
                        }
                    });
                } else {
                    Log.e("Error", "Failed to log in", result.getError());
                }
            }
        });
    }
    private void updateUI(ArrayList<SurahModel> filteredSurahs) {
        RecyclerView surahRecyclerView = findViewById(R.id.SurahRecyclerView);
        surahListAdapter = new SurahListAdapter(this, filteredSurahs, layoutType);
        surahRecyclerView.setAdapter(surahListAdapter);
        surahRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupNavigationDrawer() {
        // Set up the side menu button to open the drawer
        ImageView sideMenuButton = findViewById(R.id.sideMenuButton);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.side_navigation_view);
        sideMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(navigationView);
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                handleNavigationItemSelected(menuItem);
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }
    private void handleNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_home) {

        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_tajweed) {
            // Handle the tajweed action
        }else if(itemId == R.id.nav_test) {
            Intent intent = new Intent(MainActivity.this, TestActivity.class);
            startActivity(intent);
        } else {
            // Handle default action
        }
    }
}
