package com.phone.scrcpy.model;

import android.hardware.usb.UsbDevice;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class UsbDevicesMap {
    public static Map<String, UsbDevice> devices = new HashMap<>();

    public static void put(String name, UsbDevice usbDevice) {
        devices.put(name, usbDevice);
    }

    public static void remove(String name) {
        devices.remove(name);
    }

    public static int size() {
        return devices.size();
    }

    public static UsbDevice get(String name) {
        UsbDevice resDevice = devices.get(name);
        if (resDevice != null) {
            return resDevice;
        }
        for (Map.Entry<String, UsbDevice> entry : UsbDevicesMap.devices.entrySet()) {
            UsbDevice device = entry.getValue();
            // 假设设备名是 device.getDeviceName()，你可以根据你的实际数据结构来调整
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (device.getSerialNumber().equals(name)) {
                    return device;
                }
            } else {
                if (device.getDeviceName().equals(name)) {
                    return device;
                }
            }
        }
        return null;
    }
}
