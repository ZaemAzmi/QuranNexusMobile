package com.example.qurannexus.features.words.views
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withSave
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class WordCloudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var words: List<BookmarkWord> = emptyList()
    private val wordPositions = ConcurrentHashMap<BookmarkWord, PointF>()
    private val wordSizes = ConcurrentHashMap<BookmarkWord, Float>()
    private val wordAngles = ConcurrentHashMap<BookmarkWord, Float>() // For rotation
    private val wordColors = ConcurrentHashMap<BookmarkWord, Int>()
    private val wordOccurrences = ConcurrentHashMap<String, Int>()
    private val wordRects = ConcurrentHashMap<BookmarkWord, RectF>()

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    private val minWordSize = 24f  // Smaller minimum size
    private val maxWordSize = 150f // Larger maximum size
    private val wordPadding = 15f

    private var rotationAngle = 0f
    private var rotationAnimator: ValueAnimator? = null
    var isRotating = true
    private var rotationSpeed = 0.5f // Degrees per frame
    init {
        startRotationAnimation()
    }
    private val centerX get() = width / 2f
    private val centerY get() = height / 2f

    private val colors = listOf(
        Color.parseColor("#1976D2"), // Dark Blue
        Color.parseColor("#388E3C"), // Dark Green
        Color.parseColor("#D32F2F"), // Dark Red
        Color.parseColor("#7B1FA2"), // Dark Purple
        Color.parseColor("#E64A19")  // Dark Orange
    )

    private val arabicTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.uthmanic_scripts_hafs)
    }

    private val boundsPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
        alpha = 230
        setShadowLayer(4f, 0f, 2f, Color.parseColor("#20000000")) // Subtle shadow
    }

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    var onWordClickListener: ((BookmarkWord) -> Unit)? = null

    private fun calculateSizesAndColors() {
        if (words.isEmpty()) return

        val occurrences = words.map { wordOccurrences[it.word_text] ?: 1 }
        val maxOccurrence = occurrences.maxOrNull() ?: 1
        val minOccurrence = occurrences.minOrNull() ?: 1

        Log.d("WordCloudView", "Max occurrence: $maxOccurrence, Min occurrence: $minOccurrence")

        words.forEach { word ->
            val occurrence = wordOccurrences[word.word_text] ?: 1

            // Use logarithmic scaling for more dramatic size differences
            val logMax = log10(maxOccurrence.toFloat() + 1)
            val logMin = log10(minOccurrence.toFloat() + 1)
            val logCurrent = log10(occurrence.toFloat() + 1)

            // Calculate normalized ratio with exponential scaling
            val ratio = if (logMax == logMin) 1f else {
                ((logCurrent - logMin) / (logMax - logMin)).pow(0.7f)
            }

            // Calculate size with exponential scaling
            val size = minWordSize + (maxWordSize - minWordSize) * ratio

            wordSizes[word] = size

            // Assign color based on occurrence ratio
            val colorIndex = (ratio * (colors.size - 1)).toInt()
            wordColors[word] = colors[colorIndex.coerceIn(0, colors.size - 1)]

            Log.d("WordCloudView", """
                Word: ${word.word_text}
                Occurrence: $occurrence
                Size: $size
                Ratio: $ratio
            """.trimIndent())
        }
    }
    fun startRotationAnimation() {
        rotationAnimator?.cancel()

        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = ((360 / rotationSpeed) * 50).toLong() // Duration based on speed
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                rotationAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun toggleRotation() {
        isRotating = !isRotating
        if (isRotating) {
            startRotationAnimation()
        } else {
            rotationAnimator?.pause()
        }
    }

    fun setRotationSpeed(speed: Float) {
        rotationSpeed = speed.coerceIn(0.1f, 2f)
        if (isRotating) {
            startRotationAnimation()
        }
    }
    init {
        loadUniqueWordsFromJson()
    }
    private fun loadUniqueWordsFromJson() {
        try {
            val jsonString = context.assets.open("unique_words.json").bufferedReader().use { it.readText() }
            val wordsObject = JSONObject(jsonString).getJSONObject("words")

            wordsObject.keys().forEach { wordId ->
                val wordObj = wordsObject.getJSONObject(wordId)
                wordOccurrences[wordObj.getString("word_text")] = wordObj.getInt("total_occurrences")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWords(newWords: List<BookmarkWord>) {
        words = newWords.sortedByDescending { wordOccurrences[it.word_text] ?: 0 }
        calculateSizesAndColors()
        requestLayout()
        invalidate()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            calculateWordPositions()
        }
    }

    private fun calculateWordPositions() {
        wordPositions.clear()
        wordRects.clear()
        if (words.isEmpty() || width == 0 || height == 0) return

        val centerX = width / 2f
        val centerY = height / 2f

        // Place largest word in center
        val firstWord = words.first()
        val firstSize = wordSizes[firstWord] ?: maxWordSize
        wordPositions[firstWord] = PointF(centerX, centerY)

        // Initial radius based on the size of the first word
        var radius = firstSize * 0.8f
        var angle = 0.0
        val angleStep = 0.3 // Initial angle step

        words.drop(1).forEach { word ->
            var placed = false
            val size = wordSizes[word] ?: minWordSize

            while (!placed) {
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()

                // Check if position is valid
                if (isPositionValid(word, x, y)) {
                    wordPositions[word] = PointF(x, y)
                    placed = true
                }

                // Adjust angle and radius if needed
                angle += angleStep
                if (angle >= 2 * Math.PI) {
                    angle = 0.0
                    radius += size * 0.5f // Increase radius based on word size
                }
            }
        }
    }

    private fun isPositionValid(word: BookmarkWord, x: Float, y: Float): Boolean {
        val size = wordSizes[word] ?: minWordSize
        val padding = wordPadding + size * 0.2f // Padding proportional to word size

        // Create bounds for new word
        val newBounds = RectF(
            x - size/2 - padding,
            y - size/2 - padding,
            x + size/2 + padding,
            y + size/2 + padding
        )

        // Check collision with existing words
        return wordPositions.none { (existingWord, pos) ->
            val existingSize = wordSizes[existingWord] ?: minWordSize
            val existingPadding = wordPadding + existingSize * 0.2f

            val existingBounds = RectF(
                pos.x - existingSize/2 - existingPadding,
                pos.y - existingSize/2 - existingPadding,
                pos.x + existingSize/2 + existingPadding,
                pos.y + existingSize/2 + existingPadding
            )

            RectF.intersects(newBounds, existingBounds)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor, centerX, centerY)

        // Apply rotation around the center
        canvas.rotate(rotationAngle, centerX, centerY)

        // Draw all words
        wordPositions.forEach { (word, position) ->
            val size = wordSizes[word] ?: minWordSize
            val color = wordColors[word] ?: colors[0]

            // Counter-rotate each word to keep it readable
            canvas.save()
            canvas.translate(position.x, position.y)
            canvas.rotate(-rotationAngle)

            // Draw only the text without background
            arabicTextPaint.apply {
                textSize = size
                this.color = color
            }
            canvas.drawText(word.word_text, 0f, 0f, arabicTextPaint)

            canvas.restore()
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator?.cancel()
    }

    // Modify touch handling to stop rotation when user interacts
    private var isUserInteracting = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isUserInteracting = true
                rotationAnimator?.pause()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserInteracting = false
                if (isRotating) {
                    rotationAnimator?.resume()
                }
            }
        }

        // Handle existing touch events (scaling, dragging, etc.)
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.5f, 3f)
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
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

    private fun handleTap(x: Float, y: Float) {
        val transformedX = (x - translateX) / scaleFactor
        val transformedY = (y - translateY) / scaleFactor

        var closestWord: BookmarkWord? = null
        var minDistance = Float.MAX_VALUE

        wordPositions.forEach { (word, position) ->
            val distance = Math.hypot(
                (transformedX - position.x).toDouble(),
                (transformedY - position.y).toDouble()
            ).toFloat()

            val size = wordSizes[word] ?: minWordSize
            if (distance < minDistance && distance < size / 2) {
                minDistance = distance
                closestWord = word
            }
        }

        closestWord?.let { onWordClickListener?.invoke(it) }
    }

}