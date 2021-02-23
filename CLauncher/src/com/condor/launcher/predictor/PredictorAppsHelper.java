package com.condor.launcher.predictor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;


import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.settings.SettingsPersistence;

import java.util.HashSet;
import java.util.Set;

import static com.condor.launcher.predictor.CustomAppPredictor.PREDICTION_PREFIX;
import static com.condor.launcher.predictor.CustomAppPredictor.PREDICTION_SET;

/**
 * Created by Perry on 19-1-22
 */
public class PredictorAppsHelper {
    public static boolean isPredictorEnabled() {
        return SettingsPersistence.SHOW_PREDICTIONS.value();
    }

    public static void copyPredictorSet(Context context,  SharedPreferences from, SharedPreferences to) {
        final PackageManager pm = context.getPackageManager();
        final SharedPreferences.Editor edit = to.edit();
        final HashSet<String> originalSet = new HashSet<>();
        originalSet.addAll(from.getStringSet(PREDICTION_SET, CustomAppPredictor.EMPTY_SET));
        originalSet.addAll(to.getStringSet(PREDICTION_SET, CustomAppPredictor.EMPTY_SET));
        final Set<String> predictionSet = new HashSet<>(originalSet);
        originalSet.forEach(prediction -> {
            try {
                pm.getPackageInfo(new ComponentKey(context, prediction).componentName.getPackageName(), 0);
                String key = PREDICTION_PREFIX + prediction;
                int launchCount = Math.max(to.getInt(key, 0), from.getInt(key, 0));
                edit.putInt(key, launchCount);
            } catch (PackageManager.NameNotFoundException e) {
                predictionSet.remove(prediction);
            }
        });
        edit.putStringSet(PREDICTION_SET, predictionSet);
        edit.apply();
    }
}
