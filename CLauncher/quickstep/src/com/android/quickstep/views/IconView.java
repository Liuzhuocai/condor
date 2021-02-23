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
package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.condor.launcher.locktask.LockDrawable;
/**
 * A view which draws a drawable stretched to fit its size. Unlike ImageView, it avoids relayout
 * when the drawable changes.
 */
public class IconView extends View {

    private Drawable mDrawable;
    // Perry: Implement Lock/Unlock task function: start
    private LockDrawable mLockDrawable;
    // Perry: Implement Lock/Unlock task function: end

    public IconView(Context context) {
        super(context);
        // Perry: Implement Lock/Unlock task function: start
        init();
        // Perry: Implement Lock/Unlock task function: end
    }

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Perry: Implement Lock/Unlock task function: start
        init();
        // Perry: Implement Lock/Unlock task function: end
    }

    public IconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Perry: Implement Lock/Unlock task function: start
        init();
        // Perry: Implement Lock/Unlock task function: end
    }

    // Perry: Implement Lock/Unlock task function: start
    private void init() {
        mLockDrawable = new LockDrawable(getContext());
    }
    // Perry: Implement Lock/Unlock task function: end

    public void setDrawable(Drawable d) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
            // Perry: Implement Lock/Unlock task function: start
            mLockDrawable.setCallback(null);
            // Perry: Implement Lock/Unlock task function: end
        }
        mDrawable = d;
        if (mDrawable != null) {
            mDrawable.setCallback(this);
            mDrawable.setBounds(0, 0, getWidth(), getHeight());

            // Perry: Implement Lock/Unlock task function: start
            mLockDrawable.setCallback(this);
            mLockDrawable.updateBounds(getWidth(), getHeight());
            // Perry: Implement Lock/Unlock task function: end
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDrawable != null) {
            mDrawable.setBounds(0, 0, w, h);
            // Perry: Implement Lock/Unlock task function: start
            mLockDrawable.updateBounds(w, h);
            // Perry: Implement Lock/Unlock task function: end
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mDrawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable drawable = mDrawable;
        if (drawable != null && drawable.isStateful()
                && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
        // Perry: Implement Lock/Unlock task function: start
        if (mLockDrawable != null) {
            mLockDrawable.draw(canvas);
        }
        // Perry: Implement Lock/Unlock task function: end
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Perry: Implement Lock/Unlock task function: start
    public void setLocked(boolean locked) {
        mLockDrawable.setLocked(locked);
        invalidate();
    }
    // Perry: Implement Lock/Unlock task function: end
}
