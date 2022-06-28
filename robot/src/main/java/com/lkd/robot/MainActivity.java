package com.lkd.robot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;

import com.lkd.permission.UsesPermission;
import com.lkd.permission.permissions.Permission;
import com.lkd.webrtcmodel.WebRTCServer;
import com.lkd.webrtcmodel.peer.Peer;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{


    private EglBase rootEglBase;

    private WebRTCServer webRTCServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //请求权限
        requestPermission();
    }
    /**
     * 创建WebRtc服务
     */
    private void createWebRTCServer(){
        webRTCServer = new WebRTCServer(this) {
            @Override
            public void renderer(List<Peer> peerList) {

            }

            @Override
            public void clear() {

            }

            @Override
            public void atPeerJoin() {

            }

            @Override
            public void atPeerLeave() {

            }

            @Override
            public void handleCommand(String from, String command) {

            }

            @Override
            public void handleExecResult(String exec, JSONObject data) {

            }

            @Override
            public void onAddRemoteStream(String peerId, VideoTrack videoTrack) {

            }

            @Override
            public void onRemoveRemoteStream(String peerId) {

            }
        };
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