package com.condor.launcher.frozen;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;

import com.android.launcher3.AppInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.condor.launcher.util.ToastHelper;

import java.util.List;
import java.util.stream.Collectors;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

public class FrozenAppsManager {
    private static final String TAG = "FrozenAppsManager";
    private static FrozenAppsManager sInstance = null;
    private final PackageManager mPm;

    private FrozenAppsManager() {
        mPm = LauncherAppState.getInstanceNoCreate().
                getContext().getPackageManager();
    }

    public static FrozenAppsManager obtain() {
        if (sInstance == null) {
            sInstance = new FrozenAppsManager();
        }

        return sInstance;
    }

    public List<ActivityInfo> getFrozenActivityList() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return mPm.queryIntentActivities(mainIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
                .stream().map(info -> info.activityInfo).filter(this::notSystemActivity).
                        filter(this::activityDisable).collect(Collectors.toList());
    }

    public List<ActivityInfo> getFrozenActivity(String packageName) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        return mPm.queryIntentActivities(mainIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
                .stream().map(info -> info.activityInfo).filter(this::notSystemActivity).
                        filter(this::activityDisable).collect(Collectors.toList());
    }

    public boolean activityDisable(ActivityInfo info) {
        return activityDisable(new ComponentName(info.packageName, info.name));
    }

    public boolean activityDisable(LauncherActivityInfo info) {
        return activityDisable(info.getComponentName());
    }

    public boolean activityDisable(AppInfo info) {
        return activityDisable(info.componentName);
    }

    private boolean activityDisable(ComponentName componentName) {
        return mPm.getComponentEnabledSetting(componentName)
                == COMPONENT_ENABLED_STATE_DISABLED;
    }

    private boolean notSystemActivity(ActivityInfo info) {
        return (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
    }

    public void toggleFrozen(AppInfo info) {
        try {
            if (info.isFrozen()) {
                mPm.setComponentEnabledSetting(info.componentName, COMPONENT_ENABLED_STATE_DEFAULT,
                        0);
            } else {
                mPm.setComponentEnabledSetting(info.componentName, COMPONENT_ENABLED_STATE_DISABLED,
                        0);
            }
        }catch (SecurityException e){
            e.printStackTrace();
            ToastHelper.showMessage(LauncherAppState.getInstanceNoCreate().getContext(), R.string.msg_lose_permission);
        }

    }
}
