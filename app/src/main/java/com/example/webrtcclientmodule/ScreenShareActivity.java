package com.example.webrtcclientmodule;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

public class ScreenShareActivity extends AppCompatActivity {

    SurfaceViewRenderer mSurfaceViewRenderer;
    private EglBase.Context eglBaseContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_share);
        mSurfaceViewRenderer=findViewById(R.id.remoteView);
        eglBaseContext= EglBase.create().getEglBaseContext();
        mSurfaceViewRenderer.init(eglBaseContext, null);
        mSurfaceViewRenderer.setEnableHardwareScaler(true);

        Intent intent=getIntent();
        VideoTrack mVideoTrack=intent.getParcelableExtra("videoTrack");
        mVideoTrack.setEnabled(true);
        mVideoTrack.addSink(mSurfaceViewRenderer);

    }
}