package com.condor.launcher.theme;

import android.os.Environment;

public class ThemeConfig {
    /**Theme folders*/
    public static final String default_Theme_Folder= "/system/etc/config/themes/default";
    public static final String default_Theme_Folder_Icons = "/system/etc/config/themes/default/icons";
    public static final String extenal_Theme_Folder= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Condor Theme" + "/Theme";
    public static final String extenal_Theme_Folder_v5= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/.Condor Theme" + "/Theme/icons";

    public static final String default_Theme_Description = default_Theme_Folder+"/description.xml";
    public static final String default_Theme_Config = default_Theme_Folder+"/launcher_config.xml";
    public static final String default_Theme_Icon_Config = default_Theme_Folder+"/icon_config.xml";


    public static final String extenal_Theme_Description = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/.Condor Theme"+ "/Theme/description.xml";
    public static final String extenal_Theme_Config = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/.Condor Theme"+ "/Theme/launcher_config.xml";
    public static final String extenal_Theme_Icon_Config = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/.Condor Theme"+ "/Theme/icon_config.xml";
    /**Theme type*/
    public final static int Type_default = 0;
    public final static int  Type_external = 1;
    public final static int Type_internal = 2;
    public final static int Type_external_v5 = 3;
    public static void init(){

    }
}
