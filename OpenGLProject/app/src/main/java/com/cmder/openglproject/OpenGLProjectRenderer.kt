package com.cmder.openglproject

import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLProjectRenderer : GLSurfaceView.Renderer {
    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    override fun onDrawFrame(p0: GL10?) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);
    }

    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     */
    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, p1, p2);
    }


    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        // Set the background color to red
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)
    }

}