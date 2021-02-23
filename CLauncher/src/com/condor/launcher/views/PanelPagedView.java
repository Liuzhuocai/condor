package com.condor.launcher.views;

import android.content.Context;
import android.util.AttributeSet;

import com.android.launcher3.PagedView;
import com.android.launcher3.pageindicators.PageIndicatorLine;

/**
 * Created by Perry on 19-1-29
 */
public class PanelPagedView extends PagedView<PageIndicatorLine> {
    public PanelPagedView(Context context) {
        super(context);
    }

    public PanelPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PanelPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mPageIndicator.setScroll(l, mMaxScrollX);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mPageIndicator.setVisibility(visibility);
        // Perry: Optimizing page indicator: start
        if (visibility == VISIBLE) {
            setCurrentPage(0);
            mPageIndicator.setActiveMarker(0);
            mPageIndicator.setMarkersCount(getChildCount());
            mPageIndicator.setScroll(getScrollX(), mMaxScrollX);
        }
        // Perry: Optimizing page indicator: end
    }
}
