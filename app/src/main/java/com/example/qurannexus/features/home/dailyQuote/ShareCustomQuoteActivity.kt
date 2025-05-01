package com.example.qurannexus.features.home.dailyQuote

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.qurannexus.R
import java.io.File
import java.io.FileOutputStream

class ShareCustomQuoteActivity : AppCompatActivity() {
    private lateinit var dailyQuoteText: TextView
    private lateinit var dailyQuoteSource: TextView // Add source display
    private lateinit var quoteContainer: ConstraintLayout
    private lateinit var backgroundRadioGroup: RadioGroup
    private lateinit var alignmentRadioGroup: RadioGroup
    private lateinit var fontRadioGroup: RadioGroup
    private lateinit var backButton : ImageView
    private lateinit var shareButton: Button

    // Quote data
    private var quoteId: String? = null
    private var quoteDescription: String? = null
    private var quoteSource: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_custom_quote)

        // Initialize views
        dailyQuoteText = findViewById(R.id.dailyQuoteText)
        dailyQuoteSource = findViewById(R.id.quoteSourceText) // Make sure to add this TextView in your layout
        quoteContainer = findViewById(R.id.quoteContainer)
        backgroundRadioGroup = findViewById(R.id.backgroundRadioGroup)
        alignmentRadioGroup = findViewById(R.id.alignmentRadioGroup)
        fontRadioGroup = findViewById(R.id.fontRadioGroup)
        backButton = findViewById(R.id.customQuotePreviousButton)
        shareButton = findViewById(R.id.shareButton)

        // Get quote data from intent
        extractQuoteData()

        // Set up click listeners
        backButton.setOnClickListener {
            finish() // Closes the current activity and returns to the previous one
        }

        shareButton.setOnClickListener {
            shareQuote()
        }

        // Set up customization options
        setupBackgroundSelection()
        setupTextAlignment()
        setupFontSelection()

        // Set default background (first radio button)
        backgroundRadioGroup.check(R.id.redBackground)
    }

    private fun extractQuoteData() {
        // Extract data from intent
        quoteId = intent.getStringExtra("DAILY_QUOTE_ID")
        quoteDescription = intent.getStringExtra("DAILY_QUOTE_DESCRIPTION")
        quoteSource = intent.getStringExtra("DAILY_QUOTE_SOURCE")

        Log.d("ShareQuote", "Quote ID: $quoteId")
        Log.d("ShareQuote", "Quote Description: $quoteDescription")
        Log.d("ShareQuote", "Quote Source: $quoteSource")

        // Set the text in the UI
        if (!quoteDescription.isNullOrEmpty()) {
            dailyQuoteText.text = quoteDescription
        } else {
            dailyQuoteText.text = "No quote text provided"
        }

        if (!quoteSource.isNullOrEmpty()) {
            dailyQuoteSource.text = quoteSource
        } else {
            dailyQuoteSource.text = ""
        }
    }

    private fun shareQuote() {
        val bitmap = getBitmapFromView(quoteContainer)
        val file = saveBitmapToFile(bitmap)
        if (file != null) {
            val shareText = if (!quoteSource.isNullOrEmpty()) {
                "${dailyQuoteText.text}\n\n- ${quoteSource}"
            } else {
                dailyQuoteText.text.toString()
            }
            shareImageAndText(file, shareText)
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        // Create a Bitmap with the same dimensions as the View
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val file = File(cacheDir, "shared_quote.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareImageAndText(file: File, text: String) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Quote via"))
    }

    private fun setupBackgroundSelection() {
        backgroundRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val imageResource = when (checkedId) {
                R.id.redBackground -> R.drawable.bg_daily_quote1
                R.id.blueBackground -> R.drawable.bg_daily_quote2
                else -> 0
            }

            if (imageResource != 0) {
                Glide.with(this)
                    .asBitmap()
                    .load(imageResource)
                    .override(800, 800) // Scale down image to avoid memory issues
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            quoteContainer.background = BitmapDrawable(resources, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Handle any required cleanup here
                        }
                    })
            }
        }
    }

    private fun setupTextAlignment() {
        alignmentRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.leftAlign -> {
                    dailyQuoteText.gravity = android.view.Gravity.START
                    dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                    dailyQuoteSource.gravity = android.view.Gravity.START
                    dailyQuoteSource.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                }
                R.id.centerAlign -> {
                    dailyQuoteText.gravity = android.view.Gravity.CENTER
                    dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    dailyQuoteSource.gravity = android.view.Gravity.CENTER
                    dailyQuoteSource.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                R.id.rightAlign -> {
                    dailyQuoteText.gravity = android.view.Gravity.END
                    dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                    dailyQuoteSource.gravity = android.view.Gravity.END
                    dailyQuoteSource.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                }
            }

            // Force the layout to refresh immediately
            dailyQuoteText.post {
                dailyQuoteText.requestLayout()
                dailyQuoteText.invalidate()
                dailyQuoteSource.requestLayout()
                dailyQuoteSource.invalidate()
            }
        }
    }

    private fun setupFontSelection() {
        fontRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val typeface = when (checkedId) {
                R.id.arialFont -> Typeface.create("arial", Typeface.NORMAL)
                R.id.sansFont -> Typeface.SANS_SERIF
                R.id.serifFont -> Typeface.SERIF
                else -> dailyQuoteText.typeface
            }

            dailyQuoteText.typeface = typeface
            dailyQuoteSource.typeface = typeface
        }
    }
}