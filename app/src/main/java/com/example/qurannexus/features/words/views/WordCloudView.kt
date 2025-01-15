package com.example.qurannexus.features.words.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withSave
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkWord

class WordCloudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var words: List<BookmarkWord> = emptyList()
    private val wordPositions = mutableMapOf<BookmarkWord, PointF>()
    private val wordSizes = mutableMapOf<BookmarkWord, Float>()

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val minScale = 0.5f
    private val maxScale = 3f

    private val arabicTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs)
    }

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    var onWordClickListener: ((BookmarkWord) -> Unit)? = null

    fun setWords(newWords: List<BookmarkWord>) {
        words = newWords
        calculateWordPositions()
        invalidate()
    }

    private fun calculateWordPositions() {
        wordPositions.clear()
        wordSizes.clear()

        if (words.isEmpty()) return

        val centerX = width / 2f
        val centerY = height / 2f
        var angle = 0.0
        var radius = 0f

        words.forEach { word ->
            // Calculate size based on frequency or importance
            val baseSize = 40f // Base text size
            val size = baseSize * (0.8f + Math.random().toFloat() * 0.4f) // Random variation
            wordSizes[word] = size

            // Spiral layout
            val x = centerX + radius * Math.cos(angle).toFloat()
            val y = centerY + radius * Math.sin(angle).toFloat()

            wordPositions[word] = PointF(x, y)

            angle += 0.5
            radius += size
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateWordPositions()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.withSave {
            // Apply zoom and pan transformations
            canvas.translate(translateX, translateY)
            canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

            // Draw each word
            wordPositions.forEach { (word, position) ->
                arabicTextPaint.textSize = wordSizes[word] ?: 40f
                canvas.drawText(
                    word.word_text,
                    position.x,
                    position.y,
                    arabicTextPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        // Transform the tap coordinates based on current scale and translation
        val transformedX = (x - translateX) / scaleFactor
        val transformedY = (y - translateY) / scaleFactor

        // Find the closest word to the tap point
        var closestWord: BookmarkWord? = null
        var minDistance = Float.MAX_VALUE

        wordPositions.forEach { (word, position) ->
            val distance = Math.hypot(
                (transformedX - position.x).toDouble(),
                (transformedY - position.y).toDouble()
            ).toFloat()

            if (distance < minDistance && distance < wordSizes[word] ?: 40f) {
                minDistance = distance
                closestWord = word
            }
        }

        closestWord?.let { onWordClickListener?.invoke(it) }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            translateX -= distanceX
            translateY -= distanceY
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }
    }
}