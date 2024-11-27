package com.example.qurannexus.core.customViews

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
class AutoFitTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var minTextSize = 8f
    private var maxTextSize = 16f

    init {
        // Set the initial text size to the max size
        setTextSize(TypedValue.COMPLEX_UNIT_SP, maxTextSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustTextSize()
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        adjustTextSize()
    }

    private fun adjustTextSize() {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        // Start from the maximum text size
        var textSize = maxTextSize

        // Reduce text size until it fits within the view
        while (textSize > minTextSize) {
            paint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSize,
                resources.displayMetrics
            )

            val textBounds = android.graphics.Rect()
            paint.getTextBounds(text.toString(), 0, text.length, textBounds)

            if (textBounds.width() <= availableWidth && textBounds.height() <= availableHeight) {
                break
            }

            textSize -= 1f
        }

        // Set the calculated text size
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }
}