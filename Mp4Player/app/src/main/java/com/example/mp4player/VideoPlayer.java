package com.example.mp4player;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

public class VideoPlayer {
    private List<Surface> mSurfaces;
    private String mVideoPath;
    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private MyGLThread mMyGLThread;

    public VideoPlayer(Context context, String videoPath, List<Surface> surfaces) {
        mVideoPath = videoPath;
        mSurfaces = surfaces;
        mContext = context;
        mMediaPlayer = new MediaPlayer();
        mMyGLThread = new MyGLThread(surfaces);
        mMyGLThread.start();
        try {
            mMediaPlayer.setDataSource(context, Uri.parse(videoPath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnPreparedListener(mp -> {
            SurfaceTexture surfaceTexture = mMyGLThread.getSurfaceTexture();
            Surface glSurface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(glSurface);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();
        });

        mMediaPlayer.prepareAsync();
    }

    public static class MyGLThread extends Thread implements SurfaceTexture.OnFrameAvailableListener {
        private static final String VERTEX_SHADER_CODE =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "attribute vec2 aTexCoord;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  vTexCoord = aTexCoord;" +
                        "}";

        private static final String FRAGMENT_SHADER_CODE =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;" +
                        "uniform samplerExternalOES sTexture;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(sTexture, vTexCoord);" +
                        "}";

        private static final float[] TRIANGLE_COORDS = {
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
        };

        private static final float[] TEXTURE_COORDS = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        };

        private List<Surface> surfaces;
        private boolean running = true;
        private int width;
        private int height;
        private EGLDisplay eglDisplay;
        private EGLContext eglContext;
        private int textureId;
        private SurfaceTexture surfaceTexture;
        private FloatBuffer vertexBuffer;
        private FloatBuffer textureBuffer;
        private int program;

        private float[] mvpMatrix = new float[16];
        private float[] rotationMatrix = new float[16];
        private float[] scaleMatrix = new float[16];
        private float[] translationMatrix = new float[16];

        private float rotationAngle = 0.0f;
        private float totalRotationAngle = 0.0f;
        private float scaleX = 1.0f;
        private float scaleY = 1.0f;
        private float totalScaleX = 1.0f;
        private float totalScaleY = 1.0f;
        private float translateX = 0.0f;
        private float translateY = 0.0f;
        private float totalTranslateX = 0.0f;
        private float totalTranslateY = 0.0f;

        private EGLSurface[] eglSurfaces;

        public MyGLThread(List<Surface> surfaces) {
            this.surfaces = surfaces;
        }

        @Override
        public void run() {
            initEGL();
            while (running) {
                if (surfaceTexture != null) {
                    surfaceTexture.updateTexImage();
                }
                drawFrame();
            }
            releaseEGL();
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            // Request a new frame to be drawn
        }

        private void initEGL() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1);

            int[] configAttribs = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, configs.length, numConfigs, 0);
            EGLConfig eglConfig = configs[0];

            int[] contextAttribs = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0);

            eglSurfaces = new EGLSurface[surfaces.size()];
            for (int i = 0; i < surfaces.size(); i++) {
                eglSurfaces[i] = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaces.get(i), null, 0);
            }

            EGL14.eglMakeCurrent(eglDisplay, eglSurfaces[0], eglSurfaces[0], eglContext);

            initGL();
        }

        private void initGL() {
            textureId = createTexture();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(this);

            ByteBuffer bb = ByteBuffer.allocateDirect(TRIANGLE_COORDS.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(TRIANGLE_COORDS);
            vertexBuffer.position(0);

            ByteBuffer tb = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4);
            tb.order(ByteOrder.nativeOrder());
            textureBuffer = tb.asFloatBuffer();
            textureBuffer.put(TEXTURE_COORDS);
            textureBuffer.position(0);

            program = createProgram();
        }

        private int createTexture() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            int textureId = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            return textureId;
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private int createProgram() {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            return program;
        }

        private void drawFrame() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glUseProgram(program);

            // Calculate transformation matrices
            Matrix.setIdentityM(mvpMatrix, 0);
            Matrix.setIdentityM(rotationMatrix, 0);
            Matrix.setIdentityM(scaleMatrix, 0);
            Matrix.setIdentityM(translationMatrix, 0);

            Matrix.rotateM(rotationMatrix, 0, totalRotationAngle, 0, 0, 1);
            Matrix.scaleM(scaleMatrix, 0, totalScaleX, totalScaleY, 1);
            Matrix.translateM(translationMatrix, 0, totalTranslateX, totalTranslateY, 0);

            Matrix.multiplyMM(mvpMatrix, 0, rotationMatrix, 0, scaleMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, translationMatrix, 0, mvpMatrix, 0);

            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            int texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

            int textureHandle = GLES20.glGetUniformLocation(program, "sTexture");
            GLES20.glUniform1i(textureHandle, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

            for (EGLSurface eglSurface : eglSurfaces) {
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                EGL14.eglSwapBuffers(eglDisplay, eglSurface);
            }

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }

        public void onSurfaceChanged(int width, int height) {
            this.width = width;
            this.height = height;
            GLES20.glViewport(0, 0, width, height);
        }

        public void requestExitAndWait() {
            running = false;
            try {
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void releaseEGL() {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            for (EGLSurface eglSurface : eglSurfaces) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface);
            }
            EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglTerminate(eglDisplay);
        }

        public SurfaceTexture getSurfaceTexture() {
            return surfaceTexture;
        }

        public void setRotation(float angle) {
            this.totalRotationAngle += angle;
        }

        public void setScale(float scaleX, float scaleY) {
            this.totalScaleX *= scaleX;
            this.totalScaleY *= scaleY;
        }

        public void setTranslation(float translateX, float translateY) {
            this.totalTranslateX += translateX;
            this.totalTranslateY += translateY;
        }
    }

    public void rotate(float angle) {
        mMyGLThread.setRotation(angle);
    }

    public void scale(float scaleX, float scaleY) {
        mMyGLThread.setScale(scaleX, scaleY);
    }

    public void translate(float translateX, float translateY) {
        mMyGLThread.setTranslation(translateX, translateY);
    }

    private void toggleMute(boolean isMuted) {
        if (mMediaPlayer != null) {
            if (isMuted) {
                mMediaPlayer.setVolume(1.0f, 1.0f); // 恢复音量
            } else {
                mMediaPlayer.setVolume(0.0f, 0.0f); // 静音
            }
        }
    }

    public void release() {
        mMediaPlayer.release();
        mMyGLThread.requestExitAndWait();
    }
}
