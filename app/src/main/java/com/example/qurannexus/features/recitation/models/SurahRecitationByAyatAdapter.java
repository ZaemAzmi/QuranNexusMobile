package com.example.qurannexus.features.recitation.models;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
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
import com.example.qurannexus.features.bookmark.models.BookmarkRequest;
import com.example.qurannexus.features.home.WordDetailsActivity;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.features.home.models.WordDetails;
import com.example.qurannexus.features.home.models.WordDetailsResponse;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.core.utils.QuranMetadata;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurahRecitationByAyatAdapter extends RecyclerView.Adapter<SurahRecitationByAyatAdapter.MyViewHolder> {
    private QuranApi quranApi;
    Context context;
    ArrayList<Ayah> ayahList;
    public SurahRecitationByAyatAdapter(Context context, ArrayList<Ayah> ayahList){
        this.context = context;
        this.ayahList = ayahList;
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
        Ayah ayah = ayahList.get(position);

        // Clear any existing views in the FlexboxLayout
        holder.arabicWordsContainer.removeAllViews();

        setupWordClickListeners(holder, ayah);

        holder.englishTranslation.setText(ayah.getTranslations().get(1).getText());
        holder.ayatNumber.setText(ayah.getAyahKey());
        holder.ayatCardAddNotesIcon.setOnClickListener(view -> showAddNotesDialog());

        holder.ayatCardBookmarkIcon.setImageResource(
                ayah.isBookmarked() ? R.drawable.ic_bookmarked : R.drawable.ic_bookmark
        );

        // Bookmark click listener
        holder.ayatCardBookmarkIcon.setOnClickListener(v -> {
            if (ayah.isBookmarked()) {
                // Remove bookmark
                removeBookmark(holder, ayah, position);
            } else {
                // Add bookmark
                addBookmark(holder, ayah, position);
            }
        });
    }

    private void addBookmark(MyViewHolder holder, Ayah ayah, int position) {
//        BookmarkRequest request = new BookmarkRequest(ayah.getSurahId(), ayah.getAyahIndex());
//        quranApi.addBookmark(request).enqueue(new Callback<GenericResponse>() {
//            @Override
//            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    ayah.setBookmarked(true);
//                    holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmarked);
//                    Toast.makeText(context, "Bookmark added!", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(context, "Failed to add bookmark", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<GenericResponse> call, Throwable t) {
//                Toast.makeText(context, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void removeBookmark(MyViewHolder holder, Ayah ayah, int position) {
//        int bookmarkId = ayah.getBookmarkId(); // Assuming each Ayah has a unique bookmark ID
//        quranApi.removeBookmark(bookmarkId).enqueue(new Callback<GenericResponse>() {
//            @Override
//            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    ayah.setBookmarked(false);
//                    holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmark);
//                    Toast.makeText(context, "Bookmark removed!", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(context, "Failed to remove bookmark", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<GenericResponse> call, Throwable t) {
//                Toast.makeText(context, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void setupWordClickListeners(MyViewHolder holder, Ayah ayah) {
        holder.arabicWordsContainer.removeAllViews();

        // Create a new list to avoid modifying the original
        List<Word> words = new ArrayList<>(ayah.getWords());

        // Iterate in reverse order to ensure correct RTL wrapping
        for (int i = words.size() - 1; i >= 0; i--) {
            Word word = words.get(i);
            TextView wordView = new TextView(context);
            wordView.setText(word.getText());
            wordView.setTextColor(ContextCompat.getColor(context, R.color.white));
            wordView.setTextSize(20f);
            wordView.setPadding(8, 8, 8, 8);
            wordView.setTypeface(ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs));
            wordView.setTextDirection(View.TEXT_DIRECTION_RTL);

            // Long click: Highlight and show popup
            wordView.setOnLongClickListener(v -> {
                animateWord(v);
                highlightWord(wordView);
                showPopupHint(wordView, "Tap for word analysis");
                fetchWordDetails(word.getWordKey());
                return true;
            });

            holder.arabicWordsContainer.addView(wordView);
        }
    }
    private void highlightWord(TextView wordView) {
        wordView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
        wordView.postDelayed(() -> wordView.setBackgroundResource(0), 1000); // Reset after 1s
    }

    private void showPopupHint(View anchor, String message) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.dialog_word_hint, null);
        TextView hintTextView = popupView.findViewById(R.id.hintTextView);
        hintTextView.setText(message);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show the popup above the anchor view
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - anchor.getHeight() - 20);

        // Automatically dismiss the popup after a delay
        new Handler(Looper.getMainLooper()).postDelayed(popupWindow::dismiss, 1500);
    }


    private void fetchWordDetails(String wordKey) {
        quranApi = ApiService.getQuranClient().create(QuranApi.class);

        quranApi.getWordDetails(wordKey).enqueue(new Callback<WordDetailsResponse>() {
            @Override
            public void onResponse(Call<WordDetailsResponse> call, Response<WordDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WordDetails wordDetails = response.body().getData();

                    // Fetch Surah name from QuranMetadata
                    SurahDetails surahDetails = QuranMetadata.Companion.getInstance().getSurahDetails(Integer.parseInt(wordDetails.getSurahId()));
                    String surahNameArabic = surahDetails.getArabicName();
                    String surahNameEnglish = surahDetails.getEnglishName();

                    // Prepare intent to navigate to WordDetailsActivity
                    Intent intent = new Intent(context, WordDetailsActivity.class);
                    intent.putExtra("WORD_TEXT", wordDetails.getText());
                    intent.putExtra("TRANSLATION", wordDetails.getTranslation()); // Replace if available
                    intent.putExtra("TRANSLITERATION", wordDetails.getTransliteration()); // Replace if available
                    intent.putExtra("SURAH_NAME_ARABIC", surahNameArabic);
                    intent.putExtra("SURAH_NAME_ENGLISH", surahNameEnglish);
                    intent.putExtra("AYAH_KEY", wordDetails.getAyahKey());
                    intent.putExtra("AUDIO_URL", wordDetails.getAudioUrl());
                    intent.putExtra("SURAH_NUMBER", wordDetails.getSurahId());
                    intent.putExtra("LINE_NUMBER", wordDetails.getLineNumber());
                    intent.putExtra("WORD_NUMBER", wordDetails.getWordIndex());
                    intent.putExtra("PAGE_ID", wordDetails.getPageId());
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Failed to fetch word details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WordDetailsResponse> call, Throwable t) {
                Toast.makeText(context, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void animateWord(View wordView) {
        wordView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> wordView.animate().scaleX(1f).scaleY(1f).setDuration(200))
                .start();
    }
    @Override
    public int getItemCount() {
        return this.ayahList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        FlexboxLayout arabicWordsContainer;
        TextView englishTranslation, ayatNumber;
        ImageView ayatCardBookmarkIcon, ayatCardAddNotesIcon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            arabicWordsContainer = itemView.findViewById(R.id.arabicWordsContainer);
            englishTranslation = itemView.findViewById(R.id.EnglishTranslationTV);
            ayatNumber = itemView.findViewById(R.id.AyatNumberByAyatTV);
            ayatCardBookmarkIcon = itemView.findViewById((R.id.ayatCardBookmarkIcon));
            ayatCardAddNotesIcon = itemView.findViewById((R.id.ayatCardAddNotesIcon));

        }
    }
    private void showAddNotesDialog() {
        // Inflate the custom layout for the dialog using the adapter's context
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_notes, null);

        // Get references to EditTexts and Buttons
        EditText etNoteTitle = dialogView.findViewById(R.id.etNoteTitle);
        EditText etNoteDescription = dialogView.findViewById(R.id.etNoteDescription);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Create the AlertDialog with the context from the adapter
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set up the Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Set up the Save button
        btnSave.setOnClickListener(v -> {
            String title = etNoteTitle.getText().toString().trim();
            String description = etNoteDescription.getText().toString().trim();

            // Perform saving logic here
            if (!title.isEmpty() && !description.isEmpty()) {
                // Save the note (e.g., save to database or API call)
                saveNote(title, description);
                dialog.dismiss();
            } else {
                // Optionally, show a message if fields are empty
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog
        dialog.show();
    }
    private void saveNote(String title, String description) {
        // Implement save logic here
        // Example: Save to local database or remote server
        Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show();
    }
}
