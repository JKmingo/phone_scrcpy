package com.phone.scrcpy;

import static com.phone.scrcpy.model.ConfigParam.ENABLE_AUDIO;
import static com.phone.scrcpy.model.ConfigParam.PREFERENCE_KEY;
import static com.phone.scrcpy.model.ConfigParam.SERVER_ADDRESS;
import static com.phone.scrcpy.model.ConfigParam.SHOW_MENU;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_BITRATE;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_BITRATE_INDEX;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_RESOLUTION;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_RESOLUTION_INDEX;
import static com.phone.scrcpy.model.ConfigParam.TURN_OFF_SCREEN;
import static com.phone.scrcpy.model.ConfigParam.USE_H265;
import static com.phone.scrcpy.model.ConfigParam.VIDEO_FPS;
import static com.phone.scrcpy.model.ConfigParam.VIDEO_FPS_INDEX;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.phone.scrcpy.adb.Adb;
import com.phone.scrcpy.custom.MyEditText;
import com.phone.scrcpy.custom.MySpinner;
import com.phone.scrcpy.custom.MySwitch;
import com.phone.scrcpy.helper.ViewTools;
import com.phone.scrcpy.model.DeviceTypeUtil;
import com.phone.scrcpy.model.UsbDevicesMap;
import com.phone.scrcpy.usb.USBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MSG_USB_PERMISSION = 1001;
    private Context context;
    private String serverAdr = null;
    private boolean selectedUsbDevice = false;

    private int videoBitrate;
    private long timestamp = 0;

    private Button usbSelectButton;
    private Button startButton;
    private Button tcpipButton;
    private MyEditText editTextServerHost;
    private MySpinner resolutionSpinner;
    private MySpinner bitrateSpinner;
    private MySpinner fpsSpinner;
    private MySwitch switchAudio;
    private MySwitch switchH265;
    private MySwitch switchScreenOff;
    private MySwitch switchShowMenu;

    private UsbManager usbManager;

    private MyHandler handler;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    if (usbManager == null) {
                        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                    }
                    Log.d("USB", "USB device attached: " + usbDevice.getDeviceName());
                    getUsbDevicePermission(usbDevice);
                    updateDeviceCount(context);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    UsbDevicesMap.remove(device.getDeviceName());
                    updateDeviceCount(context);
                }
            } else if ("ACTION_USB_DEVICE_ATTACHED_PERMISSION".equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    if (usbManager.hasPermission(usbDevice)) {
                        Log.e("USB", "Permission granted for device: " + usbDevice);
                    } else {
                        Log.e("USB", "Permission not granted for device: " + usbDevice);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        handler = new MyHandler();
        if (usbManager == null) {
            usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        }
        registerUsbReceiver(this);
        initViews();

        if (DeviceTypeUtil.getDeviceType(context).equals(DeviceTypeUtil.DeviceType.PHONE)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            usbSelectButton.requestFocus();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        showDonationDialog();
    }

    private void initViews() {
        usbSelectButton = findViewById(R.id.button_select_usb);
        editTextServerHost = findViewById(R.id.editText_server_host);
        resolutionSpinner = findViewById(R.id.spinner_video_resolution);
        bitrateSpinner = findViewById(R.id.spinner_video_bitrate);
        fpsSpinner = findViewById(R.id.spinner_fps);
        switchAudio = findViewById(R.id.switch_audio);
        switchH265 = findViewById(R.id.switch_h265);
        switchScreenOff = findViewById(R.id.switch_screen_off);
        switchShowMenu = findViewById(R.id.switch_show_menu);
        startButton = findViewById(R.id.button_start);
        tcpipButton = findViewById(R.id.button_open_tcpip);

        usbSelectButton.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                editTextServerHost.requestFocus();
                editTextServerHost.performClick();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        });
        resolutionSpinner.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
                editTextServerHost.requestFocus();
                editTextServerHost.performClick();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        });

        editTextServerHost.setText(context.getSharedPreferences(PREFERENCE_KEY, 0).getString(SERVER_ADDRESS, ""));
        switchAudio.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(ENABLE_AUDIO, false));
        switchH265.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(USE_H265, false));
        switchScreenOff.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(TURN_OFF_SCREEN, false));
        switchShowMenu.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(SHOW_MENU, true));
        setSpinner(R.array.options_resolution_keys, R.id.spinner_video_resolution, SPINNER_RESOLUTION_INDEX, 3);
        setSpinner(R.array.options_bitrate_keys, R.id.spinner_video_bitrate, SPINNER_BITRATE_INDEX, 5);
        setSpinner(R.array.options_fps_keys, R.id.spinner_fps, VIDEO_FPS_INDEX, 3);

        usbSelectButton.setOnClickListener(v -> {
            // 假设 UsbDevicesMap 是一个 HashMap<String, UsbDevice>
            if (UsbDevicesMap.size() == 0) {
                Toast.makeText(this, "No USB device found", Toast.LENGTH_SHORT).show();
                return;
            }

            // 从 UsbDevicesMap 获取设备名称列表
            final List<String> usbDevicesList = new ArrayList<>();
            for (Map.Entry<String, UsbDevice> entry : UsbDevicesMap.devices.entrySet()) {
                UsbDevice device = entry.getValue();
                // 假设设备名是 device.getDeviceName()，你可以根据你的实际数据结构来调整
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    usbDevicesList.add(device.getSerialNumber());
                } else {
                    usbDevicesList.add(device.getDeviceName());
                }
            }
            // 将 List 转换为数组，因为 AlertDialog 的 setSingleChoiceItems 方法需要一个字符串数组
            final String[] usbDevicesArray = usbDevicesList.toArray(new String[0]);
            // 创建 AlertDialog
            final String[] selectedDevice = {usbDevicesArray[0]};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select USB Device")
                    .setSingleChoiceItems(usbDevicesArray, 0, (dialog, which) -> {
                        // 处理用户选择的设备
                        selectedDevice[0] = usbDevicesArray[which];
                    })
                    .setPositiveButton("OK", (dialog, id) -> {
                        // 处理确定按钮点击（如果需要做额外处理）
                        selectedUsbDevice = true;
                        editTextServerHost.setText(selectedDevice[0] + "(USB)");
                        //editTextServerHost.createButton("open tcpip").showButton();
                        tcpipButton.setEnabled(true);
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss())
                    .create()
                    .show();
        });
        startButton.setOnClickListener(v -> {
            getAttributes();
            selectedUsbDevice = editTextServerHost.getText().toString().endsWith("(USB)");
            if (selectedUsbDevice) {
                startPlayActivity();
                return;
            }
            if (!serverAdr.isEmpty()) {
                startPlayActivity();
            } else {
                Toast.makeText(context, "Server Address Empty", Toast.LENGTH_SHORT).show();
            }
        });

        tcpipButton.setOnClickListener(v -> {
            String usbDeviceName = editTextServerHost.getText().toString();
            if (!usbDeviceName.endsWith("(USB)")) {
                Toast.makeText(context, "Please select a USB device!!!", Toast.LENGTH_SHORT).show();
                return;
            }
            EditText editText = new EditText(this);
            editText.setHint("input port");
            editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            ViewTools.showInputDialog(context, "TCP/IP Port", editText, () -> {
                Dialog loadingDialog = ViewTools.showLoading(this, false, null);
                new Thread(() -> {
                    Adb adb = null;
                    try {
                        UsbDevice usbDevice = UsbDevicesMap.get(usbDeviceName.replace("(USB)", ""));
                        adb = new Adb(this, usbDevice);
                        String output = adb.restartOnTcpip(Integer.parseInt(editText.getText().toString()));
                        if (output.contains("restarting in TCP mode port: " + editText.getText().toString())) {
                            runOnUiThread(() -> ViewTools.showAlertDialog(this, "Open TCP/IP", "Success!"));
                        } else {
                            runOnUiThread(() -> ViewTools.showAlertDialog(this, "Open TCP/IP", "Fail!"));
                        }
                    } catch (Exception e) {
                        Log.e(getClass().getName(), "", e);
                    } finally {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            runOnUiThread(() -> loadingDialog.dismiss());
                        }
                        if (adb != null) {
                            adb.close();
                        }
                    }
                }).start();
            });
        });

    }

    private void setSpinner(final int textArrayOptionResId, final int textViewResId, final String preferenceId, int defaultIndex) {

        final MySpinner spinner = (MySpinner) findViewById(textViewResId);
        //ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, textArrayOptionResId, android.R.layout.simple_spinner_item);
        //arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(textArrayOptionResId);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putInt(preferenceId, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putInt(preferenceId, 0).apply();
            }
        });
        spinner.setSelection(context.getSharedPreferences(PREFERENCE_KEY, 0).getInt(preferenceId, defaultIndex));
    }

    private void getAttributes() {
        serverAdr = editTextServerHost.getText().toString();
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCE_KEY, 0).edit();
        if (!selectedUsbDevice) {
            editor.putString("Server Address", serverAdr);
        }
        int videoResolution = getResources().getIntArray(R.array.options_resolution_values)[resolutionSpinner.getSelectedItemPosition()];
        editor.putInt(SPINNER_RESOLUTION, videoResolution);
        editor.putInt(SPINNER_RESOLUTION_INDEX, resolutionSpinner.getSelectedItemPosition());

        videoBitrate = getResources().getIntArray(R.array.options_bitrate_values)[bitrateSpinner.getSelectedItemPosition()];
        editor.putInt(SPINNER_BITRATE, videoBitrate);
        editor.putInt(SPINNER_BITRATE_INDEX, bitrateSpinner.getSelectedItemPosition());

        int videoFPS = getResources().getIntArray(R.array.options_fps_values)[fpsSpinner.getSelectedItemPosition()];
        editor.putInt(VIDEO_FPS, videoFPS);
        editor.putInt(VIDEO_FPS_INDEX, fpsSpinner.getSelectedItemPosition());

        boolean isEnableAudio = switchAudio.isChecked();
        editor.putBoolean(ENABLE_AUDIO, isEnableAudio);

        boolean isH265 = switchH265.isChecked();
        editor.putBoolean(USE_H265, isH265);

        boolean isScreenOff = switchScreenOff.isChecked();
        editor.putBoolean(TURN_OFF_SCREEN, isScreenOff);

        boolean isShowMenu = switchShowMenu.isChecked();
        editor.putBoolean(SHOW_MENU, isShowMenu);
        editor.commit();
    }

    private void startPlayActivity() {
        Intent intent = new Intent(MainActivity.this, PlayActivity.class);
        intent.putExtra("server_address", serverAdr);
        startActivity(intent);
    }

    private void registerUsbReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction("ACTION_USB_DEVICE_ATTACHED_PERMISSION");
        context.registerReceiver(usbReceiver, filter);
    }

    private void updateDeviceCount(Context context) {
        int deviceCount = UsbDevicesMap.devices.size();
        Toast.makeText(context, "USB connected device count: " + deviceCount, Toast.LENGTH_SHORT).show();
    }

    private void getUsbDevicePermission(UsbDevice usbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            UsbDevicesMap.put(usbDevice.getDeviceName(), usbDevice);
            return;
        }
        Message msg = Message.obtain();
        msg.what = MSG_USB_PERMISSION;
        msg.obj = usbDevice;
        handler.sendMessageDelayed(msg, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<UsbDevice> usbDevices = USBHelper.getInstance(this).getUsbDevices();
        if (!usbDevices.isEmpty()) {
            for (UsbDevice usbDevice : usbDevices) {
                // 在后台线程中请求权限，避免阻塞主线程
                getUsbDevicePermission(usbDevice);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View currentFocusView = getCurrentFocus();
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 判断点击的区域是否在 MyEditText 外部
            if (currentFocusView instanceof MyEditText) {
                if (!isTouchInsideView(currentFocusView, ev)) {
                    // 如果点击的不是 MyEditText 区域，则清除焦点
                    currentFocusView.clearFocus();
                    // 隐藏软键盘
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocusView.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev); // 确保事件继续传递
    }

    @Override
    public void onBackPressed() {
        if (timestamp == 0) {
            timestamp = SystemClock.uptimeMillis();
            Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show();
        } else {
            long now = SystemClock.uptimeMillis();
            if (now < timestamp + 1000) {
                timestamp = 0;
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
            timestamp = 0;
        }
    }

    private boolean isTouchInsideView(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();

        float touchX = event.getRawX();
        float touchY = event.getRawY();

        return touchX >= viewX && touchX <= viewX + viewWidth && touchY >= viewY && touchY <= viewY + viewHeight;
    }

    private void showDonationDialog() {
        // 创建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("If you find this app useful, please donate to me");
        builder.setMessage("Your support helps keep this app alive and improves it!");

        // 设置“捐赠”按钮
        builder.setPositiveButton("Donate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });

        // 设置“取消”按钮
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // 关闭对话框
            }
        });

        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_USB_PERMISSION) {
                UsbDevice device = (UsbDevice) msg.obj;
                if (usbManager.hasPermission(device)) {
                    UsbDevicesMap.put(device.getDeviceName(), device);
                    return;
                }
                // 请求用户授权
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        context, 0, new Intent("ACTION_USB_DEVICE_ATTACHED_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(device, permissionIntent);
                Message message = Message.obtain();
                message.what = MSG_USB_PERMISSION;
                message.obj = device;
                handler.sendMessageDelayed(message, 1000);
            }
        }
    }
}
