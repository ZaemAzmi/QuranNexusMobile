package com.example.qurannexus.fragments;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qurannexus.R;
import com.example.qurannexus.interfaces.QuranApi;
import com.example.qurannexus.models.Ayah;
import com.example.qurannexus.models.AyatModel;
import com.example.qurannexus.models.PageVerseResponse;
import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.models.SurahNameModel;
import com.example.qurannexus.models.adapters.PageAdapter;
import com.example.qurannexus.services.UtilityService;
import com.example.qurannexus.services.retrofit.ApiService;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ByPageRecitationFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "page_number";
    private static final int TOTAL_PAGES = 604;
    private Context context;
    private int currentPageNumber;
    private QuranApi quranApi;
    private UtilityService utilityService;
    private ViewPager2 viewPager;
    private PageAdapter pageAdapter;
    private TextView pageNumberTextView;

    public static ByPageRecitationFragment newInstance(int pageNumber) {
        ByPageRecitationFragment fragment = new ByPageRecitationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
        }
        context = getContext();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_by_page_recitation, container, false);
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        viewPager = view.findViewById(R.id.fragmentByPageRecitationViewPager);
        pageNumberTextView = view.findViewById(R.id.pageNumber);

        utilityService = new UtilityService();

        setupViewPager();

        return view;
    }

    private void setupViewPager() {
        pageAdapter = new PageAdapter(this);
        viewPager.setAdapter(pageAdapter);

        // Set the initial page
        int initialPosition = TOTAL_PAGES - currentPageNumber;
        viewPager.setCurrentItem(initialPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPageNumber = TOTAL_PAGES - position;
                updatePageNumber();
            }
        });

        updatePageNumber();
    }

    private void updatePageNumber() {
        pageNumberTextView.setText("Page: " + currentPageNumber);
    }

    // This method will be called by the QuranPageAdapter to fetch the page content
    public void fetchPageVerses(int pageNumber, PageAdapter.PageContentCallback callback) {
        Log.e("page num", String.valueOf(pageNumber));
        quranApi.getPageVerses(pageNumber, true).enqueue(new Callback<PageVerseResponse>() {
            @Override
            public void onResponse(Call<PageVerseResponse> call, Response<PageVerseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ayah> verseList = response.body().getData().getAyahs();
                    SpannableStringBuilder pageContent = new SpannableStringBuilder();
                    int previousSurahId = -1;

                    for (Ayah ayah : verseList) {
                        int currentSurahId = Integer.parseInt(ayah.getSurahId());
                        int currentAyahNumber = Integer.parseInt(ayah.getAyahIndex());

                        // Insert calligraphy at the start of a new surah
                        if (currentAyahNumber == 1) {
                            appendAndCenterCalligraphy(pageContent, "surah_0", "surah_" + currentSurahId);
                            pageContent.append("\n");
                        }

                        // Append the ayah text
                        String arabicNumber = utilityService.convertToArabicNumber(currentAyahNumber);
                        pageContent.append(ayah.getArabicText()).append(arabicNumber).append("");

                        previousSurahId = currentSurahId;
                    }

                    callback.onPageContentFetched(pageContent);
                } else {
                    callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Failed to fetch page content"));
                }
            }

            @Override
            public void onFailure(Call<PageVerseResponse> call, Throwable t) {
                callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Error fetching page: " + t.getMessage()));
            }
        });
    }

    private void appendCalligraphyToContent(SpannableStringBuilder pageContent, String fileName) {
        Drawable calligraphyDrawable = getSurahCalligraphyDrawable(fileName);
        if (calligraphyDrawable != null) {
            calligraphyDrawable.setBounds(0, 0, calligraphyDrawable.getIntrinsicWidth(), calligraphyDrawable.getIntrinsicHeight());
            calligraphyDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            // Create an ImageSpan with centered alignment
            ImageSpan calligraphySpan = new ImageSpan(calligraphyDrawable, DynamicDrawableSpan.ALIGN_BASELINE);

            pageContent.append(" ");  // Add leading spaces for centering
            int start = pageContent.length();
            pageContent.append(" "); // Placeholder for the image
            pageContent.setSpan(calligraphySpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply white color to the image span
            ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.white));
            pageContent.setSpan(whiteSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private Drawable getSurahCalligraphyDrawable(String fileName) {
        // Convert drawable resource name to resource ID
        int resId = context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
        if (resId != 0) {
            // Return the VectorDrawableCompat for SVG files
            return ContextCompat.getDrawable(context, resId);
        }
        return null;
    }


    private void appendAndCenterCalligraphy(SpannableStringBuilder pageContent, String fileName1, String fileName2) {
        int startIndex = pageContent.length();
        appendCalligraphyToContent(pageContent, fileName2);
        appendCalligraphyToContent(pageContent, fileName1);

        int endIndex = pageContent.length();

        // Apply center alignment to the entire span containing both calligraphy images
        pageContent.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}