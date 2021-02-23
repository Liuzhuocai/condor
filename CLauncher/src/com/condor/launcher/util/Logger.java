package com.condor.launcher.util;

import android.util.Log;

import com.android.launcher3.BuildConfig;

/**
 * Created by Perry on 19-1-11
 */
public class Logger {
    private static final String PREFIX = "CLauncher";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static void i(String tag, String msg) {
        if (DEBUG) Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable e) {
        if (DEBUG) Log.i(tag, msg, e);
    }

    public static void w(String tag, String msg) {
        if (DEBUG) Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable e) {
        if (DEBUG) Log.w(tag, msg, e);
    }

    public static void d(String tag, String msg) {
        if (DEBUG) Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable e) {
        if (DEBUG) Log.d(tag, msg, e);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        Log.e(tag, msg, e);
    }

}
