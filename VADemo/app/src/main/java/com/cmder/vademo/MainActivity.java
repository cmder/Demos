package com.cmder.vademo;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

// 1. agora 视频播放 2. visualon 视频播放 3. 从RTMP直播流切换到VOD流播放
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SAMPLE_MOVIE_URL = "https://agora-adc-artifacts.s3.cn-north-1.amazonaws.com.cn/resources/sample.mp4";

    private RtcEngine engine;
    private IMediaPlayer mediaPlayer;
    private ChannelMediaOptions options = new ChannelMediaOptions();

    private SurfaceView surfaceView;

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

        surfaceView = findViewById(R.id.surfaceView);

        RtcEngineConfig config = new RtcEngineConfig();
        config.mAppId = getString(R.string.agora_app_id);

        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        config.mEventHandler = new IRtcEngineEventHandler() {
        };
        try {
            engine = RtcEngine.create(config);
            mediaPlayer = engine.createMediaPlayer();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClick(View view) {
        VideoCanvas videoCanvas = new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0);
        videoCanvas.sourceType = Constants.VIDEO_SOURCE_MEDIA_PLAYER;
        videoCanvas.mediaPlayerId = mediaPlayer.getMediaPlayerId();
        engine.setupLocalVideo(videoCanvas);
        engine.setDefaultAudioRoutetoSpeakerphone(true);

        engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.autoSubscribeVideo = true;
        options.autoSubscribeAudio = true;
        options.publishScreenCaptureVideo = false;
        options.publishCameraTrack = false;
        options.publishMicrophoneTrack = false;
        options.enableAudioRecordingOrPlayout = true;

        String channelId = "testChannel";
        TokenUtils.gen(this, channelId, 0, ret -> {
            /* Allows a user to join a channel.
             if you do not specify the uid, we will generate the uid for you*/
            engine.joinChannel(ret, channelId, 0, options);
        });

        mediaPlayer.open(SAMPLE_MOVIE_URL, 0);

        mediaPlayer.play();
    }
}