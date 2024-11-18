package com.example.qurannexus.models.adapters;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qurannexus.R;
import com.example.qurannexus.models.Ayah;
import com.example.qurannexus.models.AyatModel;
import com.example.qurannexus.services.DatabaseService;

import java.util.ArrayList;

public class SurahRecitationByAyatAdapter extends RecyclerView.Adapter<SurahRecitationByAyatAdapter.MyViewHolder> {

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
        holder.arabicScript.setText(ayah.getArabicText());
        holder.englishTranslation.setText(ayah.getTranslations().get(1).getText());
        holder.ayatNumber.setText(ayah.getAyahKey());
        holder.ayatCardAddNotesIcon.setOnClickListener(view -> showAddNotesDialog());
//        holder.ayatCardBookmarkIcon.setImageResource(
//                ayah.isBookmarked() ? R.drawable.ic_bookmarked : R.drawable.ic_bookmark
//        );
//        holder.ayatCardBookmarkIcon.setOnClickListener(v -> {
//            int surahIndex = ayahList.getSurahIndex();
//            int ayatIndex = ayahList.getAyatIndex();
//
//            if (ayatModel.isBookmarked()) {
//                // Remove bookmark
//
//            } else {
//                // Add bookmark
//
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return this.ayahList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView arabicScript, englishTranslation, ayatNumber;
        ImageView ayatCardBookmarkIcon;
        ImageView ayatCardAddNotesIcon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            arabicScript = itemView.findViewById(R.id.ArabicScriptTV);
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
