package com.condor.launcher.settings;

/**
 * Created by Perry on 19-1-11
 */
public class SettingsKey {
    public static final String KEY_QUICK_OPTIONS_CATEGORY = "pref_quick_options_category";
    public static final String KEY_MORE_FUNCTION_CATEGORY = "pref_more_function_category";

    public static final String KEY_FIRST_LOADED = "first_loaded";
    // Perry: Cyclic sliding of desktop pages: start
    public static final String KEY_DESKTOP_LOOP_OPEN = DefaultKey.DESKTOP_LOOP_OPEN.stringKey();
    // Perry: Cyclic sliding of desktop pages: end
    public static final String KEY_MINUS_ONE_ENABLE = DefaultKey.MINUS_ONE_ENABLE.stringKey();
    public static final String KEY_DESKTOP_MODE = DefaultKey.DESKTOP_MODE.stringKey();
    public static final String KEY_START_PAGE = DefaultKey.START_PAGE.stringKey();
    // Perry: desktop layout switch function: start
    public static final String KEY_DESKTOP_LAYOUT = DefaultKey.DESKTOP_LAYOUT.stringKey();
    // Perry: desktop layout switch function: end
    // Perry: Implement sliding effect function: start
    public static final String KEY_EFFECT = DefaultKey.EFFECT.stringKey();
    // Perry: Implement sliding effect function: end
    // Perry: Fixed screen function: start
    public static final String KEY_SCREEN_LOCKED = DefaultKey.SCREEN_LOCKED.stringKey();
    // Perry: Fixed screen function: end
    // Perry: Add predicted applications: start
    public static final String KEY_SHOW_PREDICTIONS = DefaultKey.SHOW_PREDICTIONS.stringKey();
    // Perry: Add predicted applications: end
    // Perry: Add live icon: start
    public static final String KEY_LIVE_ICON_ENABLE = DefaultKey.LIVE_ICON_ENABLE.stringKey();
    // Perry: Add live icon: end
    // Perry: adjust settings UI: start
    public static final String KEY_MORE_SETTINGS = "pref_more_settings";
    // Perry: adjust settings UI: end
    // Perry: Implement frozen apps: start
    public static final String KEY_APP_FROZEN = "pref_app_frozen";
    // Perry: Implement frozen apps: end
    // liuzuo: add for wallpaper layout: start
    public static final String KEY_WALLPAPER_SETTINGS = DefaultKey.WALLPAPER_LAYOUT.stringKey();
    // liuzuo: add for wallpaper layout: end
}
