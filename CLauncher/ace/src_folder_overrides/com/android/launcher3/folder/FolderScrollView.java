package com.android.launcher3.folder;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.folder.Folder;

public class FolderScrollView extends SpringScrollView {
    private Folder mFolder;
    private Runnable mScrollEndRunnable = null;
    public FolderScrollView(Context context) {
        this(context, null);
    }

    public FolderScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        setNestedScrollingEnabled(true);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mFolder.updateScrollbar();
    }

    public void scrollToPosition(int position, Runnable r) {
        if (mScrollEndRunnable != null) {
            mScrollEndRunnable.run();
            mScrollEndRunnable = null;
        }

        mScrollEndRunnable = r;
        if (position != getScrollY()) {
            smoothScrollTo(0, position);
        } else {
            if (mScrollEndRunnable != null) {
                mScrollEndRunnable.run();
                mScrollEndRunnable = null;
            }
        }
    }

    @Override
    protected void onScrollEnd() {
        if (mScrollEndRunnable != null) {
            mScrollEndRunnable.run();
            mScrollEndRunnable = null;
        }
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        if (getScrollY() > 0) {
            return false;
        }
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (getScrollY() > 0) {
            return false;
        }
        return super.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (getScrollY() > 0) {
            return false;
        }
        return super.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
        if (getScrollY() > 0) {
            return false;
        }
        return super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }
}
