package com.condor.launcher.switcher.desktopmode;

import android.content.Context;

import com.condor.launcher.switcher.Switcher;

import static com.condor.launcher.switcher.desktopmode.DesktopModeSwitcher.CLASSIC;
import static com.condor.launcher.switcher.desktopmode.DesktopModeSwitcher.DEFAULT;

/**
 * Created by Perry on 19-1-11
 */
public class DesktopModeHelper {
    private static DesktopMode get() {
        return Switcher.DESKTOP_MODE_SWITCHER.get();
    }

    public static boolean isDefaultMode() {
        return get() == DEFAULT;
    }

    public static boolean isClassicMode() {
        return get() == CLASSIC;
    }

    public static String getDbFile() {
        return get().getDbFile();
    }

    public static boolean isDbCreated() {
        return get().isDbCreated();
    }

    public static void markDbCreated() {
        get().markDbCreated();
    }

    public static void erasureDbCreatedMark() {
        get().erasureDbCreatedMark();
    }

    public static String getLayoutFile(Context context) {
        return get().getLayoutFile(context);
    }

    public static int getLayoutResId(Context context) {
        return get().getLayoutResId(context);
    }
}
