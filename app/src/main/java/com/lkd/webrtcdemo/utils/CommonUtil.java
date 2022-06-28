package com.lkd.webrtcdemo.utils;

import java.util.List;

public class CommonUtil {
    public static String join(String join,List<String> stringArrayList){
        StringBuffer sb=new StringBuffer();
        int len = stringArrayList.size();
        for(int i=0;i<len;i++){
            if(i==(len-1)){
                sb.append(stringArrayList.get(i));
            }else{
                sb.append(stringArrayList.get(i)).append(join);
            }
        }
        return sb.toString();
    }
    public static String join(List<String> stringArrayList){
        StringBuffer sb=new StringBuffer();
        int len = stringArrayList.size();
        for(int i=0;i<len;i++){
            if(i==(len-1)){
                sb.append(stringArrayList.get(i));
            }else{
                sb.append(stringArrayList.get(i)).append(",");
            }
        }
        return sb.toString();
    }
}
