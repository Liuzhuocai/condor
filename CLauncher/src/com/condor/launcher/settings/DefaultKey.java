package com.condor.launcher.settings;

import com.condor.launcher.util.Logger;

import java.util.Locale;

/**
 * Created by Perry on 19-1-11
 */
public enum DefaultKey {
    // Perry: Cyclic sliding of desktop pages: start
    DESKTOP_LOOP_OPEN,
    // Perry: Cyclic sliding of desktop pages: end
    MINUS_ONE_ENABLE, DESKTOP_MODE, START_PAGE,
    // Perry: desktop layout switch function: start
    DESKTOP_LAYOUT,
    // Perry: desktop layout switch function: end
    // Perry: Implement sliding effect function: start
    EFFECT,
    // Perry: Implement sliding effect function: end
    // Perry: Fixed screen function: start
    SCREEN_LOCKED,
    // Perry: Fixed screen function: end
    // Perry: Add predicted applications: start
    SHOW_PREDICTIONS,
    // Perry: Add predicted applications: end
    // Perry: Add live icon: start
    LIVE_ICON_ENABLE,
    // Perry: Add live icon: end
    // liuzuo: add for wallpaper layout: start
    WALLPAPER_LAYOUT;
    // liuzuo: add for wallpaper layout: end

    private static final String TAG = "DefaultKey";
    private static final String PREFIX = "pref_";
    private static int idCounter = 0;
    private int id;
    private String key;

    DefaultKey() {
        this.id  = generateId();
        this.key = PREFIX + this.toString().toLowerCase(Locale.ENGLISH);
        Logger.d(TAG, "add " + id + ": " + key);
    }

    DefaultKey(String key) {
        this.id  = generateId();
        this.key = key;
    }

    DefaultKey(int id, String key) {
        this.id = id;
        this.key = key;
    }

    private int generateId() {
        return idCounter++;
    }

    public int intKey() {
        return id;
    }

    public static int getIntKey(String key) {
        return valueOf(key.toUpperCase(Locale.ENGLISH)).id;
    }

    public String stringKey() {
        return key;
    }
}
