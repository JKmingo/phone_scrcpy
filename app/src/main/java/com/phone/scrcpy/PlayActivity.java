package com.phone.scrcpy;

import static com.phone.scrcpy.model.ConfigParam.PREFERENCE_KEY;
import static com.phone.scrcpy.model.ConfigParam.SHOW_MENU;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phone.scrcpy.helper.StatusBarUtil;
import com.phone.scrcpy.helper.ViewTools;
import com.phone.scrcpy.model.DeviceTypeUtil;
import com.phone.scrcpy.scrcpy.IScrcpy;
import com.phone.scrcpy.scrcpy.Scrcpy;


public class PlayActivity extends Activity implements IScrcpy.ServiceCallbacks {

    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceView surfaceView;
    private Surface surface;
    private IScrcpy scrcpy;

    private static int screenWidth;
    private static int screenHeight;

    private int videoWidth;
    private int videoHeight;

    private Dialog loadingDialog;

    private String serverAdr;

    private FrameLayout rootLayout;
    private RelativeLayout movableContainer;
    private TextView menuButton;
    private TextView closeButton;
    private boolean isExpanded = false; // 当前是否已展开
    private int lastX, lastY;
    private int floatW, floatH;
    private int offsetX, offsetY;
    private boolean isDragging;
    private long touchDownTime;
    private long touchUpTime;

    private View playView = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            scrcpy = ((Scrcpy.MyServiceBinder) iBinder).getService();
            scrcpy.setServiceCallbacks(PlayActivity.this);
            scrcpy.start(surface, serverAdr);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surface_no_nav);
        loadingDialog = ViewTools.showLoading(this, false, () -> finish());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        setTheme(R.style.AppTheme_NoActionBar_TransparentStatusBar_ImmerseStatusBar);
        StatusBarUtil.setImmerseStatusBarSystemUiVisibility(this);
        serverAdr = getIntent().getStringExtra("server_address");
        if (serverAdr != null && !serverAdr.isEmpty()) {
            if (DeviceTypeUtil.getDeviceType(this).equals(DeviceTypeUtil.DeviceType.PHONE)) {
                mTextureView = findViewById(R.id.decoder_texture);
                mTextureView.setVisibility(View.VISIBLE);
                playView = mTextureView;
                mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                        if (mSurfaceTexture != null) {//其实最主要的代码在这里：利用之前创建的SurfaceTexture，在TextureView重新可见的时候设置进去
                            mTextureView.setSurfaceTexture(mSurfaceTexture);
                        } else if (mSurfaceTexture == null) {
                            mSurfaceTexture = surfaceTexture;
                        }
                        if (scrcpy == null) {
                            surface = new Surface(surfaceTexture);
                            startScrcpyService();
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                        mSurfaceTexture = surface;
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                    }
                });
            } else {
                surfaceView = findViewById(R.id.decoder_surface);
                surfaceView.setVisibility(View.VISIBLE);
                playView = surfaceView;
                surface = surfaceView.getHolder().getSurface();
                startScrcpyService();
            }
        } else {
            Toast.makeText(this, "Server Address is empty", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(SHOW_MENU, true)) {
            initMenu();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initMenu() {
        rootLayout = findViewById(R.id.root_layout);
        movableContainer = findViewById(R.id.movable_container);
        movableContainer.setVisibility(View.VISIBLE);
        menuButton = findViewById(R.id.menu_button);
        closeButton = findViewById(R.id.close_button);

        // 监听视图树布局完成事件
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 移除监听器，以避免多次调用
                rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // 在这里获取视图的高度
                int rootHeight = rootLayout.getHeight();
                int movableContainerHeight = movableContainer.getHeight();

                // 设置初始位置
                FrameLayout.LayoutParams initLayout = (FrameLayout.LayoutParams) movableContainer.getLayoutParams();
                initLayout.leftMargin = 20; // 左边距
                initLayout.topMargin = rootHeight - movableContainerHeight - 70; // 底部边距，确保在左下角
                movableContainer.setLayoutParams(initLayout);
            }
        });

        // X 按钮点击事件
        closeButton.setOnClickListener(v -> {
            ViewTools.showSimpleAlertDialog(this,
                    "Leave this page?",
                    "Do you want to close this page now?",
                    true, this::finish);
        });

        movableContainer.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录初始位置
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    floatW = movableContainer.getWidth();
                    floatH = movableContainer.getHeight();
                    offsetX = menuButton.getWidth() / 2;
                    offsetY = menuButton.getHeight() / 2;
                    isDragging = false; // 是否是拖动操作
                    touchDownTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();

                    // 判断是否移动超过阈值
                    if (!isDragging && (Math.abs(lastX - x) > 10 || Math.abs(lastY - y) > 10)) {
                        isDragging = true;
                    }

                    // 如果是拖动操作，更新位置
                    if (isDragging) {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) v.getLayoutParams();

                        if (screenWidth == 0 || screenHeight == 0) {
                            screenWidth = playView.getWidth();
                            screenHeight = playView.getHeight();
                        }

                        // 防止控件移出屏幕
                        int maxLeft = screenWidth - floatW - offsetX;
                        int maxTop = screenHeight - floatH - offsetY;
                        int left = Math.max(0, Math.min(x - offsetX, maxLeft));
                        int top = Math.max(0, Math.min(y - offsetY, maxTop));

                        layoutParams.leftMargin = left;
                        layoutParams.topMargin = top;
                        v.setLayoutParams(layoutParams);

                        // 更新触摸位置
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        rootLayout.invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    touchUpTime = System.currentTimeMillis();
                    // 如果没有拖动操作，则认为是点击事件
                    if (!isDragging) {
                        if (touchUpTime - touchDownTime < 1000) {
                            if (isExpanded) {
                                // 当前是展开状态，收回按钮
                                menuButton.setText("⋮"); // 恢复竖点
                                closeButton.setVisibility(View.GONE); // 隐藏 X 按钮
                            } else {
                                // 当前是收起状态，展开按钮
                                menuButton.setText("⋯"); // 改为横点
                                closeButton.setVisibility(View.VISIBLE); // 显示 X 按钮
                                if (movableContainer.getX() + menuButton.getWidth() * 2 > screenWidth) {
                                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) v.getLayoutParams();
                                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) closeButton.getLayoutParams();
                                    layoutParams.leftMargin = (int) movableContainer.getX() - menuButton.getWidth() - marginLayoutParams.leftMargin;
                                    v.setLayoutParams(layoutParams);
                                }
                            }
                            isExpanded = !isExpanded; // 切换状态
                        }
                    }
                    return true;

                default:
                    return false;
            }
        });

    }

    private void startScrcpyService() {
        Intent intent = new Intent(this, Scrcpy.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        ViewTools.showSimpleAlertDialog(this,
                "Leave this page?",
                "Do you want to close this page now?",
                true, this::finish);
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        if (surface != null) {
            surface.release();
            surface = null;
            surfaceView = null;
        }
        if (scrcpy != null) {
            scrcpy.stopService();
        }
        super.onDestroy();
    }

    @Override
    public void loadNewRotation(int viewWidth, int viewHeight) {
        videoWidth = viewWidth;
        videoHeight = viewHeight;
        if (screenWidth <= 0) {
            playView.getWidth();
        }
        if (screenHeight <= 0) {
            playView.getHeight();
        }
        if (videoWidth > videoHeight) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (screenWidth < screenHeight) {
                swapDimensions();
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (screenWidth > screenHeight) {
                swapDimensions();
            }
        }
        playView.setOnTouchListener((v, event) -> scrcpy.touchevent(v, event, videoWidth, videoHeight));
        // 隐藏
        if (loadingDialog != null && loadingDialog.isShowing()) {
            runOnUiThread(() -> loadingDialog.dismiss());
        }
    }

    private void swapDimensions() {
        int temp = screenHeight;
        screenHeight = screenWidth;
        screenWidth = temp;
    }
}
