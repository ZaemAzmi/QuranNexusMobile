package com.example.qurannexus.features.recitation.models;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qurannexus.R;
import com.example.qurannexus.features.auth.AuthActivity;
import com.example.qurannexus.features.bookmark.models.BookmarkRequest;
import com.example.qurannexus.features.bookmark.models.BookmarkResponse;
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse;
// Remove unused TextUtils import if it's not used elsewhere
// import com.example.qurannexus.features.recitation.extensions.TextUtils;
import com.example.qurannexus.features.words.WordDetailsActivity; // Correct import
import com.example.qurannexus.core.interfaces.QuranApi;
// Remove unused WordDetails and WordDetailsResponse from home.models if not used for other things
// import com.example.qurannexus.features.home.models.WordDetails;
// import com.example.qurannexus.features.home.models.WordDetailsResponse;
import com.example.qurannexus.core.network.ApiService;
// Remove unused SurahDetails if QuranMetadata provides all needed info directly or not needed here
// import com.example.qurannexus.core.utils.SurahDetails;
// import com.example.qurannexus.core.utils.QuranMetadata;
import com.example.qurannexus.features.recitation.audio.AudioPlayerManager;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@androidx.media3.common.util.UnstableApi
public class SurahRecitationByAyatAdapter extends RecyclerView.Adapter<SurahRecitationByAyatAdapter.MyViewHolder> {
    private QuranApi quranApi;
    Context context;
    ArrayList<ChapterAyah> ayahList;
    private String authToken;
    private AudioPlayerManager audioPlayerManager;
    // private MaterialCardView expandedAudioPlayer; // Not used in this adapter, can be removed

    public SurahRecitationByAyatAdapter(Context context, ArrayList<ChapterAyah> ayahList){
        this.context = context;
        this.ayahList = ayahList;
        this.quranApi = ApiService.getQuranClient().create(QuranApi.class);
        this.authToken = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("token", null);
        this.audioPlayerManager = new AudioPlayerManager(context, quranApi);
    }

    @NonNull
    @Override
    public SurahRecitationByAyatAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.card_item_single_ayat, parent, false);
        return new SurahRecitationByAyatAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurahRecitationByAyatAdapter.MyViewHolder holder, int position) {
        ChapterAyah ayah = ayahList.get(position);
        holder.arabicWordsContainer.removeAllViews(); // Clear before re-populating

        // This call will now set up the word TextViews and their long click listeners
        setupWordViewsAndClickListeners(holder, ayah);

        // Assuming getTranslations().get(1) is English. Consider a safer way to get specific translation.
        if (ayah.getTranslations() != null && ayah.getTranslations().size() > 1) {
            holder.englishTranslation.setText(ayah.getTranslations().get(1).getText());
        } else if (ayah.getTranslations() != null && !ayah.getTranslations().isEmpty()){
            holder.englishTranslation.setText(ayah.getTranslations().get(0).getText()); // Fallback
        } else {
            holder.englishTranslation.setText("No translation available.");
        }

        holder.ayatNumber.setText(ayah.getAyahKey()); // e.g., "1:1"

        holder.ayatCardAddNotesIcon.setOnClickListener(view -> {
            if (authToken == null) {
                showLoginDialog();
            } else {
                showAddNotesDialog(holder, ayah);
            }
        });
        holder.ayatCardBookmarkIcon.setImageResource(
                ayah.isBookmarked() ? R.drawable.ic_bookmarked : R.drawable.ic_bookmark
        );

        holder.ayatCardBookmarkIcon.setOnClickListener(v -> {
            if (authToken == null) {
                showLoginDialog();
            } else {
                if (ayah.isBookmarked()) {
                    removeBookmark(holder, ayah, position);
                } else {
                    addBookmarkWithNotes(holder, ayah, "");
                }
            }
        });

        holder.ayatCardPlayAudioIcon.setOnClickListener(v -> {
            // ChapterAyah chapterAyah = ayahList.get(position); // Already have 'ayah'
            audioPlayerManager.playAyah(ayah.getAyahKey());
        });
    }

    // Merged setupWordClickListeners and addWordTextView
    private void setupWordViewsAndClickListeners(MyViewHolder holder, ChapterAyah ayah) {
        if (ayah.getWords() == null) return;

        List<Word> words = new ArrayList<>(ayah.getWords());
        String waqafSign = "";
        String ayahNumberInArabic = new com.example.qurannexus.core.utils.UtilityService()
                .convertToArabicNumber(Integer.parseInt(ayah.getAyahIndex()));

        // Handle waqaf sign: it's usually the last "word" object in the API response for an ayah
        if (!words.isEmpty()) {
            Word lastWordObject = words.get(words.size() - 1);
            // A simple heuristic: if the last word's text is short (like a symbol)
            // and its translation is null or just the ayah number in parens.
            boolean isLikelyWaqf = (lastWordObject.getText() != null && lastWordObject.getText().length() <= 2 &&
                    (lastWordObject.getTranslation() == null ||
                            lastWordObject.getTranslation().matches("\\(\\d+\\)")));

            if (isLikelyWaqf) {
                // Don't use lastWordObject.getText() for waqaf. Use standard ayah end symbol.
                // The waqafView will now contain the ayah number.
                words.remove(words.size() - 1); // Remove it so it's not treated as a clickable word
            }
        }

        FlexboxLayout container = holder.arabicWordsContainer;
        container.setFlexDirection(FlexDirection.ROW_REVERSE); // For RTL

        // Add clickable word TextViews
        for (Word word : words) {
            if (word == null || word.getText() == null || word.getText().isEmpty()) continue;

            TextView wordView = new TextView(context);
            wordView.setText(word.getText());
            wordView.setTextColor(ContextCompat.getColor(context, R.color.white)); // Or your theme color
            float textSizeSp = context.getResources().getDimension(R.dimen.arabic_text_size) /
                    context.getResources().getDisplayMetrics().density;
            wordView.setTextSize(textSizeSp);
            wordView.setPadding(8, 8, 8, 8); // Adjust padding as needed
            wordView.setTypeface(ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs));
            wordView.setTextDirection(View.TEXT_DIRECTION_RTL);

            wordView.setOnLongClickListener(v -> {
                animateWord(v); // Optional animation
                // highlightWord(wordView); // Optional highlight
                // showPopupHint(wordView, "Tap for word analysis"); // Optional hint

                // Directly navigate to WordDetailsActivity with the word's Arabic text
                String clickedWordText = word.getText();
                String wordKey = ayah.getSurahId() + ":" + ayah.getAyahIndex() + ":" + word.getWordIndex(); // Construct S:A:W
                if (clickedWordText != null && !clickedWordText.isEmpty()) {
                    Intent intent = new Intent(context, WordDetailsActivity.class);
                    intent.putExtra(WordDetailsActivity.EXTRA_WORD_KEY_FROM_RECITATION, wordKey);
                    intent.putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, clickedWordText);
                    Log.d("RecitationAdapter", "Navigating with WordKey: " + wordKey + ", WordText: " + clickedWordText);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Word data not available.", Toast.LENGTH_SHORT).show();
                }
                return true; // Consume the long click
            });
            container.addView(wordView);
        }

        // Add the ayah number (waqf) at the end (which is visually left in RTL)
        TextView waqafView = new TextView(context);
        waqafView.setText(String.format(" %s ", ayahNumberInArabic)); // Add spaces for padding from circle
        waqafView.setTextColor(ContextCompat.getColor(context, R.color.white)); // Ayah number color
//        waqafView.setBackgroundResource(R.drawable.ayah_number_background); // Circular background
        waqafView.setGravity(Gravity.CENTER);
        waqafView.setTextSize(context.getResources().getDimension(R.dimen.arabic_text_size) /
                context.getResources().getDisplayMetrics().density);
        waqafView.setTypeface(ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs));
        waqafView.setPadding(8,0,8,0);

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8,0,8,0); // Add some margin around the ayah number
        waqafView.setLayoutParams(params);

        container.addView(waqafView);
    }


    // REMOVE fetchWordDetails method as it's no longer needed for this navigation
    // private void fetchWordDetails(String wordKey) { ... }


    // ... (keep highlightWord, showPopupHint, animateWord if you still want those UI effects)
    private void highlightWord(TextView wordView) {
        wordView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
        wordView.postDelayed(() -> wordView.setBackgroundResource(0), 1000);
    }

    private void showPopupHint(View anchor, String message) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.dialog_word_hint, null);
        TextView hintTextView = popupView.findViewById(R.id.hintTextView);
        hintTextView.setText(message);

        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - anchor.getHeight() - 20);
        new Handler(Looper.getMainLooper()).postDelayed(popupWindow::dismiss, 1500);
    }

    private void animateWord(View wordView) {
        wordView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> wordView.animate().scaleX(1f).scaleY(1f).setDuration(200))
                .start();
    }

    // ... (keep addBookmarkWithNotes, removeBookmark, getItemCount, MyViewHolder, showAddNotesDialog, showLoginDialog)
    // Make sure MyViewHolder has `FlexboxLayout arabicWordsContainer;`
    private void addBookmarkWithNotes(MyViewHolder holder, ChapterAyah ayah, String notes) {
        Map<String, Object> verseProperties = new HashMap<>();
        verseProperties.put("verse_id", String.valueOf(ayah.getId()));
        verseProperties.put("chapter_id", ayah.getSurahId());

        BookmarkRequest request = new BookmarkRequest(
                "verse",
                verseProperties,
                notes
        );

        Call<BookmarkResponse> call = quranApi.addBookmark("Bearer " + authToken, request);
        call.enqueue(new Callback<BookmarkResponse>() {
            @Override
            public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BookmarkResponse bookmarkResponse = response.body();
                    if ("success".equals(bookmarkResponse.getStatus())) {
                        ayah.setBookmarked(true);
                        holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmarked);
                        String message = notes.isEmpty() ?
                                "Bookmark added successfully" :
                                "Bookmark and notes added successfully";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to add bookmark", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                        Toast.makeText(context, "Failed to add bookmark: " + errorBody,
                                Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(context, "Failed to add bookmark", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<BookmarkResponse> call, Throwable t) {
                Toast.makeText(context, "Error adding bookmark: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void removeBookmark(MyViewHolder holder, ChapterAyah ayah, int position) {
        if (authToken == null) {
            Toast.makeText(context, "Please login to remove bookmark", Toast.LENGTH_SHORT).show();
            return;
        }
        Call<RemoveBookmarkResponse> call = quranApi.removeBookmark("Bearer " + authToken, "verse", ayah.getId());
        call.enqueue(new Callback<RemoveBookmarkResponse>() {
            @Override
            public void onResponse(Call<RemoveBookmarkResponse> call,
                                   Response<RemoveBookmarkResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RemoveBookmarkResponse removeResponse = response.body();
                    if ("success".equals(removeResponse.getStatus())) {
                        ayah.setBookmarked(false);
                        holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmark);
                        Toast.makeText(context, "Bookmark removed successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to remove bookmark",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                        Toast.makeText(context, "Failed to remove bookmark: " + errorBody,
                                Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(context, "Failed to remove bookmark", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<RemoveBookmarkResponse> call, Throwable t) {
                Toast.makeText(context, "Error removing bookmark: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.ayahList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        FlexboxLayout arabicWordsContainer; // Make sure this ID exists in card_item_single_ayat.xml
        TextView englishTranslation, ayatNumber;
        ImageView ayatCardBookmarkIcon, ayatCardAddNotesIcon, ayatCardPlayAudioIcon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            arabicWordsContainer = itemView.findViewById(R.id.arabicWordsContainer); // Ensure this ID is correct
            englishTranslation = itemView.findViewById(R.id.EnglishTranslationTV);
            ayatNumber = itemView.findViewById(R.id.AyatNumberByAyatTV);
            ayatCardBookmarkIcon = itemView.findViewById((R.id.ayatCardBookmarkIcon));
            ayatCardAddNotesIcon = itemView.findViewById((R.id.ayatCardAddNotesIcon));
            ayatCardPlayAudioIcon = itemView.findViewById(R.id.ayatCardPlayAudioIcon);
        }
    }
    private void showAddNotesDialog(MyViewHolder holder, ChapterAyah ayah) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_notes, null);
        EditText etNoteDescription = dialogView.findViewById(R.id.etNoteDescription);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String description = etNoteDescription.getText().toString().trim();
            addBookmarkWithNotes(holder, ayah, description);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Login Required")
                .setMessage("Please login to use bookmark feature")
                .setPositiveButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(context, AuthActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}