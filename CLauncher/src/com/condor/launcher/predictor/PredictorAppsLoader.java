package com.condor.launcher.predictor;

import android.content.Context;

import com.condor.launcher.util.JsonLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Perry on 19-1-22
 */
public class PredictorAppsLoader {
    private static final String PREDICTOR_APPS_FILE = "predictor_apps.json";
    private static PredictorAppsLoader sInstance = null;
    private final List<String> mPredictorApps = new ArrayList<>();

    public static PredictorAppsLoader obtain() {
        if (sInstance == null) {
            sInstance = new PredictorAppsLoader();
        }
        return sInstance;
    }

    public void load(Context context) {
        if (!mPredictorApps.isEmpty()) {
            return;
        }

        PredictorApps apps = JsonLoader.obtain().load(context, PREDICTOR_APPS_FILE,
                PredictorApps.class);
        mPredictorApps.clear();
        mPredictorApps.addAll(apps.getApps());
    }

    public List<String> getPredictorApps() {
        return mPredictorApps;
    }
}
