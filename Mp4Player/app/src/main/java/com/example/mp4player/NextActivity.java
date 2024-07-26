package com.example.mp4player;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class NextActivity extends Activity {
    private SurfaceView mSurfaceView;
    private Button mBtnRotate;
    private Button mBtnScale;
    private Button mBtnTranslate;
    private VideoPlayer mVideoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        mSurfaceView = findViewById(R.id.surfaceView);
        mBtnRotate = findViewById(R.id.btn_rotate);
        mBtnScale = findViewById(R.id.btn_scale);
        mBtnTranslate = findViewById(R.id.btn_translate);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Surface surface = holder.getSurface();
                mVideoPlayer = new VideoPlayer(
                        NextActivity.this,
                        "android.resource://" + getPackageName() + "/" + R.raw.test,
                        surface);
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

    }
}

