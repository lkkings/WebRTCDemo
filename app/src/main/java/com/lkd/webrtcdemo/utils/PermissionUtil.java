package com.lkd.webrtcdemo.utils;

import android.content.Context;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;

import java.util.concurrent.atomic.AtomicReference;

public class PermissionUtil {
    public static Boolean chick(Context appContext, String ...PermissionGroup){
        if(AndPermission.hasPermissions(appContext,PermissionGroup)){
            return true;
        }
        AtomicReference<Boolean> isGranted = new AtomicReference<>(false);
        AndPermission.with(appContext)
                .runtime()
                .permission(PermissionGroup)
                .onGranted(permission -> {
                            isGranted.set(true);
                        }
                )
                .onDenied(permissions -> {
                    Toast.makeText(appContext, "开启权限失败，该功能不能使用", Toast.LENGTH_SHORT);
                })
                .start();
        return isGranted.get();
    }

    public static Boolean chick(Context appContext, String[] ...PermissionGroup){
        if(AndPermission.hasPermissions(appContext,PermissionGroup)){
            return true;
        }
        AtomicReference<Boolean> isGranted = new AtomicReference<>(false);
        AndPermission.with(appContext)
                .runtime()
                .permission(PermissionGroup)
                .onGranted(permission -> {
                            isGranted.set(true);
                        }
                )
                .onDenied(permissions -> {
                    Toast.makeText(appContext, "开启权限失败，该功能不能使用", Toast.LENGTH_SHORT);
                })
                .start();
        return isGranted.get();
    }
}
