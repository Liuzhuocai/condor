/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.condor.launcher.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.R;
import com.condor.launcher.AllTaskClearClient;
import com.condor.launcher.util.MemoryInfoManager;
import com.condor.launcher.util.ToastHelper;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.condor.launcher.util.MemoryInfoManager.GB;
import static com.condor.launcher.util.MemoryInfoManager.MB;

/**
 * Created by Perry on 19-1-24
 */
public class ClearAllButton extends LinearLayout implements AllTaskClearClient {
    private WaveView mButton;
    private TextView mInfo;
    private final Rect mBounds = new Rect();
    private final AnimatorSet mAnimator = new AnimatorSet();
    private final MemoryInfoManager mMIM;
    private OnAllTaskClearListener mListener;
    // Perry: Optimizing memory info: start
    private final AtomicBoolean mClearing = new AtomicBoolean(false);
    // Perry: Optimizing memory info: end

    public ClearAllButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mMIM = MemoryInfoManager.getInstance(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mInfo = findViewById(R.id.memory_info);
        mButton = findViewById(R.id.clear_button);
        mButton.setOnClickListener(this::clearTasks);
    }

    // Perry: Optimizing memory info: start
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateMemoryInfo();
    }
    // Perry: Optimizing memory info: end

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Perry: Optimizing memory info: start
        updateMemoryInfoLayout();
        // Perry: Optimizing memory info: end
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setParent((View) getParent()); // Pretend we are a part of the task carousel.
    }

    @Override
    public void getHitRect(Rect outRect) {
        mButton.getHitRect(outRect);
        // Perry: Invalid click clear all button: start
        int width = outRect.width();
        int height = outRect.height();
        outRect.left += getLeft();
        outRect.top += getTop();
        outRect.right = outRect.left + width;
        outRect.bottom = outRect.top + height;
        // Perry: Invalid click clear all button: end
    }

    @Override
    public void setOnAllTaskClearListener(OnAllTaskClearListener listener) {
        mListener = listener;
    }

    private void clearTasks(View view) {
        // Perry: Optimizing memory info: start
        mClearing.set(true);
        // Perry: Optimizing memory info: end
        final float availableMemory = mMIM.getAvailable(MB);
        if (mListener != null) {
            mListener.dismissAllTasks(null);
        }
        float ratio = mButton.getWaterLevelRatio();
        ObjectAnimator animDown = ObjectAnimator.ofFloat(
                mButton, "waterLevelRatio", ratio, 0.3f * ratio);
        animDown.setDuration(500);
        animDown.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator animUp = ObjectAnimator.ofFloat(
                mButton, "waterLevelRatio", 0.3f * ratio, ratio);
        animUp.setDuration(500);
        animUp.setInterpolator(new DecelerateInterpolator());
        mAnimator.playSequentially(animDown, animUp);
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                float memoryCleared = mMIM.getAvailable(MB) - availableMemory;
                notifyMemoryCleared(memoryCleared);
                // Perry: Optimizing memory info: start
                mClearing.set(false);
                post(ClearAllButton.this::updateMemoryInfo);
                // Perry: Optimizing memory info: end
                mListener.onAllTasksRemoved();
            }
        });
        mAnimator.start();
    }

    private void notifyMemoryCleared(float memoryCleared) {
        if (memoryCleared > 1) {
            ToastHelper.showMessage(getContext(),
                    getResources().getString(R.string.release_msg_mb, (int)memoryCleared));
        } else if (memoryCleared > 0) {
            int releaseKB = (int)(memoryCleared * 1024);
            if (releaseKB > 0) {
                ToastHelper.showMessage(getContext(),
                        getResources().getString(R.string.release_msg_kb, releaseKB));
            } else {
                ToastHelper.showMessage(getContext(),
                        getResources().getString(R.string.release_msg_nothing));
            }
        } else {
            ToastHelper.showMessage(getContext(),
                    getResources().getString(R.string.release_msg_nothing));
        }
    }

    // Perry: Optimizing memory info: start
    @Override
    public void updateMemoryInfo() {
        if (mClearing.get()) {
            return;
        }

        float available = mMIM.getAvailable(GB);
        String text;
            if (available >= 1.0f) {
                text = getResources().getString(R.string.memory_info_gb_fmt,
                        available, mMIM.getTotal(GB));
            } else {
                text = getResources().getString(R.string.memory_info_mb_fmt,
                        (int)mMIM.getAvailable(MB), mMIM.getTotal(GB));
            }
        mInfo.setText(text);
        updateMemoryInfoLayout();
    }

    private void updateMemoryInfoLayout() {
        if (getWidth() == 0) {
            return;
        }

        String text = mInfo.getText().toString();
        int index = text.indexOf('|');
        // liuzuo: fix display error in ar layout:start
        if(index==-1){
            index = text.indexOf('/');
        }
        // liuzuo: fix display error in ar layout:start
        TextPaint paint = mInfo.getPaint();
        paint.getTextBounds(text, 0, index+1, mBounds);
        int left = getWidth() / 2 - mBounds.width();
        LayoutParams lp = (LayoutParams) mInfo.getLayoutParams();
        lp.width = getWidth();
        // Perry: fix display error in rtl layout:start
        lp.gravity = Gravity.START;
        lp.setMarginStart(left);
        // Perry: fix display error in rtl layout:end
        mInfo.setLayoutParams(lp);
        mButton.setWaterLevelRatio(mMIM.getUsedRate());
        mButton.setContentType(WaveView.ContentType.CLEAR);
    }
    // Perry: Optimizing memory info: end
}
