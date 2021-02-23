package com.condor.launcher.minusone;

import com.android.launcher3.Launcher;
import com.google.android.libraries.launcherclient.LauncherClient;

/**
 * Created by Perry on 19-1-11
 */
public class MinusOneOverlay implements Launcher.LauncherOverlay {
    private LauncherClient mClient;

    public void setLauncherClient(LauncherClient client) {
        mClient = client;
    }

    @Override
    public void onScrollInteractionBegin() {
        mClient.startMove();
    }

    @Override
    public void onScrollInteractionEnd() {
        mClient.endMove();
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        mClient.updateMove(progress);
    }

    @Override
    public void setOverlayCallbacks(Launcher.LauncherOverlayCallbacks callbacks) {
    }
}
