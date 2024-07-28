package com.example.mp4player;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class NextActivity extends Activity {
    private SurfaceView mSurfaceView1;
    private SurfaceView mSurfaceView2;
    private Button mBtnRotate;
    private Button mBtnScale;
    private Button mBtnTranslate;
    private Button mBtnPlay;
    private VideoPlayer mVideoPlayer;
    private List<Surface> mSurfaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        mSurfaceView1 = findViewById(R.id.surfaceView1);
        mSurfaceView2 = findViewById(R.id.surfaceView2);
        mBtnRotate = findViewById(R.id.btn_rotate);
        mBtnScale = findViewById(R.id.btn_scale);
        mBtnTranslate = findViewById(R.id.btn_translate);
        mBtnPlay = findViewById(R.id.btn_play);
        mSurfaces = new ArrayList<>();

        mSurfaceView1.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Surface surface = holder.getSurface();
                mSurfaces.add(surface);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mVideoPlayer.release();
            }
        });

        mSurfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Surface surface = holder.getSurface();
                mSurfaces.add(surface);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mVideoPlayer.release();
            }
        });

        mBtnRotate.setOnClickListener(v -> {
            mVideoPlayer.rotate(90f);
        });

        mBtnScale.setOnClickListener(v -> {
            mVideoPlayer.scale(0.8f, 0.8f);
        });

        mBtnTranslate.setOnClickListener(v -> {
            mVideoPlayer.translate(0.8f, 0.8f);
        });

        mBtnPlay.setOnClickListener(v -> {
            mVideoPlayer = new VideoPlayer(
                    NextActivity.this,
                    "android.resource://" + getPackageName() + "/" + R.raw.test,
                    mSurfaces);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

