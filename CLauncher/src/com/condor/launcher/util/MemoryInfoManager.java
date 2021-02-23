package com.condor.launcher.util;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Created by Perry on 19-1-24
 */
public class MemoryInfoManager {
    public final static int KB = 1024;
    public final static int MB = 1024 * KB;
    public final static int GB = MB * 1024;
    private static MemoryInfoManager sInstance;
    private ActivityManager mManager;
    private ActivityManager.MemoryInfo mInfo;
    private final long mTotalMemory;

    public static MemoryInfoManager getInstance(Context context) {
        if (sInstance == null) {
            // Perry: To solve the problem of memory leak: start
            sInstance = new MemoryInfoManager(context.getApplicationContext());
            // Perry: To solve the problem of memory leak: end
        }

        return sInstance;
    }

    private MemoryInfoManager(Context context) {
        mManager =(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mInfo = new ActivityManager.MemoryInfo();
        mManager.getMemoryInfo(mInfo);
        mTotalMemory = translateCapacity(mInfo.totalMem);
    }

    public void update() {
        mManager.getMemoryInfo(mInfo);
    }

    public float getAvailable(int unit) {
        update();
        return mInfo.availMem / (float)unit;
    }

    public float getTotal(int unit) {
        return mTotalMemory / (float)unit;
    }

    public float getAvailable() {
        return getAvailable(GB);
    }

    public float getTotal() {
        return getTotal(GB);
    }

    public float getUsedRate() {
        return 1 - mInfo.availMem / (float) mTotalMemory;
    }

    private long translateCapacity(long capacity) {
        long result = capacity;
        if (capacity < 67108864L) {
            result = 67108864L;
        } else if (capacity < 134217728L) {
            result = 134217728L;
        } else if (capacity < 268435456L) {
            result = 268435456L;
        } else if (capacity < 536870912L) {
            result = 536870912L;
        } else if (capacity < 1073741824L) {
            result = 1073741824L;
        } else if (capacity < 1610612736L) {
            result = 1610612736L;
        } else if (capacity < 2147483648L) {
            result = 2147483648L;
        } else if (capacity < 3221225472L) {
            result = 3221225472L;
        } else if (capacity < 4294967296L) {
            result = 4294967296L;
        } else if (capacity < 6442450944L) {
            result = 6442450944L;
        }else if (capacity < 8589934592L) {
            result = 8589934592L;
        } else if (capacity < 17179869184L) {
            result = 17179869184L;
        } else if (capacity < 32000000000L) {
            result = 34359738368L;
        }else if(capacity < 64000000000L){
            result = 68719476736L;
        } else if(capacity < 128000000000L){
            result = 68719476736L * 2;
        }
        return result;
    }
}
