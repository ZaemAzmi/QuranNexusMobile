package com.example.qurannexus.core.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.example.qurannexus.core.activities.MainActivity
import com.etebarian.meowbottomnavigation.MeowBottomNavigation

class ScrollAwareBottomNavigationBehavior(
    context: Context,
    attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<MeowBottomNavigation>(context, attrs) {

    private var dyDirectionChange = false
    private var isScrollingDown = false
    private var lastKnownDownScroll = 0f
    private var lastKnownUpScroll = 0f
    private var bottomNavigationHidden = false

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: MeowBottomNavigation,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: MeowBottomNavigation,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(
            coordinatorLayout, child, target, dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, type, consumed
        )

        // Early return if scroll is minimal
        if (dyConsumed == 0) return

        if (dyConsumed > 0) {
            // Scrolling down
            lastKnownDownScroll += dyConsumed
            if (!isScrollingDown && lastKnownDownScroll > 20) {
                isScrollingDown = true
                dyDirectionChange = true
                lastKnownUpScroll = 0f
            }
        } else {
            // Scrolling up
            lastKnownUpScroll -= dyConsumed
            if (isScrollingDown && lastKnownUpScroll > 20) {
                isScrollingDown = false
                dyDirectionChange = true
                lastKnownDownScroll = 0f
            }
        }

        if (dyDirectionChange) {
            dyDirectionChange = false
            val activity = coordinatorLayout.context as? MainActivity
            if (isScrollingDown && !bottomNavigationHidden) {
                activity?.setBottomNavigationVisibility(false)
                bottomNavigationHidden = true
            } else if (!isScrollingDown && bottomNavigationHidden) {
                activity?.setBottomNavigationVisibility(true)
                bottomNavigationHidden = false
            }
        }
    }
}