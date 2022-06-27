package com.lkd.robot;

import static java.lang.String.join;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.widget.TextView;

import com.lkd.permission.UsesPermission;
import com.lkd.permission.permissions.Permission;
import com.lkd.robot.utils.CommonUtil;
import com.lkd.robot.webrtcmodule.PeerConnectionParameters;
import com.lkd.robot.webrtcmodule.RtcListener;
import com.lkd.robot.webrtcmodule.WebRtcClient;

import org.webrtc.EglBase;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements RtcListener {


    private PeerConnectionParameters peerConnectionParameters;

    private EglBase rootEglBase;

    private WebRtcClient webRtcClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //请求权限
        requestPermission();
        //配置参数
        createPeerConnectionParameters();
        //创建webrtc客户端
        createWebRtcClient();
    }
    /**
     * 创建WebRtc客户端
     */
    private void createWebRtcClient(){
        //创建视频渲染器
        rootEglBase = EglBase.create();
        //WebRtcClient对象
        webRtcClient = new WebRtcClient(getApplicationContext(),
                rootEglBase,
                peerConnectionParameters,
                MainActivity.this);
    }
    /**
     * 创建对等连接配置参数
     */
    private void createPeerConnectionParameters(){
        //获取webRtc 音视频配置参数
        Point displaySize = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.set(480,320);
        peerConnectionParameters = new PeerConnectionParameters(true, false,
                false, displaySize.x, displaySize.y, 30,
                0, "VP8",
                true, false, 0, "OPUS",
                false, false, false, false, false, false,
                false, false, false, false);

    }

    @Override
    public void onAddRemoteStream(String peerId, VideoTrack videoTrack) {

    }

    @Override
    public void onRemoveRemoteStream(String peerId) {

    }
    private void requestPermission() {
        new UsesPermission(this, this
                , Permission.CAMERA
                , Permission.RECORD_AUDIO
                ) {
            @Override
            protected void onFalse(@NonNull ArrayList<String> rejectFinalPermissions, @NonNull ArrayList<String> rejectPermissions, @NonNull ArrayList<String> invalidPermissions) {
                System.exit(0);
            }
        };
    }
}