package com.lkd.webrtcmodel;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;


import com.lkd.webrtcmodel.constant.WebRTC;
import com.lkd.webrtcmodel.peer.PeerConnectionParameters;
import com.lkd.webrtcmodel.peer.PeerListener;
import com.lkd.webrtcmodel.peer.PeerRenderer;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.io.IOException;

public abstract class WebRTCServer implements RtcListener, PeerListener, PeerRenderer {
    /**
     * 日志
     */
    private final static String TAG = WebRTCServer.class.getCanonicalName();
    /**
     * 摄像头状态
     */
    public boolean isCameraOpen = false;
    /**
     * 录屏状态
     */
    public boolean isRecord = false;
    /**
     * 提供EGL的渲染上下文及EGL的版本兼容
     */
    private EglBase eglBase;

    /**
     * 创建webrtc客户端
     */
    private WebRtcClient webRtcClient;

    /**
     * 对等连接参数
     */
    private PeerConnectionParameters peerConnectionParameters;

    /**
     * webrtc活动
     */
    public Activity activity;

    /**
     * 文件保存路径
     */
    public String filePath;

    /**
     * 是否处于连接状态
     */
    public boolean isLine;

    public WebRTCServer(@NonNull Activity activity){
        this.eglBase = EglBase.create();
        this.activity = activity;
        init();
    }

    private void init() {
        //配置参数
        peerConnectionParameters = createPeerConnectionParameters();
        //创建webrtc客户端
        webRtcClient = createWebRtcClient(peerConnectionParameters,activity,eglBase);
    }
    /**
     * 创建对等连接配置参数
     */
    private PeerConnectionParameters createPeerConnectionParameters(){
        //获取webRtc 音视频配置参数
        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.set(480,320);
        return new PeerConnectionParameters(true, false,
                false, displaySize.x, displaySize.y, 30,
                0, "VP8",
                true, false, 0, "OPUS",
                false, false, false, false, false, false,
                false, false, false, false);
    }
    /**
     * 创建WebRtc客户端
     */
    private WebRtcClient createWebRtcClient(PeerConnectionParameters peerConnectionParameters,Activity activity,EglBase eglBase){
        //WebRtcClient对象
        return new WebRtcClient(activity,
                eglBase,
                peerConnectionParameters,
                this,this,this);
    }

    /**
     * 开启远程服务
     * @param roomId 开启服务的房间id
     */
    public void startRemoteServer(String roomId){
        if (isLine)
            return;
        webRtcClient.createAndJoinRoom(roomId);
        isLine = true;
    }
    /**
     * 关闭远程服务
     */
    public void stopRemoteServer(){
        if (!isLine)
            return;
        webRtcClient.exitRoom();
        isLine = false;
    }

    /**
     * 通过选定摄像头收集音视频呈现在本地窗口上
     * @param localSurfaceView 本地视频窗口
     */
    public void startCameraOnView(SurfaceViewRenderer localSurfaceView){
        if (isCameraOpen)
            return;
        initSurfaceView(localSurfaceView);
        //启动摄像头
        webRtcClient.startByCamera(localSurfaceView,WebRTC.FONT_FACTING);
        //状态设置
        isCameraOpen = true;
    }

    /**
     * 因为某些事情意外关闭时重启摄像头
     */
    public void restartCameraOnView(SurfaceViewRenderer localSurfaceView){
        if (isCameraOpen)
            webRtcClient.startByCamera(localSurfaceView,WebRTC.FONT_FACTING);
    }
    public void closeCameraOnView(SurfaceViewRenderer localSurfaceView){
        if (!isCameraOpen)
            return;
        //关闭
        webRtcClient.closeCamera();
        //数据
        localSurfaceView.clearImage();
        //localSurfaceViewRenderer.setForeground(new ColorDrawable(R.color.colorBlack));
        localSurfaceView.release();
        isCameraOpen = false;
    }

    public void switchCamera(){
        if (webRtcClient != null){
            webRtcClient.switchCamera();
        }
    }

    public void startRecord(String peerId){
        if (isRecord)
            return;
        try {
            webRtcClient.startRecord(peerId);
            isRecord = true;
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
    }

    public void closeRecord(){
        if (!isRecord)
            return;
        webRtcClient.stopRecord();
        isRecord = false;
    }
    public String getDirPath(){
        return webRtcClient.getDirPath();
    }

    public void saveRecord(){
        if (isRecord)
            return;
        filePath = webRtcClient.getFilePath();
        webRtcClient.saveLocal();
    }

    public void initSurfaceView( SurfaceViewRenderer surfaceViewRenderer){
        //初始化渲染源
        surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
        //填充模式
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        surfaceViewRenderer.setZOrderMediaOverlay(true);
        surfaceViewRenderer.setEnableHardwareScaler(false);
        surfaceViewRenderer.setMirror(true);
        surfaceViewRenderer.setBackground(null);
    }

    public void sendCommand(String peerId,String command){
        if (!isLine)
            return;
        webRtcClient.executeCommand(peerId,command);
    }

    public void sendExecResult(String to,String exec, JSONObject data){
        if (!isLine)
            return;
        try {
            JSONObject result = new JSONObject();
            result.put("exec",exec);
            result.put("data",data);
            webRtcClient.executeResult(to,result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
