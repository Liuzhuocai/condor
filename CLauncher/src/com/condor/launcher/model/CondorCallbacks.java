package com.condor.launcher.model;

import android.util.LongSparseArray;

import com.android.launcher3.ItemInfo;

/**
 * Created by Perry on 19-1-15
 */
public interface CondorCallbacks {
    // relaunch activity used for desktop mode and desktop layout switch
    public void relaunch();
    // Perry: Automatically fill vacancies: start
    public void bindScreenItemsMoved(long screenId, LongSparseArray<ItemInfo> movedItems, boolean animate);
    // Perry: Automatically fill vacancies: end
}
