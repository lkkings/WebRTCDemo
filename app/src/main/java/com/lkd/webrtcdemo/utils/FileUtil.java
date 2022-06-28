package com.lkd.webrtcdemo.utils;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FileUtil {
    private static final String mformatType = "yyyy/MM/dd HH:mm:ss";
    private static String getFileLastModifiedTime(File file) {
        Calendar cal = Calendar.getInstance();
        long time = file.lastModified();
        SimpleDateFormat formatter = new SimpleDateFormat(mformatType);
        cal.setTimeInMillis(time);
        return formatter.format(cal.getTime());
    }

    public static List<JSONObject> getFileOfCurPath(String path) throws JSONException {
        File filePath = new File(path);
        if (!filePath.exists()){
            System.out.println("该目录不存在");
            return null;
        }
        List<JSONObject> fileList = new ArrayList<>();
        File[] files = filePath.listFiles();
        for (File file: files){
            JSONObject fileJson = new JSONObject();
            fileJson.put("fileName",file.getName());
            fileJson.put("size",FileUtil.FormetFileSize(FileUtil.getFileSize(file)));
            fileJson.put("createTime",getFileLastModifiedTime(file));
            fileList.add(fileJson);
        }
        return fileList;
    }

    /**
     　　* 获取指定文件大小
     　　* @param f
     　　* @return
     　　* @throws Exception
     */
    private static long getFileSize(File file){
        long size = 0;
        if (file.exists()){
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return size;
    }
    /**
     　　* 转换文件大小
     　　* @param fileS
     　　* @return
     */
    private static String FormetFileSize(long fileS)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize="0B";
        if(fileS==0){
            return wrongSize;
        }
        if (fileS < 1024){
            fileSizeString = df.format((double) fileS) + "B";
        }
        else if (fileS < 1048576){
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        }
        else if (fileS < 1073741824){
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        }
        else{
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }
}
