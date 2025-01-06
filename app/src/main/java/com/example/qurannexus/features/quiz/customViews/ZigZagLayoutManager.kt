package com.example.qurannexus.features.quiz.customViews

import android.util.Log
import androidx.recyclerview.widget.RecyclerView

class ZigZagLayoutManager : RecyclerView.LayoutManager() {
    // Increase horizontal offset for more pronounced zig-zag
    private val horizontalOffset = 300 // Larger value for more dramatic left-right movement
    private val verticalSpacing = 300  // Increased for better spacing between items

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)

        if (itemCount == 0) {
            return
        }

        var currentTop = paddingTop
        val centerX = width / 2

        for (i in 0 until itemCount) {
            val view = recycler.getViewForPosition(i)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            val decoratedMeasuredWidth = getDecoratedMeasuredWidth(view)
            val decoratedMeasuredHeight = getDecoratedMeasuredHeight(view)

            // Calculate horizontal position with more dramatic alternating pattern
            val xOffset = if (i % 2 == 0) {
                // Left side items
                centerX - decoratedMeasuredWidth / 2 - horizontalOffset
            } else {
                // Right side items
                centerX - decoratedMeasuredWidth / 2 + horizontalOffset
            }

            // Add slight horizontal curve effect
            val curveOffset = (Math.sin(i * Math.PI / 6) * 70).toInt()

            layoutDecorated(
                view,
                xOffset + curveOffset,
                currentTop,
                xOffset + decoratedMeasuredWidth + curveOffset,
                currentTop + decoratedMeasuredHeight
            )

            // Adjust vertical spacing to create a smoother path
            currentTop += if (i % 2 == 0) {
                verticalSpacing - 50 // Slightly closer spacing for connected feel
            } else {
                verticalSpacing
            }
        }
    }

    override fun canScrollVertically(): Boolean = true

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        offsetChildrenVertical(-dy)
        return dy
    }
}