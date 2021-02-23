package com.condor.launcher;

import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.android.launcher3.SettingsActivity;
import com.condor.launcher.util.Utils;

/**
 * Created by Perry on 19-1-25
 */
public class CLauncherFastSettingsActivity extends SettingsActivity {
    @Override
    protected PreferenceFragment getNewFragment() {
        return CLauncherSettingsActivity.CLauncherSettingsFragment.create(CLauncherSettingsActivity.FAST_ACTIVITY);
    }

    @Override
    public void onAttachedToWindow() {
        final Window window = getWindow();
        final WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = Utils.getSize(window).x - 2 * Utils.dp2px(this, 10);
        window.setAttributes(lp);
        setFinishOnTouchOutside(true);
        super.onAttachedToWindow();
    }
}
