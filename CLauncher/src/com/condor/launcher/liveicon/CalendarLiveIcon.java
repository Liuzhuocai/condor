package com.condor.launcher.liveicon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import com.android.launcher3.ItemInfo;
import com.condor.launcher.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Perry on 19-2-18
 */
public class CalendarLiveIcon extends BaseLiveIcon<LiveIcons.Calendar> {

    public CalendarLiveIcon(Context context, LiveIcons.BaseConfig config) {
        super(context, config);
    }

    @Override
    public BaseLiveDrawable newLiveDrawable(ItemInfo info) {
        return new CalendarLiveDrawable(info);
    }

    final class CalendarLiveDrawable extends BaseLiveDrawable {
        private static final int ANIMATION_DURATION = 1000;
        private static final int RIGHT_ANGLE = 90;
        private static final int STRAIGHT_ANGLE = 180;
        private static final float GAP_BETWEEN_WEEK_AND_DAY = 19f;

        private final SimpleDateFormat mWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        private final SimpleDateFormat mDayFormat = new SimpleDateFormat("dd", Locale.getDefault());

        private final Paint mWeekdayPaint;
        private final Paint mDayPaint;
        private final ValueAnimator mAnimator;
        private final Matrix mMatrix;
        private final Camera mCamera;

        private final int mCenterX;
        private final int mCenterY;

        private float mRotationX;
        private String mOldDay;
        private String mOldWeekday;
        private float mWeekdayHeight;

        public CalendarLiveDrawable(ItemInfo info) {
            super(info);
            mRotationX = STRAIGHT_ANGLE;
            Date d = Calendar.getInstance().getTime();
            mOldDay = getFormatDay(d);
            mOldWeekday = getFormatWeekday(d);

            mMatrix = new Matrix();
            mCamera = new Camera();

            mCenterX = mBitmap.getWidth();
            mCenterY = mBitmap.getHeight();

            mWeekdayPaint = mConfig.getWeekText().toPaint(mContext,
                    Paint.FAKE_BOLD_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            mWeekdayHeight = Utils.getTextHeight(mWeekdayPaint);
            mDayPaint = mConfig.getDayText().toPaint(mContext, Paint.ANTI_ALIAS_FLAG);

            mAnimator = ValueAnimator.ofFloat(0, 1);
            mAnimator.setDuration(ANIMATION_DURATION);
            mAnimator.addUpdateListener((animation)-> {
                mRotationX = animation.getAnimatedFraction() * STRAIGHT_ANGLE;
                invalidateSelf();
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    Date d = Calendar.getInstance().getTime();
                    mOldDay = getFormatDay(d);
                    mOldWeekday = getFormatWeekday(d);
                    mRotationX = STRAIGHT_ANGLE;
                    invalidateSelf();
                    super.onAnimationEnd(animation);
                }
            });
        }

        @Override
        protected void drawInternal(Canvas canvas, Rect bounds) {
            super.drawInternal(canvas, bounds);
            float scaleX = getBounds().width() / (float)mBitmap.getWidth();
            float scaleY = getBounds().height() / (float)mBitmap.getHeight();
            canvas.save();
            canvas.scale(scaleX, scaleY);
            drawAnim(canvas);
            canvas.restore();
        }

        private void drawAnim(Canvas canvas) {
            if (mIsDisabled) {
                drawDate(canvas, Calendar.getInstance().getTime());
                return;
            }

            if (mRotationX < STRAIGHT_ANGLE) {
                canvas.save();
                canvas.clipRect(0, 0, mCenterX, mCenterY / 2);
                drawDate(canvas, Calendar.getInstance().getTime());
                canvas.restore();

                canvas.save();
                canvas.clipRect(0, mCenterY / 2, mCenterX, mCenterY);
                drawDate(canvas, mOldWeekday, mOldDay);
                canvas.restore();

                Rect dst = new Rect(0, 0, mCenterX, mCenterY);

                canvas.save();
                if (mRotationX > RIGHT_ANGLE) {
                    rotate(mRotationX - STRAIGHT_ANGLE);
                    canvas.clipRect(0, mCenterY / 2, mCenterX, mCenterY);
                    canvas.concat(mMatrix);
                    canvas.drawBitmap(mBitmap, null, dst, mPaint);
                    drawDate(canvas, Calendar.getInstance().getTime());
                } else {
                    rotate(-mRotationX);
                    canvas.clipRect(0, 0, mCenterX, mCenterY / 2);
                    canvas.concat(mMatrix);
                    canvas.drawBitmap(mBitmap, null, dst, mPaint);
                    drawDate(canvas, mOldWeekday, mOldDay);
                }
                canvas.restore();
            } else {
                drawDate(canvas, Calendar.getInstance().getTime());
            }
        }

        @Override
        protected void onUpdateFilter(ColorFilter filter) {
            super.onUpdateFilter(filter);
            mDayPaint.setColorFilter(filter);
            mWeekdayPaint.setColorFilter(filter);
        }

        private void rotate(float delta) {
            mMatrix.reset();
            mCamera.save();
            mCamera.rotateX(delta);
            mCamera.getMatrix(mMatrix);
            mCamera.restore();

            mMatrix.preTranslate(-mCenterX / 2f, -mCenterY / 2f);
            mMatrix.postTranslate(mCenterX / 2f, mCenterY / 2f);
        }

        private void drawDate(Canvas canvas, Date date) {
            String weekdayText = getFormatWeekday(date);
            String dayText = getFormatDay(date);

            drawDate(canvas, weekdayText, dayText);
        }

        private void drawDate(Canvas canvas, String weekdayText, String dayText) {
            float density = Utils.getDensity(mContext);

            float weekdayOffsetX = (mBitmap.getWidth() - mWeekdayPaint.measureText(weekdayText)) / 2;
            float offsetY = mConfig.getPaddingTop();
            float weekdayOffsetY = (offsetY != -1) ? (mBitmap.getHeight() * offsetY): (float) mBitmap.getHeight() / 3.0f;
            canvas.drawText(weekdayText, weekdayOffsetX, weekdayOffsetY, mWeekdayPaint);

            float dayOffsetX = (mBitmap.getWidth() - mDayPaint.measureText(dayText)) / 2;
            float dayOffsetY = weekdayOffsetY + mWeekdayHeight + GAP_BETWEEN_WEEK_AND_DAY  * density;
            canvas.drawText(dayText, dayOffsetX, dayOffsetY, mDayPaint);
        }

        private String getFormatWeekday(Date date) {
            mWeekFormat.setTimeZone(TimeZone.getDefault());
            return mWeekFormat.format(date);
        }

        private String getFormatDay(Date date) {
            mDayFormat.setTimeZone(TimeZone.getDefault());
            return mDayFormat.format(date);
        }

        @Override
        public void start() {
            mAnimator.start();
        }

        @Override
        public void stop() {
            mAnimator.end();
        }

        @Override
        public boolean isRunning() {
            return mAnimator.isRunning();
        }

        @Override
        protected boolean needRefresh() {
            if (mIsDisabled) {
                return false;
            }

            Date d = Calendar.getInstance().getTime();

            return !mOldWeekday.equals(getFormatWeekday(d))
                    || !mOldDay.equals(getFormatDay(d));
        }
    }
}
