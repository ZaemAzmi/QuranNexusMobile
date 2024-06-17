package com.example.qurannexus;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.qurannexus.Models.SurahModel;
import org.bson.Document;
import java.util.ArrayList;
import io.realm.Realm;
import io.realm.internal.async.RealmResultTaskImpl;
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
    private TextView test;;
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    ArrayList<SurahModel> surahModels = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm.init(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setUpSurahListModels();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


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
                    mongoDatabase = mongoClient.getDatabase("sample_mflix");
                    MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("movies");
//                   Document testDoc = mongoCollection.count();
                    Document queryFilter = new Document().append("title","The Great Train Robbery");
                    RealmResultTask<MongoCursor<Document>> findtask = mongoCollection.find().iterator();
                    mongoCollection.findOne(queryFilter).getAsync(findResult -> {
                        if(findResult.isSuccess()){
                            Document resultData = findResult.get();
                            String data =resultData.getString("index");
                            if(data != null && !data.isEmpty())
                                test.setText(resultData.getString("title"));
                            else test.setText("kosong");
                        }else{
                            test.setText("nothing");
                        }
                    });
                } else {
                    Log.e("Error", "Failed to log in", result.getError());
                }
            }
        });

//        String [] surahNames = getResources().getStringArray(R.array.)
//        String [] arabicSurahNames = getResources().getStringArray(R.array.);
//        String [] surahNumbers = getResources().getStringArray(R.array.);
//        String [] surahMeanings = getResources().getStringArray(R.array.);
//        int [] surahNames = getResources().getStringArray(R.array.);
// instead of map everything and display, just straight away display kot
        for(int i=1; i<= surahModels.size() ; i++){

            }
    }
}
