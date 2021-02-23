package com.condor.launcher.switcher.wallpaperlayout;

import android.content.Context;

import com.android.launcher3.R;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.SwitchEntry;
import com.condor.launcher.switcher.Switcher;

/**
 * Created by Perry on 19-1-14
 */
public class WallpaperLayoutSwitcher implements Switcher<SwitchEntry> {

    @Override
    public String getDefault() {
        return "0";
    }

    @Override
    public CharSequence[] entryValues(Context context) {
        return context.getResources().getStringArray(R.array.pref_wallpaper_layout_values);
    }

    @Override
    public CharSequence[] entries(Context context) {
        return context.getResources().getStringArray(R.array.pref_wallpaper_layout_names);
    }

    @Override
    public boolean doSwitch(Context context, String value) {
        getPersistence().save(value);
        return true;
    }

    @Override
    public SwitchEntry get() {
        return new SwitchEntry(getPersistence().load());
    }

    @Override
    public SettingsPersistence.StringPersistence getPersistence() {
        return SettingsPersistence.WALLPAPER_LAYOUT;
    }
}
