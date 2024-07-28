package com.example.rotateimage;


import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private int texture;

    private final float[] vertexData = {
            // X, Y, Z, U, V
            -1f, 1f, 0f, 0f, 0f,
            -1f, -1f, 0f, 0f, 1f,
            1f, -1f, 0f, 1f, 1f,
            1f, 1f, 0f, 1f, 0f
    };

    private final short[] indexData = {
            0, 1, 2, 0, 2, 3
    };

    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;

    private final String vertexShaderCode =
            "attribute vec4 a_Position;" +
                    "attribute vec2 a_TexCoord;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "    gl_Position = a_Position;" +
                    "    v_TexCoord = a_TexCoord;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "    gl_FragColor = texture2D(u_Texture, v_TexCoord);" +
                    "}";

    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int textureUniformHandle;

    private float rotationAngle = 0.0f;  // 旋转角度

    public MyGLRenderer(Context context) {
        this.context = context;

        ByteBuffer vb = ByteBuffer.allocateDirect(vertexData.length * 4);
        vb.order(ByteOrder.nativeOrder());
        vertexBuffer = vb.asFloatBuffer();
        vertexBuffer.put(vertexData);
        vertexBuffer.position(0);

        ByteBuffer ib = ByteBuffer.allocateDirect(indexData.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(indexData);
        indexBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord");
        textureUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture");

        texture = loadTexture(context, R.drawable.image);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);

        updateVertexDataWithRotation();

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(textureUniformHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexData.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int loadTexture(Context context, int resourceId) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    // 添加旋转接口
    public void setRotationAngle(float angle) {
        rotationAngle = angle;
    }

    // 更新顶点数据以应用旋转变换
    private void updateVertexDataWithRotation() {
        float[] rotatedVertexData = new float[vertexData.length];
        float centerX = 0.0f;
        float centerY = 0.0f;

        for (int i = 0; i < vertexData.length; i += 5) {
            float x = vertexData[i];
            float y = vertexData[i + 1];

            float newX = (float) ((x - centerX) * Math.cos(Math.toRadians(rotationAngle)) -
                    (y - centerY) * Math.sin(Math.toRadians(rotationAngle)) + centerX);
            float newY = (float) ((x - centerX) * Math.sin(Math.toRadians(rotationAngle)) +
                    (y - centerY) * Math.cos(Math.toRadians(rotationAngle)) + centerY);

            rotatedVertexData[i] = newX;
            rotatedVertexData[i + 1] = newY;
            rotatedVertexData[i + 2] = vertexData[i + 2]; // Z
            rotatedVertexData[i + 3] = vertexData[i + 3]; // U
            rotatedVertexData[i + 4] = vertexData[i + 4]; // V
        }

        vertexBuffer.clear();  // 清除缓冲区
        vertexBuffer.put(rotatedVertexData);
        vertexBuffer.position(0);
    }
}
