package com.condor.launcher.locktask;

import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;

import com.android.launcher3.LauncherModel;
import com.android.launcher3.util.ComponentKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Perry on 19-1-24
 */
public class LockTaskManager {
    private static LockTaskManager sInstance = null;
    private final List<ComponentKey> mLockTask = new ArrayList<>();
    private volatile ContentObserver mObserver = null;

    private LockTaskManager() {
    }

    public static LockTaskManager obtain() {
        if (sInstance == null) {
            sInstance = new LockTaskManager();
        }

        return sInstance;
    }

    public void registerObserver(Context context, Runnable refreshUI) {
        if (mObserver != null) {
            return;
        }
        mObserver = new LockTaskObserver(context, refreshUI);
        LockTaskModel.registerObserver(context, mObserver);
    }

    public void unregisterObserver(Context context) {
        if (mObserver != null) {
            LockTaskModel.unregisterObserver(context, mObserver);
            mObserver = null;
        }
    }

    public void loadLockTasks(Context context) {
        loadLockTasks(context, null);
    }

    public void loadLockTasks(Context context, Runnable refreshUI) {
        runOnWorkThread(()-> {
            synchronized (mLockTask) {
                mLockTask.clear();
                mLockTask.addAll(LockTaskModel.loadLockTasks(context));
            }
            if (refreshUI != null) {
                refreshUI.run();
            }
        });
    }

    public void addLockTask(Context context, ComponentName component, UserHandle user) {
        if (component == null || user == null) {
            return;
        }
        ComponentKey componentKey = new ComponentKey(component, user);
        synchronized (mLockTask) {
            mLockTask.add(componentKey);
            runOnWorkThread(()-> LockTaskModel.addLockTask(context, componentKey));
        }
    }

    public void removeLockTask(Context context, ComponentName component, UserHandle user) {
        if (component == null || user == null) {
            return;
        }
        ComponentKey componentKey = new ComponentKey(component, user);
        synchronized (mLockTask) {
            mLockTask.remove(componentKey);
            runOnWorkThread(()-> LockTaskModel.removeLockTask(context, componentKey));
        }
    }

    public boolean isTaskLocked(ComponentName component, UserHandle user) {
        if (component == null) {
            return false;
        }
        return mLockTask.contains(new ComponentKey(component, user));
    }

    private void runOnWorkThread(Runnable r) {
        LauncherModel.runOnWorkerThread(r);
    }

    private class LockTaskObserver extends ContentObserver {
        private final Context mContext;
        private final Runnable mRefreshUI;
        public LockTaskObserver(Context context, Runnable refreshUI) {
            super(new Handler());
            mContext = context;
            mRefreshUI = refreshUI;
        }

        @Override
        public void onChange(boolean selfChange) {
            loadLockTasks(mContext, mRefreshUI);
        }
    }
}
