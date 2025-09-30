package com.cmder.vademo;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

// https://doc.shengwang.cn/doc/rtc/android/advanced-features/media-player
public class MainActivity extends AppCompatActivity {

    private RtcEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RtcEngineConfig config = new RtcEngineConfig();
        config.mAppId = "8757f90c0bdb4a168a0106e4156c5dbb";
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
    }
}