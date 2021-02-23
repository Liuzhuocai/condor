package com.condor.launcher.util;

import android.os.Environment;

/**
 * Created by Perry on 19-1-11
 */
public class Constants {
    public static final String TEST_EVN_FOLDER_NAME = "claunchertest1234567890";
    public static final String DEBUG_EVN_FOLDER_NAME = "clauncherdebug1234567890";
    public static final String LAUNCHER_CONFIG_DIRECTION;
    public static final String CLASSIC_EMPTY_DATABASE_CREATED = "CLASSIC_EMPTY_DATABASE_CREATED";
    // Perry: convert old version settings: start
    public static final String OLD_VERSION_SCREEN_LOCK_SETTINGS = "pref_screen_lock_settings";
    public static final String OLD_VERSION_EFFECT_TYPE = "launcher.pageview_animation_type";
    public final static String OLD_VERSION_MINUS_ONE_SETTINGS = "pref_negative_screen_settings";
    // Perry: convert old version settings: end

    static {
        if (Utils.isTestEnvironment()) {
            LAUNCHER_CONFIG_DIRECTION = Environment.getExternalStorageDirectory().getPath();
        } else {
            LAUNCHER_CONFIG_DIRECTION = "/system/etc/config/launcher/";
        }
    }

    private static final String CONFIG_FILE_NAME = "setting_default_values.xml";
    public static final String CONFIG_FILE_NAME_PATH = LAUNCHER_CONFIG_DIRECTION + CONFIG_FILE_NAME;
    private static final String DEVICE_PROFILES_NAME = "device_profiles.xml";
    public static final String DEVICE_PROFILE_NAME_PATH = LAUNCHER_CONFIG_DIRECTION + DEVICE_PROFILES_NAME;
}
