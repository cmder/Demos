package com.example.mp4player;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;" +
                    "attribute vec4 aTextureCoord;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uSTMatrix;" +
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * aPosition;" +
                    "    vTextureCoord = (uSTMatrix * aTextureCoord).xy;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform samplerExternalOES sTexture;" +
                    "void main() {" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);" +
                    "}";

    private SurfaceTexture mSurfaceTexture;
    private int mTextureID;
    private MediaPlayer mMediaPlayer;
    private float[] mSTMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float currentRotationAngle = 0.0f;
    private float currentScaleFactor = 1.0f;

    private int mProgram;
    private int mPositionHandle;
    private int mTextureHandle;
    private int mMVPMatrixHandle;
    private int mSTMatrixHandle;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    private GLSurfaceView mGLSurfaceView;

    private int mViewWidth;
    private int mViewHeight;

    private final float[] mVerticesData = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

    private final float[] mTextureData = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    public VideoRenderer(Context context, GLSurfaceView glSurfaceView) {
        mMediaPlayer = MediaPlayer.create(context, R.raw.test);
        mGLSurfaceView = glSurfaceView;
        Matrix.setIdentityM(mMVPMatrix, 0);

        ByteBuffer bb = ByteBuffer.allocateDirect(mVerticesData.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mVerticesData);
        mVertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(mTextureData.length * 4);
        tb.order(ByteOrder.nativeOrder());
        mTextureBuffer = tb.asFloatBuffer();
        mTextureBuffer.put(mTextureData);
        mTextureBuffer.position(0);
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(mSurfaceTexture);
        mMediaPlayer.setSurface(surface);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        GLES20.glUseProgram(mProgram);

        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mTextureHandle, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(mTextureHandle);

        // 计算长宽比
        float aspectRatio = (float) mViewWidth / mViewHeight;
        float[] projectionMatrix = new float[16];
        if (mViewWidth > mViewHeight) {
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, -1, 1);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / aspectRatio, 1 / aspectRatio, -1, 1);
        }

        // 计算最终的变换矩阵
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.translateM(mMVPMatrix, 0, 0, 0, 0);
        Matrix.rotateM(mMVPMatrix, 0, currentRotationAngle, 0, 0, 1);
        Matrix.scaleM(mMVPMatrix, 0, currentScaleFactor, currentScaleFactor, 1);
        Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGLSurfaceView.requestRender();
    }

    public void rotate(float angle) {
        currentRotationAngle += angle;
    }

    public void scale(float scaleFactor) {
        currentScaleFactor *= scaleFactor;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}