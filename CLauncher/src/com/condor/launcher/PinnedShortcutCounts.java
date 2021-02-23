package com.condor.launcher;

import android.util.MutableInt;

import com.android.launcher3.shortcuts.ShortcutKey;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Perry on 19-1-25
 */
public class PinnedShortcutCounts {
    /**
     * Map of ShortcutKey to the number of times it is pinned for default desktop mode.
     */
    public final Map<ShortcutKey, MutableInt> defaultShortcuts = new HashMap<>();

    /**
     * Map of ShortcutKey to the number of times it is pinned for classic desktop mode.
     */
    public final Map<ShortcutKey, MutableInt> classicShortcuts = new HashMap<>();

    public void clear() {
        if (DesktopModeHelper.isClassicMode()) {
            classicShortcuts.clear();
        } else {
            defaultShortcuts.clear();
        }
    }

    public MutableInt get(ShortcutKey key) {
        if (defaultShortcuts.get(key) != null) {
            return defaultShortcuts.get(key);
        }

        return classicShortcuts.get(key);
    }

    public MutableInt put(ShortcutKey key, MutableInt count) {
        if (DesktopModeHelper.isClassicMode()) {
            return classicShortcuts.put(key, count);
        }

        return defaultShortcuts.put(key, count);
    }
}
