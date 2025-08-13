package com.example.rotateimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ShortBuffer;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);  // 使用OpenGL ES 2.0
        glSurfaceView.setRenderer(new MyRenderer());  // 设置自定义的Renderer
        setContentView(glSurfaceView);  // 设置GLSurfaceView为Activity的内容视图
    }

    private class MyRenderer implements GLSurfaceView.Renderer {
        // 顶点缓冲区和纹理缓冲区
        private FloatBuffer vertexBuffer;
        private FloatBuffer textureBuffer;
        private ShortBuffer indexBuffer;

        // 顶点数据
        private final float[] vertices = {
                -1.0f,  1.0f, 0.0f,  // 左上角
                -1.0f, -1.0f, 0.0f,  // 左下角
                1.0f, -1.0f, 0.0f,  // 右下角
                1.0f,  1.0f, 0.0f   // 右上角
        };

        // 索引数据
        private final short[] indices = { 0, 1, 2, 0, 2, 3 };

        // 纹理坐标
        private final float[] textureCoords = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        // 纹理ID数组
        private int[] textures = new int[1];

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // 设置清屏颜色
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // 初始化顶点缓冲区
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // 初始化纹理缓冲区
            ByteBuffer tb = ByteBuffer.allocateDirect(textureCoords.length * 4);
            tb.order(ByteOrder.nativeOrder());
            textureBuffer = tb.asFloatBuffer();
            textureBuffer.put(textureCoords);
            textureBuffer.position(0);

            // 初始化索引缓冲区
            ByteBuffer ib = ByteBuffer.allocateDirect(indices.length * 2);
            ib.order(ByteOrder.nativeOrder());
            indexBuffer = ib.asShortBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            // 生成纹理
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

            // 设置纹理过滤参数
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // 加载位图
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            // 加载和编译着色器
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            // 创建OpenGL程序并链接
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            GLES20.glUseProgram(program);

            // 获取并启用顶点属性
            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

            // 获取并启用纹理坐标属性
            int textureCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

            // 获取并设置纹理
            int textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
            GLES20.glUniform1i(textureHandle, 0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // 设置视口
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // 清屏
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            // 绘制元素
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        }

        // 加载着色器
        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        // 顶点着色器代码
        private final String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "attribute vec2 aTexCoord;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "  vTexCoord = aTexCoord;" +
                        "}";

        // 片段着色器代码
        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
                        "}";
    }
}
