package com.condor.launcher.switcher.desktopmode;

import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.util.Logger;

/**
 * Created by Perry on 19-1-11
 */
public class StartPageHelper {
    private static final String TAG = "StartPageHelper";
    public static final int INVALID_START_PAGE = -1;

    public static int getStartPage() {
        return SettingsPersistence.START_PAGE.value();
    }

    public static void saveStartPage(int page) {
        SettingsPersistence.START_PAGE.save(page);
    }

    public static void deletePage(int page) {
        int startPage = getStartPage();
        Logger.d(TAG, "startPage = " + startPage + ", page = " + page);
        if (startPage > page) {
            startPage--;
            if (startPage >= 0) {
                saveStartPage(startPage);
            }
        }
    }

    public static int loadStartPage(int defaultPageSize) {
        int startPage = SettingsPersistence.START_PAGE.load();
        if (startPage == INVALID_START_PAGE
                || startPage > defaultPageSize) {
            startPage = defaultPageSize;
            saveStartPage(defaultPageSize);
        }

        return startPage;
    }
}
