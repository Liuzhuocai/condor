package com.condor.launcher.locktask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;

/**
 * Created by Perry on 19-1-24
 */
public class LockDrawable extends Drawable {
    private final static int LOCK_SHADOW_RADIUS = 5;
    private final static int LOCK_BACKGROUND_COLOR = 0xff00a6ce;
    private final static int LOCK_BACKGROUND_SHADOW_COLOR = 0xff006983;
    private final Drawable mDrawable;
    private final Paint mBackgroundPaint;
    private final int mSize;
    private boolean mIsLocked;

    public LockDrawable(Context context) {
        mDrawable = context.getDrawable(R.drawable.ic_lock_flag);
        mSize = dp2px(context, 16);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(LOCK_BACKGROUND_COLOR);
        mBackgroundPaint.setShadowLayer(LOCK_SHADOW_RADIUS, 0, 0,
                LOCK_BACKGROUND_SHADOW_COLOR);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mIsLocked) {
            Rect bounds = mDrawable.getBounds();
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, mBackgroundPaint);
            mDrawable.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mDrawable.setAlpha(alpha);
        mBackgroundPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mDrawable.setColorFilter(colorFilter);
        mBackgroundPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return mDrawable.getOpacity();
    }

    public void setLocked(boolean locked) {
        mIsLocked = locked;
    }

    public void updateBounds(int width, int height) {
        int left = width - mSize - LOCK_SHADOW_RADIUS;
        int top  = height - mSize - LOCK_SHADOW_RADIUS;
        int right = width - LOCK_SHADOW_RADIUS;
        int bottom = height - LOCK_SHADOW_RADIUS;
        mDrawable.setBounds(left, top, right, bottom);
    }

    private int dp2px(Context context, int dp) {
        return Utilities.pxFromDp(dp, context.getResources().
                getDisplayMetrics());
    }
}
