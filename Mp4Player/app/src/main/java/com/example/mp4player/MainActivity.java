package com.example.mp4player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends Activity {
    private VideoGLSurfaceView mGLSurfaceView;
    private Button btnRotateLeft, btnRotateRight, btnZoomIn, btnZoomOut, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        btnRotateLeft = findViewById(R.id.btn_rotate_left);
        btnRotateRight = findViewById(R.id.btn_rotate_right);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        btnNext = findViewById(R.id.btn_next);

        btnRotateLeft.setOnClickListener(v -> {
            mGLSurfaceView.getRenderer().rotate(-10);
            mGLSurfaceView.requestRender();
        });

        btnRotateRight.setOnClickListener(v -> {
            mGLSurfaceView.getRenderer().rotate(10);
            mGLSurfaceView.requestRender();
        });

        btnZoomIn.setOnClickListener(v -> {
            mGLSurfaceView.getRenderer().scale(1.1f);
            mGLSurfaceView.requestRender();
        });

        btnZoomOut.setOnClickListener(v -> {
            mGLSurfaceView.getRenderer().scale(0.9f);
            mGLSurfaceView.requestRender();
        });

        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NextActivity.class));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mGLSurfaceView.onPause();
        if (mGLSurfaceView != null && mGLSurfaceView.getRenderer() != null) {
            VideoRenderer renderer = mGLSurfaceView.getRenderer();
            if (renderer.getMediaPlayer() != null) {
                renderer.getMediaPlayer().stop();
                renderer.getMediaPlayer().release();
                renderer.setMediaPlayer(null);
            }
            mGLSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放MediaPlayer资源
        if (mGLSurfaceView != null && mGLSurfaceView.getRenderer() != null) {
            VideoRenderer renderer = mGLSurfaceView.getRenderer();
            if (renderer.getMediaPlayer() != null) {
                renderer.getMediaPlayer().stop();
                renderer.getMediaPlayer().release();
                renderer.setMediaPlayer(null);
            }
            mGLSurfaceView.onPause();
        }
    }
}
