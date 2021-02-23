package com.condor.launcher.switcher.desktoplayout;

import android.content.Context;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.SettingsActivity;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.Switcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.condor.launcher.switcher.desktopmode.DesktopModeSwitcher.CLASSIC;
import static com.condor.launcher.switcher.desktopmode.DesktopModeSwitcher.DEFAULT;

/**
 * Created by Perry on 19-1-14
 */
public class DesktopLayoutSwitcher implements Switcher<DesktopLayout> {
    private final ArrayList<DesktopLayout> mLayouts = new ArrayList<>();

    public void init(List<InvariantDeviceProfile> closestProfiles) {
        mLayouts.clear();
        mLayouts.addAll(closestProfiles.stream().map(DesktopLayout::new).
                collect(Collectors.toList()));
    }

    public InvariantDeviceProfile getCurrentProfile(InvariantDeviceProfile defaultProfile) {
        DesktopLayout l = get();
        if (l == null) {
            l = new DesktopLayout(defaultProfile);
            getPersistence().save(DEFAULT.value, l.value);
            getPersistence().save(CLASSIC.value, l.value);
        }

        return l.getIdp();
    }

    // Perry: Reset device profile when switch desktop mode: start
    public void reset(Context context) {
        InvariantDeviceProfile profile = LauncherAppState.getIDP(context);
        InvariantDeviceProfile closestProfile = getCurrentProfile(null);
        if (closestProfile != null) {
            profile.reset(context, closestProfile, mLayouts.stream().map(DesktopLayout::getIdp).
                    collect(Collectors.toList()));
        }
    }
    // Perry: Reset device profile when switch desktop mode: end

    @Override
    public String getDefault() {
        return null;
    }

    @Override
    public CharSequence[] entryValues(Context context) {
        String[] values = new String[mLayouts.size()];
        for (int i = 0; i < mLayouts.size(); i++) {
            values[i] = mLayouts.get(i).value;
        }

        return values;
    }

    @Override
    public CharSequence[] entries(Context context) {
        String[] values = new String[mLayouts.size()];
        for (int i = 0; i < mLayouts.size(); i++) {
            values[i] = mLayouts.get(i).getEntry();
        }

        return values;
    }

    @Override
    public boolean doSwitch(Context context, String value) {
        // Perry: Resovle null pointer exception: start
        DesktopLayout layout = get();
        if (layout == null || !value.equals(layout.value)) {
            getPersistence().save(value);
            InvariantDeviceProfile profile = LauncherAppState.getIDP(context);
            InvariantDeviceProfile closestProfile = getCurrentProfile(null);
            if (closestProfile != null) {
                profile.reset(context, closestProfile, mLayouts.stream().map(DesktopLayout::getIdp).
                        collect(Collectors.toList()));
                LauncherAppState app = LauncherAppState.getInstance(context);
                LauncherModel model = app.getModel();
                if (model != null) {
                    model.enqueueModelUpdateTask(new SwitchDesktopLayoutTask());
                }
                return true;
            }
        }
        // Perry: Resovle null pointer exception: end
        return false;
    }

    @Override
    public DesktopLayout get() {
        String value = getPersistence().load();
        if (value == null) {
            return null;
        }

        for (DesktopLayout l : mLayouts) {
            if (l.value.equals(value)) {
                return l;
            }
        }

        return null;
    }

    @Override
    public SettingsPersistence.StringPersistence getPersistence() {
        return SettingsPersistence.DESKTOP_LAYOUT;
    }
}
