package com.phone.scrcpy.scrcpy;

import android.view.MotionEvent;

import java.nio.ByteBuffer;

public class ControlPacket {

    public static ByteBuffer screenOff() {
        int msgType = 10;
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) msgType);
        buffer.put((byte) 0);
        byte[] injectData = buffer.array();
        return ByteBuffer.wrap(injectData);
    }

    public static ByteBuffer screenOn() {
        int msgType = 10;
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) msgType);
        buffer.put((byte) 1);
        byte[] injectData = buffer.array();
        return ByteBuffer.wrap(injectData);
    }

    public static ByteBuffer sendKeyDown(int keyCode) {
        ByteBuffer buffer = ByteBuffer.allocate(14);
        int type = 0;
        int action = 0;
        int repeat = 0;
        int metastate = 0;
        buffer.put((byte) type);
        buffer.put((byte) action);
        buffer.putInt(keyCode);
        buffer.putInt(repeat);
        buffer.putInt(metastate);
        byte[] injectData = buffer.array();
        return ByteBuffer.wrap(injectData);
    }

    public static ByteBuffer sendKeyUp(int keyCode) {
        ByteBuffer buffer = ByteBuffer.allocate(14);
        int type = 0;
        int action = 1;
        int repeat = 0;
        int metastate = 0;
        buffer.put((byte) type);
        buffer.put((byte) action);
        buffer.putInt(keyCode);
        buffer.putInt(repeat);
        buffer.putInt(metastate);
        byte[] injectData = buffer.array();
        return ByteBuffer.wrap(injectData);
    }

    public static ByteBuffer sendTouchEvent(int action, int touchId, int x, int y, int resolutionWidth, int resolutionHeight) {
        ByteBuffer buffer = ByteBuffer.allocate(32); // Allocate buffer of 32 bytes (size of the structure)
        //SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT = 2
        int msgType = 0x02;
        // Ensure touch position is not negative
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        int pressure = 1;
        // If action is AMOTION_EVENT_ACTION_UP, set pressure to 0
        if (action == MotionEvent.ACTION_UP) {
            pressure = 0;
        }
        // Pack the data into the buffer (Big-endian order)
        buffer.put((byte) msgType);         // B: msg_type
        buffer.put((byte) action);          // B: action
        buffer.putLong(touchId);     // q: touch_id
        buffer.putInt(x);            // i: x
        buffer.putInt(y);            // i: y
        buffer.putShort((short) resolutionWidth);      // H: width
        buffer.putShort((short) resolutionHeight);     // H: height
        buffer.putShort((short) pressure);   // H: pressure
        buffer.putInt(1);      // i: buttons
        buffer.putInt(pressure);// i: pressure again

        byte[] injectData = buffer.array();       // Convert to byte array
        return ByteBuffer.wrap(injectData);       // Return wrapped ByteBuffer
    }
}
