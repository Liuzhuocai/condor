package com.condor.launcher;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.config.PlatformFeatureFlags;

/**
 * Created by Perry on 19-1-14
 */
public class CondorFeatureFlags extends PlatformFeatureFlags {
    public static final boolean SUPPORT_EXTRA_SYSTEM_SHORTCUTS = true;
    // Perry: external layout loader: start
    public static final boolean SUPPORT_AUTO_INSTALLS_LAYOUT = false;
    // Perry: external layout loader: end
    // Perry: Implement Lock/Unlock task function: start
    public static final String GLOBAL_AUTHORITY = (BuildConfig.APPLICATION_ID +".global").intern();
    // Perry: Implement Lock/Unlock task function: end
}
