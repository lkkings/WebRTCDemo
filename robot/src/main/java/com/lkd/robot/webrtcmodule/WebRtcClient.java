package com.lkd.robot.webrtcmodule;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.lkd.robot.webrtcmodule.constant.ICEServer;
import com.lkd.robot.webrtcmodule.constant.SocketIOServer;
import com.lkd.robot.webrtcmodule.constant.WebRTC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.LegacyAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

/**
 * WebRtcClient类 封装PeerConnectionFactory工厂类及Socket.IO信令服务器
 */
public class WebRtcClient {
    //Log Tag
    private final static String TAG = WebRtcClient.class.getCanonicalName();
    /**
     * PeerConnectionFactory工厂类
     */
    private PeerConnectionFactory factory;
    /**
     * Peer集合，socketID为键，Peer为值
     */
    private HashMap<String, Peer> peers = new HashMap<>();
    /**
     * Peer列表
     */
    private List<Peer> peerList = new ArrayList<>();
    /**
     * IceServer集合 用于构建PeerConnection
     */
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    /**
     * PeerConnectFactory构建参数
     */
    private PeerConnectionParameters pcParams;
    /**
     * PeerConnect构建参数
     */
    PeerConnection.RTCConfiguration rtcConfig;
    /**
     * PeerConnect 音频约束
     */
    private MediaConstraints audioConstraints;
    /**
     * PeerConnect sdp约束
     */
    private MediaConstraints sdpMediaConstraints;
    /**
     * 本地Video视频资源
     */
    private VideoSource localVideoSource;
    /**
     * 视频Track
     */
    private VideoTrack localVideoTrack;
    /**
     * 本地Audio音频资源
     */
    private AudioSource localAudioSource;
    /**
     * 音频Track
     */
    private AudioTrack localAudioTrack;
    /**
     * 本地摄像头视频捕获
     */
    private CameraVideoCapturer cameraVideoCapturer;
    /**
     * 页面context
     */
    private Context appContext;
    /**
     * WebRtc EglContext环境
     */
    private EglBase eglBase;
    /**
     * RtcListener回调接口
     */
    private RtcListener rtcListener;
    /**
     * socket.io信令交互
     */
    private Socket client;
    /**
     * 本地socket ID
     */
    private String socketId;
    /**
     * 房间 ID
     */
    private String  roomId;
    /**
     * 音视频录制
     */
    private VideoFileRenderer videoFileRenderer;
    /**
     * 录屏保存路径
     */
    private String filePath;

    /**
     *对方PeerId
     */
    private String remotePeerId;

    public WebRtcClient(Context appContext,
                        EglBase eglBase,
                        PeerConnectionParameters peerConnectionParameters,
                        RtcListener listener) {
        //参数设置
        this.appContext = appContext;
        this.eglBase = eglBase;
        this.pcParams = peerConnectionParameters;
        this.rtcListener = listener;
        //PeerConnectionFactory工厂类构建
        createPeerConnectionFactoryInternal();
        //创建iceServers
        createIceServers();
        //创建RTCConfiguration参数
        createRtcConfig();
        //创建Pc及Sdp约束
        createMediaConstraintsInternal();
        //Socket.IO信令服务器构建
        createSocket();
    }

    public RtcListener getRtcListener() {
        return rtcListener;
    }

    public void setRemotePeerId(String remotePeerId) {
        this.remotePeerId = remotePeerId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSocketId() {
        return socketId;
    }

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case 1:

                default:
                    break;
            }
        }
    };

    public List<Peer> getPeerList() {
        return peerList;
    }

    public String getRoomId() {
        return roomId;
    }
    /**
     * 创建IceServers参数
     */
    private void createIceServers() {
        PeerConnection.IceServer stunServer = PeerConnection.IceServer.builder(ICEServer.STUN_HOST).createIceServer();
        iceServers.add(stunServer);
        PeerConnection.IceServer turnServer = PeerConnection.IceServer.builder(ICEServer.TURN_HOST)
                .setUsername(ICEServer.TURN_USER)
                .setPassword(ICEServer.TURN_PASS)
                .createIceServer();
        iceServers.add(turnServer);
    }
    /**
     *  创建RTCConfiguration参数
     */
    private void createRtcConfig() {
        rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = !pcParams.loopback;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
    }
    /**
     * 创建PeerConnection工厂类
     */
    private void createPeerConnectionFactoryInternal() {
        //创建webRtc连接工厂类
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;
        final boolean enableH264HighProfile =
                "H264 High".equals(pcParams.videoCodec);
        //音频模式
        final AudioDeviceModule adm = pcParams.useLegacyAudioDevice
                ? createLegacyAudioDevice()
                : createJavaAudioDevice();
        //编解码模式【硬件加速，软编码】
        if (pcParams.videoCodecHwAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(
                    eglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
            decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }
        //PeerConnectionFactory.initialize
        String fieldTrials = "";
        if (pcParams.videoFlexfecEnabled) {
            fieldTrials += WebRTC.VIDEO_FLEXFEC_FIELDTRIAL;
        }
        fieldTrials += WebRTC.VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
        if (pcParams.disableWebRtcAGCAndHPF) {
            fieldTrials += WebRTC.DISABLE_WEBRTC_AGC_FIELDTRIAL;
        }
        //PeerConnectionFactory.initialize
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setFieldTrials(fieldTrials)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions());
        //构建PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

    }
    /**
     * 创建信令服务器及监听
     */
    private void createSocket() {
        //socket模式连接信令服务器
        try {
            //普通连接
            //client = IO.socket(host);
            //SSL加密连接
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .sslSocketFactory(getSSLSocketFactory(), new TrustAllCerts())
                    .build();
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);
            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            client = IO.socket(SocketIOServer.SOCKET_HOST, opts);

            ////设置消息监听
            //created [id,room,peers]
            client.on("created", createdListener);
            //joined [id,room]
            client.on("joined", joinedListener);
            //offer [from,to,room,sdp]
            client.on("offer", offerListener);
            //answer [from,to,room,sdp]
            client.on("answer", answerListener);
            //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
            client.on("candidate", candidateListener);
            //exit [from,room]
            client.on("exit", exitListener);
            //开始连接
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    // UI操作相关
    /**
     * 创建并加入
     */
    public void createAndJoinRoom(String roomId){
        //构建信令数据并发送
        try {
            JSONObject message = new JSONObject();
            message.put("room",roomId);
            //向信令服务器发送信令
            sendMessage("createAndJoinRoom",message);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    /**
     * 退出room
     */
    public void exitRoom(){
        //信令服务器发送 exit [from room]
        try {
            JSONObject message = new JSONObject();
            message.put("from",socketId);
            message.put("room",roomId);
            //向信令服务器发送信令
            sendMessage("exit",message);
        }catch (JSONException e){
            e.printStackTrace();
        }
        //循环遍历 peer关闭
        for(Peer pc: peers.values())
        {
            pc.getPc().close();
        }
        //数据重置
        socketId = "";
        roomId = "";
        peers.clear();
    }
    //WebRtc相关
    /**
     *  构建webRtc连接并返回
     */
    private Peer getOrCreateRtcConnect(String socketId) {
        Peer pc = peers.get(socketId);
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = new Peer(socketId,factory,rtcConfig,WebRtcClient.this);
            //设置本地数据流
            pc.getPc().addTrack(localVideoTrack);
            pc.getPc().addTrack(localAudioTrack);
            //保存peer连接
            peers.put(socketId,pc);
            peerList.add(pc);
            Message message = Message.obtain();
            message.what = 1;
            handler.sendMessage(message);
        }
        return pc;
    }

    /**
     * 启动摄像头进行视频采集关联video
     */
    public void startCamera(VideoSink localRender,int type){
        if(pcParams.videoCallEnabled){
            //创建VideoCapturer
            if (cameraVideoCapturer == null){
                String cameraname = "";
                Camera1Enumerator camera1Enumerator = new Camera1Enumerator();
                String[] deviceNames = camera1Enumerator.getDeviceNames();
                if (type == WebRTC.FONT_FACTING){
                    //前置摄像头
                    for (String deviceName : deviceNames){
                        if (camera1Enumerator.isFrontFacing(deviceName)){
                            cameraname = deviceName;
                        }
                    }
                }else {
                    //后置摄像头
                    for (String deviceName : deviceNames){
                        if (camera1Enumerator.isBackFacing(deviceName)){
                            cameraname = deviceName;
                        }
                    }
                }
                cameraVideoCapturer = camera1Enumerator.createCapturer(cameraname,null);
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper
                        .create("CaptureThread", eglBase.getEglBaseContext());

                localVideoSource = factory.createVideoSource(false);
                localAudioSource = factory.createAudioSource(audioConstraints);

                cameraVideoCapturer.initialize(surfaceTextureHelper, appContext, localVideoSource.getCapturerObserver());
                cameraVideoCapturer.startCapture(pcParams.videoWidth,pcParams.videoHeight,pcParams.videoFps);


                localVideoTrack = factory.createVideoTrack(WebRTC.VIDEO_TRACK_ID, localVideoSource);
                localVideoTrack.setEnabled(true);
                localVideoTrack.addSink(localRender);

                localAudioTrack = factory.createAudioTrack(WebRTC.AUDIO_TRACK_ID,localAudioSource);
                localAudioTrack.setEnabled(true);

            }else{
                cameraVideoCapturer.startCapture(pcParams.videoWidth,pcParams.videoHeight,pcParams.videoFps);
            }
        }
    }
    /**
     * 开始录制视频
     */
    public void startRecord() throws IOException {
        filePath = appContext.getExternalFilesDir(null).getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp4";
        videoFileRenderer = new VideoFileRenderer(filePath, eglBase.getEglBaseContext(), true);
        Peer pc = peers.get(remotePeerId);
        if (pc == null){
            localVideoTrack.addSink(videoFileRenderer);
        }
        else {
            pc.getVideoTrack().addSink(videoFileRenderer);
        }

    }
    /**
     * 停止录制视频
     */
    public void stopRecord(){
        if (videoFileRenderer != null) {
            localVideoTrack.removeSink(videoFileRenderer);
        }
    }
    /**
     * 保存在本地
     */
    public void saveLocal(){
        videoFileRenderer.release();
        videoFileRenderer = null;
    }
    /**
     * 切换摄像头
     */
    public void switchCamera(){
        if(cameraVideoCapturer != null){
            cameraVideoCapturer.switchCamera(null);
        }
    }
    /**
     * 关闭摄像头
     */
    public void closeCamera(){
        if(cameraVideoCapturer != null){
            try {
                cameraVideoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    // 信令服务器处理相关
    /**
     * created [id,room,peers]
     */
    private Emitter.Listener createdListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "created:" + data);
            try {
                //设置socket id
                socketId = data.getString("id");
                //设置room id
                roomId = data.getString("room");
                //获取peer数据
                JSONArray peers = data.getJSONArray("peers");
                //根据回应peers 循环创建WebRtcPeerConnection，创建成功后发送offer消息 [from,to,room,sdp]
                for (int i = 0; i < peers.length(); i++) {
                    JSONObject otherPeer = peers.getJSONObject(i);
                    String otherSocketId = otherPeer.getString("id");
                    //创建WebRtcPeerConnection
                    Peer pc = getOrCreateRtcConnect(otherSocketId);
                    //设置offer
                    pc.getPc().createOffer(pc,sdpMediaConstraints);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * joined [id,room]
     */
    private Emitter.Listener joinedListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "joined:" + data);
            try {
                //获取新加入socketId
                String fromId = data.getString("id");
                //构建pcconnection
                getOrCreateRtcConnect(fromId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * offer [from,to,room,sdp]
     */
    private Emitter.Listener offerListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "offer:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //获取peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //构建RTCSessionDescription参数
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm("offer"),
                        data.getString("sdp")
                );
                //设置远端setRemoteDescription
                pc.getPc().setRemoteDescription(pc,sdp);
                //设置answer
                pc.getPc().createAnswer(pc,sdpMediaConstraints);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * answer [from,to,room,sdp]
     */
    private Emitter.Listener answerListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "answer:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //获取peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //构建RTCSessionDescription参数
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm("answer"),
                        data.getString("sdp")
                );
                //设置远端setRemoteDescription
                pc.getPc().setRemoteDescription(pc,sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
     */
    private Emitter.Listener candidateListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "candidate:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //获取peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //获取candidate
                JSONObject candidate = data.getJSONObject("candidate");
                IceCandidate iceCandidate = new IceCandidate(
                        candidate.getString("sdpMid"), //描述协议id
                        candidate.getInt("sdpMLineIndex"),//描述协议的行索引
                        candidate.getString("sdp")//描述协议
                );

                //添加远端设备路由描述
                pc.getPc().addIceCandidate(iceCandidate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * exit [from,room]
     */
    private Emitter.Listener exitListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "exit:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //判断是否为当前连接
                Peer pc = peers.get(fromId);
                if (pc != null){
                    //peer关闭
                    getOrCreateRtcConnect(fromId).getPc().close();
                    //删除peer对象
                    peers.remove(fromId);
                    peerList.remove(pc);
                    Message message = Message.obtain();
                    message.what = 1;
                    handler.sendMessage(message);
                    //通知UI界面移除video
                    rtcListener.onRemoveRemoteStream(fromId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /** 信令服务器发送消息 **/
    public void sendMessage(String event,JSONObject message){
        client.emit(event, message);
    }

    // WebRtc 音视频相关辅助函数
    /**
     * 创建Media及Sdp约束
     */
    private void createMediaConstraintsInternal() {
        // 音频约束
        audioConstraints = new MediaConstraints();
        // added for audio performance measurements
        if (pcParams.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }

        //SDP约束 createOffer  createAnswer
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true" ));
        sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }
    /**
     * 创建PeerConnection
     */
    private void createPeerConnectionInternal() {

    }
    /**
     * 创建音频模式LegacyAudioDevice
     */
    private AudioDeviceModule createLegacyAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!pcParams.useOpenSLES) {
            Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
        } else {
            Log.d(TAG, "Allow OpenSL ES audio if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }
        if (pcParams.disableBuiltInAEC) {
            Log.d(TAG, "Disable built-in AEC even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        } else {
            Log.d(TAG, "Enable built-in AEC if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
        }
        if (pcParams.disableBuiltInNS) {
            Log.d(TAG, "Disable built-in NS even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        } else {
            Log.d(TAG, "Enable built-in NS if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        }
        //WebRtcAudioRecord.setOnAudioSamplesReady(saveRecordedAudioToFile);
        // Set audio record error callbacks.
        WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecord.WebRtcAudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    WebRtcAudioRecord.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
            }
        });

        WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.ErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    WebRtcAudioTrack.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
            }
        });

        return new LegacyAudioDeviceModule();
    }
    /**
     * 创建音频模式JavaAudioDevice
     */
    private AudioDeviceModule createJavaAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!pcParams.useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.");
        }
        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
            }
        };

        JavaAudioDeviceModule.SamplesReadyCallback samplesReadyCallback = new JavaAudioDeviceModule.SamplesReadyCallback() {
            @Override
            public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {
                if (videoFileRenderer != null) {
                    // 把音频数据传给VideoFileRenderer
                    videoFileRenderer.onWebRtcAudioRecordSamplesReady(audioSamples);
                }
            }
        };

        return JavaAudioDeviceModule.builder(appContext)
                .setSamplesReadyCallback(samplesReadyCallback)
                .setUseHardwareAcousticEchoCanceler(!pcParams.disableBuiltInAEC)
                .setUseHardwareNoiseSuppressor(!pcParams.disableBuiltInNS)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }

    /**
     * 返回SSLSocketFactory 用于ssl连接
     */
    private  SSLSocketFactory getSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());

            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssfFactory;
    }
}
