package com.condor.launcher.theme;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.android.launcher3.R;
import com.condor.launcher.theme.bean.ThemeConfigBean;
import com.condor.launcher.theme.bean.ThemeDescriptionBean;
import com.condor.launcher.theme.utils.ResUtils;

import java.util.HashMap;

public class InternalIcongetter implements IIcongetter {
    HashMap<String, String> iconMap = new HashMap<>();
    private Context mContext;
    int desity;
    private ThemeDescriptionBean tdb;
    private ThemeConfigBean tcb;
    public InternalIcongetter(Context context) {
        mContext = context.getApplicationContext();
        desity = context.getResources().getConfiguration().densityDpi;
        String[] packageClasseIcons = context.getResources().getStringArray(R.array.icon_array);
        for (String packageClasseIcon : packageClasseIcons) {
            String[] packageClasses_Icon = packageClasseIcon.split("#");
            if (packageClasses_Icon.length == 2) {
                String[] packageClasses = packageClasses_Icon[0].split("\\|");
                for (String s : packageClasses) {
                    iconMap.put(s.trim(), packageClasses_Icon[1]);
                }
            }
        }
    }

    @Override
    public Drawable getIconDrawable(LauncherActivityInfo info, int iconDpi) {
        Drawable d = null;
        String iconName = iconMap.get(info.getComponentName().getPackageName() + "_" + info.getComponentName().getClassName());
        if (iconName != null) {
            d = ResUtils.getIconMipmap(mContext.getPackageName(), iconName, mContext, iconDpi);
        }
        return d;
    }

    @Override
    public ThemeConfigBean getThemeConfigBean() {
        return tcb;
    }

    @Override
    public ThemeDescriptionBean getThemeDescriptionBean() {
        return tdb;
    }

    @Override
    public Bitmap getBitmapByName(String iconName) {
        return null;
    }
}
