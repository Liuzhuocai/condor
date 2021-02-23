package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.util.UiThreadHelper;

public class FolderEditText extends EditText {
    public interface TextChangeListener {
        void onTextChanged(String s);
    }
    private ExtendedEditText.OnBackKeyListener mBackKeyListener;
    private TextChangeListener mTextChangeListener;

    public FolderEditText(Context context) {
        super(context);
        init();
    }

    public FolderEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setSelectAllOnFocus(true);
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addTextChangedListener(editChangedListener);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    private TextWatcher editChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mTextChangeListener != null) {
                mTextChangeListener.onTextChanged(s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public void setTextChangeListener(TextChangeListener listener) {
        mTextChangeListener = listener;
    }

    public void setOnBackKeyListener(ExtendedEditText.OnBackKeyListener listener) {
        mBackKeyListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // If this is a back key, propagate the key back to the listener
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mBackKeyListener != null) {
                return mBackKeyListener.onBackKey();
            }
            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void dispatchBackKey() {
        UiThreadHelper.hideKeyboardAsync(getContext(), getWindowToken());
        if (mBackKeyListener != null) {
            mBackKeyListener.onBackKey();
        }
    }
}
