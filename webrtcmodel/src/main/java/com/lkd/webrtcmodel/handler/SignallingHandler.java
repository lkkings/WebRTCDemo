package com.lkd.webrtcmodel.handler;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lkd.webrtcmodel.WebRtcClient;
import com.lkd.webrtcmodel.peer.Peer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.socket.emitter.Emitter;

public class SignallingHandler {
    private final static String TAG = SignallingHandler.class.getCanonicalName();

    private static WebRtcClient webRtcClient;

    public static void setWebRtcClient(WebRtcClient webRtcClient){
        SignallingHandler.webRtcClient = webRtcClient;
    }
    public static Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case 1:
                    webRtcClient.peerListener.atPeerJoin();
                    break;
                case 2:
                    webRtcClient.peerListener.atPeerLeave();
                    break;
                case 3:
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    try {
                        String from = jsonObject.getString("from");
                        String command = jsonObject.getString("command");
                        webRtcClient.peerListener.handleCommand(from,command);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case 4:
                    JSONObject jsonObject1 = (JSONObject) msg.obj;
                    try {
                        String exec = jsonObject1.getString("exec");
                        JSONObject data = jsonObject1.getJSONObject("data");
                        webRtcClient.peerListener.handleExecResult(exec,data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     *  构建webRtc连接并返回
     */
    private static Peer getOrCreateRtcConnect(String socketId) {
        Peer pc = webRtcClient.peers.get(socketId);
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = new Peer(socketId,webRtcClient.factory,webRtcClient.rtcConfig,webRtcClient);
            //设置本地数据流
            pc.getPc().addTrack(webRtcClient.localVideoTrack);
            pc.getPc().addTrack(webRtcClient.localAudioTrack);
            //保存peer连接
            webRtcClient.peers.put(socketId,pc);
            webRtcClient.peerList.add(pc);

            Message message = Message.obtain();
            message.what = 1;
            handler.sendMessage(message);
        }
        return pc;
    }
    // 信令服务器处理相关
    /**
     * created [id,room,peers]
     */
    public static Emitter.Listener createdListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "created:" + data);
            try {
                //设置socket id
                webRtcClient.setSocketId(data.getString("id"));
                //设置room id
                webRtcClient.setRoomId(data.getString("room"));
                //获取peer数据
                JSONArray peers = data.getJSONArray("peers");
                //根据回应peers 循环创建WebRtcPeerConnection，创建成功后发送offer消息 [from,to,room,sdp]
                for (int i = 0; i < peers.length(); i++) {
                    JSONObject otherPeer = peers.getJSONObject(i);
                    String otherSocketId = otherPeer.getString("id");
                    //创建WebRtcPeerConnection
                    Peer pc = getOrCreateRtcConnect(otherSocketId);
                    //设置offer
                    pc.getPc().createOffer(pc,webRtcClient.sdpMediaConstraints);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * joined [id,room]
     */
    public static Emitter.Listener joinedListener = new Emitter.Listener() {
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
    public static Emitter.Listener offerListener = new Emitter.Listener() {
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
                pc.getPc().createAnswer(pc,webRtcClient.sdpMediaConstraints);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * answer [from,to,room,sdp]
     */
    public static Emitter.Listener answerListener = new Emitter.Listener() {
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
    public static Emitter.Listener candidateListener = new Emitter.Listener() {
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
    public static Emitter.Listener exitListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "exit:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //判断是否为当前连接
                Peer pc = webRtcClient.peers.get(fromId);
                if (pc != null){
                    //peer关闭
                    getOrCreateRtcConnect(fromId).getPc().close();
                    //删除peer对象
                    webRtcClient.peers.remove(fromId);
                    webRtcClient.peerList.remove(pc);
                    //通知UI界面移除video
                    webRtcClient.rtcListener.onRemoveRemoteStream(fromId);
                    Message message = Message.obtain();
                    message.what = 2;
                    handler.sendMessage(message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * execute [from,to,command]
     */
    public static Emitter.Listener commandListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "command:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //获取当前命令
                String command = data.getString("message");
                //封装
                JSONObject command1 = new JSONObject();
                command1.put("from",fromId);
                command1.put("command",command);
                //判断是否为当前连接
                Peer pc = webRtcClient.peers.get(fromId);
                if (pc != null){
                    Message message = Message.obtain();
                    message.what = 3;
                    message.obj = command1;
                    handler.sendMessage(message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * execute [from,to,result]
     */
    public static Emitter.Listener executeListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "command:" + data);
            try {
                //获取id
                String fromId = data.getString("from");
                //获取执行结果
                JSONObject result = data.getJSONObject("message");
                //判断是否为当前连接
                Peer pc = webRtcClient.peers.get(fromId);
                if (pc != null){
                    Message message = Message.obtain();
                    message.what = 4;
                    message.obj = result;
                    handler.sendMessage(message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
}
