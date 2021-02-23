package com.condor.launcher.liveicon;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.text.TextUtils;

import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.condor.launcher.util.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Perry on 19-2-18
 */
public abstract class BaseLiveIcon<T extends LiveIcons.BaseConfig> {
    protected final Context mContext;
    protected final T mConfig;
    protected final ArrayList<WeakReference<BaseLiveDrawable>> mDrawables = new ArrayList<>();

    protected final Drawable mBackground;

    public BaseLiveIcon(Context context, LiveIcons.BaseConfig config) {
        mContext = context;
        mConfig  = (T)config;
        mBackground = getDrawable(mConfig.getBackground());
    }

    protected Drawable getDrawable(String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        }

        int resId = Utils.getResourceId(mContext, s);
        if (resId < 0) {
            return null;
        }

        return mContext.getDrawable(resId);
    }

    protected Bitmap getBitmap(String s) {
        Drawable d = getDrawable(s);
        if (d == null) {
            return null;
        } else if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable)d).getBitmap();
        }

        LauncherIcons li = LauncherIcons.obtain(mContext);

        BitmapInfo bi = li.createBadgedIconBitmap(d, Process.myUserHandle(),
                0, mContext);
        li.recycle();

        return bi.icon;
    }

    public void refreshView() {
        notifyRefresh();
    }

    public void notifyRefresh() {
        synchronized (mDrawables) {
            mDrawables.removeIf((ref)-> ref.get() == null);
            mDrawables.stream()
                    .map(WeakReference::get)
                    .forEach((d)-> {
                        if (d.needRefresh() && !d.isRunning()) {
                            d.start();
                        }
                    });
        }
    }

    public void release() {
        synchronized (mDrawables) {
            mDrawables.stream()
                    .map(WeakReference::get).filter(d->d != null)
                    .forEach(BaseLiveDrawable::stop);
            mDrawables.clear();
        }
    }

    public boolean isLiveViewIcon(ComponentName component) {
        if (component == null) {
            return false;
        }

        return mConfig.getComponents().contains(component.flattenToString());
    }

    public boolean supportFromTheme() {
        return mConfig != null && mBackground != null;
    }

    protected BitmapInfo getBackground(ItemInfo info) {
        LauncherIcons li = LauncherIcons.obtain(mContext);

        BitmapInfo bi = li.createBadgedIconBitmap(
                mBackground, info.user,
                28, mContext);
        li.recycle();

        return bi;
    }

    public abstract BaseLiveDrawable newLiveDrawable(ItemInfo info);

    public final FastBitmapDrawable getDrawable(ItemInfo info) {
        BaseLiveDrawable d = newLiveDrawable(info);
        mDrawables.add(new WeakReference<>(d));
        return d;
    }

    public boolean hasSecond() {
        return false;
    }

    public Set<ComponentName> getComponents() {
        return mConfig.getComponents().stream().map(ComponentName::unflattenFromString).collect(Collectors.toSet());
    }

    public abstract class BaseLiveDrawable extends FastBitmapDrawable implements Animatable {
        public BaseLiveDrawable(ItemInfo info) {
            super(getBackground(info));
        }

        protected abstract boolean needRefresh();
    }
}
