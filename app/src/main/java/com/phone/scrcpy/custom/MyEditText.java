package com.phone.scrcpy.custom;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

public class MyEditText extends AppCompatEditText {

    public MyEditText(Context context) {
        super(context);
        init(context);
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 监听焦点变化
        this.setOnFocusChangeListener((v, hasFocus) -> {
            // 失去焦点时，主动跳转焦点
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) {
                return;
            }
            if (hasFocus) {
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                setSelection(getText().length());
            } else {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            View nextFocus = FocusFinder.getInstance().findNextFocus((ViewGroup) this.getParent(), this, View.FOCUS_DOWN);
            if (nextFocus != null) {
                nextFocus.requestFocus();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            View preFocus = FocusFinder.getInstance().findNextFocus((ViewGroup) this.getParent(), this, View.FOCUS_UP);
            if (preFocus != null) {
                preFocus.requestFocus();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);  // 默认处理其他事件
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
}
