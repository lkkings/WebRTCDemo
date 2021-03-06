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
     *  ??????webRtc???????????????
     */
    private static Peer getOrCreateRtcConnect(String socketId) {
        Peer pc = webRtcClient.peers.get(socketId);
        if (pc == null) {
            //??????RTCPeerConnection PeerConnection??????????????????Peer???
            pc = new Peer(socketId,webRtcClient.factory,webRtcClient.rtcConfig,webRtcClient);
            //?????????????????????
            pc.getPc().addTrack(webRtcClient.localVideoTrack);
            pc.getPc().addTrack(webRtcClient.localAudioTrack);
            //??????peer??????
            webRtcClient.peers.put(socketId,pc);
            webRtcClient.peerList.add(pc);

            Message message = Message.obtain();
            message.what = 1;
            handler.sendMessage(message);
        }
        return pc;
    }
    // ???????????????????????????
    /**
     * created [id,room,peers]
     */
    public static Emitter.Listener createdListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "created:" + data);
            try {
                //??????socket id
                webRtcClient.setSocketId(data.getString("id"));
                //??????room id
                webRtcClient.setRoomId(data.getString("room"));
                //??????peer??????
                JSONArray peers = data.getJSONArray("peers");
                //????????????peers ????????????WebRtcPeerConnection????????????????????????offer?????? [from,to,room,sdp]
                for (int i = 0; i < peers.length(); i++) {
                    JSONObject otherPeer = peers.getJSONObject(i);
                    String otherSocketId = otherPeer.getString("id");
                    //??????WebRtcPeerConnection
                    Peer pc = getOrCreateRtcConnect(otherSocketId);
                    //??????offer
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
                //???????????????socketId
                String fromId = data.getString("id");
                //??????pcconnection
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
                //??????id
                String fromId = data.getString("from");
                //??????peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //??????RTCSessionDescription??????
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm("offer"),
                        data.getString("sdp")
                );
                //????????????setRemoteDescription
                pc.getPc().setRemoteDescription(pc,sdp);
                //??????answer
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
                //??????id
                String fromId = data.getString("from");
                //??????peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //??????RTCSessionDescription??????
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm("answer"),
                        data.getString("sdp")
                );
                //????????????setRemoteDescription
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
                //??????id
                String fromId = data.getString("from");
                //??????peer
                Peer pc = getOrCreateRtcConnect(fromId);
                //??????candidate
                JSONObject candidate = data.getJSONObject("candidate");
                IceCandidate iceCandidate = new IceCandidate(
                        candidate.getString("sdpMid"), //????????????id
                        candidate.getInt("sdpMLineIndex"),//????????????????????????
                        candidate.getString("sdp")//????????????
                );

                //??????????????????????????????
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
                //??????id
                String fromId = data.getString("from");
                //???????????????????????????
                Peer pc = webRtcClient.peers.get(fromId);
                if (pc != null){
                    //peer??????
                    getOrCreateRtcConnect(fromId).getPc().close();
                    //??????peer??????
                    webRtcClient.peers.remove(fromId);
                    webRtcClient.peerList.remove(pc);
                    //??????UI????????????video
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
                //??????id
                String fromId = data.getString("from");
                //??????????????????
                String command = data.getString("message");
                //??????
                JSONObject command1 = new JSONObject();
                command1.put("from",fromId);
                command1.put("command",command);
                //???????????????????????????
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
                //??????id
                String fromId = data.getString("from");
                //??????????????????
                JSONObject result = data.getJSONObject("message");
                //???????????????????????????
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
