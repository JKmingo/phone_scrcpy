package com.phone.scrcpy.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.phone.scrcpy.MyApp;
import com.phone.scrcpy.databinding.LoadingBinding;
import com.phone.scrcpy.databinding.ModuleDialogBinding;

public class ViewTools {

    private static int screenWidth;
    private static int screenHeight;

    // 设置全面屏
    public static void setFullScreen(Activity context) {
        // 全屏显示
        context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    // 设置状态栏导航栏颜色
    public static void setStatusAndNavBar(Activity context) {
        // 导航栏
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //    context.getWindow().setNavigationBarColor(context.getResources().getColor(R.color.background));
        //    // 状态栏
        //    context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //    context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //    context.getWindow().setStatusBarColor(context.getResources().getColor(R.color.background));
        //}
        // 设置异形屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = context.getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            context.getWindow().setAttributes(lp);
        }
    }

    // 创建弹窗
    public static Dialog showLoading(Context context, boolean canCancel, Runnable timeoutRunner) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(canCancel);
        ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
        View loading = LoadingBinding.inflate(LayoutInflater.from(context)).getRoot();
        dialogView.addView(loading);
        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(canCancel);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 设置超时自动关闭
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                if (timeoutRunner != null) {
                    timeoutRunner.run();
                }
                Toast.makeText(context, "加载超时失败", Toast.LENGTH_SHORT).show();
            }
        }, 10000);
        dialog.show();
        return dialog;
    }

    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showSimpleAlertDialog(Context context, String title, String message, boolean showCancelButton, Runnable runnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (runnable != null) {
                        runnable.run();
                    }
                });
        if (showCancelButton) {
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showInputDialog(Context context, String title, EditText input, Runnable runnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setView(input);

        // 创建对话框实例
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setGravity(Gravity.START); // 设置标题左对齐
            }
        });
        // 自定义确定按钮的行为
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialogInterface, which) -> {
            // 点击时会被覆盖，具体逻辑见下面
        });
        dialog.show();
        // 重写“确定”按钮的点击逻辑
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String inputValue = input.getText().toString().trim();
            if (inputValue.isEmpty()) {
                // 输入为空时提示
                Toast.makeText(context, "The value cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    int value = Integer.parseInt(inputValue);
                    if (value > 0) {
                        if (runnable != null) {
                            runnable.run();
                        }
                        dialog.dismiss();
                    } else {
                        // 时间必须大于 0
                        Toast.makeText(context, "Must be greater than 0!", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    // 输入非数字时提示
                    Toast.makeText(context, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public static Pair<Integer, Integer> getScreenSize(Context context) {
        if (screenWidth > 0 && screenHeight > 0) {
            return new Pair<>(screenWidth, screenHeight);
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;  // 屏幕宽度
        int screenHeight = metrics.heightPixels; // 屏幕高度
        int statusBarHeight = 0;
        int resourceId1 = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId1 > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId1);
        }
        int navigationBarHeight = 0;
        int resourceId2 = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId2 > 0) {
            navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId2);
        }
        screenHeight = screenHeight + statusBarHeight + navigationBarHeight;
        return new Pair<>(screenWidth, screenHeight);
    }

    public static Point mapScreenToVideo(int viewWidth, int viewHeight, int videoWidth, int videoHeight, float xScreen, float yScreen) {
        int mScreenWidth;
        int mScreenHeight;
        if (videoWidth > videoHeight) {
            mScreenWidth = Math.max(viewWidth, viewHeight);
            mScreenHeight = Math.min(viewWidth, viewHeight);
        } else {
            mScreenHeight = Math.max(viewWidth, viewHeight);
            mScreenWidth = Math.min(viewWidth, viewHeight);
        }
        // 计算横向和纵向的缩放比例
        float scaleX = (float) videoWidth / mScreenWidth;
        float scaleY = (float) videoHeight / mScreenHeight;

        // 映射坐标
        int xVideo = (int) (xScreen * scaleX);
        int yVideo = (int) (yScreen * scaleY);

        return new Point(xVideo, yVideo);
    }

}
