package com.lkd.permission;

import com.lkd.permission.permissions.Permission;

import java.util.ArrayList;

/**
 *  权限调用接口
 */
public final class XXPermission extends Permission {

    /**
     * 获取权限名称列表"权限名1,权限名2,权限名3"
     */
    static public String QueryNames(ArrayList<String> permissions){
        StringBuilder names=new StringBuilder();
        for (String n : permissions) {
            names.append(XXPermission.QueryName(n)).append(",");
        }
        if(names.length()>0){
            names.setLength(names.length()-1);
        }
        return names.toString();
    }
}