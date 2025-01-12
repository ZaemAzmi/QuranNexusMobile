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
                playerWidth = child.width
            }

            fabX = dependency.x

            // Position the player next to the FAB
            val targetX = if (fabX <= parent.width / 2) {
                // FAB is on left side
                fabX + fabWidth + 16 // Add spacing
            } else {
                // FAB is on right side
                fabX - playerWidth - 16 // Add spacing
            }

            child.x = targetX
            child.y = dependency.y // Keep on same vertical line
            return true
        }
        return false
    }
}