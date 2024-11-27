package com.example.qurannexus.features.home

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.qurannexus.R
import java.io.IOException


class WordDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }
        val wordTextView = findViewById<TextView>(R.id.wordText)
        val translationTextView = findViewById<TextView>(R.id.translationText)
        val transliterationTextView = findViewById<TextView>(R.id.transliterationText)
        val surahNameTextView = findViewById<TextView>(R.id.surahNameText)
        val ayahKeyTextView = findViewById<TextView>(R.id.ayahKeyText)
        val surahNumberText = findViewById<TextView>(R.id.surahNumberText)
        val lineNumberText = findViewById<TextView>(R.id.lineNumberText)
        val wordNumberText = findViewById<TextView>(R.id.wordNumberText)
        val pageIdText = findViewById<TextView>(R.id.pageIdText)
        val playAudioButton = findViewById<Button>(R.id.playAudioButton)

        // Retrieve data from the intent
        val wordText = intent.getStringExtra("WORD_TEXT")
        val translation = intent.getStringExtra("TRANSLATION")
        val transliteration = intent.getStringExtra("TRANSLITERATION")
        val surahNameArabic = intent.getStringExtra("SURAH_NAME_ARABIC")
        val surahNameEnglish = intent.getStringExtra("SURAH_NAME_ENGLISH")
        val ayahKey = intent.getStringExtra("AYAH_KEY")
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val surahNumber = intent.getStringExtra("SURAH_NUMBER")
        val lineNumber = intent.getIntExtra("LINE_NUMBER", -1)
        val wordNumber = intent.getStringExtra("WORD_NUMBER")
        val pageId = intent.getStringExtra("PAGE_ID")

        // Set data to views
        wordTextView.text = wordText
        translationTextView.text = "Translation: $translation"
        transliterationTextView.text = "Transliteration: $transliteration"
        surahNameTextView.text = "Surah: $surahNameArabic ($surahNameEnglish)"
        ayahKeyTextView.text = "Ayah Key: $ayahKey"
        surahNumberText.text = "Surah Number: $surahNumber"
        lineNumberText.text = "Line Number: $lineNumber"
        wordNumberText.text = "Word Number: $wordNumber"
        pageIdText.text = "Page ID: $pageId"

        // Set up audio playback
        playAudioButton.setOnClickListener {
            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(audioUrl)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to play audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

