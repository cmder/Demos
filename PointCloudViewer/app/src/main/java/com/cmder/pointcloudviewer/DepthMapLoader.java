package com.cmder.pointcloudviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DepthMapLoader {

    // 从 assets 或其他来源加载 PNG 深度图并生成点云
    public static List<float[]> loadDepthMapAndGeneratePointCloud(Context context, String assetFileName) throws IOException {
        InputStream is = context.getAssets().open(assetFileName);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[][] depthMap = new float[height][width];

        // 遍历图像的像素，提取红色通道值作为深度值
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int red = (pixel >> 16) & 0xff;  // 提取红色通道的值

                // 将红色通道的值归一化到 0.0 到 1.0 之间
                float depthValue = red / 255.0f;

                depthMap[y][x] = depthValue;
            }
        }

        // 使用提供的相机内参生成点云
        float fx = 238.31752014160156f;
        float fy = 238.31752014160156f;
        float cx = 158.93447875976562f;
        float cy = 100.52653503417969f;

        // 生成点云
        return convertDepthMapToPointCloud(depthMap, fx, fy, cx, cy);
    }

    // 将深度图转换为点云的 List<float[]>
    private static List<float[]> convertDepthMapToPointCloud(float[][] depthMap, float fx, float fy, float cx, float cy) {
        List<float[]> pointCloud = new ArrayList<>();

        int height = depthMap.length;  // 深度图的高度
        int width = depthMap[0].length;  // 深度图的宽度

        // 遍历每个像素，将其转换为3D点
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                float z = depthMap[v][u] * 10;  // 获取深度值并假设最大深度为10（可以根据需要调整）

                // 跳过没有深度值的像素
                if (z == 0) continue;

                // 使用相机内参将2D像素坐标 (u, v) 和深度 z 转换为 3D世界坐标 (X, Y, Z)
                float x = (u - cx) * z / fx;
                float y = (v - cy) * z / fy;

                // 将点加入点云列表
                pointCloud.add(new float[]{x, -y, -z}); // 根据需求翻转 y 和 z 轴
            }
        }

        return pointCloud;
    }
}
