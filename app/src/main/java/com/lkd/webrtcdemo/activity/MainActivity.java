package com.lkd.webrtcdemo.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lkd.webrtcdemo.R;
import com.lkd.webrtcdemo.ui.PeerAdapter;
import com.lkd.webrtcdemo.utils.PermissionUtil;
import com.lkd.webrtcmodel.WebRTCServer;
import com.lkd.webrtcmodel.constant.WebRTC;
import com.lkd.webrtcmodel.peer.Peer;
import com.yanzhenjie.permission.Permission;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //控件
    private EditText roomName;
    private Button openCamera;
    private Button switchCamera;
    private Button createRoom;
    private Button exitRoom;
    private Button startRecord;
    private Button stopRecord;
    private Button saveLocal;
    private Button saveCould;
    private SurfaceViewRenderer localSurfaceViewRenderer;
    private LinearLayout remoteVideoLl;
    private HashMap<String,View> remoteViews;
    private ListView peerListView;
    /**
     * 记录用户首次点击返回键的时间
     */
    private long firstTime = 0;
    /**
     * 录屏对象peerID
     */
    private String curRecordPeerId = "";
    /**
     * 录屏开始时间
     */
    private long startTime = -1;
    /**
     * 录屏结束时间
     */
    private long stopTime = -1;

    /**
     * Peer列表适配器
     */
    private PeerAdapter peerAdapter;
    /**
     * webRTC服务
     */
    private WebRTCServer webRTCServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        roomName =  findViewById(R.id.room);
        openCamera = findViewById(R.id.openCamera);
        openCamera.setOnClickListener(this);
        switchCamera =  findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(this);
        createRoom = findViewById(R.id.create);
        createRoom.setOnClickListener(this);
        exitRoom = findViewById(R.id.exit);
        exitRoom.setOnClickListener(this);
        startRecord = findViewById(R.id.startRecord);
        startRecord.setOnClickListener(this);
        stopRecord = findViewById(R.id.stopRecord);
        stopRecord.setEnabled(false);
        stopRecord.setOnClickListener(this);
        saveLocal = findViewById(R.id.saveLocal);
        saveLocal.setOnClickListener(this);
        saveLocal.setEnabled(false);
        saveCould = findViewById(R.id.saveCould);
        saveCould.setOnClickListener(this);
        localSurfaceViewRenderer = findViewById(R.id.localVideo);
        remoteVideoLl = findViewById(R.id.remoteVideoLl);
        remoteViews = new HashMap<>();
        createWebRTCServer();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //某些机型锁屏点亮后需要重新开启摄像头
        webRTCServer.restartCameraOnView(localSurfaceViewRenderer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //数据销毁
        localSurfaceViewRenderer.release();
        localSurfaceViewRenderer = null;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                long secondTime=System.currentTimeMillis();
                if(secondTime-firstTime>2000){
                    Toast.makeText(MainActivity.this,"再按一次退出程序",Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;
                    return true;
                }else{
                    System.exit(0);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openCamera:
                if (!PermissionUtil.chick(this,Permission.Group.CAMERA)){
                    break;
                }
                if (!PermissionUtil.chick(this,Permission.Group.MICROPHONE)){
                    break;
                }
                //开启_关闭摄像头
                if(webRTCServer.isCameraOpen){
                    //关闭
                    webRTCServer.closeCameraOnView(localSurfaceViewRenderer);
                    openCamera.setText("开启摄像头");
                }else{
                    webRTCServer.startCameraOnView(localSurfaceViewRenderer);
                    openCamera.setText("关闭摄像头");
                }
                break;
            case R.id.switchCamera:
                //切换摄像头
                webRTCServer.switchCamera();
                break;
            case R.id.create:
                //创建并加入聊天室
                String roomId = roomName.getText().toString();
                if(webRTCServer.isCameraOpen){
                    webRTCServer.startRemoteServer(roomId);
                    createRoom.setEnabled(false);
                }else{
                    Toast.makeText(this,"请先开启摄像头",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.exit:
                //退出聊天室
                webRTCServer.stopRemoteServer();
                createRoom.setEnabled(true);
                break;
            case R.id.startRecord:
                if (!webRTCServer.isCameraOpen){
                    Toast.makeText(this,"请先开启摄像头",Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!PermissionUtil.chick(this,Permission.Group.STORAGE)){
                    break;
                }
                webRTCServer.startRecord(curRecordPeerId);
                startTime = System.currentTimeMillis();
                startRecord.setEnabled(false);
                stopRecord.setEnabled(true);
                Toast.makeText(this, "开始录制屏幕", Toast.LENGTH_SHORT).show();
                break;
            case R.id.stopRecord:
                webRTCServer.closeRecord();
                stopTime = System.currentTimeMillis();
                stopRecord.setEnabled(false);
                saveLocal.setEnabled(true);
                Toast.makeText(this,"停止录制屏幕",Toast.LENGTH_SHORT).show();
                break;
            case R.id.saveLocal:
                startRecord.setEnabled(true);
                stopRecord.setEnabled(true);
                saveLocal.setEnabled(false);
                if (stopTime - startTime < 1000){
                    Toast.makeText(this, "录制时间太短", Toast.LENGTH_SHORT).show();
                    break;
                }
                webRTCServer.saveRecord();
                startTime = -1;
                stopTime = -1;
                Toast.makeText(MainActivity.this, "文件保存在"+webRTCServer.filePath, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    /**
     * 创建WebRtc服务
     */
    private void createWebRTCServer(){
        webRTCServer = new WebRTCServer(this) {
            @Override
            public void renderer(List<Peer> peerList) {
                peerListView = findViewById(R.id.list_view);
                peerAdapter = new PeerAdapter(MainActivity.this,R.layout.peer_item,peerList);
                peerListView.setAdapter(peerAdapter);
                peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (view instanceof LinearLayout) {
                            LinearLayout layout = (LinearLayout) view;
                            View view1 = layout.getChildAt(1);
                            TextView textView = (TextView) view1;
                            Toast.makeText(MainActivity.this, textView.getText(), Toast.LENGTH_SHORT).show();
                            curRecordPeerId = textView.getText().toString();
                        }
                    }
                });
            }

            @Override
            public void clear() {
                remoteVideoLl.removeAllViews();
            }

            @Override
            public void atPeerJoin() {
                updateList();
            }

            @Override
            public void atPeerLeave() {
                updateList();
            }

            @Override
            public void onAddRemoteStream(String peerId, VideoTrack videoTrack) {
                webRTCServer.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ////UI线程执行
                        //构建远端view
                        SurfaceViewRenderer remoteView = new SurfaceViewRenderer(MainActivity.this);
                        webRTCServer.initSurfaceView(remoteView);
                        //控件布局
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,500);
                        layoutParams.topMargin = 20;
                        layoutParams.setMargins(4,4,4,4);
                        remoteVideoLl.addView(remoteView,layoutParams);
                        //添加至hashmap中
                        remoteViews.put(peerId,remoteView);
                        //添加数据
                        //VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                        videoTrack.addSink(remoteView);
                    }
                });
            }

            @Override
            public void onRemoveRemoteStream(String peerId) {
                webRTCServer.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ////UI线程执行
                        //移除远端view
                        SurfaceViewRenderer remoteView = (SurfaceViewRenderer)remoteViews.get(peerId);
                        if (remoteView != null){
                            remoteVideoLl.removeView(remoteView);
                            remoteViews.remove(peerId);
                            //数据销毁
                            remoteView.release();
                            remoteView = null;
                        }
                    }
                });
            }
        };
    }
    /**
     * 通知UI刷新列表
     */
    private void updateList(){
        peerAdapter.notifyDataSetChanged();
    }

}
