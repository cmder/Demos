package com.example.mp4player;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class VideoGLSurfaceView extends GLSurfaceView {
    private VideoRenderer mRenderer;

    public VideoGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public VideoGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mRenderer = new VideoRenderer(context, this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public VideoRenderer getRenderer() {
        return mRenderer;
    }
}

