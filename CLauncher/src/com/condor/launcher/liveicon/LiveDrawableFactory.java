package com.condor.launcher.liveicon;

import android.content.Context;

import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.graphics.DrawableFactory;

/**
 * Created by Perry on 19-2-18
 */
public class LiveDrawableFactory extends DrawableFactory {
    public LiveDrawableFactory(Context context) {
    }

    @Override
    public FastBitmapDrawable newIcon(ItemInfoWithIcon info) {
        FastBitmapDrawable drawable = LiveIconsManager.obtain().getLiveViewDrawable(info);
        if (drawable == null) {
            drawable = new FastBitmapDrawable(info);
        }
        drawable.setIsDisabled(info.isDisabled());
        return drawable;
    }
}
