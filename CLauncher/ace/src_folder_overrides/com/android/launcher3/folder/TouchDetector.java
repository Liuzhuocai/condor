package com.android.launcher3.folder;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class TouchDetector {
    public enum TouchState {
        RESET,
        HORIZONTAL_SCROLLING,
        VERTICAL_SCROLLING,
    }

    private float mLastMotionX;
    private float mLastMotionY;
    private int mTouchSlop;
    private TouchState mTouchState = TouchState.RESET;

    public TouchDetector(Context context) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    public void onInterceptTouchEvent(final MotionEvent ev) {
        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final float x = ev.getX();
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                boolean xMoved = xDiff > mTouchSlop;

                final float y = ev.getY();
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                boolean yMoved = yDiff > mTouchSlop;

                if (xMoved) {
                    if (xDiff >= yDiff)
                        mTouchState = TouchState.HORIZONTAL_SCROLLING;
                    mLastMotionX = x;
                }

                if (yMoved) {
                    if (yDiff > xDiff)
                        mTouchState = TouchState.VERTICAL_SCROLLING;
                    mLastMotionY = y;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mTouchState = TouchState.RESET;
                mLastMotionY = ev.getY();
                mLastMotionX = ev.getX();
                break;
            default:
                break;
        }
    }

    public TouchState getTouchState() {
        return mTouchState;
    }

    public boolean isScrolling() {
        return mTouchState != TouchState.RESET;
    }

    public boolean isVerticalScrolling() {
        return mTouchState == TouchState.VERTICAL_SCROLLING;
    }

    public boolean isHorizontalScrolling() {
        return mTouchState == TouchState.HORIZONTAL_SCROLLING;
    }
}
