/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;
import com.condor.launcher.frozen.FrozenAppsManager;

/**
 * Represents an app in AllAppsView.
 */
public class AppInfo extends ItemInfoWithIcon {

    /**
     * The intent used to start the application.
     */
    public Intent intent;

    public ComponentName componentName;

    public AppInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
    }

    @Override
    public Intent getIntent() {
        return intent;
    }

    /**
     * Must not hold the Context.
     */
    public AppInfo(Context context, LauncherActivityInfo info, UserHandle user) {
        this(info, user, UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }

    // Perry: Implement frozen apps: start
    public AppInfo(Context context, ActivityInfo info, UserHandle user) {
        this(info, user, UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }
    // Perry: Implement frozen apps: end

    public AppInfo(LauncherActivityInfo info, UserHandle user, boolean quietModeEnabled) {
        this.componentName = info.getComponentName();
        this.container = ItemInfo.NO_ID;
        this.user = user;
        intent = makeLaunchIntent(info);

        if (quietModeEnabled) {
            runtimeStatusFlags |= FLAG_DISABLED_QUIET_USER;
        }
        updateRuntimeFlagsForActivityTarget(this, info);
    }

    // Perry: Implement frozen apps: start
    public AppInfo(ActivityInfo info, UserHandle user, boolean quietModeEnabled) {
        this.componentName = new ComponentName(info.packageName, info.name);
        this.container = ItemInfo.NO_ID;
        this.user = user;
        intent = makeLaunchIntent(componentName);

        if (quietModeEnabled) {
            runtimeStatusFlags |= FLAG_DISABLED_QUIET_USER;
        }

        updateRuntimeFlagsForActivityTarget(this, info, user);
    }
    // Perry: Implement frozen apps: end

    public AppInfo(AppInfo info) {
        super(info);
        componentName = info.componentName;
        title = Utilities.trim(info.title);
        intent = new Intent(info.intent);
    }

    @Override
    protected String dumpProperties() {
        return super.dumpProperties() + " componentName=" + componentName;
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(componentName, user);
    }

    public static Intent makeLaunchIntent(LauncherActivityInfo info) {
        return makeLaunchIntent(info.getComponentName());
    }

    public static Intent makeLaunchIntent(ComponentName cn) {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    public static void updateRuntimeFlagsForActivityTarget(
            ItemInfoWithIcon info, LauncherActivityInfo lai) {
        ApplicationInfo appInfo = lai.getApplicationInfo();
        if (PackageManagerHelper.isAppSuspended(appInfo)) {
            info.runtimeStatusFlags |= FLAG_DISABLED_SUSPENDED;
        }
        info.runtimeStatusFlags |= (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                ? FLAG_SYSTEM_NO : FLAG_SYSTEM_YES;

        if (Utilities.ATLEAST_OREO
                && appInfo.targetSdkVersion >= Build.VERSION_CODES.O
                && Process.myUserHandle().equals(lai.getUser())) {
            // The icon for a non-primary user is badged, hence it's not exactly an adaptive icon.
            info.runtimeStatusFlags |= FLAG_ADAPTIVE_ICON;
        }

        // Perry: Implement frozen apps: start
        if (FrozenAppsManager.obtain().activityDisable(lai)) {
            info.runtimeStatusFlags |= FLAG_DISABLED_FROZEN;
        }
        // Perry: Implement frozen apps: end
    }

    // Perry: Implement frozen apps: start
    public static void updateRuntimeFlagsForActivityTarget(
            ItemInfoWithIcon info, ActivityInfo lai, UserHandle user) {
        ApplicationInfo appInfo = lai.applicationInfo;
        if (PackageManagerHelper.isAppSuspended(appInfo)) {
            info.runtimeStatusFlags |= FLAG_DISABLED_SUSPENDED;
        }
        info.runtimeStatusFlags |= (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                ? FLAG_SYSTEM_NO : FLAG_SYSTEM_YES;

        if (Utilities.ATLEAST_OREO
                && appInfo.targetSdkVersion >= Build.VERSION_CODES.O
                && Process.myUserHandle().equals(user)) {
            // The icon for a non-primary user is badged, hence it's not exactly an adaptive icon.
            info.runtimeStatusFlags |= FLAG_ADAPTIVE_ICON;
        }

        if (FrozenAppsManager.obtain().activityDisable(lai)) {
            info.runtimeStatusFlags |= FLAG_DISABLED_FROZEN;
        }
    }

    public boolean updateFrozenFlags() {
        int oldFlags = runtimeStatusFlags;
        if (FrozenAppsManager.obtain().activityDisable(this)) {
            runtimeStatusFlags |= FLAG_DISABLED_FROZEN;
        } else {
            runtimeStatusFlags &= ~FLAG_DISABLED_FROZEN;
        }
        return oldFlags != runtimeStatusFlags;
    }

    public boolean isFrozen() {
        return (runtimeStatusFlags & FLAG_DISABLED_FROZEN)
                == FLAG_DISABLED_FROZEN;
    }

    public boolean isSystemApp() {
        return (runtimeStatusFlags & FLAG_SYSTEM_YES)
                == FLAG_SYSTEM_YES;
    }
    // Perry: Implement frozen apps: end
}
