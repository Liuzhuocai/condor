/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.pageindicators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;

/**
 * {@link PageIndicator} which shows dots per page. The active page is shown with the current
 * accent color.
 */
public class PageIndicatorDots extends View implements PageIndicator {

    private static final float SHIFT_PER_ANIMATION = 0.5f;
    private static final float SHIFT_THRESHOLD = 0.1f;
    private static final long ANIMATION_DURATION = 150;

    private static final int ENTER_ANIMATION_START_DELAY = 300;
    private static final int ENTER_ANIMATION_STAGGERED_DELAY = 150;
    private static final int ENTER_ANIMATION_DURATION = 400;

    // This value approximately overshoots to 1.5 times the original size.
    private static final float ENTER_ANIMATION_OVERSHOOT_TENSION = 4.9f;

    private static final RectF sTempRect = new RectF();

    private static final Property<PageIndicatorDots, Float> CURRENT_POSITION
            = new Property<PageIndicatorDots, Float>(float.class, "current_position") {
        @Override
        public Float get(PageIndicatorDots obj) {
            return obj.mCurrentPosition;
        }

        @Override
        public void set(PageIndicatorDots obj, Float pos) {
            obj.mCurrentPosition = pos;
            obj.invalidate();
            obj.invalidateOutline();
        }
    };

    private final Paint mCirclePaint;
    private final float mDotRadius;
    private final float mDotRatio;
    private final int mActiveColor;
    private final int mInActiveColor;
    private final boolean mIsRtl;
    private final Drawable mMinusOneIcon;
    private final int mDotSize;

    private int mNumPages;
    private int mActivePage;

    /**
     * The current position of the active dot including the animation progress.
     * For ex:
     *   0.0  => Active dot is at position 0
     *   0.33 => Active dot is at position 0 and is moving towards 1
     *   0.50 => Active dot is at position [0, 1]
     *   0.77 => Active dot has left position 0 and is collapsing towards position 1
     *   1.0  => Active dot is at position 1
     */
    private float mCurrentPosition;
    private float mFinalPosition;
    private ObjectAnimator mAnimator;

    private float[] mEntryAnimationRadiusFactors;
    private final Launcher mLauncher;
    // Perry: Only show minus one icon on workspace: start
    private boolean mIsMinusOneIconVisible = false;
    // Perry: Only show minus one icon on workspace: end

    public PageIndicatorDots(Context context) {
        this(context, null);
    }

    public PageIndicatorDots(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorDots(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Perry: desktop switch function: start
        mLauncher = Launcher.getLauncher(context);
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Style.FILL);
        setOutlineProvider(new MyOutlineProver());
        mIsRtl = Utilities.isRtl(getResources());

        int defaultActiveColor = Themes.getAttrColor(context, R.attr.workspaceTextColor);
        int defaultInActiveColor = Utilities.weakenColor(defaultActiveColor);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageIndicatorDots);
        mDotRadius = a.getDimension(R.styleable.PageIndicatorDots_dotsRadius,
                getResources().getDimension(R.dimen.page_indicator_dot_size) / 2);
        mDotRatio = a.getDimension(R.styleable.PageIndicatorDots_dotsRatio,
                mDotRadius);
        mActiveColor = a.getColor(R.styleable.PageIndicatorDots_activeColor,
                defaultActiveColor);
        mInActiveColor = a.getColor(R.styleable.PageIndicatorDots_inActiveColor,
                defaultInActiveColor);
        Drawable d = a.getDrawable(R.styleable.PageIndicatorDots_minusOneIcon);
        if (d == null) {
            d = context.getResources().getDrawable(R.drawable.ic_minus_one_indicator);
        }
        mMinusOneIcon = d;
        mDotSize = getResources().getDimensionPixelSize(R.dimen.page_indicator_dot_size);
        mMinusOneIcon.setBounds(0, 0, mDotSize, mDotSize);
        mMinusOneIcon.setTint(defaultActiveColor);
        a.recycle();
        // Perry: desktop switch function: end
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        if (mNumPages > 1) {
            if (mIsRtl) {
                currentScroll = totalScroll - currentScroll;
            }
            int scrollPerPage = totalScroll / (mNumPages - 1);
            int pageToLeft = scrollPerPage == 0 ? 0 : currentScroll / scrollPerPage;
            int pageToLeftScroll = pageToLeft * scrollPerPage;
            int pageToRightScroll = pageToLeftScroll + scrollPerPage;

            float scrollThreshold = SHIFT_THRESHOLD * scrollPerPage;
            // Perry: Cyclic sliding of desktop pages: start
            if (currentScroll < 0) {
                animateToPosition(pageToLeft - 1);
            } else if (currentScroll < pageToLeftScroll + scrollThreshold) {
                // scroll is within the left page's threshold
                animateToPosition(pageToLeft);
            } else if (currentScroll > pageToRightScroll - scrollThreshold) {
                // scroll is far enough from left page to go to the right page
                animateToPosition(pageToLeft + 1);
            } else {
                // scroll is between left and right page
                animateToPosition(pageToLeft + SHIFT_PER_ANIMATION);
            }
            // Perry: Cyclic sliding of desktop pages: end
        }
    }

    private void animateToPosition(float position) {
        mFinalPosition = position;
        if (Math.abs(mCurrentPosition - mFinalPosition) < SHIFT_THRESHOLD) {
            mCurrentPosition = mFinalPosition;
        }
        if (mAnimator == null && Float.compare(mCurrentPosition, mFinalPosition) != 0) {
            // Perry: Cyclic sliding of desktop pages: start
            /*float positionForThisAnim = mCurrentPosition > mFinalPosition ?
                    mCurrentPosition - SHIFT_PER_ANIMATION : mCurrentPosition + SHIFT_PER_ANIMATION;
            mAnimator = ObjectAnimator.ofFloat(this, CURRENT_POSITION, positionForThisAnim);
            mAnimator.addListener(new AnimationCycleListener());
            mAnimator.setDuration(ANIMATION_DURATION);
            mAnimator.start();*/
			 if (mFinalPosition < 0 || mFinalPosition > (mNumPages-1)) {
                mFinalPosition = mActivePage;
            }
            CURRENT_POSITION.set(this, mFinalPosition);
            // Perry: Cyclic sliding of desktop pages: end
        }
    }

    public void stopAllAnimations() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mFinalPosition = mActivePage;
        CURRENT_POSITION.set(this, mFinalPosition);
    }

    /**
     * Sets up up the page indicator to play the entry animation.
     * {@link #playEntryAnimation()} must be called after this.
     */
    public void prepareEntryAnimation() {
        mEntryAnimationRadiusFactors = new float[mNumPages];
        invalidate();
    }

    public void playEntryAnimation() {
        int count  = mEntryAnimationRadiusFactors.length;
        if (count == 0) {
            mEntryAnimationRadiusFactors = null;
            invalidate();
            return;
        }

        Interpolator interpolator = new OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION);
        AnimatorSet animSet = new AnimatorSet();
        for (int i = 0; i < count; i++) {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1).setDuration(ENTER_ANIMATION_DURATION);
            final int index = i;
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mEntryAnimationRadiusFactors[index] = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            anim.setInterpolator(interpolator);
            anim.setStartDelay(ENTER_ANIMATION_START_DELAY + ENTER_ANIMATION_STAGGERED_DELAY * i);
            animSet.play(anim);
        }

        animSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                mEntryAnimationRadiusFactors = null;
                invalidateOutline();
                invalidate();
            }
        });
        animSet.start();
    }

    @Override
    public void setActiveMarker(int activePage) {
        if (mActivePage != activePage) {
            mActivePage = activePage;
        }
    }

    @Override
    public void setMarkersCount(int numMarkers) {
        mNumPages = numMarkers;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Add extra spacing of mDotRadius on all sides so than entry animation could be run.
        // Perry: desktop switch function: start
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ?
                MeasureSpec.getSize(widthMeasureSpec) : (int) (((mNumPages * 3 + 2) * mDotRadius)
                + getMinusOneIconWidth());
        // Perry: desktop switch function: end
        int height= MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ?
                MeasureSpec.getSize(heightMeasureSpec) : (int) (4 * mDotRadius);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw all page indicators;
        float circleGap = 3 * mDotRadius;
        // Perry: desktop switch function: start
        float width = mNumPages * circleGap + getMinusOneIconWidth();
        float startX = (getWidth() - width + mDotRadius) / 2;

        float x = startX + mDotRadius;
        float y = canvas.getHeight() / 2;

        if (!mIsRtl && minusOneIconVisible()) {
            canvas.save();
            canvas.translate(x - mDotSize / 2, y - mDotSize / 2);
            mMinusOneIcon.draw(canvas);
            canvas.restore();
            x += circleGap;
        }

        if (mEntryAnimationRadiusFactors != null) {
            // During entry animation, only draw the circles
            if (mIsRtl) {
                x = getWidth() - x;
                circleGap = -circleGap;
            }
            for (int i = 0; i < mEntryAnimationRadiusFactors.length; i++) {
                mCirclePaint.setColor(i == mActivePage ? mActiveColor : mInActiveColor);
                canvas.drawCircle(x, y, mDotRatio * mEntryAnimationRadiusFactors[i], mCirclePaint);
                x += circleGap;
            }
        } else {
            mCirclePaint.setColor(mInActiveColor);
            for (int i = 0; i < mNumPages; i++) {
                canvas.drawCircle(x, y, mDotRatio, mCirclePaint);
                x += circleGap;
            }

            mCirclePaint.setColor(mActiveColor);
            canvas.drawRoundRect(getActiveRect(), mDotRatio, mDotRatio, mCirclePaint);
        }

        if (mIsRtl && minusOneIconVisible()) {
            canvas.save();
            canvas.translate(x - mDotSize / 2, y - mDotSize / 2);
            mMinusOneIcon.draw(canvas);
            canvas.restore();
        }
        // Perry: desktop switch function: end
    }

    private RectF getActiveRect() {
        // Perry: desktop switch function: start
        float startCircle = (int) mCurrentPosition;
        float delta = mCurrentPosition - startCircle;
        float diameter = 2 * mDotRatio;
        float circleGap = 3 * mDotRadius;
        float width = mNumPages * circleGap + getMinusOneIconWidth();
        float startX = (getWidth() - width + mDotRadius) / 2 + getMinusOneIconWidth() + mDotRadius - mDotRatio;

        sTempRect.top = getHeight() * 0.5f - mDotRatio;
        sTempRect.bottom = getHeight() * 0.5f + mDotRatio;
        // Perry: desktop switch function: end
        sTempRect.left = startX + startCircle * circleGap;
        sTempRect.right = sTempRect.left + diameter;

        if (delta < SHIFT_PER_ANIMATION) {
            // dot is capturing the right circle.
            sTempRect.right += delta * circleGap * 2;
        } else {
            // Dot is leaving the left circle.
            sTempRect.right += circleGap;

            delta -= SHIFT_PER_ANIMATION;
            sTempRect.left += delta * circleGap * 2;
        }

        if (mIsRtl) {
            float rectWidth = sTempRect.width();
            sTempRect.right = getWidth() - sTempRect.left;
            sTempRect.left = sTempRect.right - rectWidth;
        }
        return sTempRect;
    }

    private class MyOutlineProver extends ViewOutlineProvider {

        @Override
        public void getOutline(View view, Outline outline) {
            if (mEntryAnimationRadiusFactors == null) {
                RectF activeRect = getActiveRect();
                outline.setRoundRect(
                        (int) activeRect.left,
                        (int) activeRect.top,
                        (int) activeRect.right,
                        (int) activeRect.bottom,
                        mDotRadius
                );
            }
        }
    }

    /**
     * Listener for keep running the animation until the final state is reached.
     */
    private class AnimationCycleListener extends AnimatorListenerAdapter {

        private boolean mCancelled = false;

        @Override
        public void onAnimationCancel(Animator animation) {
            mCancelled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mCancelled) {
                mAnimator = null;
                animateToPosition(mFinalPosition);
            }
        }
    }

    // Perry: desktop switch function: start
    @Override
    public void setShouldAutoHide(boolean shouldAutoHide) {
    }

    private float getMinusOneIconWidth() {
        return minusOneIconVisible() ? 3 * mDotRadius : 0;
    }

    private boolean minusOneIconVisible() {
        return mIsMinusOneIconVisible && mLauncher.minusOneEnable();
    }

    public void setMinusOneIconVisible(boolean visible) {
        mIsMinusOneIconVisible = visible;
    }
    // Perry: desktop switch function: end
}
