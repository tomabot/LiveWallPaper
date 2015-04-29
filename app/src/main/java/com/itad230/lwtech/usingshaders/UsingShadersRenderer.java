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

import util.FloatResourceReader;
import util.LoggerConfig;
import util.ShaderHelper;
import util.TextResourceReader;

import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * Created by tomabot on 4/22/15.
 */
public class UsingShadersRenderer implements GLSurfaceView.Renderer {
    private static final String USINGSHADERSRENDERER = "UsingShadersRenderer";

    private final Context context;

    private float[] modelMatrix = new float[16];        // model transformation matrix
    private float[] viewMatrix = new float[16];         // view transformation matrix
    private float[] projectionMatrix = new float[16];   // 2D projection matrix
    private float[] mvpMatrix = new float[16];          // combined model/view/projection matrix
    private float[] lightModelMatrix = new float[16];   //copy of the model matrix for the light position


    // names for fields in the shaders
    private static final String A_COLOR = "a_Color";      // constant name for attribute a_Color
    private static final String A_POSITION = "a_Position";    // constant name for attribute a_Position
    private static final String U_MVPMATRIX = "u_MVPMatrix";  // constant name for uniform MVP matrix

    // id's (or handles) for fields in the shaders
    private int mvpMatrixId;    // for passing model/view/projection matrix
    private int mvMatrixId;     // for passing model/view matrix
    private int lightPosId;     // for passing light position
    private int positionId;     // for passing model position information
    private int colorId;        // for passing model color information
    private int normalId;       // for passing model normal information

    // float buffers for cube data
    private FloatBuffer cubeVertexes;
    private FloatBuffer cubeColors;
    private FloatBuffer cubeNormals;

    // sizes and offsets for regions in the float buffers
    private final int bytesPerFloat = 4;        // no. of bytes per float
    //private final int strideBytes = 7 * bytesPerFloat;  // no. of elements per row
    private final int positionOffset = 0;       // offset of the position data
    private final int positionDataSize = 3;     // no. of position data elements
    private final int colorOffset = 3;          // offset of the color data
    private final int colorDataSize = 4;        // size of color data in elements
    private final int normalDataSize = 3;       // size of normal data in elements

    // data descriptions of the in-scene light source
    private final float[] lightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f };

    // current pos of light in world space xformed by model matrix
    private float[] lightPosInWorldSpace = new float[4];
    // xformed pos of light in eye space after xformed by model/view matrix
    private float[] lightPosInEyeSpace = new float[4];

    // id's (or handles) of shader programs
    private int cubeProgramId;      // handle to per-fragment cube shading program
    private int lightProgramId;     // handle to light point program

    public UsingShadersRenderer(Context context)
    {
        Log.d(USINGSHADERSRENDERER, "UsingShadersRenderer");
        this.context = context;

        float[] cubeVertexData = FloatResourceReader.readFloatFileFromResource(context, R.raw.cube_vertexes);
        float[] cubeColorData  = FloatResourceReader.readFloatFileFromResource(context, R.raw.cube_colors);
        float[] cubeNormalData = FloatResourceReader.readFloatFileFromResource(context, R.raw.cube_normals);

        // Initialize the float buffers for position, color, and normals
        cubeVertexes = ByteBuffer.allocateDirect(cubeVertexData.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeVertexes.put(cubeVertexData).position(0);

        cubeColors = ByteBuffer.allocateDirect(cubeColorData.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeColors.put(cubeColorData).position(0);

        cubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeNormals.put(cubeNormalData).position(0);
    }  // public UsingShadersRenderer(

    /** GLSurfaceView calls this method when the surface is created, like
     * when the application is run for the first time, or after the user
     * switches back to the UsingShadersActivity. */
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        //Log.d(USINGSHADERSRENDERER, "OnSurfaceCreated");
        glClearColor(0.0f, 0.0f, 0.25f, 0.0f);
        glEnable(GLES20.GL_CULL_FACE);
        glEnable(GLES20.GL_DEPTH_TEST);
        setupEyePosition();
        setupShaders();
    }

    /** GLSurfaceView calls this after the surface is created and when the
     * size is changed, like when the device orientation changes. */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        //Log.d(USINGSHADERSRENDERER, "OnSurfaceChanged");
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        float fastAngleInDegrees = (360.0f / 5000.0f) * ((int) time);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(cubeProgramId);

        // Set program id's (handles) for cube drawing.
        mvpMatrixId = GLES20.glGetUniformLocation(cubeProgramId, "u_MVPMatrix");
        mvMatrixId  = GLES20.glGetUniformLocation(cubeProgramId, "u_MVMatrix");
        lightPosId  = GLES20.glGetUniformLocation(cubeProgramId, "u_LightPos");
        positionId  = GLES20.glGetAttribLocation(cubeProgramId,  "a_Position");
        colorId     = GLES20.glGetAttribLocation(cubeProgramId,  "a_Color");
        normalId    = GLES20.glGetAttribLocation(cubeProgramId,  "a_Normal");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(lightModelMatrix, 0);
        Matrix.translateM  (lightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM     (lightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM  (lightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace,   0, viewMatrix,       0, lightPosInWorldSpace, 0);

        // Draw some cubes.
        // right
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM  (modelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.rotateM     (modelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);
        drawCube();

        // left
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM  (modelMatrix, 0, -4.0f, 0.0f, -7.0f);
        Matrix.rotateM     (modelMatrix, 0, -angleInDegrees, 0.0f, 1.0f, 0.0f);
        drawCube();

        // top
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM  (modelMatrix, 0, 0.0f, 4.0f, -7.0f);
        Matrix.rotateM     (modelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        drawCube();

        // bottom
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM  (modelMatrix, 0, 0.0f, -4.0f, -7.0f);
        Matrix.rotateM     (modelMatrix, 0, -angleInDegrees, 0.0f, 1.0f, 0.0f); // tom was here
        drawCube();

        // center
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM  (modelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM     (modelMatrix, 0, fastAngleInDegrees, 0.0f, 1.0f, 1.0f);
        drawCube();

        // Draw a point to indicate the light.
        GLES20.glUseProgram(lightProgramId);
        drawLight();
    }  // public void onDrawFrame(

    private void drawCube()
    {
        // Pass in the position information
        cubeVertexes.position(0);
        GLES20.glVertexAttribPointer(positionId, positionDataSize, GLES20.GL_FLOAT, false,
                0, cubeVertexes);

        GLES20.glEnableVertexAttribArray(positionId);

        // Pass in the color information
        cubeColors.position(0);
        GLES20.glVertexAttribPointer(colorId, colorDataSize, GLES20.GL_FLOAT, false,
                0, cubeColors);

        GLES20.glEnableVertexAttribArray(colorId);

        // Pass in the normal information
        cubeNormals.position(0);
        GLES20.glVertexAttribPointer(normalId, normalDataSize, GLES20.GL_FLOAT, false,
                0, cubeNormals);

        GLES20.glEnableVertexAttribArray(normalId);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mvMatrixId, 1, false, mvpMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mvpMatrixId, 1, false, mvpMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(lightPosId, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }  // private void drawCube(

    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(lightProgramId, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(lightProgramId, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, lightPosInModelSpace[0], lightPosInModelSpace[1], lightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, lightModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }  // private void drawLight(

    private void setupEyePosition() {
        // Eye position
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

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
    } // private void setupEyePosition(

    private void setupShaders() {
        // compile and link the cube drawing shader program first
        String vertexShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.vshader_perfragmentlighting);

        String fragmentShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.fshader_perfragmentlighting);

        // compile the shaders
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        // link the shaders
        cubeProgramId = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        // see if the shaders were combined into a valid program
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(cubeProgramId);
        }


        // compile and link the light drawing shader program
        vertexShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.vshader_pointlightsrc);

        fragmentShaderSource =
                TextResourceReader.readTextFileFromResource(context, R.raw.fshader_pointlightsrc);

        // compile the shaders
        vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        // link the shaders
        lightProgramId = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        // see if the shaders were combined into a valid program
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(lightProgramId);
        }
    }  // private void setupShaders(
}
