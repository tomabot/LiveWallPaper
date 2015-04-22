package com.itad230.lwtech.usingshaders;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import util.LoggerConfig;
import util.ShaderHelper;
import util.TextResourceReader;

import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

/**
 * Created by tomabot on 4/22/15.
 */
public class UsingShadersRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private int program;

    // constant name for the uniform u_Color
    private static final String U_COLOR = "u_Color";

    // location of the uniform u_Color, used to update it
    private int uColorLocation;

    // constant name for the attribute a_Position
    private static final String A_POSITION = "a_Position";

    // location of the attribute a_Position, used to set it
    private int aPositionLocation;

    public UsingShadersRenderer(Context context) {
        this.context = context;
    }

    // GLSurfaceView calls this method when the surface is created, like
    // when the application is run for the first time, or after the user
    // switches back to the Week3Activity.
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.75f, 0.75f, 0.0f);

        // read the shader code into a string
        String vertexShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.vertex_shader);
        String fragmentShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.fragment_shader);

        // compile the shaders
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        // link the shaders
        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        // see if the shaders were combined into a valid program
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        // Enable the open gl program. This tells open gl to use this program
        // when anything is drawn to the screen.
        glUseProgram(program);

        // get the location of the uniform u_Color
        uColorLocation = glGetUniformLocation(program, U_COLOR);

        // get the location of the attribute a_Position
        aPositionLocation = glGetAttribLocation(program, A_POSITION);

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
