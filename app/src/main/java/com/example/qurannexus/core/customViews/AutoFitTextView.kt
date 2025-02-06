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

    private var minTextSize = 12.5f
    private var maxTextSize = 24f

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, maxTextSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustTextSize()
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        adjustTextSize()
    }

    private fun adjustTextSize() {
        val text = text?.toString() ?: return

        // Calculate text size based on digit count
        val textSize = when (text.length) {
            1 -> maxTextSize
            2 -> maxTextSize * 0.75f
            else -> maxTextSize * 0.5f
        }

        // Ensure text size is not smaller than minimum
        val finalTextSize = textSize.coerceAtLeast(minTextSize)

        setTextSize(TypedValue.COMPLEX_UNIT_SP, finalTextSize)
    }
}