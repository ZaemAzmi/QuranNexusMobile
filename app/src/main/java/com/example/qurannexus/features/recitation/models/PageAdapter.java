package com.example.qurannexus.features.recitation.models;

import static com.example.qurannexus.features.recitation.ByPageRecitationFragment.TOTAL_PAGES;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
import com.example.qurannexus.core.database.entities.WordData;
import com.example.qurannexus.features.recitation.ByPageRecitationFragment;
import com.example.qurannexus.features.recitation.WordClickableSpan;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.QuranPageViewHolder> {

    private final ByPageRecitationFragment fragment;
    private final SparseArray<SpannableStringBuilder> pageContents = new SparseArray<>();
    private final Gson gson = new Gson();

    public PageAdapter(ByPageRecitationFragment fragment, List<QuranAyahDetailEntity> initialAyahs, int initialPageNum) {
        this.fragment = fragment;
        if (initialAyahs != null && !initialAyahs.isEmpty()) {
            pageContents.put(initialPageNum, renderPageContent(initialAyahs));
        }
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
            holder.setContent(new SpannableStringBuilder("Page " + pageNumber + "\nLoading..."));
        }
        holder.scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (fragment.getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) fragment.getActivity();
                        int dy = scrollY - oldScrollY;

                        if (dy > 10) { // Scrolling down
                            mainActivity.setBottomNavigationVisibility(false);
                        } else if (dy < -10) { // Scrolling up
                            mainActivity.setBottomNavigationVisibility(true);
                        }
                    }
                });
    }

    public void updatePageContent(int pageNumber, List<QuranAyahDetailEntity> ayahs) {
        if (ayahs != null && !ayahs.isEmpty()) {
            pageContents.put(pageNumber, renderPageContent(ayahs));
            int position = TOTAL_PAGES - pageNumber;
            if (position >= 0 && position < getItemCount()) {
                notifyItemChanged(position);
            }
        }
    }

    private SpannableStringBuilder renderPageContent(List<QuranAyahDetailEntity> ayahs) {
        if (ayahs == null || ayahs.isEmpty()) {
            return new SpannableStringBuilder("No content for this page.");
        }

        Context context = fragment.requireContext();
        SpannableStringBuilder pageBuilder = new SpannableStringBuilder();

        // Step 1: Group all ayahs on the page by their Surah ID
        Map<Integer, List<QuranAyahDetailEntity>> surahsOnPage = new LinkedHashMap<>();
        for (QuranAyahDetailEntity ayah : ayahs) {
            surahsOnPage.computeIfAbsent(ayah.getSurahId(), k -> new ArrayList<>()).add(ayah);
        }

        // Step 2: Sort the Surah IDs to ensure they are rendered in the correct order
        List<Integer> sortedSurahIds = new ArrayList<>(surahsOnPage.keySet());
        Collections.sort(sortedSurahIds);

        boolean isFirstSurahBlockOnPage = true;

        // Step 3: Iterate through each Surah block
        for (Integer surahId : sortedSurahIds) {
            List<QuranAyahDetailEntity> ayahsInSurah = surahsOnPage.get(surahId);
            if (ayahsInSurah == null || ayahsInSurah.isEmpty()) continue;

            // Add a separator between Surahs (but not before the very first one on the page)
            if (!isFirstSurahBlockOnPage) {
                pageBuilder.append("\n\n\n");
            }

            // Render the header ONLY if the first verse of this Surah is on the page
            if (ayahsInSurah.get(0).getAyahIndex() == 1) {
                appendSurahHeader(pageBuilder, context, surahId);
            }

            // Render all verses for THIS surah as a single "paragraph"
            int lastLineNumber = -1;
            for (QuranAyahDetailEntity ayahEntity : ayahsInSurah) {
                Type wordListType = new TypeToken<List<WordData>>() {}.getType();
                List<WordData> words = gson.fromJson(ayahEntity.getWordsDataJson(), wordListType);

                if (words != null) {
                    for (WordData word : words) {
                        if (word.getLine_number() != lastLineNumber && lastLineNumber != -1) {
                            pageBuilder.append("\n");
                        }
                        lastLineNumber = word.getLine_number();

                        // Append the clickable word
                        String cleanText = word.getText().replace("\u06DF", "");
                        String wordWithSpace = cleanText + " ";
                        SpannableString wordSpannable = new SpannableString(wordWithSpace);
                        WordClickableSpan clickableSpan = new WordClickableSpan(context, word.getWord_key(), cleanText);
                        wordSpannable.setSpan(clickableSpan, 0, cleanText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        pageBuilder.append(wordSpannable);
                    }
                }

                // Append the ayah number symbol
                String arabicNumber = new com.example.qurannexus.core.utils.UtilityService()
                        .convertToArabicNumber(ayahEntity.getAyahIndex());
                pageBuilder.append(String.format(" %s ", arabicNumber));
            }

            isFirstSurahBlockOnPage = false;
        }
        if (pageBuilder.length() > 0) {
            pageBuilder.append("\n\n");
        }
        // Apply global font and size styling at the very end
        if (pageBuilder.length() > 0) {
            pageBuilder.setSpan(new TypefaceSpan(ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs)), 0, pageBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int textSizePx = (int) context.getResources().getDimension(R.dimen.arabic_text_size);
            pageBuilder.setSpan(new AbsoluteSizeSpan(textSizePx), 0, pageBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return pageBuilder;
    }

    private void appendSurahHeader(SpannableStringBuilder builder, Context context, int surahId) {
        // Create a new SpannableStringBuilder just for the header line
        SpannableStringBuilder headerLine = new SpannableStringBuilder();

        // 1. Get the drawable for the Surah name (e.g., "surah_112" for Al-Ikhlas)
        String surahNameDrawableName = "surah_" + surahId;
        Drawable surahNameDrawable = getWhiteDrawable(context, surahNameDrawableName, 1.2f);

        // 2. Get the drawable for the "Surah" text (surah_0)
        String surahTextDrawableName = "surah_0";
        Drawable surahTextDrawable = getWhiteDrawable(context, surahTextDrawableName, 1.2f);

        // 3. Append the images in the correct RTL order: Name first, then "Surah" text
        if (surahNameDrawable != null) {
            SpannableString nameSpan = new SpannableString(" ");
            nameSpan.setSpan(new ImageSpan(surahNameDrawable, ImageSpan.ALIGN_CENTER), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            headerLine.append(nameSpan);
            headerLine.append("  "); // Add some space between the images
        }

        if (surahTextDrawable != null) {
            SpannableString textSpan = new SpannableString(" ");
            textSpan.setSpan(new ImageSpan(surahTextDrawable, ImageSpan.ALIGN_CENTER), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            headerLine.append(textSpan);
        }

        // 4. If we successfully added images, apply a single centering span to the whole line
        if (headerLine.length() > 0) {
            headerLine.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, headerLine.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(headerLine).append("\n\n"); // Append the header and add space below it
        }

        // 5. Always add the Bismillah (except for Surah 9)
        if (surahId != 9 && surahId != 1) {
            String bismillahText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";
            SpannableString bismillahSpannable = new SpannableString(bismillahText);
            bismillahSpannable.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, bismillahText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(bismillahSpannable).append("\n");
        }
    }
    private Drawable getWhiteDrawable(Context context, String drawableName, float scaleFactor) {
        int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        if (resId == 0) return null;

        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) return null;

        // Make a mutable copy to avoid changing the original drawable resource
        Drawable mutableDrawable = drawable.mutate();

        // Scale it
        int scaledWidth = (int) (mutableDrawable.getIntrinsicWidth() * scaleFactor);
        int scaledHeight = (int) (mutableDrawable.getIntrinsicHeight() * scaleFactor);
        mutableDrawable.setBounds(0, 0, scaledWidth, scaledHeight);

        // Color it white
        mutableDrawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));

        return mutableDrawable;
    }

    // NEW HELPER METHOD for cleaner image appending
    private void appendCenteredImage(SpannableStringBuilder builder, Context context, String drawableName, float scaleFactor) {
        int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        if (resId == 0) return;

        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) return;

        int scaledWidth = (int) (drawable.getIntrinsicWidth() * scaleFactor);
        int scaledHeight = (int) (drawable.getIntrinsicHeight() * scaleFactor);
        drawable.setBounds(0, 0, scaledWidth, scaledHeight);

        SpannableString spannableString = new SpannableString(" "); // Placeholder
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        builder.append(spannableString).append("\n");
    }
    // NEW HELPER METHOD for calligraphy
    private void appendAndCenterCalligraphy(SpannableStringBuilder builder, String drawableName, float scaleFactor) {
        Context context = fragment.requireContext();
        int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
        if (resId == 0) return;

        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) return;

        int scaledWidth = (int) (drawable.getIntrinsicWidth() * scaleFactor);
        int scaledHeight = (int) (drawable.getIntrinsicHeight() * scaleFactor);
        drawable.setBounds(0, 0, scaledWidth, scaledHeight);

        // Create a placeholder for the span
        String placeholder = " ";
        int start = builder.length();
        builder.append(placeholder);
        int end = builder.length();

        // Create and apply the ImageSpan
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        builder.setSpan(imageSpan, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        // Create and apply the AlignmentSpan to center the line
        builder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.append("\n"); // Add a newline after the centered image
    }
    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }

    static class QuranPageViewHolder extends RecyclerView.ViewHolder {
        private final TextView contentTextView;
        private final NestedScrollView scrollView;
        QuranPageViewHolder(@NonNull View itemView) {
            super(itemView);
            scrollView = itemView.findViewById(R.id.pageNestedScrollView);
            contentTextView = itemView.findViewById(R.id.recitationByPageTextView);

            // *** IMPORTANT: This makes the links clickable ***
            contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
            Log.e("pageadapter","clicked");
            contentTextView.setHighlightColor(Color.TRANSPARENT); // set the text color for text that can be clicked
            // Get a reference to the MainActivity
            final MainActivity mainActivity = (itemView.getContext() instanceof MainActivity) ? (MainActivity) itemView.getContext() : null;

            // --- 1. SOLVE THE TOUCH ISSUE BY LISTENING ON THE TEXTVIEW ---
            // We attach the listener to the TextView, as it's the view that receives the initial touch.
            contentTextView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // The MovementMethod handles clicks on the spans. We let it do its job first.
                    boolean handledByMovementMethod = contentTextView.getMovementMethod().onTouchEvent(contentTextView, (Spannable) contentTextView.getText(), event);
                    // If the touch was a click on a word (handled by MovementMethod), we don't do anything else.
                    if (handledByMovementMethod) {
                        return true;
                    }
                    // If it was NOT a click on a word, we can now handle our custom logic.
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (mainActivity != null) {
                            mainActivity.setBottomNavigationVisibility(false);
                        }
                        // --- 2. SOLVE THE ACCESSIBILITY WARNING ---
                        // Since this is a "click" on the background, we call performClick.
                        v.performClick();
                        return true; // We handled this "background click".
                    }

                    // For other touch events (like scrolling), we let the default handler take over.
                    return false;
                }
            });

            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        if (mainActivity != null) {
                            int dy = scrollY - oldScrollY;
                            // We only care about scrolling up to show the nav bar.
                            // The touch listener handles hiding it.
                            if (dy < -10) { // Scrolling up
                                mainActivity.setBottomNavigationVisibility(true);
                            }
                        }
                    });
        }

        void setContent(SpannableStringBuilder content) {
            contentTextView.setText(content);
        }
    }
}
