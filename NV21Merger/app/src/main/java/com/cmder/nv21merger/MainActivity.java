package com.cmder.nv21merger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Camera camera;
    private CameraPreview cameraPreview;
    private SurfaceView surfaceView;
    private Button mergeButton;
    private ImageView imageView;
    private byte[][] frames = new byte[6][];
    private int frameWidth;
    private int frameHeight;
    private int frameCount = 0;

    private ArrayBlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<>(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        mergeButton = findViewById(R.id.mergeButton);
        imageView = findViewById(R.id.imageView);

        mergeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mergeFrames();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if (checkCameraPermission()) {
            setupCamera();
        } else {
            requestCameraPermission();
        }

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NV21Merger.isMerging = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NV21Merger.encodeToMp4(getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/output.mp4",
                                    frameQueue, frameWidth * 3, frameHeight * 2, 30, 1000000);
                        } catch (IOException e) {
                            Log.e("NV21Merger", "Failed to encode to mp4", e);
                        }
                    }
                }).start();
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NV21Merger.isMerging = false;
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    private void setupCamera() {
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        cameraPreview = new CameraPreview(this, camera, this);
        frameWidth = parameters.getPreviewSize().width;
        frameHeight = parameters.getPreviewSize().height;
        surfaceView.getHolder().addCallback(cameraPreview);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (frameCount < 6) {
            frames[frameCount] = data.clone();
            frameCount++;
        }
    }

    // Convert NV21 to Bitmap
    private Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private void mergeFrames() throws InterruptedException {
        if (frameCount == 6) {
            byte[] mergedFrame = NV21Merger.mergeNV21Frames(frames, frameWidth, frameHeight);
            frameQueue.put(mergedFrame);
            Bitmap bitmap = nv21ToBitmap(mergedFrame, frameWidth * 3, frameHeight * 2);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            Log.d("NV21Merger", "Merged frame created");
            frameCount = 0;
        } else {
            Toast.makeText(this, "Waiting for 6 frames", Toast.LENGTH_SHORT).show();
        }
    }
}
