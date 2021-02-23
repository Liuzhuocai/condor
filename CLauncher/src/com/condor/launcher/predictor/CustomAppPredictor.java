package com.condor.launcher.predictor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;

import com.android.launcher3.AppFilter;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.UnreadMessageActivity;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;
import com.condor.launcher.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.condor.launcher.settings.SettingsPersistence.SHOW_PREDICTIONS;

/**
 * Created by Perry on 19-1-22
 */
public class CustomAppPredictor implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int MAX_PREDICTIONS = 10;
    private static final int BOOST_ON_OPEN = 9;
    public static final String PREDICTION_SET = "pref_prediction_set";
    public static final String PREDICTION_PREFIX = "pref_prediction_count_";
    public static final Set<String> EMPTY_SET = new HashSet<>();
    private final Context mContext;
    private final AppFilter mAppFilter;
    private final SharedPreferences mPrefs;
    private final PackageManager mPackageManager;

    private static CustomAppPredictor sInstance = null;

    public static CustomAppPredictor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CustomAppPredictor(context);
        }

        return sInstance;
    }

    private CustomAppPredictor(Context context) {
        mContext = context;
        mAppFilter = AppFilter.newInstance(mContext);
        mPrefs = Utilities.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mPackageManager = context.getPackageManager();
    }

    public List<ComponentKey> getPredictions() {
        List<ComponentKey> list = new ArrayList<>();
        if (isPredictorEnabled()) {
            clearNonExistentPackages();

            List<String> predictionList = new ArrayList<>(getStringSetCopy());

            predictionList.sort((o1, o2)-> Integer.compare(getLaunchCount(o2), getLaunchCount(o1)));

            for (String prediction : predictionList) {
                list.add(getComponentFromString(prediction));
            }

            if (list.size() < MAX_PREDICTIONS) {
                for (String placeHolder : PredictorAppsLoader.obtain().getPredictorApps()) {
                    Intent intent = mPackageManager.getLaunchIntentForPackage(placeHolder);
                    if (intent != null) {
                        ComponentName componentInfo = intent.getComponent();
                        if (componentInfo != null) {
                            ComponentKey key = new ComponentKey(componentInfo, Process.myUserHandle());
                            if (!predictionList.contains(key.toString())) {
                                list.add(key);
                            }
                        }
                    }
                }
            }

            if (list.size() > MAX_PREDICTIONS) {
                list = list.subList(0, MAX_PREDICTIONS);
            }
        }
        return list;
    }

    public void logAppLaunch(View v, Intent intent, UserHandle user) {
        if (isPredictorEnabled() && recursiveIsDrawer(v)) {
            ComponentName componentInfo = intent.getComponent();
            if (componentInfo != null && mAppFilter.shouldShowApp(componentInfo)) {
                clearNonExistentPackages();

                Set<String> predictionSet = getStringSetCopy();
                SharedPreferences.Editor edit = mPrefs.edit();

                String prediction = new ComponentKey(componentInfo, user).toString();
                if (predictionSet.contains(prediction)) {
                    edit.putInt(PREDICTION_PREFIX + prediction, getLaunchCount(prediction) + BOOST_ON_OPEN);
                } else if (predictionSet.size() < MAX_PREDICTIONS || decayHasSpotFree(predictionSet, edit)) {
                    predictionSet.add(prediction);
                }

                edit.putStringSet(PREDICTION_SET, predictionSet);
                edit.apply();
            }
        }

    }

    private boolean decayHasSpotFree(Set<String> toDecay, SharedPreferences.Editor edit) {
        boolean spotFree = false;
        Set<String> toRemove = new HashSet<>();
        for (String prediction : toDecay) {
            int launchCount = getLaunchCount(prediction);
            if (launchCount > 0) {
                edit.putInt(PREDICTION_PREFIX + prediction, --launchCount);
            } else if (!spotFree) {
                edit.remove(PREDICTION_PREFIX + prediction);
                toRemove.add(prediction);
                spotFree = true;
            }
        }
        for (String prediction : toRemove) {
            toDecay.remove(prediction);
        }
        return spotFree;
    }

    /**
     * Zero-based launch count of a shortcut
     * @param component serialized component
     * @return the number of launches, at least zero
     */
    private int getLaunchCount(String component) {
        return mPrefs.getInt(PREDICTION_PREFIX + component, 0);
    }

    private boolean recursiveIsDrawer(View v) {
        return true;
    }

    private boolean isPredictorEnabled() {
        return PredictorAppsHelper.isPredictorEnabled();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SHOW_PREDICTIONS.key()) && !isPredictorEnabled()) {
            Set<String> predictionSet = getStringSetCopy();

            SharedPreferences.Editor edit = mPrefs.edit();
            for (String prediction : predictionSet) {
                Logger.d("Predictor", "Clearing " + prediction + " at " + getLaunchCount(prediction));
                edit.remove(PREDICTION_PREFIX + prediction);
            }
            edit.putStringSet(PREDICTION_SET, EMPTY_SET);
            edit.apply();
        }
        //Bruce : add for unread message  : begin
        if (FeatureFlags.UNREAD_MESSAGE&&(UnreadMessageActivity.UNREAD_PHONE_PREF.equals(key) || UnreadMessageActivity.UNREAD_SMS_PREF.equals(key))) {
            UnreadInfoManager.getInstance(mContext).init();
        }
        //Bruce : add for unread message : end

    }

    private ComponentKey getComponentFromString(String str) {
        return new ComponentKey(mContext, str);
    }

    private void clearNonExistentPackages() {
        Set<String> originalSet = mPrefs.getStringSet(PREDICTION_SET, EMPTY_SET);
        Set<String> predictionSet = new HashSet<>(originalSet);

        SharedPreferences.Editor edit = mPrefs.edit();
        for (String prediction : originalSet) {
            ComponentKey key = new ComponentKey(mContext, prediction);
            try {
                mPackageManager.getPackageInfo(key.componentName.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                predictionSet.remove(prediction);
                edit.remove(PREDICTION_PREFIX + prediction);
            }
        }

        edit.putStringSet(PREDICTION_SET, predictionSet);
        edit.apply();
    }

    private Set<String> getStringSetCopy() {
        return new HashSet<>(mPrefs.getStringSet(PREDICTION_SET, EMPTY_SET));
    }
}
