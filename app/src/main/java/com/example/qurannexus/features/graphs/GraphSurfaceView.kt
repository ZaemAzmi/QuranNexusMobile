package com.example.qurannexus.features.graphs

import android.content.Context
import android.graphics.PixelFormat
import android.util.AttributeSet
import org.rajawali3d.view.SurfaceView
import com.example.qurannexus.features.graphs.GraphRenderer
import org.rajawali3d.util.Capabilities
import org.rajawali3d.view.ISurface
class GraphSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs) {

    init {
        // Set up EGL
        setEGLContextClientVersion(2)
        setEGLConfigChooser(CustomConfigChooser())

        // Create and set renderer
        val renderer = GraphRenderer(context)
        setSurfaceRenderer(renderer)

        // Enable touch interaction
        setOnTouchListener { _, event ->
            renderer.onTouchEvent(event)
            true
        }
    }
}