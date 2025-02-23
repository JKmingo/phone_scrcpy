package com.phone.scrcpy.model;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class DeviceTypeUtil {

    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        PHONE, TABLET, SMART_TV
    }

    /**
     * 获取设备类型
     *
     * @param context 上下文
     * @return 设备类型（DeviceType.PHONE, DeviceType.TABLET, DeviceType.SMART_TV）
     */
    public static DeviceType getDeviceType(Context context) {
        // 判断是否是智能电视
        if (context.getPackageManager().hasSystemFeature("android.software.leanback")) {
            return DeviceType.SMART_TV;
        }

        // 根据设备属性进一步判断智能电视
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        if (manufacturer.contains("tv") || product.contains("tv") || model.contains("tv")) {
            return DeviceType.SMART_TV;
        }

        // 判断是否是平板
        if ((context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return DeviceType.TABLET;
        }

        // 默认设备为手机
        return DeviceType.PHONE;
    }
}
