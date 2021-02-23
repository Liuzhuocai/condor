package com.android.launcher3.config;

import com.android.launcher3.folder.FolderIcon;

public class PlatformFeatureFlags {
    public static final String PLATFORM = "Condor";
    public static final boolean SUPPORT_DESKTOP_MODE_SWITCH = true;
    public static final boolean SUPPORT_FAST_SETTINGS_ACTIVITY = true;
    //liuzuo: add foldericon mode:begin
    public static FolderIcon.FolderIconMode LAUNCHER3_LEGACY_FOLDER_ICON_RULE = FolderIcon.FolderIconMode.SudokuFolderIconLayoutRule;
    //liuzuo: add foldericon mode:end

    //liuzuo: add folder tap mode:start
    public static final boolean SUPPORT_FOLDER_TAP_MODE= false;
    //liuzuo: add folder tap mode:end
}
