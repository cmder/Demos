package com.cmder.pointcloudviewer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.io.IOException;
import java.util.List;

public class MyGLSurfaceView extends GLSurfaceView {
    private final MyGLRenderer renderer;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    public MyGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2); // OpenGL ES 2.0

        List<float[]> vertices;

//        try {
//            vertices = PLYParser.readPLY(context, "pointcloud.ply");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        try {
            vertices = DepthMapLoader.loadDepthMapAndGeneratePointCloud(context, "depthmap.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        renderer = new MyGLRenderer(vertices);

        gestureDetector = new GestureDetector(context, new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 传递事件给手势检测器
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private float previousX;
        private float previousY;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dx = e2.getX() - previousX;
            float dy = e2.getY() - previousY;
            renderer.setRotation(dx, dy);
            requestRender();  // 触发重新绘制
            previousX = e2.getX();
            previousY = e2.getY();
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            renderer.setScale(scaleFactor);
            requestRender();  // 触发重新绘制
            return true;
        }
    }
}

