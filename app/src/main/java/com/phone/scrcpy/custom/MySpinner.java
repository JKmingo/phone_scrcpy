package com.phone.scrcpy.custom;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;


public class MySpinner extends LinearLayout {

    private TextView textView;
    private Spinner spinner;

    public MySpinner(Context context) {
        super(context);
        init(context, null);
    }

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MySpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);

        // 动态创建TextView并添加到LinearLayout中
        textView = new TextView(context);
        textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setTextSize(16f);
        textView.setPadding(0, 0, 0, 8);  // 设置TextView底部间距
        textView.setTextColor(context.getResources().getColor(android.R.color.black));
        addView(textView);

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
        }

        // 动态创建Spinner并添加到LinearLayout中
        spinner = new Spinner(context);
        spinner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(spinner);
        spinner.setOnFocusChangeListener((v, hasFocus) -> {
            TextView tv = (TextView) v;
            if (hasFocus) {
                tv.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
            } else {
                tv.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            }
        });
        super.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                ((TextView) spinner.getSelectedView()).setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
            } else {
                textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
                ((TextView) spinner.getSelectedView()).setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            }
        });
    }

    public void setHintText(String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    public void setAdapter(int textArrayOptionResId) {
        if (spinner != null) {
            // 创建自定义适配器
            String[] options = getResources().getStringArray(textArrayOptionResId);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, options) {
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    // 获取原始下拉项视图
                    View view = super.getDropDownView(position, convertView, parent);
                    // 设置下拉选项的样式
                    TextView textView = (TextView) view;
                    textView.setTextSize(18f); // 大字体
                    textView.setPadding(20, 20, 20, 20);
                    textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
                    return view;
                }
            };
            // 将下拉框宽度设置为与 Spinner 宽度一致
            spinner.post(() -> spinner.setDropDownWidth(spinner.getWidth()));
            // 设置适配器
            spinner.setAdapter(adapter);
        }
    }

    public int getSelectedItemPosition() {
        return spinner.getSelectedItemPosition();
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        spinner.setOnItemSelectedListener(listener);
    }

    public void setSelection(int position) {
        spinner.setSelection(position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return spinner.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return spinner.onKeyUp(keyCode, event);
    }
}
