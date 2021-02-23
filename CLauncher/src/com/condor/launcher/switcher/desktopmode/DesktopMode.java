package com.condor.launcher.switcher.desktopmode;

import android.content.Context;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.SwitchEntry;


import java.util.Locale;

/**
 * Created by Perry on 19-1-11
 */
public class DesktopMode extends SwitchEntry {
    private static final String FORMATTED_LAYOUT_NAME = "%s_workspace_%dx%d";
    private static final String FORMATTED_LAYOUT_FILE = FORMATTED_LAYOUT_NAME + ".xml";
    private final String dbFile;
    private final SettingsPersistence.BooleanPersistence dbCreated;

    public DesktopMode(String name, String dbFile,
                       SettingsPersistence.BooleanPersistence dbCreated) {
        super(name);
        this.dbFile = dbFile;
        this.dbCreated = dbCreated;
    }

    public String getDbFile() {
        return dbFile;
    }

    public boolean isDbCreated() {
        return dbCreated.value();
    }

    public void markDbCreated() {
        dbCreated.save(true);
    }

    public void erasureDbCreatedMark() {
        dbCreated.remove();
    }

    public String getLayoutFile(Context context) {
        InvariantDeviceProfile grid = LauncherAppState.getIDP(context);
        return String.format(Locale.ENGLISH, FORMATTED_LAYOUT_FILE,
                value, grid.numRows, grid.numColumns);
    }

    public int getLayoutResId(Context context) {
        InvariantDeviceProfile grid = LauncherAppState.getIDP(context);
        String layoutName = String.format(Locale.ENGLISH, FORMATTED_LAYOUT_NAME,
                value, grid.numRows, grid.numColumns);
        return context.getResources().getIdentifier(layoutName
                , "xml", context.getPackageName());
    }
}
