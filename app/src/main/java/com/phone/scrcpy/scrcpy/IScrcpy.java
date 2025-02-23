package com.phone.scrcpy.scrcpy;

import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

public interface IScrcpy {
    void pause();

    void resume();

    void stopService();

    void setServiceCallbacks(ServiceCallbacks callbacks);

    void start(Surface surface, String serverAdr);


    void sendKeyevent(int keyCode);

    boolean touchevent(View view, MotionEvent touch_event, int displayW, int displayH);

    interface ServiceCallbacks {
        void loadNewRotation(int w, int h);
    }
}

