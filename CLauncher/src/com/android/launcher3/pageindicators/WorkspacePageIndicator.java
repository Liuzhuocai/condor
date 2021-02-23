package com.android.launcher3.pageindicators;

import android.content.Context;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;

/**
 * Created by Perry on 19-1-14
 */
public class WorkspacePageIndicator extends FrameLayout implements Insettable, PageIndicator {
    private final Launcher mLauncher;
    private final PageIndicator mIndicator;

    public WorkspacePageIndicator(@NonNull Context context) {
        this(context, null);
    }

    public WorkspacePageIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspacePageIndicator(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        if (DesktopModeHelper.isDefaultMode()) {
            mIndicator = (PageIndicator) LayoutInflater.from(context).inflate(R.layout.page_indicator_dots, null);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            // Perry: Only show minus one icon on workspace: start
            ((PageIndicatorDots)mIndicator).setMinusOneIconVisible(true);
            // Perry: Only show minus one icon on workspace: end
            addView((View) mIndicator, lp);
        } else {
            mIndicator = (PageIndicator) LayoutInflater.from(context).inflate(R.layout.page_indicator_line, null);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    Utilities.pxFromDp(12, context.getResources().getDisplayMetrics()));
            addView((View) mIndicator, lp);
        }
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();

        if (grid.isVerticalBarLayout()) {
            Rect padding = grid.workspacePadding;
            lp.leftMargin = padding.left + grid.workspaceCellPaddingXPx;
            lp.rightMargin = padding.right + grid.workspaceCellPaddingXPx;
            lp.bottomMargin = 0;
        } else {
            lp.leftMargin = lp.rightMargin = 0;
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            lp.bottomMargin = grid.hotseatBarSizePx + insets.bottom;
        }
        setLayoutParams(lp);
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        mIndicator.setScroll(currentScroll, totalScroll);
    }

    @Override
    public void setActiveMarker(int activePage) {
        mIndicator.setActiveMarker(activePage);
    }

    @Override
    public void setMarkersCount(int numMarkers) {
        mIndicator.setMarkersCount(numMarkers);
    }

    @Override
    public void setShouldAutoHide(boolean shouldAutoHide) {
        mIndicator.setShouldAutoHide(shouldAutoHide);
    }

    public void pauseAnimations() {
        if (mIndicator instanceof PageIndicatorLine) {
            ((PageIndicatorLine)mIndicator).pauseAnimations();
        }
    }

    public void skipAnimationsToEnd() {
        if (mIndicator instanceof PageIndicatorLine) {
            ((PageIndicatorLine)mIndicator).skipAnimationsToEnd();
        }
    }
}
