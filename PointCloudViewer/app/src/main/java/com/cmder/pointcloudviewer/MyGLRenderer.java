package com.cmder.pointcloudviewer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float scale = 1.0f;

    private FloatBuffer vertexBuffer;
    private int program;
    private int positionHandle;

    private float boundingRadius = 1.0f;  // 点云的包围球半径

    // 顶点着色器代码
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // 片段着色器代码
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private float[] vertices;  // 从PLY文件解析的顶点

    public MyGLRenderer(List<float[]> plyVertices) {
        vertices = new float[plyVertices.size() * 3];
        float sumX = 0, sumY = 0, sumZ = 0;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;

        // 遍历点云，计算几何中心和包围盒
        for (int i = 0; i < plyVertices.size(); i++) {
            float[] vertex = plyVertices.get(i);
            vertices[i * 3] = vertex[0];
            vertices[i * 3 + 1] = vertex[1];
            vertices[i * 3 + 2] = vertex[2];

            // 累加坐标值以计算平均值
            sumX += vertex[0];
            sumY += vertex[1];
            sumZ += vertex[2];

            // 更新包围盒
            minX = Math.min(minX, vertex[0]);
            minY = Math.min(minY, vertex[1]);
            minZ = Math.min(minZ, vertex[2]);

            maxX = Math.max(maxX, vertex[0]);
            maxY = Math.max(maxY, vertex[1]);
            maxZ = Math.max(maxZ, vertex[2]);
        }

        // 计算几何中心
        float centerX = sumX / plyVertices.size();
        float centerY = sumY / plyVertices.size();
        float centerZ = sumZ / plyVertices.size();

        // 将几何中心平移到原点
        for (int i = 0; i < plyVertices.size(); i++) {
            vertices[i * 3] -= centerX;
            vertices[i * 3 + 1] -= centerY;
            vertices[i * 3 + 2] -= centerZ;
        }

        // 计算包围球半径
        float dx = maxX - minX;
        float dy = maxY - minY;
        float dz = maxZ - minZ;
        boundingRadius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);  // 初始化模型矩阵为单位矩阵
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // 编译着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // 创建OpenGL程序并链接着色器
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 使用程序
        GLES20.glUseProgram(program);

        // 设置视图矩阵，确保相机正对几何中心，距离足够显示整个点云
        float cameraDistance = boundingRadius * 2.5f;  // 让相机远离原点，确保点云完整显示
        Matrix.setLookAtM(viewMatrix, 0,
                0, 0, cameraDistance,  // 相机位置
                0, 0, 0,               // 观察原点
                0, 1, 0);              // 上向量

        // 计算投影矩阵和视图矩阵的乘积
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // 应用缩放和旋转变换
        Matrix.setIdentityM(modelMatrix, 0);

        // 旋转和缩放
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
        Matrix.rotateM(modelMatrix, 0, rotationX, 0, 1, 0);  // 绕Y轴旋转
        Matrix.rotateM(modelMatrix, 0, rotationY, 1, 0, 0);  // 绕X轴旋转

        // 将模型矩阵与MVP矩阵相乘
        float[] finalMatrix = new float[16];
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        // 传递最终的MVP矩阵给着色器
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        // 获取顶点位置句柄
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");

        // 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle);

        // 准备顶点数据
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        // 设置颜色
        int colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, new float[]{0.0f, 1.0f, 0.0f, 1.0f}, 0);

        // 绘制点云
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertices.length / 3);

        // 禁用顶点数组
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // 设置投影矩阵
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 100);
    }

    // 加载着色器的方法
    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    // 设置旋转角度
    public void setRotation(float dx, float dy) {
        rotationX += dx;
        rotationY += dy;
    }

    // 设置缩放比例
    public void setScale(float scaleFactor) {
        scale *= scaleFactor;
        if (scale < 0.1f) scale = 0.1f;  // 防止缩放过小
        if (scale > 10.0f) scale = 10.0f;  // 防止缩放过大
    }
}
