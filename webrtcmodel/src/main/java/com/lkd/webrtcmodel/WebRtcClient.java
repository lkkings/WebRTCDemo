package com.lkd.webrtcmodel;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lkd.webrtcmodel.constant.ICEServer;
import com.lkd.webrtcmodel.constant.SocketIOServer;
import com.lkd.webrtcmodel.constant.WebRTC;
import com.lkd.webrtcmodel.handler.SignallingHandler;
import com.lkd.webrtcmodel.peer.Peer;
import com.lkd.webrtcmodel.peer.PeerConnectionParameters;
import com.lkd.webrtcmodel.peer.PeerListener;
import com.lkd.webrtcmodel.peer.PeerRenderer;
import com.lkd.webrtcmodel.recorder.VideoFileRenderer;
import com.lkd.webrtcmodel.ssl.TrustAllCerts;

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
import org.webrtc.FileVideoCapturer;
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
    public PeerConnectionFactory factory;
    /**
     * Peer集合，socketID为键，Peer为值
     */
    public HashMap<String, Peer> peers = new HashMap<>();
    /**
     * Peer列表
     */
    public List<Peer> peerList = new ArrayList<>();
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
    public PeerConnection.RTCConfiguration rtcConfig;
    /**
     * PeerConnect 音频约束
     */
    private MediaConstraints audioConstraints;
    /**
     * PeerConnect sdp约束
     */
    public MediaConstraints sdpMediaConstraints;
    /**
     * 本地Video视频资源
     */
    private VideoSource localVideoSource;
    /**
     * 视频Track
     */
    public VideoTrack localVideoTrack;
    /**
     * 本地Audio音频资源
     */
    private AudioSource localAudioSource;
    /**
     * 音频Track
     */
    public AudioTrack localAudioTrack;
    /**
     * 本地摄像头视频捕获
     */
    private CameraVideoCapturer cameraVideoCapturer;
    /**
     * 视频文件视频捕获
     */
    private FileVideoCapturer fileVideoCapturer;
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
    public RtcListener rtcListener;
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
     * 录屏保文件夹
     */
    private String dirPath ;
    /**
     *当前录制对象的PeerId
     */
    private String curRecordPeerId;
    /**
     * 当前活动
     */
    private Activity activity;
    /**
     *peer监听
     */
    public PeerListener peerListener;
    /**
     * 呈现接口
     */
    private PeerRenderer peerRenderer;



    public WebRtcClient(Activity activity,
                        EglBase eglBase,
                        PeerConnectionParameters peerConnectionParameters,
                        RtcListener listener, PeerListener peerListener, PeerRenderer peerRenderer) {
        //参数设置
        this.appContext = activity.getApplicationContext();
        this.activity = activity;
        this.eglBase = eglBase;
        this.pcParams = peerConnectionParameters;
        this.rtcListener = listener;
        this.peerListener = peerListener;
        this.peerRenderer = peerRenderer;
        this.dirPath = appContext.getExternalFilesDir(null).getAbsolutePath();
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
        peerRenderer.renderer(peerList);
    }

    public RtcListener getRtcListener() {
        return rtcListener;
    }

    public String getDirPath() {
        return dirPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSocketId() {
        return socketId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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
            //设置监听
            SignallingHandler.setWebRtcClient(this);
            ////设置消息监听
            //created [id,room,peers]
            client.on("created", SignallingHandler.createdListener);
            //joined [id,room]
            client.on("joined", SignallingHandler.joinedListener);
            //offer [from,to,room,sdp]
            client.on("offer", SignallingHandler.offerListener);
            //answer [from,to,room,sdp]
            client.on("answer", SignallingHandler.answerListener);
            //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
            client.on("candidate", SignallingHandler.candidateListener);
            //exit [from,room]
            client.on("exit", SignallingHandler.exitListener);
            //command [from,to,command]
            client.on("command",SignallingHandler.commandListener);
            //executed
            client.on("executed",SignallingHandler.executeListener);
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
     * 发送命令
     */
    public void executeCommand(String peerId,String command){
        curRecordPeerId = peerId==null ? socketId: peerId;
        try {
            JSONObject message = new JSONObject();
            message.put("from",socketId);
            message.put("to",curRecordPeerId);
            message.put("message",command);
            sendMessage("command",message);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    /**
     * 对命令执行结果进行反馈
     */
    public void executeResult(String to,JSONObject result){
        try {
            JSONObject message = new JSONObject();
            message.put("from",socketId);
            message.put("to",to);
            message.put("message",result);
            sendMessage("executed",message);
            System.out.println("===========================================");
        } catch (JSONException e){
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
        //通知UI清空远端摄像头
        peerRenderer.clear();
    }
    //WebRtc相关

    /**
     * 启动摄像头进行视频采集关联video
     */
    public void startByCamera(VideoSink localRender,int type){
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
     * 打开文件进行视频采集
     */
    public void startByFile(String filePath,VideoSink videoSink) throws IOException {
        if (pcParams.videoCallEnabled){
            if (fileVideoCapturer == null){
                fileVideoCapturer = new FileVideoCapturer(filePath);

                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper
                        .create("CaptureThread", eglBase.getEglBaseContext());


                localVideoSource = factory.createVideoSource(false);
                localAudioSource = factory.createAudioSource(audioConstraints);

                fileVideoCapturer.initialize(surfaceTextureHelper, appContext, localVideoSource.getCapturerObserver());
                fileVideoCapturer.startCapture(pcParams.videoWidth,pcParams.videoHeight,pcParams.videoFps);

                localVideoTrack = factory.createVideoTrack(WebRTC.VIDEO_TRACK_ID, localVideoSource);
                localVideoTrack.setEnabled(true);
                localVideoTrack.addSink(videoSink);

                localAudioTrack = factory.createAudioTrack(WebRTC.AUDIO_TRACK_ID,localAudioSource);
                localAudioTrack.setEnabled(true);
            }
            else {
                fileVideoCapturer.startCapture(pcParams.videoWidth,pcParams.videoHeight,pcParams.videoFps);
            }
        }

    }
    /**
     * 开始录制视频
     */
    public void startRecord(String peerId) throws IOException {
        filePath =  dirPath + File.separator + System.currentTimeMillis() + ".mp4";
        videoFileRenderer = new VideoFileRenderer(filePath, eglBase.getEglBaseContext(), true);
        curRecordPeerId = peerId==null ? socketId: peerId;
        Peer pc = peers.get(peerId);
        if (pc == null) {
            localVideoTrack.addSink(videoFileRenderer);
            return;
        }
        pc.getVideoTrack().addSink(videoFileRenderer);
    }
    /**
     * 停止录制视频
     */
    public void stopRecord(){
        if (videoFileRenderer != null) {
            Peer pc = peers.get(curRecordPeerId);
            if (pc == null) {
                localVideoTrack.removeSink(videoFileRenderer);
                return;
            }
            pc.getVideoTrack().removeSink(videoFileRenderer);
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
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(WebRTC.AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
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
