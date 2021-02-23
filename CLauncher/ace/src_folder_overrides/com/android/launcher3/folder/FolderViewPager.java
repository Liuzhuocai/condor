package com.android.launcher3.folder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * Created by wangqing on 11/26/15.
 */
public class FolderViewPager extends ViewPager {
    private final TouchDetector mTouchDetector;
    private Folder mFolder;
    private boolean mIsScrolling;

    public FolderViewPager(Context context) {
        this(context, null);
    }

    public FolderViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchDetector = new TouchDetector(context);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        mTouchDetector.onInterceptTouchEvent(ev);
        if (mTouchDetector.isHorizontalScrolling()) {
            return true;
        }

        return super.onInterceptTouchEvent(ev);
    }

    public boolean isScrolling() {
        return mIsScrolling;
    }

    public void setCurrentFolder(Folder folder) {
        mFolder = folder;
    }

    public Folder getCurrentFolder() {
        return mFolder;
    }

    public void setScrollState(boolean scrolling) {
        mIsScrolling = scrolling;
    }
}
