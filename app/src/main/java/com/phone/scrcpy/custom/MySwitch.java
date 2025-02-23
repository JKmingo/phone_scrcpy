package com.phone.scrcpy.custom;

import static android.view.Gravity.CENTER;
import static android.view.Gravity.RIGHT;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MySwitch extends LinearLayout {

    private TextView textView;
    private Switch switchView;

    public MySwitch(Context context) {
        super(context);
        init(context, null);
    }

    public MySwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MySwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL); // 水平布局
        setPadding(16, 16, 16, 16);  // 设置控件内边距

        // 动态创建 TextView 并添加到 LinearLayout 中
        textView = new TextView(context);
        LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textParams.weight = 1;  // 使 TextView 占据剩余的空间
        textView.setLayoutParams(textParams);
        textView.setTextSize(16f);
        textView.setPadding(12, 16, 16, 16);  // 设置 TextView 右侧间距
        textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        addView(textView);

        // 动态创建 Switch 控件并添加到 LinearLayout 中
        switchView = new Switch(context);
        LayoutParams switchParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        switchParams.gravity = CENTER | RIGHT;  // 使 Switch 控件靠右
        switchView.setLayoutParams(switchParams);
        addView(switchView);

        if (attrs != null) {
            // 获取 `android:text` 的属性值（资源 ID 或文本）
            String textValue = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "text");
            if (textValue != null && textValue.startsWith("@")) {
                int resId = Integer.parseInt(textValue.substring(1));
                String realText = context.getResources().getString(resId);
                textView.setText(realText); // 设置到 TextView 上
            } else {
                textView.setText("Option");
            }
            String checkedVal = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "checked");
            if (checkedVal.equals("true")) {
                switchView.setChecked(true);
            }
        }

        // 设置焦点变化时的 TextView 颜色变化
        super.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
            } else {
                textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            }
        });

        textView.setOnClickListener(v -> {
            switchView.performClick();
        });
    }

    // 设置提示文本
    public void setHintText(String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    // 获取 Switch 的当前状态
    public boolean isChecked() {
        return switchView.isChecked();
    }

    // 设置 Switch 的状态
    public void setChecked(boolean checked) {
        switchView.setChecked(checked);
    }

    // 获取 Switch 控件
    public Switch getSwitch() {
        return switchView;
    }

    // 设置 OnCheckedChangeListener
    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        switchView.setOnCheckedChangeListener(listener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return switchView.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return switchView.onKeyUp(keyCode, event);
    }
}
