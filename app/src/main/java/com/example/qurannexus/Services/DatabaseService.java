package com.example.qurannexus.Services;

import android.util.Log;

import com.example.qurannexus.Models.AyatModel;
import com.example.qurannexus.Models.SurahModel;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class DatabaseService {
    private static final String TAG = "DatabaseHelper";
    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> surahCollection;
    private MongoCollection<Document> ayahCollection;
    private MongoCollection<Document> bookmarkCollection;
    private DatabaseInitCallback initCallback;
    private MongoCollection<Document> bookmarkAyatCollection;

    public DatabaseService(String appID) {
        App app = new App(new AppConfiguration.Builder(appID).build());
        app.loginAsync(Credentials.anonymous(), new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if (result.isSuccess()) {
                    Log.v(TAG, "Logged in");
                    User user = app.currentUser();
                    MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                    mongoDatabase = mongoClient.getDatabase("quran_database");
                    surahCollection = mongoDatabase.getCollection("surahs");
                    ayahCollection = mongoDatabase.getCollection("ayahs");
                } else {
                    Log.e(TAG, "Failed to log in", result.getError());
                }
            }
        });
    }
    public DatabaseService(String appID, DatabaseInitCallback initCallback) {
        this.initCallback = initCallback;
        App app = new App(new AppConfiguration.Builder(appID).build());
        app.loginAsync(Credentials.anonymous(), new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if (result.isSuccess()) {
                    Log.v(TAG, "Logged in");
                    User user = app.currentUser();
                    MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                    mongoDatabase = mongoClient.getDatabase("quran_database");
                    surahCollection = mongoDatabase.getCollection("surahs");
                    ayahCollection = mongoDatabase.getCollection("ayahs");
                    bookmarkCollection = mongoDatabase.getCollection("bookmarked_ayahs");

                    initCallback.onInitSuccess();
                } else {
                    Log.e(TAG, "Failed to log in", result.getError());
                    initCallback.onInitFailure(result.getError());
                }
            }
        });
    }

    public void getCombinedVerses(int surahIndex, VersesCallback callback) {
        Document query = new Document("surah_index", surahIndex);
        RealmResultTask<MongoCursor<Document>> findTask = ayahCollection.find(query).iterator();
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                StringBuilder combinedVerses = new StringBuilder();
                int ayatNumber = 1;

                while (results.hasNext()) {
                    Document currentDoc = results.next();
                    String arabicAyatNumber = convertToArabicNumber(ayatNumber);
                    String waqafSymbol = " {" + arabicAyatNumber + "}۝ "; // Custom Waqaf symbol with Arabic number inside

                    combinedVerses.append(currentDoc.getString("text")).append(waqafSymbol);
                    ayatNumber++;
                }
                callback.onVersesLoaded(combinedVerses.toString().trim());
            } else {
                Log.e(TAG, "Failed to fetch verses", task.getError());
            }
        });
    }

    private String convertToArabicNumber(int number) {
        String[] arabicNumbers = {"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"};
        StringBuilder arabicNumber = new StringBuilder();
        String numStr = String.valueOf(number);

        for (char digit : numStr.toCharArray()) {
            arabicNumber.append(arabicNumbers[Character.getNumericValue(digit)]);
        }

        return arabicNumber.toString();
    }

    public void getVersesByAyat(int surahIndex, AyatCallback callback) {
        Document query = new Document("surah_index", surahIndex);
        RealmResultTask<MongoCursor<Document>> findTask = ayahCollection.find(query).iterator();
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                List<AyatModel> ayahList = new ArrayList<>();
                List<Integer> bookmarkedAyatIndices = new ArrayList<>();

                // Fetch all bookmarked ayat indices for this surah
                Document bookmarkQuery = new Document("surah_index", surahIndex);
                RealmResultTask<MongoCursor<Document>> bookmarkFindTask = bookmarkCollection.find(bookmarkQuery).iterator();
                bookmarkFindTask.getAsync(bookmarkTask -> {
                    if (bookmarkTask.isSuccess()) {
                        MongoCursor<Document> bookmarkResults = bookmarkTask.get();
                        while (bookmarkResults.hasNext()) {
                            Document bookmarkDoc = bookmarkResults.next();
                            bookmarkedAyatIndices.add(bookmarkDoc.getInteger("ayah_index"));
                        }

                        // Iterate through the ayat results and create AyatModel instances
                        while (results.hasNext()) {
                            Document currentDoc = results.next();
                            String arabicScript = "";
                            String englishTranslation = "";
                            int intAyatNumber = currentDoc.getInteger("index");
                            String ayatNumber = "";
                            boolean isBookmarked = bookmarkedAyatIndices.contains(intAyatNumber);

                            if (currentDoc.getString("text") != null) {
                                arabicScript = currentDoc.getString("text");
                            }
                            if (currentDoc.getString("translate_mal") != null) {
                                englishTranslation = currentDoc.getString("translate_mal");
                            }
                            if (intAyatNumber != 0) {
                                ayatNumber = String.valueOf(intAyatNumber);
                            }
                            ayahList.add(new AyatModel(
                                    arabicScript,
                                    englishTranslation,
                                    ayatNumber,
                                    surahIndex,
                                    intAyatNumber,
                                    isBookmarked
                            ));
                        }
                        callback.onAyatLoaded(ayahList);
                    } else {
                        Log.e(TAG, "Failed to fetch bookmarks", bookmarkTask.getError());
                    }
                });
            } else {
                Log.e(TAG, "Failed to fetch ayat", task.getError());
            }
        });
    }

    public void updateBookmarkStatus(String surahNumber, boolean isBookmarked, UpdateCallback callback) {
        Document filter = new Document("index", Integer.parseInt(surahNumber));
        Document updateDoc = new Document("$set", new Document("bookmarked", isBookmarked));

        surahCollection.updateOne(filter, updateDoc).getAsync(task -> {
            if (task.isSuccess()) {
                callback.onUpdateComplete(true);
            } else {
                Log.e(TAG, "Failed to update bookmark status", task.getError());
                callback.onUpdateComplete(false);
            }
        });
    }

    public void addBookmark(int surahIndex, int ayatIndex, UpdateCallback callback) {
        Document bookmark = new Document("surah_index", surahIndex)
                .append("ayah_index", ayatIndex);

        MongoCollection<Document> bookmarksCollection = mongoDatabase.getCollection("bookmarked_ayahs");

        bookmarksCollection.insertOne(bookmark).getAsync(task -> {
            if (task.isSuccess()) {
                callback.onUpdateComplete(true);
            } else {
                Log.e(TAG, "Failed to add bookmark", task.getError());
                callback.onUpdateComplete(false);
            }
        });
    }
    public void removeBookmark(int surahIndex, int ayatIndex, UpdateCallback callback) {
        Document filter = new Document("surah_index", surahIndex)
                .append("ayah_index", ayatIndex);
        MongoCollection<Document> bookmarkCollection = mongoDatabase.getCollection("bookmarked_ayahs");

        bookmarkCollection.deleteOne(filter).getAsync(task -> {
            if (task.isSuccess()) {
                callback.onUpdateComplete(true);
            } else {
                Log.e(TAG, "Failed to remove bookmark", task.getError());
                callback.onUpdateComplete(false);
            }
        });
    }
    public interface UpdateCallback {
        void onUpdateComplete(boolean success);
    }
    public interface DatabaseInitCallback {
        void onInitSuccess();
        void onInitFailure(Throwable error);
    }
    public interface SurahListCallback {
        void onSurahListLoaded(List<SurahModel> surahModels);
    }

    public interface VersesCallback {
        void onVersesLoaded(String combinedVerses);
    }

    public interface AyatCallback {
        void onAyatLoaded(List<AyatModel> ayahList);
    }
}
