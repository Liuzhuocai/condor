package com.condor.launcher;

import android.content.Context;

import com.android.launcher3.MainProcessInitializer;
import com.condor.launcher.predictor.PredictorAppsLoader;
import com.condor.launcher.settings.SettingsPersistence;

/**
 * Created by Perry on 19-1-11
 */
public class CondorProcessInitializer extends MainProcessInitializer {
    public CondorProcessInitializer(Context context) {

    }

    @Override
    protected void init(Context context) {
        super.init(context);

        SettingsPersistence.load(context);
        // Perry: Add predicted applications: start
        PredictorAppsLoader.obtain().load(context);
        // Perry: Add predicted applications: end
    }
}
