package com.condor.launcher.liveicon;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.android.launcher3.LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
import static com.condor.launcher.settings.SettingsPersistence.LIVE_ICON_ENABLE;

/**
 * Created by Perry on 19-2-18
 */
public class LiveIconsManager {
    private static final String TAG = "LiveIconsManager";
    private Context mContext;
    private LiveIcons mLiveIcons;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mTimeTickReceiver;
    private final HashMap<ComponentName, BaseLiveIcon> mIcons = new HashMap<>();
    private final HashSet<BubbleTextView> mHostViews = new HashSet<>();
    private static final LiveIconsManager INSTANCE = new LiveIconsManager();
    private boolean mIsRegister = false;
    private boolean mHasSecond = false;
    private Handler mHandler;

    private LiveIconsManager() {
        LIVE_ICON_ENABLE.registerValueChangedListener(p-> mHostViews.forEach(IconUpdateListener::onIconUpdated));
    }

    public static LiveIconsManager obtain() {
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mLiveIcons = LiveIcons.load(context);
        mHandler = new Handler();

        mLiveIcons.getLiveIcons().stream()
                .map(name-> mLiveIcons.newLiveIcon(context, name))
                .filter(icon-> icon != null && icon.supportFromTheme())
                .forEach(icon-> {
                    Set<ComponentName> set = icon.getComponents();
                    for (ComponentName c : set) {
                        mIcons.put(c, icon);
                    }
                });

        if (mIcons.isEmpty()) {
            return;
        }

        mHasSecond = mIcons.values().stream().anyMatch(BaseLiveIcon::hasSecond);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        mIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        mTimeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mIcons.values().forEach(BaseLiveIcon::notifyRefresh);
            }
        };
    }

    private final Runnable mTicker = new Runnable() {
        public void run() {
            synchronized (mIcons) {
                mIcons.values().forEach(icon-> {
                    if (icon.hasSecond()) {
                        icon.notifyRefresh();
                    }
                });

                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - now % 1000);

                mHandler.postAtTime(mTicker, next);
            }
        }
    };

    public void reset() {
        release();
        init(mContext);
        resume();
    }

    public FastBitmapDrawable getLiveViewDrawable(ItemInfo info) {
        if (!supportLiveIcons()) {
            return null;
        }

        if (info.itemType != ITEM_TYPE_APPLICATION) {
            return null;
        }

        if (!LIVE_ICON_ENABLE.value()) {
            return null;
        }

        BaseLiveIcon icon = mIcons.get(info.getTargetComponent());
        if (icon != null) {
            return icon.getDrawable(info);
        }

        return null;
    }

    public void resume() {
        if (mIcons.isEmpty() || mIsRegister) {
            return;
        }
        mIcons.values().forEach(BaseLiveIcon::refreshView);
        if (mHasSecond) {
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);
            mHandler.removeCallbacks(mTicker);
            mHandler.postAtTime(mTicker, next);
        }
        mContext.registerReceiver(mTimeTickReceiver, mIntentFilter);
        mIsRegister = true;
    }

    public void pause() {
        if (mIcons.isEmpty() || !mIsRegister) {
            return;
        }
        if (mHasSecond) {
            mHandler.removeCallbacks(mTicker);
        }
        if (mIsRegister) {
            try {
                mContext.unregisterReceiver(mTimeTickReceiver);
            } catch (Exception e) {}
            mIsRegister = false;
        }
    }

    public void release() {
        if (mIcons.isEmpty()) {
            return;
        }
        mIcons.values().forEach(BaseLiveIcon::release);
        if (mHasSecond) {
            mHandler.removeCallbacks(mTicker);
        }
        if (mIsRegister) {
            try {
                mContext.unregisterReceiver(mTimeTickReceiver);
            } catch (Exception e) {
            }
            mIsRegister = false;
        }

        mIcons.clear();
        mHostViews.clear();
    }

    public boolean supportLiveIcons() {
        return !mIcons.isEmpty();
    }

    public void addHostView(ItemInfoWithIcon info, BubbleTextView textView) {
        if (mIcons.containsKey(info.getTargetComponent())) {
            mHostViews.add(textView);
        }
    }

    public void clear() {
        mHostViews.clear();
    }
}
