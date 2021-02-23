package com.condor.launcher.switcher.desktopmode;

import android.content.Context;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.graphics.LauncherIcons;
import com.condor.launcher.CondorLauncherFiles;
import com.condor.launcher.liveicon.LiveIconsManager;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.Switcher;
import com.condor.launcher.util.Utils;

import static com.android.launcher3.LauncherProvider.EMPTY_DATABASE_CREATED;
import static com.condor.launcher.settings.SettingsPersistence.DESKTOP_MODE;
import static com.condor.launcher.util.Constants.CLASSIC_EMPTY_DATABASE_CREATED;

/**
 * Created by Perry on 19-1-11
 */
public class DesktopModeSwitcher implements Switcher<DesktopMode> {
    public final static DesktopMode DEFAULT = new DesktopMode("default", LauncherFiles.LAUNCHER_DB,
            new SettingsPersistence.BooleanPersistence(EMPTY_DATABASE_CREATED, false));
    public final static DesktopMode CLASSIC = new DesktopMode("classic", CondorLauncherFiles.CLASSIC_DB,
            new SettingsPersistence.BooleanPersistence(CLASSIC_EMPTY_DATABASE_CREATED, false));

    @Override
    public String getDefault() {
        return DEFAULT.value;
    }

    @Override
    public CharSequence[] entryValues(Context context) {
        return new CharSequence[]{DEFAULT.value, CLASSIC.value};
    }

    @Override
    public CharSequence[] entries(Context context) {
        return context.getResources().getStringArray(R.array.pref_desktop_mode_names);
    }

    @Override
    public boolean doSwitch(Context context, String value) {
        if (value.equals(getPersistence().value())) {
            return false;
        }

        // Perry: Icon shape override for different desktop modes: start
        LauncherAppState app = LauncherAppState.getInstance(context);
        LauncherModel model = app.getModel();
        if (model != null) {
            model.schedule(()-> {
                getPersistence().save(value);
                LauncherSettings.Settings.call(context.getContentResolver(),
                        LauncherSettings.Settings.METHOD_CHANGE_DATABASE);
                // Perry: Reset device profile when switch desktop mode: start
                DESKTOP_LAYOUT_SWITCHER.reset(context);
                // Perry: Reset device profile when switch desktop mode: end
                IconShapeOverride.apply(context);
                Utils.resetAdaptiveIconMask();
                app.clearIcons();
                LauncherIcons.obtain(context).clear();
                LiveIconsManager.obtain().reset();
            }, LauncherModel.Callbacks::relaunch);
            return true;
        }
        // Perry: Icon shape override for different desktop modes: end

        return false;
    }

    @Override
    public DesktopMode get() {
        String value = getPersistence().value();
        if (CLASSIC.value.equals(value)) {
            return CLASSIC;
        }

        return DEFAULT;
    }

    @Override
    public SettingsPersistence.StringPersistence getPersistence() {
        return DESKTOP_MODE;
    }
}
