package com.phone.scrcpy.helper;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.phone.scrcpy.MyApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AdbHelper {

    // 静态单例实例
    private static AdbHelper instance;

    // 用于保存 adb 路径，避免每次都重新计算
    private String adbPath;
    private Context context;
    private String hostIp;
    private int hostPort;
    private boolean isConnected;

    // 私有化构造函数，防止外部实例化
    private AdbHelper(String hostIp, int hostPort) {
        this.hostIp = hostIp;
        this.hostPort = hostPort;
        this.context = MyApp.getAppContext();
        String adbLibPath = this.context.getApplicationInfo().nativeLibraryDir + "/libadb.so";
        File libAdb = new File(adbLibPath);
        if (libAdb.exists()) {
            this.adbPath = adbLibPath;
        } else {
            this.adbPath = null; // 如果找不到adb文件，可以设置为null
        }
    }

    // 获取单例实例
    public static synchronized AdbHelper getInstance(String hostIp, int hostPort) {
        if (instance == null) {
            instance = new AdbHelper(hostIp, hostPort);
        } else {
            instance.hostIp = hostIp;
            instance.hostPort = hostPort;
        }
        if (!instance.isConnected) {
            String res = instance.runAdb("connect " + hostIp + ":" + hostPort, false);
            if (res.contains("failed to authenticate to")) {
                while (true) {
                    String devices = instance.runAdb("devices", false);
                    if (devices.contains(instance.hostIp + ":" + instance.hostPort + "\tdevice")) {
                        break;
                    }
                    SystemClock.sleep(50);
                }
            } else if (!res.startsWith("already connected to") && !res.startsWith("connected to")) {
                Log.w(AdbHelper.class.getName(), "connect to adb failed:" + res);
                return null;
            } else {
                instance.isConnected = true;
            }
        }
        return instance;
    }

    // 执行 adb 命令
    public String runAdb(String command, boolean useBindDevice) {
        if (adbPath == null) {
            return null; // 如果 adb 路径不存在，则返回 null
        }
        List<String> commandList = new ArrayList<>();
        commandList.add("sh");
        commandList.add("-c");
        if (useBindDevice) {
            commandList.add(adbPath + " -s " + hostIp + ":" + hostPort + " " + command);
        } else {
            commandList.add(adbPath + " " + command);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(new File(adbPath).getParentFile()); // 设置工作目录
        processBuilder.redirectErrorStream(true);
        Map<String, String> env = processBuilder.environment();
        env.put("HOME", context.getFilesDir().getPath());
        env.put("TMPDIR", context.getCacheDir().getPath());
        processBuilder.environment().putAll(env);
        try {
            Process adbProcess = processBuilder.start();
            StringBuilder result = new StringBuilder();
            if (adbProcess != null) {
                adbProcess.waitFor();
                Scanner scanner = new Scanner(adbProcess.getInputStream());
                while (scanner.hasNext()) {
                    result.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                Scanner scannerErr = new Scanner(adbProcess.getErrorStream());
                while (scannerErr.hasNext()) {
                    result.append(scannerErr.nextLine()).append("\n");
                }
                scannerErr.close();
            } else {
                result.append("adb path not found");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(AdbHelper.class.getName(), "", e);
        }
        return null;
    }


    // 执行 adb 命令并获取结果
    public String runAdbShell(String command) {
        if (!isConnected) {
            String res = runAdb("connect " + hostIp + ":" + hostPort, false);
            if (!res.startsWith("already connected to") && !res.startsWith("connected to")) {
                Log.w(AdbHelper.class.getName(), "connect to adb failed:" + res);
                return null;
            }
            isConnected = true;
        }
        return runAdb("shell " + command, true);
    }
}
