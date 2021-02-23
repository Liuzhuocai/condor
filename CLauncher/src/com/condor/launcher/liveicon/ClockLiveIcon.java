package com.condor.launcher.liveicon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.DateFormat;

import com.android.launcher3.ItemInfo;
import com.condor.launcher.util.Utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Perry on 19-2-18
 */
public class ClockLiveIcon extends BaseLiveIcon<LiveIcons.ClockText> {
    public ClockLiveIcon(Context context, LiveIcons.BaseConfig config) {
        super(context, config);
    }

    @Override
    public BaseLiveDrawable newLiveDrawable(ItemInfo info) {
        return new ClockLiveDrawable(info);
    }

    final class ClockLiveDrawable extends BaseLiveDrawable {
        private static final int POINT_DURATION = 250;
        private static final String SEMICOLON = ":";

        private final Paint mMinutePaint;
        private final Paint mHourPaint;
        private final Paint mSemiPaint;

        private final float mHourPointWidth;
        private final float mMinutePointWidth;
        private final float mSemiPointWidth;

        private final int mMinuteHeight;
        private final int mHourHeight;
        private final int mSemiHeight;
        private final ValueAnimator mAnimator;

        private int[] mTimePoints = new int[] {0, 0, 0, 0};
        private float mFactor;

        public ClockLiveDrawable(ItemInfo info) {
            super(info);
            mMinutePaint = mConfig.getMinuteText().toPaint(mContext);
            mMinutePaint.setAntiAlias(true);

            mHourPaint = mConfig.getHourText().toPaint(mContext);
            mHourPaint.setAntiAlias(true);

            mSemiPaint = mConfig.getSemiText().toPaint(mContext);
            mSemiPaint.setAntiAlias(true);

            mHourPointWidth = mHourPaint.measureText("8");
            mMinutePointWidth = mMinutePaint.measureText("8");
            mSemiPointWidth = mSemiPaint.measureText(SEMICOLON);
            mMinuteHeight = Utils.getTextHeight(mMinutePaint);
            mHourHeight = Utils.getTextHeight(mHourPaint);
            mSemiHeight = Utils.getTextHeight(mSemiPaint);

            getTimePoints(mTimePoints);

            mAnimator = ValueAnimator.ofFloat(0, 1);
            mFactor = 0;
            mAnimator.addUpdateListener(animation -> {
                mFactor = animation.getAnimatedFraction();
                invalidateSelf();
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFactor = 0;
                    getTimePoints(mTimePoints);
                    invalidateSelf();
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

        private void drawTime(Canvas canvas) {
            int[] timePoints = getTimePoints();
            float offsetY = mConfig.getPaddingTop() == -1 ? mBitmap.getHeight() / 3f :
                    mConfig.getPaddingTop() * mBitmap.getHeight();

            float semiOffsetX = (mBitmap.getWidth() - mSemiPointWidth) / 2f;
            float semiOffsetY = (mBitmap.getHeight() - mSemiHeight) / 2f + offsetY;
            canvas.drawText(SEMICOLON, semiOffsetX, semiOffsetY, mSemiPaint);

            float pointTwoOffsetX = semiOffsetX - mHourPointWidth;
            float pointOneOffsetX = pointTwoOffsetX - mHourPointWidth;
            float hourOffsetY = (mBitmap.getHeight() - mHourHeight) / 2f + offsetY;

            drawPoint(canvas, pointOneOffsetX, hourOffsetY, mHourPointWidth,
                    timePoints[0], mHourPaint);

            drawPoint(canvas, pointTwoOffsetX, hourOffsetY, mHourPointWidth,
                    timePoints[1], mHourPaint);

            float pointThreeOffsetX = semiOffsetX + mSemiPointWidth;
            float pointFourOffsetX = pointThreeOffsetX + mMinutePointWidth;
            float minuteOffsetY = (mBitmap.getHeight() - mMinuteHeight) / 2f + offsetY;

            drawPoint(canvas, pointThreeOffsetX, minuteOffsetY, mMinutePointWidth,
                    timePoints[2], mMinutePaint);

            drawPoint(canvas, pointFourOffsetX, minuteOffsetY, mMinutePointWidth,
                    timePoints[3], mMinutePaint);
        }

        private void drawAnim(Canvas canvas) {
            if (mFactor <= 0) {
                drawTime(canvas);
            } else {
                if (mIsDisabled) {
                    drawTime(canvas);
                    return;
                }
                int[] timePoints = getTimePoints();
                int count = (int) (mAnimator.getDuration() / POINT_DURATION);
                float offsetY = mConfig.getPaddingTop() == -1 ? mBitmap.getHeight() / 3f :
                        mConfig.getPaddingTop() * mBitmap.getHeight();

                float semiOffsetX = (mBitmap.getWidth() - mSemiPointWidth) / 2f;
                float semiOffsetY = (mBitmap.getHeight() - mSemiHeight) / 2f + offsetY;
                canvas.drawText(SEMICOLON, semiOffsetX, semiOffsetY, mSemiPaint);

                float pointTwoOffsetX = semiOffsetX - mHourPointWidth;
                float pointOneOffsetX = pointTwoOffsetX - mHourPointWidth;
                float hourOffsetY = (mBitmap.getHeight() - mHourHeight) / 2f + offsetY;

                float pointOneDuration = (count - 3) / (float) count;
                if (count >= 4 && mFactor < pointOneDuration) {
                    mTimePoints[0] = timePoints[0];
                    drawPoint(canvas, pointOneOffsetX, hourOffsetY, mHourPointWidth, mHourHeight,
                            timePoints[0], mHourPaint, mFactor, pointOneDuration, count);
                } else {
                    drawPoint(canvas, pointOneOffsetX, hourOffsetY, mHourPointWidth,
                            mTimePoints[0], mHourPaint);
                }

                float pointTwoDuration = (count - 2) / (float) count;
                if (count >= 3 && mFactor > pointOneDuration && mFactor < pointTwoDuration) {
                    mTimePoints[1] = timePoints[1];
                    drawPoint(canvas, pointTwoOffsetX, hourOffsetY, mHourPointWidth, mHourHeight,
                            timePoints[1], mHourPaint, mFactor, pointTwoDuration, count);
                } else {
                    drawPoint(canvas, pointTwoOffsetX, hourOffsetY, mHourPointWidth,
                            mTimePoints[1], mHourPaint);
                }

                float pointThreeOffsetX = semiOffsetX + mSemiPointWidth;
                float pointFourOffsetX = pointThreeOffsetX + mMinutePointWidth;
                float minuteOffsetY = (mBitmap.getHeight() - mMinuteHeight) / 2f + offsetY;

                float pointThreeDuration = (count - 1) / (float) count;
                if (count >= 2 && mFactor > pointTwoDuration && mFactor < pointThreeDuration) {
                    mTimePoints[2] = timePoints[2];
                    drawPoint(canvas, pointThreeOffsetX, minuteOffsetY, mMinutePointWidth, mMinuteHeight,
                            timePoints[2], mMinutePaint, mFactor, pointThreeDuration, count);
                } else {
                    drawPoint(canvas, pointThreeOffsetX, minuteOffsetY, mMinutePointWidth,
                            mTimePoints[2], mMinutePaint);
                }

                if (count >= 1 && mFactor > pointThreeDuration && mFactor < 1f) {
                    mTimePoints[3] = timePoints[3];
                    drawPoint(canvas, pointFourOffsetX, minuteOffsetY, mMinutePointWidth, mMinuteHeight,
                            timePoints[3], mMinutePaint, mFactor, 1f, count);
                } else {
                    drawPoint(canvas, pointFourOffsetX, minuteOffsetY, mMinutePointWidth,
                            mTimePoints[3], mMinutePaint);
                }
            }
        }

        private void drawPoint(Canvas canvas, float offsetX, float offsetY, float pointWidth,
                               int point, Paint paint) {
            String pointText = String.valueOf(point);
            offsetX += (pointWidth - paint.measureText(pointText)) / 2f;
            canvas.drawText(pointText, offsetX, offsetY, paint);
        }

        private void drawPoint(Canvas canvas, float offsetX, float offsetY, float pointWidth, float pointHeight,
                               int point, Paint paint, float factor, float duration, int count) {
            String pointText = String.valueOf(point);
            float pointOffsetX = offsetX + (pointWidth - paint.measureText(pointText)) / 2f;

            canvas.drawText(pointText, pointOffsetX, offsetY + pointHeight / 3f * (factor - duration) * count, paint);
        }

        @Override
        protected void onUpdateFilter(ColorFilter filter) {
            super.onUpdateFilter(filter);
            mHourPaint.setColorFilter(filter);
            mMinutePaint.setColorFilter(filter);
            mSemiPaint.setColorFilter(filter);
        }

        private int[] getTimePoints() {
            return getTimePoints(null, null);
        }

        private int[] getTimePoints(int[] timePoints) {
            return getTimePoints(null, timePoints);
        }

        private int[] getTimePoints(Date date, int[] timePoints) {
            if (timePoints == null) {
                timePoints = new int[4];
            }
            Calendar calendar = Calendar.getInstance();
            if (date != null) {
                calendar.setTime(date);
            }
            int hour, minute;
            if (DateFormat.is24HourFormat(mContext)) {
                hour = calendar.get(Calendar.HOUR_OF_DAY);
            } else {
                hour = calendar.get(Calendar.HOUR);
            }
            minute = calendar.get(Calendar.MINUTE);

            timePoints[0] = hour / 10;
            timePoints[1] = hour % 10;
            timePoints[2] = minute / 10;
            timePoints[3] = minute % 10;

            return timePoints;
        }

        @Override
        public void start() {
            long duration = 0;
            int[] timePoints = getTimePoints();
            if (timePoints[0] != mTimePoints[0]) {
                duration = 4 * POINT_DURATION;
            } else if (timePoints[1] != mTimePoints[1]) {
                duration = 3 * POINT_DURATION;
            } else if (timePoints[2] != mTimePoints[2]) {
                duration = 2 * POINT_DURATION;
            } else if (timePoints[3] != mTimePoints[3]) {
                duration = POINT_DURATION;
            }

            if (duration != 0) {
                mAnimator.setDuration(duration);
                mAnimator.start();
            }
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
            return !mIsDisabled;
        }
    }
}
