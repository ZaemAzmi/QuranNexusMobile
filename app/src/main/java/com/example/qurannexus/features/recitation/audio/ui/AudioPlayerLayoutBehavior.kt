package com.example.qurannexus.features.recitation.audio.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AudioPlayerBehavior(
    context: Context,
    attrs: AttributeSet?
) : CoordinatorLayout.Behavior<MaterialCardView>(context, attrs) {

    private var fabX = 0f
    private var fabWidth = 0
    private var playerWidth = 0
    private val MARGIN = 16 // dp

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: MaterialCardView,
        dependency: View
    ): Boolean {
        return dependency is FloatingActionButton
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: MaterialCardView,
        dependency: View
    ): Boolean {
        if (dependency is FloatingActionButton) {
            if (fabWidth == 0) {
                fabWidth = dependency.width
            }

            fabX = dependency.x

            // Calculate the x position to avoid overlap
            // We want the card to end before the FAB starts
            val cardEndX = dependency.x - MARGIN.dpToPx(parent.context)
            child.x = MARGIN.dpToPx(parent.context) // Start from left with margin
            child.y = dependency.y // Keep on same vertical line

            // Set the width to fill the space up to the FAB
            val newWidth = cardEndX - child.x
            val params = child.layoutParams
            params.width = newWidth.toInt()
            child.layoutParams = params

            return true
        }
        return false
    }

    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
}