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
import com.lkd.webrtcdemo.entity.File;

import java.util.List;

public class FileAdapter extends ArrayAdapter<File>{
    public FileAdapter (@NonNull Context context, int resource, @NonNull List<File> objects) {
        super(context, resource, objects);
    }
    //每个子项被滚动到屏幕内的时候会被调用
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        File file=getItem(position);//得到当前项的 Peer 实例
        //为每一个子项加载设定的布局
        View view= LayoutInflater.from(getContext()).inflate(R.layout.file_item,parent,false);
        TextView fileName =view.findViewById(R.id.fileName);
        TextView size =view.findViewById(R.id.size);
        TextView createTime =view.findViewById(R.id.createTime);
        fileName.setText(file.getFileName());
        size.setText(file.getSize());
        createTime.setText(file.getCreateTime());
        return view;
    }
}
