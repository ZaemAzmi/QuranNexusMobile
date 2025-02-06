package com.example.qurannexus.features.graphs
import android.opengl.GLSurfaceView
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.materials.Material
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.util.Stack
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLDisplay

class GraphRenderer(context: Context) : Renderer(context) {
    override fun initScene() {
        currentCamera.position = Vector3(0.0, 0.0, 10.0)
        currentCamera.setLookAt(0.0, 0.0, 0.0)
        createGraph()
    }

    private fun createGraph() {
        val points = Stack<Vector3>()

        // Generate points for a 3D sine wave graph
        for (x in -5..5) {
            for (z in -5..5) {
                val y = Math.sin(x.toDouble()) * Math.cos(z.toDouble())
                points.add(Vector3(x.toDouble(), y, z.toDouble()))
            }
        }

        val line3D = Line3D(points, 1f, 0x00FF00)
        val material = Material()
        material.color = 0x00FF00
        material.enableLighting(true)
        line3D.material = material

        currentScene.addChild(line3D)
    }

    override fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onTouchEvent(event: MotionEvent?) {
        // Handle touch events if needed
    }
}

class CustomConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val attributes = intArrayOf(
            EGL10.EGL_LEVEL, 0,
            EGL10.EGL_RENDERABLE_TYPE, 4,  // EGL_OPENGL_ES2_BIT
            EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_SAMPLE_BUFFERS, 0,
            EGL10.EGL_SAMPLES, 0,
            EGL10.EGL_NONE
        )

        val configsCount = IntArray(1)
        egl.eglChooseConfig(display, attributes, null, 0, configsCount)

        val numConfigs = configsCount[0]
        if (numConfigs <= 0) {
            throw IllegalArgumentException("No EGL configs match configuration")
        }

        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        egl.eglChooseConfig(display, attributes, configs, numConfigs, configsCount)

        return configs[0] ?: throw IllegalArgumentException("No EGL config chosen")
    }
}

