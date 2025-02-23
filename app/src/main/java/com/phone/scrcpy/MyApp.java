package com.phone.scrcpy;

import android.app.Application;
import android.content.Context;

public class MyApp extends Application {

    // 全局Context变量
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取应用程序的全局Context
        appContext = getApplicationContext();
    }

    // 提供一个静态方法来获取全局Context
    public static Context getAppContext() {
        return appContext;
    }
}

