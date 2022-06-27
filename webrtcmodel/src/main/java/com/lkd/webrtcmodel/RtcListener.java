package com.lkd.webrtcmodel;

import org.webrtc.VideoTrack;

/**
 * UI页面事件监听
 */

public interface RtcListener {

    //远程音视频流加入 Peer通道
    void onAddRemoteStream(String peerId,VideoTrack videoTrack);

    //远程音视频流移除 Peer通道销毁
    void onRemoveRemoteStream(String peerId);
}
