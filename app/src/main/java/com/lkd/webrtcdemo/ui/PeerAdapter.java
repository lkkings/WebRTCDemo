package com.lkd.webrtcdemo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lkd.webrtcdemo.R;
import com.lkd.webrtcmodel.peer.Peer;

import java.util.List;

public class PeerAdapter extends ArrayAdapter<Peer> {
    public PeerAdapter(@NonNull Context context, int resource, @NonNull List<Peer> objects) {
        super(context, resource, objects);
    }
    //每个子项被滚动到屏幕内的时候会被调用
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Peer Peer=getItem(position);//得到当前项的 Peer 实例
        //为每一个子项加载设定的布局
        View view= LayoutInflater.from(getContext()).inflate(R.layout.peer_item,parent,false);
        //分别获取 image view 和 textview 的实例
        ImageView peerImage =view.findViewById(R.id.peerImage);
        TextView peerId =view.findViewById(R.id.peerId);
        // 设置要显示的图片和文字
        peerImage.setImageResource(R.drawable.peer);
        peerId.setText(Peer.getId());
        return view;
    }

}
