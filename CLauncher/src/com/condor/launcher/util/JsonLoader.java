package com.condor.launcher.util;

import android.content.Context;

import com.android.launcher3.LauncherAppState;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import static com.condor.launcher.util.Utils.escapedTime;


public class JsonLoader {
    private static final String TAG = "JsonLoader";
    private static JsonLoader sInstance = null;
    private final Gson gson;

    private JsonLoader() {
        this.gson = new Gson();
    }

    public static JsonLoader obtain() {
        if (sInstance == null) {
            sInstance = new JsonLoader();
        }

        return sInstance;
    }

    public <T> T load(Context context, String file, Class<T> tClass) {
        long start = System.currentTimeMillis();
        InputStream in = openFile(context, file);
        if (in == null) {
            return null;
        }
        Logger.d(TAG, "start read " + tClass.getSimpleName());
        try {
            T result = gson.fromJson(Utils.getText(in), tClass);
            Logger.d(TAG, "read " + tClass.getSimpleName() + " in " + escapedTime(start) + "ms");
            return result;
        } catch (IOException e) {
            Logger.e(TAG, "read error, " + e.getMessage());
        }
        return null;
    }

    public <T> T load(Context context, String file, Type typeOfT) {
        long start = System.currentTimeMillis();
        InputStream in = openFile(context, file);
        if (in == null) {
            return null;
        }
        Logger.d(TAG, "start read " + typeOfT);
        try {
            T result = gson.fromJson(Utils.getText(in), typeOfT);
            Logger.d(TAG, "read " + typeOfT + " in " + escapedTime(start) + "ms");
            return result;
        } catch (IOException e) {
            Logger.e(TAG, "read error, " + e.getMessage());
        }
        return null;
    }

    private InputStream openFile(Context context, String file) {
        long start = System.currentTimeMillis();
        Logger.d(TAG, "start open " + file);
        InputStream in = fromConfigs(file);
        if (in == null) {
            in = fromAssets(context, file);
        }
        Logger.d(TAG, "open " + file + " in " + escapedTime(start) + "ms");
        return in;
    }

    private InputStream fromAssets(Context context, String file) {
        try {
            if (context==null){
                context = LauncherAppState.getInstanceNoCreate().getContext();
            }
            return context.getAssets().open(file);
        } catch (Exception e) {
            Logger.e(TAG, "open " + file + " failed, " + e.getMessage());
        }

        return null;
    }

    private InputStream fromConfigs(String file) {
        File f = new File(Constants.LAUNCHER_CONFIG_DIRECTION + file);
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                Logger.e(TAG, "open " + file + " failed, " + e.getMessage());
            }
        }

        return null;
    }
}
