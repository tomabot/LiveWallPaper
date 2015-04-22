package com.itad230.lwtech.usingshaders;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;

/**
 * Created by tomabot on 4/22/15.
 */
public class UsingShadersRenderer implements GLSurfaceView.Renderer {
    private static final String USINGSHADERSRENDERER = "UsingShadersRenderer";

    private final Context context;
    private int program;

    private static final String A_COLOR = "a_Color";    // constant name for attribute a_Color
    //private int aColorLocation;                         // location of a_Color
    private int colorId;                                // used to pass value to a_Color

    private static final String A_POSITION = "a_Position";  // constant name for attribute a_Position
    //private int aPositionLocation;                          // location of a_Position
    private int positionId;                                 // used to pass value to a_Position

    private static final String U_MVPMATRIX = "u_MVPMatrix";    // constant name for uniform MVP matrix
    //private int uMVPMatrixLocation;                             // location of u_MVPMatrix
    private int mvpMatrixId;                                    // used to pass value to u_MVPMatrix

    private float[] modelMatrix = new float[16];    // model transformation matrix
    private float[] viewMatrix = new float[16];     // view transformation matrix
    private float[] projectionMatrix = new float[16];   // 2D projection matrix
    private float[] mvpMatrix = new float[16];      // combined model/view/projection matrix

    private final int bytesPerFloat = 4;        // no. of bytes per float
    private final int strideBytes = 7 * bytesPerFloat;  // no. of elements per row
    private final int positionOffset = 0;       // offset of the position data
    private final int positionDataSize = 3;     // no. of position data elements
    private final int colorOffset = 3;          // offset of the color data
    private final int colorDataSize = 4;        // no. of color data in elements

    private final FloatBuffer triangleVertexes_fbuff;


    public UsingShadersRenderer(Context context) {
        Log.d(USINGSHADERSRENDERER, "UsingShadersRenderer");
        this.context = context;

        // a triangle with red, green, and blue vertexes.
        final float[] triangleVertexesData = {
                -0.5f, -0.25f, 0.0f,        // x, y, z
                1.0f, 0.0f, 0.0f, 1.0f,     // r, g, b, a

                0.5f, -0.25f, 0.0f,         // x, y, z
                0.0f, 0.0f, 1.0f, 1.0f,     // r, g, b, a

                0.0f, 0.559016994f, 0.0f,   // x, y, z
                0.0f, 1.0f, 0.0f, 1.0f};    // r, g, b, a

        // allocate and format a float buffer
        triangleVertexes_fbuff = ByteBuffer
                .allocateDirect(triangleVertexesData.length *bytesPerFloat)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // put the triangle vertexes into the float buffer
        triangleVertexes_fbuff.put(triangleVertexesData).position(0);
    }

    /** GLSurfaceView calls this method when the surface is created, like
     * when the application is run for the first time, or after the user
     * switches back to the Week3Activity. */
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        Log.d(USINGSHADERSRENDERER, "OnSurfaceCreated");

        glClearColor(0.0f, 0.75f, 0.75f, 0.0f);
        setupEyePosition();
        setupShaders();

    }

    /** GLSurfaceView calls this after the surface is created and when the
     * size is changed, like when the device orientation changes. */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.d(USINGSHADERSRENDERER, "OnSurfaceChanged");
        // Set the viewport to fill the entire surface
        glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
    }

    /* GLSurfaceView calls this to draw a frame. You have to draw something
     * or you get flickering. */
    @Override
    public void onDrawFrame(GL10 glUnused) {
         // clear the rendering surface
        //glClear(GL_COLOR_BUFFER_BIT);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        Log.d(USINGSHADERSRENDERER, "onDrawFrame - " + Long.toString(time));

        // Draw the triangle facing straight on.
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        drawTriangle(triangleVertexes_fbuff);
    }

    private void setupEyePosition() {
        // Eye position
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // Look At vector
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Up vector
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }

    private void setupShaders() {
        // read the shader code into a string
        String vertexShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.vshader);

        String fragmentShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.fshader);

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
        colorId = glGetAttribLocation(program, A_COLOR);

        // get the location of the attribute a_Position
        positionId = glGetAttribLocation(program, A_POSITION);

        // get the location of the uniform u_MVPMatrix
        mvpMatrixId = glGetUniformLocation(program, U_MVPMATRIX);

    }

    private void drawTriangle(final FloatBuffer aTriangleBuffer)
    {
        // Pass in the position information
        aTriangleBuffer.position(positionOffset);
        glVertexAttribPointer(positionId, positionDataSize, GL_FLOAT, false,
                strideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(positionId);

        // Pass in the color information
        aTriangleBuffer.position(colorOffset);
        GLES20.glVertexAttribPointer(colorId, colorDataSize, GLES20.GL_FLOAT, false,
                strideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(colorId);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixId, 1, false, mvpMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }


}
