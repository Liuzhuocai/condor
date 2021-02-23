package com.condor.launcher.editmode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.views.OptionsPopupView;
import com.condor.launcher.CLauncherFastSettingsActivity;
import com.condor.launcher.CondorFeatureFlags;

/**
 * Created by Perry on 19-1-18
 */
public class EditNormalPanel extends LinearLayout implements Insettable {
    private final Launcher mLauncher;
    public EditNormalPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        findViewById(R.id.wallpaper_button).setOnClickListener(OptionsPopupView::startWallpaperPicker);
        findViewById(R.id.widget_button).setOnClickListener(OptionsPopupView::onWidgetsClicked);
        // Perry: adjust settings UI: start
        findViewById(R.id.settings_button).setOnClickListener(v-> {
            if (CondorFeatureFlags.SUPPORT_FAST_SETTINGS_ACTIVITY) {
                mLauncher.startActivity(new Intent(getContext(), CLauncherFastSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                mLauncher.startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                        .setPackage(getContext().getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }

        });
        // Perry: adjust settings UI: end
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        int visibleChildCount = getVisibleChildCount();
        int totalItemWidth = visibleChildCount * grid.editModeBarItemWidthPx;
        int maxWidth = totalItemWidth + (visibleChildCount - 1) * grid.editModeBarSpacerWidthPx;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.width = Math.min(grid.availableWidthPx, maxWidth);
        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    private int getVisibleChildCount() {
        int visibleChildren = 0;
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getVisibility() != View.GONE) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }
}
