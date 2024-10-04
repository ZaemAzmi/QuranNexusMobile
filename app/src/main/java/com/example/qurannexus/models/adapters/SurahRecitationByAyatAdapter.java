package com.example.qurannexus.models.adapters;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qurannexus.R;
import com.example.qurannexus.models.AyatModel;
import com.example.qurannexus.services.DatabaseService;

import java.util.ArrayList;

public class SurahRecitationByAyatAdapter extends RecyclerView.Adapter<SurahRecitationByAyatAdapter.MyViewHolder> {

    Context context;
    ArrayList<AyatModel> ayatModels;
    DatabaseService databaseService;
    public SurahRecitationByAyatAdapter(Context context, ArrayList<AyatModel> ayatModels, DatabaseService databaseService){
        this.context = context;
        this.ayatModels = ayatModels;
        this.databaseService = databaseService;
    }
    @NonNull
    @Override
    public SurahRecitationByAyatAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.ayat_card_view, parent, false);
        return new SurahRecitationByAyatAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurahRecitationByAyatAdapter.MyViewHolder holder, int position) {
        AyatModel ayatModel = ayatModels.get(position);
        holder.arabicScript.setText(ayatModel.arabicScript);
        holder.englishTranslation.setText(ayatModel.englishTranslation);
        holder.ayatNumber.setText(ayatModel.ayatNumber);

        if (ayatModel.isBookmarked()) {
            holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmarked); // Replace with your bookmarked icon
        } else {
            holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmark); // Replace with your unbookmarked icon
        }
        holder.ayatCardBookmarkIcon.setOnClickListener(v -> {
            int surahIndex = ayatModel.getSurahIndex();
            int ayatIndex = ayatModel.getAyatIndex();

            if (ayatModel.isBookmarked()) {
                // Remove bookmark
                databaseService.removeBookmark(surahIndex, ayatIndex, success -> {
                    if (success) {
                        ayatModel.setBookmarked(false);
                        holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmark); // Replace with your unbookmarked icon
                        Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to remove bookmark", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Add bookmark
                databaseService.addBookmark(surahIndex, ayatIndex, success -> {
                    if (success) {
                        ayatModel.setBookmarked(true);
                        holder.ayatCardBookmarkIcon.setImageResource(R.drawable.ic_bookmarked); // Replace with your bookmarked icon
                        Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to add bookmark", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.ayatModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView arabicScript, englishTranslation, ayatNumber;
        ImageView ayatCardBookmarkIcon;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            arabicScript = itemView.findViewById(R.id.ArabicScriptTV);
            englishTranslation = itemView.findViewById(R.id.EnglishTranslationTV);
            ayatNumber = itemView.findViewById(R.id.AyatNumberByAyatTV);
            ayatCardBookmarkIcon = itemView.findViewById((R.id.ayatCardBookmarkIcon));
        }
    }
}
