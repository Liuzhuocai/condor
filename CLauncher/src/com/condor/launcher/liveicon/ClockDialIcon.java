package com.condor.launcher.liveicon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.icu.util.Calendar;

import com.android.launcher3.ItemInfo;
import com.condor.launcher.util.CalendarHelper;

/**
 * Created by Perry on 19-2-18
 */
public class ClockDialIcon extends BaseLiveIcon<LiveIcons.ClockDial> {
    private Bitmap mHourIcon;
    private Bitmap mMinuteIcon;
    private Bitmap mSecondIcon;

    public ClockDialIcon(Context context, LiveIcons.BaseConfig config) {
        super(context, config);
        mHourIcon = getBitmap(mConfig.getHour());
        mMinuteIcon = getBitmap(mConfig.getMinute());
        mSecondIcon = getBitmap(mConfig.getSecond());
    }

    @Override
    public boolean supportFromTheme() {
        return super.supportFromTheme() && mHourIcon != null
                && mMinuteIcon != null;
    }

    @Override
    public BaseLiveDrawable newLiveDrawable(ItemInfo info) {
        return new ClockDialDrawable(info);
    }

    @Override
    public boolean hasSecond() {
        return false;
    }

    final class ClockDialDrawable extends BaseLiveDrawable {
        private final Camera mCamera;
        private final Matrix mMatrix;

        private final float mScale;
        private final float mIconCenterX;
        private final float mIconCenterY;

        private final float mCenterX;
        private final float mCenterY;

        private int mSecond;
        private int mMinute;
        private int mHour;

        public ClockDialDrawable(ItemInfo info) {
            super(info);
            mCamera = new Camera();
            mMatrix = new Matrix();

            mCenterX = mBitmap.getWidth() / 2.0f;
            mCenterY = mBitmap.getHeight() / 2.0f;

            mScale = mBitmap.getWidth() / (float)mMinuteIcon.getWidth();
            mIconCenterX = mMinuteIcon.getWidth() / 2.0f;
            mIconCenterY = mMinuteIcon.getHeight() / 2.0f;

            mSecond = CalendarHelper.getSecond();
            mMinute = CalendarHelper.getMinute();
            mHour = CalendarHelper.getHour();
        }

        @Override
        protected void drawInternal(Canvas canvas, Rect bounds) {
            super.drawInternal(canvas, bounds);
            float scaleX = getBounds().width() / (float)mBitmap.getWidth();
            float scaleY = getBounds().height() / (float)mBitmap.getHeight();
            canvas.save();
            canvas.scale(scaleX, scaleY);
            drawTime(canvas);
            canvas.restore();
        }

        private void drawTime(Canvas canvas) {
            // Perry: fix clock pointer: start
            drawSecond(canvas);
            drawMinute(canvas);
            drawHour(canvas);
            // Perry: fix clock pointer: end
        }

        private void drawSecond(Canvas canvas) {
            if (!hasSecond() || mSecondIcon == null) {
                return;
            }

            final float second = CalendarHelper.getSecond();
            final float delta = (360.0f / 60) * second;

            canvas.save();
            rotate(delta);
            canvas.drawBitmap(mSecondIcon, mMatrix, mPaint);
            canvas.restore();
        }

        private void drawMinute(Canvas canvas) {
            final float minute = CalendarHelper.getMinute();
            final float delta = (360.0f / 60) * minute;

            canvas.save();
            rotate(delta);
            canvas.drawBitmap(mMinuteIcon, mMatrix, mPaint);
            canvas.restore();
        }

        private void drawHour(Canvas canvas) {
            final float hour = Calendar.getInstance().get(Calendar.HOUR);
            final float delta = (360.0f / 12) * (hour + CalendarHelper.getMinute() / 60.0f);

            canvas.save();
            rotate(delta);
            canvas.drawBitmap(mHourIcon, mMatrix, mPaint);
            canvas.restore();
        }

        private void rotate(float delta) {
            mMatrix.reset();
            mCamera.save();
            mCamera.rotateZ(-delta);
            mCamera.getMatrix(mMatrix);
            mCamera.restore();
            // Perry: fix clock pointer: start
            mMatrix.preScale(1, 1, mIconCenterX, mIconCenterX);
            mMatrix.preTranslate(-mIconCenterX, -mIconCenterY);
            mMatrix.postTranslate(mCenterX, mCenterY);
            mMatrix.postScale(mScale, mScale, mCenterX, mCenterX);
            // Perry: fix clock pointer: end
        }

        @Override
        protected boolean needRefresh() {
            if (mIsDisabled) {
                return false;
            }

            return (hasSecond() && mSecond != CalendarHelper.getSecond())
                    || mMinute != CalendarHelper.getMinute()
                    || mHour != CalendarHelper.getHour();
        }

        @Override
        public void start() {
            invalidateSelf();
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }
    }
}
