package com.example.qurannexus.features.recitation;


import android.content.Context;
import android.content.Intent;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.qurannexus.features.words.WordDetailsActivity;

public class WordClickableSpan extends ClickableSpan {

    private final String wordKey;
    private final String wordText;
    private final Context context;

    public WordClickableSpan(Context context, String wordKey, String wordText) {
        this.context = context;
        this.wordKey = wordKey;
        this.wordText = wordText;
    }

    @Override
    public void onClick(@NonNull View widget) {
        // Create and start the intent to WordDetailsActivity
        Intent intent = new Intent(context, WordDetailsActivity.class);
        intent.putExtra(WordDetailsActivity.EXTRA_WORD_KEY_FROM_RECITATION, wordKey);
        intent.putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, wordText);
        context.startActivity(intent);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        // Optional: style the clickable word. For now, we keep the default look.
        super.updateDrawState(ds);
        ds.setUnderlineText(false); // Remove underline
    }
}