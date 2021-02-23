package com.condor.launcher;

import android.app.Application;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.config.FeatureFlags;

/**
 * Created by Perry on 19-2-21
 */
public class CondorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LauncherAppState.getInstance(this);
        //liuzuo :add for unread message:start
        if(FeatureFlags.GO_PROJECT){
            CustomTools.getInstance(this.getApplicationContext()).init();
        }
        //liuzuo :add for unread message:end
    }
}
