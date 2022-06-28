package com.lkd.webrtcmodel.peer;

import org.json.JSONObject;

public interface PeerListener {
    public void atPeerJoin();
    public void atPeerLeave();
    public void handleCommand(String from,String command);
    public void handleExecResult(String exec,JSONObject data);
}
