package com.condor.launcher.theme;

import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.condor.launcher.theme.bean.ThemeConfigBean;
import com.condor.launcher.theme.bean.ThemeDescriptionBean;

public interface IIcongetter {
    Drawable getIconDrawable(LauncherActivityInfo info, int iconDpi);

    ThemeConfigBean getThemeConfigBean();
    ThemeDescriptionBean getThemeDescriptionBean();

    Bitmap getBitmapByName(String iconName);
}
