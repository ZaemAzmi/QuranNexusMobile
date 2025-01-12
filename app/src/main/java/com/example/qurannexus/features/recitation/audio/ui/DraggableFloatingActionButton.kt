package com.example.qurannexus.features.recitation.audio.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DraggableFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var isDragging: Boolean = false
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val edgePadding = context.resources.getDimensionPixelSize(
        com.example.qurannexus.R.dimen.fab_edge_padding
    )

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isDragging) {
                    performClick()
                    return true
                }
                return false
            }
        })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                lastX = event.rawX
                lastY = event.rawY
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isDragging = true
                    }
                }

                if (isDragging) {
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    val parent = parent as? ViewGroup
                    if (parent != null) {
                        val maxX = parent.width - width - edgePadding
                        val newX = max(edgePadding.toFloat(),
                            min(maxX.toFloat(), x + deltaX))
                        val newY = max(edgePadding.toFloat(),
                            min(parent.height - height - edgePadding.toFloat(),
                                y + deltaY))

                        animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                    }

                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val parent = parent as? ViewGroup
                    if (parent != null) {
                        val center = x + width / 2
                        val targetX = if (center <= parent.width / 2) {
                            edgePadding.toFloat()
                        } else {
                            parent.width - width - edgePadding.toFloat()
                        }

                        animate()
                            .x(targetX)
                            .setDuration(150)
                            .start()
                    }
                }
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        if (isDragging) return false
        return super.performClick()
    }
}