package com.condor.launcher.switcher;

import android.content.Context;

import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.desktopmode.DesktopModeSwitcher;
import com.condor.launcher.switcher.desktoplayout.DesktopLayoutSwitcher;
import com.condor.launcher.switcher.effect.EffectSwitcher;
import com.condor.launcher.switcher.wallpaperlayout.WallpaperLayoutSwitcher;

/**
 * Created by Perry on 19-1-11
 */
public interface Switcher<T extends SwitchEntry> {
    DesktopModeSwitcher DESKTOP_MODE_SWITCHER = new DesktopModeSwitcher();
    // Perry: desktop layout switch function: start
    DesktopLayoutSwitcher DESKTOP_LAYOUT_SWITCHER = new DesktopLayoutSwitcher();
    // Perry: desktop layout switch function: end
    EffectSwitcher EFFECT_SWITCHER = new EffectSwitcher();
    // liuzuo: add for wallpaper layout: start
    WallpaperLayoutSwitcher WALLPAPER_LAYOUT_SWITCHER = new WallpaperLayoutSwitcher();
    // liuzuo: add for wallpaper layout: end
    // Perry: Implement sliding effect function: start
    // Perry: Implement sliding effect function: end

    String getDefault();
    CharSequence[] entryValues(Context context);
    CharSequence[] entries(Context context);
    boolean doSwitch(Context context, String value);
    T get();
    SettingsPersistence.StringPersistence getPersistence();
}
