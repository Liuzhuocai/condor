package com.condor.launcher;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.LongSparseArray;
import android.view.View;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.liveicon.LiveIconsManager;
import com.condor.launcher.minusone.MinusOneCallbacks;
import com.condor.launcher.predictor.CustomAppPredictor;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.util.ThreadUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.condor.launcher.switcher.Switcher.EFFECT_SWITCHER;

/**
 * Created by Perry on 19-1-11
 */
public class CLauncherActivity extends Launcher {
    public CLauncherActivity() {
        new MinusOneCallbacks(this);
    }

    // Perry: Implement sliding effect function: start
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsPersistence.EFFECT.registerValueChangedListener(p-> getWorkspace().
                setTransformer(EFFECT_SWITCHER.get().getTransformer()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Action action = Action.from(intent);
        if (action != null) {
            action.handle(this);
        } else {
            super.onNewIntent(intent);
        }
    }
    // Perry: Implement sliding effect function: end


    // Perry: Add predicted applications: start
    @Override
    protected void onResume() {
        if (isInState(ALL_APPS)) {
            tryAndUpdatePredictedApps();
        }

        super.onResume();

        // Perry: Add live icon: start
        LiveIconsManager.obtain().resume();
        // Perry: Add live icon: end
    }

    // Perry: Add live icon: start
    @Override
    protected void onPause() {
        super.onPause();

        LiveIconsManager.obtain().pause();
    }
    // Perry: Add live icon: end

    @Override
    public void bindAppInfosRemoved(ArrayList<AppInfo> appInfos) {
        super.bindAppInfosRemoved(appInfos);
        tryAndUpdatePredictedApps();
    }

    @Override
    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        if (super.startActivitySafely(v, intent, item)) {
            UserHandle user = item == null ? null : item.user;
            // Perry: To solve the problem of memory leak: start
            CustomAppPredictor.getInstance(getApplicationContext()).logAppLaunch(v, intent, user);
            // Perry: To solve the problem of memory leak: end
            return true;
        }

        return false;
    }
    // Perry: Add predicted applications: end

    @Override
    public void relaunch() {
        if (getModel() != null) {
            getModel().setForceReload();
        }

        if (getAppWidgetHost() != null) {
            getAppWidgetHost().clearViews();
        }

        // Perry: Optimizing window switching animation for Settings: start
        Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(getPackageName())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        // Perry: Optimizing window switching animation for Settings: end
    }

    // Perry: locate application function: start
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        SettingsPersistence.dump(writer);
        ThreadUtils.dump(writer);
        super.dump(prefix, fd, writer, args);
    }

    @Override
    public void onDestroy() {
        // Perry: fix bugs: start
        if (getModel().isCurrentCallbacks(this)) {
            ThreadUtils.clear();
            // Perry: Add live icon: start
            LiveIconsManager.obtain().clear();
            // Perry: Add live icon: end
        }
        // Perry: fix bugs: end

        // Perry: To solve the problem of memory leak: start
        UiFactory.unregisterRemoteAnimations(this);

        super.onDestroy();
        // Perry: To solve the problem of memory leak: end
    }
    // Perry: locate application function: end

    // Perry: Add predicted applications: start
    @Override
    public void tryAndUpdatePredictedApps() {
        // Perry: Optimizing the sliding speed of Allapps: start
        LauncherModel.runOnWorkerThread(()-> {
            if (getAppsView() != null) {
                List<ComponentKey> apps = getPredictedApps();
                if (apps != null) {
                    getAppsView().post(()-> {
                        getAppsView().setPredictedApps(apps);
                    });
                }
            }
        });
        // Perry: Optimizing the sliding speed of Allapps: end
    }

    @Override
    public List<ComponentKey> getPredictedApps() {
        // Perry: To solve the problem of memory leak: start
        return CustomAppPredictor.getInstance(getApplicationContext()).getPredictions();
        // Perry: To solve the problem of memory leak: end
    }
    // Perry: Add predicted applications: end

    // Perry: Add clear all button to recents: start
    @Override
    public float getShiftRange() {
        return getAllAppsController().getShiftRange();
    }
    // Perry: Add clear all button to recents: end


    // Perry: Automatically fill vacancies: start
    @Override
    public void bindScreenItemsMoved(long screenId, LongSparseArray<ItemInfo> movedItems, boolean animate) {
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        final ArrayList<View> movedViews = getWorkspace().moveScreenItemsByMatcher(screenId, movedItems);
        if (animate) {
            for (int i = 0; i < movedViews.size(); i++) {
                View v = movedViews.get(i);
                v.setAlpha(0f);
                v.setScaleX(0f);
                v.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(v, i));
            }
            anim.playTogether(bounceAnims);
            anim.start();
        }
    }
    // Perry: Automatically fill vacancies: end
}
