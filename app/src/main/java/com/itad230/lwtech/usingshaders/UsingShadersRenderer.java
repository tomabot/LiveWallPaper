package com.itad230.lwtech.usingshaders;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import util.TextResourceReader;

import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

/**
 * Created by tomabot on 4/22/15.
 */
public class UsingShadersRenderer implements GLSurfaceView.Renderer {
    private final Context context;

    public UsingShadersRenderer(Context context) {
        this.context = context;
    }

    // GLSurfaceView calls this method when the surface is created, like
    // when the application is run for the first time, or after the user
    // switches back to the Week3Activity.
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.75f, 0.75f, 0.0f);

        // read in the shaders
        String vertexShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.vertex_shader);
        String fragmentShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.fragment_shader);

    }

    // GLSurfaceView calls this after the surface is created and when the
    // size is changed, like when the device orientation changes.
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the viewport to fill the entire surface
        glViewport(0, 0, width, height);
    }

    // GLSurfaceView calls this to draw a frame. You have to draw something
    // or you will get flickering.
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // clear the rendering surface
        glClear(GL_COLOR_BUFFER_BIT);
    }

}
