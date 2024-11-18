package com.example.qurannexus.activities

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.qurannexus.R

class ShareCustomQuoteActivity : AppCompatActivity() {
    private lateinit var dailyQuoteText: TextView
    private lateinit var quoteContainer: ConstraintLayout
    private lateinit var backgroundRadioGroup: RadioGroup
    private lateinit var alignmentRadioGroup: RadioGroup
    private lateinit var fontRadioGroup: RadioGroup
    private lateinit var backButton : ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_custom_quote)

        dailyQuoteText = findViewById(R.id.dailyQuoteText)
        quoteContainer = findViewById(R.id.quoteContainer)
        backgroundRadioGroup = findViewById(R.id.backgroundRadioGroup)
        alignmentRadioGroup = findViewById(R.id.alignmentRadioGroup)
        fontRadioGroup = findViewById(R.id.fontRadioGroup)
        backButton = findViewById(R.id.customQuotePreviousButton)

        backButton.setOnClickListener {
            finish() // Closes the current activity and returns to the previous one
        }
        setupBackgroundSelection()
        setupTextAlignment()
        setupFontSelection()
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
                R.id.leftAlign -> dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                R.id.centerAlign -> dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                R.id.rightAlign -> dailyQuoteText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            }

            // Force refresh on main thread
            dailyQuoteText.post {
                dailyQuoteText.requestLayout()
                dailyQuoteText.invalidate()
            }
        }
    }


    private fun setupFontSelection() {
        fontRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            dailyQuoteText.typeface = when (checkedId) {
                R.id.arialFont -> Typeface.create("arial", Typeface.NORMAL)
                R.id.sansFont -> Typeface.SANS_SERIF
                R.id.serifFont -> Typeface.SERIF
                else -> dailyQuoteText.typeface
            }
        }
    }
}