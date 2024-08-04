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
                mergeFrames();
            }
        });

        if (checkCameraPermission()) {
            setupCamera();
        } else {
            requestCameraPermission();
        }
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

    private Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private void mergeFrames() {
        if (frameCount == 6) {
            byte[] mergedFrame = NV21Merger.mergeNV21Frames(frames, frameWidth, frameHeight);
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
