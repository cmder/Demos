package com.cmder.pointcloudviewer;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PLYParser {

    public static List<float[]> readPLY(Context context, String assetFileName) throws IOException {
        List<float[]> vertices = new ArrayList<>();

        InputStream is = context.getAssets().open(assetFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        boolean headerEnded = false;
        while ((line = br.readLine()) != null) {
            if (headerEnded) {
                String[] parts = line.trim().split("\\s+");
                float x = Float.parseFloat(parts[0]);
                float y = Float.parseFloat(parts[1]);
                float z = Float.parseFloat(parts[2]);
                vertices.add(new float[]{x, y, z});
            } else if (line.startsWith("end_header")) {
                headerEnded = true;
            }
        }
        br.close();
        return vertices;
    }
}

