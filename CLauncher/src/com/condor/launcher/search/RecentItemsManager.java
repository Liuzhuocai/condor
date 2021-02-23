package com.condor.launcher.search;

import android.content.Context;


import com.android.launcher3.AppInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.allapps.AlphabeticalAppsList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Perry on 19-1-24
 */
public class RecentItemsManager {
    private static RecentItemsManager sInstance = null;
    private final List<SearchKey> mRecentItems = new ArrayList<>();

    public static RecentItemsManager getInstance() {
        if (sInstance == null) {
            sInstance = new RecentItemsManager();
        }

        return sInstance;
    }

    public void loadRecentItems(Context context) {
        runOnWorkThread(()-> {
            synchronized (mRecentItems) {
                mRecentItems.clear();
                mRecentItems.addAll(RecentUtils.loadRecentItems(context));
            }
        });
    }

    public void addRecentItem(Context context, AlphabeticalAppsList apps, final SearchKey key) {
        runOnWorkThread(()-> {
            synchronized (mRecentItems) {
                if (mRecentItems.contains(key)) {
                    mRecentItems.remove(key);
                    runOnWorkThread(()-> RecentUtils.updateRecentItem(context, key));
                } else {
                    runOnWorkThread(()-> RecentUtils.addRecentItem(context, key));
                }

                mRecentItems.add(0, key);
            }

            apps.asyncUpdateAdapterItems();
        });
    }

    public void addRecentItem(Context context, AlphabeticalAppsList apps, final AppInfo info) {
        runOnWorkThread(()-> {
            synchronized (mRecentItems) {
                final SearchKey key = info.toComponentKey();
                if (mRecentItems.contains(key)) {
                    mRecentItems.remove(key);
                    runOnWorkThread(()-> RecentUtils.updateRecentItem(context, info));
                } else {
                    runOnWorkThread(()-> RecentUtils.addRecentItem(context, info));
                }

                mRecentItems.add(0, key);
            }

            apps.asyncUpdateAdapterItems();
        });
    }

    public void clearRecentItems(Context context, AlphabeticalAppsList apps) {
        LauncherModel.runOnWorkerThread(()-> {
            synchronized (mRecentItems) {
                mRecentItems.clear();
                runOnWorkThread(()-> RecentUtils.clearRecentItems(context));
            }

            apps.asyncUpdateAdapterItems();
        });
    }

    private void runOnWorkThread(Runnable r) {
        LauncherModel.runOnWorkerThread(r);
    }

    public List<SearchKey> getRecentItems() {
        return mRecentItems;
    }
}
