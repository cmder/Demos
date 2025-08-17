package com.cmder.yuvgl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class YuvToRgbRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Vertex shader source code (GLSL)
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform mat4 uMVPMatrix;

        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    // Fragment shader source code (GLSL) for YUV420P to RGB conversion
    // Assumes three separate textures: Y (luminance), U (chroma blue), V (chroma red)
    // For other formats like NV21, adjust the sampling accordingly (e.g., VU interleaved).
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D yTexture;
        uniform sampler2D uTexture;
        uniform sampler2D vTexture;

        void main() {
            float y = texture2D(yTexture, vTexCoord).r;
            float u = texture2D(uTexture, vTexCoord).r - 0.5;
            float v = texture2D(vTexture, vTexCoord).r - 0.5;

            // YUV to RGB conversion matrix (BT.601 standard)
            float r = y + 1.402 * v;
            float g = y - 0.344136 * u - 0.714136 * v;
            float b = y + 1.772 * u;

            gl_FragColor = vec4(r, g, b, 1.0);
        }
    """.trimIndent()

    // Program handle
    private var program: Int = 0

    // Handles for attributes and uniforms
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var yTextureHandle: Int = 0
    private var uTextureHandle: Int = 0
    private var vTextureHandle: Int = 0

    // Texture IDs
    private var yTextureId: Int = 0
    private var uTextureId: Int = 0
    private var vTextureId: Int = 0

    // MVP matrix
    private val mvpMatrix = FloatArray(16)

    // Vertex data: positions and texture coordinates for a full-screen quad
    private val vertexData = floatArrayOf(
        -1f, -1f, 0f, 0f, 1f,  // Bottom-left
        1f, -1f, 0f, 1f, 1f,   // Bottom-right
        -1f, 1f, 0f, 0f, 0f,   // Top-left
        1f, 1f, 0f, 1f, 0f     // Top-right
    )
    private lateinit var vertexBuffer: FloatBuffer

    // Example YUV data dimensions (adjust based on your input)
    private var width: Int = 640
    private var height: Int = 480

    // Example YUV buffers (in real use, fill these from Camera or Video source)
    private lateinit var yBuffer: ByteBuffer
    private lateinit var uBuffer: ByteBuffer
    private lateinit var vBuffer: ByteBuffer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Compile and link shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Get handles
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        yTextureHandle = GLES20.glGetUniformLocation(program, "yTexture")
        uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        vTextureHandle = GLES20.glGetUniformLocation(program, "vTexture")

        // Initialize vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
            .apply { position(0) }

        // Create textures
        val textureIds = IntArray(3)
        GLES20.glGenTextures(3, textureIds, 0)
        yTextureId = textureIds[0]
        uTextureId = textureIds[1]
        vTextureId = textureIds[2]

        // Bind and set textures (initially empty, update in draw)
        bindTexture(yTextureId, GLES20.GL_TEXTURE0)
        bindTexture(uTextureId, GLES20.GL_TEXTURE1)
        bindTexture(vTextureId, GLES20.GL_TEXTURE2)

        // Initialize example YUV buffers (in real app, update these dynamically)
        initializeYuvBuffers()

        // Set orthographic projection
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Update textures with YUV data
        updateTextures()

        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1i(yTextureHandle, 0)
        GLES20.glUniform1i(uTextureHandle, 1)
        GLES20.glUniform1i(vTextureHandle, 2)

        // Enable attributes
        val stride = 5 * 4  // 5 floats per vertex * 4 bytes
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(positionHandle)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable attributes
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun bindTexture(textureId: Int, unit: Int) {
        GLES20.glActiveTexture(unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun initializeYuvBuffers() {
        // Example: Allocate buffers (in real use, get from CameraPreview or MediaCodec)
        yBuffer = ByteBuffer.allocateDirect(width * height)
        uBuffer = ByteBuffer.allocateDirect(width * height / 4)
        vBuffer = ByteBuffer.allocateDirect(width * height / 4)

        // Fill with dummy data for testing (e.g., gray image)
        yBuffer.put(ByteArray(width * height) { 128.toByte() })
        uBuffer.put(ByteArray(width * height / 4) { 128.toByte() })
        vBuffer.put(ByteArray(width * height / 4) { 128.toByte() })
        yBuffer.position(0)
        uBuffer.position(0)
        vBuffer.position(0)
    }

    private fun updateTextures() {
        // Update Y texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_LUMINANCE,
            width,
            height,
            0,
            GLES20.GL_LUMINANCE,
            GLES20.GL_UNSIGNED_BYTE,
            yBuffer
        )

        // Update U texture (half size)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_LUMINANCE,
            width / 2,
            height / 2,
            0,
            GLES20.GL_LUMINANCE,
            GLES20.GL_UNSIGNED_BYTE,
            uBuffer
        )

        // Update V texture (half size)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_LUMINANCE,
            width / 2,
            height / 2,
            0,
            GLES20.GL_LUMINANCE,
            GLES20.GL_UNSIGNED_BYTE,
            vBuffer
        )
    }

    // In real app, call this to update YUV data from external source
    fun updateYuvData(
        newY: ByteArray,
        newU: ByteArray,
        newV: ByteArray,
        newWidth: Int,
        newHeight: Int
    ) {
        width = newWidth
        height = newHeight
        yBuffer = ByteBuffer.wrap(newY)
        uBuffer = ByteBuffer.wrap(newU)
        vBuffer = ByteBuffer.wrap(newV)
    }
}