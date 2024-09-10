package com.example.qurannexus.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.fragments.RecitationPageFragment;

import java.util.ArrayList;

public class SurahListAdapter extends RecyclerView.Adapter<SurahListAdapter.MyViewHolder> {
    Context context;
    FragmentActivity fragmentActivity;
    ArrayList<SurahModel> surahModels;
    String layoutType;
    public SurahListAdapter(FragmentActivity fragmentActivity, ArrayList<SurahModel> surahModels, String layoutType){
        this.fragmentActivity = fragmentActivity;
        this.surahModels = surahModels;
        this.layoutType = layoutType;
    }
    @NonNull
    @Override
    public SurahListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(fragmentActivity);
        View view = inflater.inflate(R.layout.surah_card_view, parent, false);
        return new SurahListAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurahListAdapter.MyViewHolder holder, int position) {
        // assign values to the views created in the recycler view row layout file based on the position of the RV
        SurahModel surahModel = surahModels.get(position);
        String tempAyatNumber = surahModel.getAyatNumber()+" Ayahs";
        holder.surahName.setText(surahModel.getSurahName());
        holder.surahMeaning.setText(surahModel.getSurahMeaning());
        holder.arabicSurahName.setText(surahModel.getArabicSurahName());
        holder.ayatNumber.setText(tempAyatNumber);
        holder.surahNumber.setText(surahModel.getSurahNumber());

        holder.itemView.setOnClickListener(view -> {
            RecitationPageFragment recitationPageFragment = RecitationPageFragment.newInstance(surahModel, layoutType, position);
            fragmentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainFragmentContainer, recitationPageFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
    @Override
    public int getItemCount() {
        return this.surahModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView surahName, surahMeaning, arabicSurahName, ayatNumber, surahNumber;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            surahName = itemView.findViewById(R.id.SurahNameTV);
            surahMeaning = itemView.findViewById(R.id.SurahMeaningTV);
            arabicSurahName = itemView.findViewById(R.id.ArabicSurahNameTV);
            ayatNumber = itemView.findViewById(R.id.AyatNumberTV);
            surahNumber = itemView.findViewById(R.id.SurahNumberTV);
        }
    }
}
