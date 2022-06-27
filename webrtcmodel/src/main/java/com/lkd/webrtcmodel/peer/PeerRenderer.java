package com.lkd.webrtcmodel.peer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PeerRenderer {
    /**
     * peer呈现
     * @param peerList 关联的peers
     */
    public void renderer(List<Peer> peerList);
    /**
     * 清空远端摄像头
     */
    public void clear();
}
