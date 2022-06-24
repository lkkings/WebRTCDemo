package com.lkd.webrtcdemo.activity;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
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

import com.google.android.material.navigation.NavigationBarView;
import com.lkd.webrtcdemo.R;
import com.lkd.webrtcdemo.ui.PeerAdapter;
import com.lkd.webrtcdemo.utils.PermissionUtil;
import com.lkd.webrtcdemo.webrtcmodule.Peer;
import com.lkd.webrtcdemo.webrtcmodule.PeerConnectionParameters;
import com.lkd.webrtcdemo.webrtcmodule.RtcListener;
import com.lkd.webrtcdemo.webrtcmodule.WebRtcClient;
import com.lkd.webrtcdemo.webrtcmodule.constant.WebRTC;
import com.yanzhenjie.permission.Permission;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements RtcListener,View.OnClickListener, AdapterView.OnItemClickListener {
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
     * 提供EGL的渲染上下文及EGL的版本兼容
     */
    private EglBase rootEglBase;
    /**
     * WebRtc客户端
     */
    private WebRtcClient webRtcClient;
    /**
     * 对等连接参数设置
     */
    private PeerConnectionParameters peerConnectionParameters;
    /**
     * 记录用户首次点击返回键的时间
     */
    private long firstTime = 0;
    /**
     * 摄像头是否开启
     */
    private boolean isCameraOpen = false;
    /**
     * 是否处于录屏状态
     */
    private boolean isRecord = false;
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
        //创建WebRtcClient
        createWebRtcClient();
        peerListView = findViewById(R.id.list_view);
        peerAdapter = new PeerAdapter(MainActivity.this,R.layout.peer_item,webRtcClient.getPeerList());
        peerListView.setAdapter(peerAdapter);
        peerListView.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //某些机型锁屏点亮后需要重新开启摄像头
        if (isCameraOpen){
            webRtcClient.startCamera(localSurfaceViewRenderer, WebRTC.FONT_FACTING);
        }
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
                if(isCameraOpen){
                    //关闭
                    webRtcClient.closeCamera();
                    //数据
                    localSurfaceViewRenderer.clearImage();
                    localSurfaceViewRenderer.setBackground(new ColorDrawable(getResources().getColor(R.color.purple_200)));
                    //localSurfaceViewRenderer.setForeground(new ColorDrawable(R.color.colorBlack));
                    localSurfaceViewRenderer.release();
                    isCameraOpen = false;
                    openCamera.setText("开启摄像头");
                }else{
                    startCamera();
                }
                break;
            case R.id.switchCamera:
                //切换摄像头
                switchCamera();
                break;
            case R.id.create:
                //创建并加入聊天室
                String roomId = roomName.getText().toString();
                if(isCameraOpen){
                    webRtcClient.createAndJoinRoom(roomId);
                    createRoom.setEnabled(false);
                }else{
                    Toast.makeText(this,"请先开启摄像头",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.exit:
                //退出聊天室
                webRtcClient.exitRoom();
                createRoom.setEnabled(true);
                break;
            case R.id.startRecord:
                try {
                    if (!isCameraOpen){
                        Toast.makeText(this,"请先开启摄像头",Toast.LENGTH_SHORT).show();
                        break;
                    }
                    if (!PermissionUtil.chick(this,Permission.Group.STORAGE)){
                        break;
                    }
                    webRtcClient.startRecord();
                    startTime = System.currentTimeMillis();
                    isRecord = true;
                    startRecord.setEnabled(false);
                    stopRecord.setEnabled(true);
                    Toast.makeText(this, "开始录制屏幕", Toast.LENGTH_SHORT).show();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stopRecord:
                webRtcClient.stopRecord();
                stopTime = System.currentTimeMillis();
                isRecord = false;
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
                webRtcClient.saveLocal();
                startTime = -1;
                stopTime = -1;
                Toast.makeText(MainActivity.this, "文件保存在"+webRtcClient.getFilePath(), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    /**
     * 创建对等连接配置参数
     */
    private void createPeerConnectionParameters(){
        //获取webRtc 音视频配置参数
        Point displaySize = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.set(480,320);
        peerConnectionParameters =  new PeerConnectionParameters(true, false,
                    false, displaySize.x, displaySize.y, 30,
                    0, "VP8",
                    true,false,0,"OPUS",
                    false,false,false,false,false,false,
                    false,false,false,false);
    }

    /**
     * 创建WebRtc客户端
     */
    private void createWebRtcClient(){
        //配置参数
        createPeerConnectionParameters();
        //创建视频渲染器
        rootEglBase = EglBase.create();
        //WebRtcClient对象
        webRtcClient = new WebRtcClient(getApplicationContext(),
                rootEglBase,
                peerConnectionParameters,
                MainActivity.this);
    }

    /**
     * 开启摄像头
     */
    private void startCamera(){
        //初始化渲染源
        localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
        //填充模式
        localSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localSurfaceViewRenderer.setZOrderMediaOverlay(true);
        localSurfaceViewRenderer.setEnableHardwareScaler(false);
        localSurfaceViewRenderer.setMirror(true);
        localSurfaceViewRenderer.setBackground(null);
        //启动摄像头
        webRtcClient.startCamera(localSurfaceViewRenderer,WebRTC.FONT_FACTING);
        //状态设置
        isCameraOpen = true;
        openCamera.setText("关闭摄像头");
    }

    /**
     * 切换摄像头
     */
    private void switchCamera(){
        if (webRtcClient != null){
            webRtcClient.switchCamera();
        }
    }
    /**
     * 上传到云端
     */
    private void saveCould(){

    }
    /**
     * 清空远端摄像头
     */
    public void clearRemoteCamera(){
        remoteVideoLl.removeAllViews();
    }

    /**
     * 通知UI刷新列表
     */
    public void updateList(){
        peerAdapter.notifyDataSetChanged();
    }

    // RtcListener 数据回调
    @Override
    public void onAddRemoteStream(String peerId,VideoTrack videoTrack) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ////UI线程执行
                //构建远端view
                SurfaceViewRenderer remoteView = new SurfaceViewRenderer(MainActivity.this);
                //初始化渲染源
                remoteView.init(rootEglBase.getEglBaseContext(), null);
                //填充模式
                remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                remoteView.setZOrderMediaOverlay(true);
                remoteView.setEnableHardwareScaler(false);
                remoteView.setMirror(true);
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
        this.runOnUiThread(new Runnable() {
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            View view1 = layout.getChildAt(1);
            TextView textView = (TextView) view1;
            Toast.makeText(MainActivity.this, textView.getText(), Toast.LENGTH_SHORT).show();
            webRtcClient.setRemotePeerId(textView.getText().toString());
        }
    }
}
