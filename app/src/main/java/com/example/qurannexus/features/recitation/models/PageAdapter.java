package com.example.qurannexus.features.recitation.models;

import android.text.SpannableStringBuilder;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.features.recitation.ByPageRecitationFragment;

@UnstableApi
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.QuranPageViewHolder> {
    private static final int TOTAL_PAGES = 604;
    private final ByPageRecitationFragment fragment;
    private final SparseArray<SpannableStringBuilder> pageContents = new SparseArray<>();
    // Add new SparseArray to store page data
    private final SparseArray<PageVerseResponse.PageData> pageDataCache = new SparseArray<>();
    public PageAdapter(ByPageRecitationFragment fragment) {
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public QuranPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item_single_page_recitation, parent, false);
        return new QuranPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuranPageViewHolder holder, int position) {
        int pageNumber = TOTAL_PAGES - position;
        SpannableStringBuilder content = pageContents.get(pageNumber);
        if (content != null) {
            holder.setContent(content);
        } else {
            holder.setContent(SpannableStringBuilder.valueOf("Fetching ayahs..."));
            fragment.fetchPageVerses(pageNumber, new PageContentCallback() {
                @Override
                public void onPageContentFetched(SpannableStringBuilder pageContent) {
                    pageContents.put(pageNumber, pageContent);
                    if (fragment.getResponseData() != null) {
                        pageDataCache.put(pageNumber, fragment.getResponseData());
                    }
                    holder.setContent(pageContent);
                }

                @Override
                public void onPageContentFetchFailed(SpannableStringBuilder errorMessage) {
                    holder.setContent(SpannableStringBuilder.valueOf("Error: " + errorMessage));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }
    public void cachePageData(int pageNumber, PageVerseResponse.PageData data) {
        pageDataCache.put(pageNumber, data);
    }

    public PageVerseResponse.PageData getCurrentPageData(int pageNumber) {
        return pageDataCache.get(pageNumber);
    }

    static class QuranPageViewHolder extends RecyclerView.ViewHolder {
        private final TextView contentTextView;

        QuranPageViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.recitationByPageTextView);
        }

        void setContent(SpannableStringBuilder content) {
            contentTextView.setText(content);
        }
    }

    public interface PageContentCallback {
        void onPageContentFetched(SpannableStringBuilder pageContent);
        void onPageContentFetchFailed(SpannableStringBuilder errorMessage);
    }


}
