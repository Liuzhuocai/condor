package com.condor.launcher.minusone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.condor.launcher.settings.SettingsPersistence;
import com.google.android.libraries.launcherclient.LauncherClient;
import com.google.android.libraries.launcherclient.LauncherClientCallbacks;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Perry on 19-1-11
 */
public class MinusOneCallbacks implements LauncherCallbacks, SettingsPersistence.OnValueChangedListener {
    private static final String TAG = "MinusOneCallbacks";
    private final Launcher mLauncher;
    private LauncherClient mClient;
    private MinusOneOverlay mOverlay;

    private boolean mStarted;
    private boolean mResumed;
    private boolean mAlreadyOnHome;

    public MinusOneCallbacks(Launcher launcher) {
        mLauncher = launcher;
        mLauncher.setLauncherCallbacks(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mOverlay = new MinusOneOverlay();
        mClient = new LauncherClient(mLauncher, new MinusOneClientCallbacks(), isMinusOneEnable());
        mOverlay.setLauncherClient(mClient);
        SettingsPersistence.MINUS_ONE_ENABLE.registerValueChangedListener(this);
    }

    @Override
    public void onResume() {
        mResumed = true;
        if (mStarted) {
            mAlreadyOnHome = true;
        }

        mClient.onResume();

    }

    @Override
    public void onPause() {
        mResumed = false;
        mClient.onPause();
    }

    @Override
    public void onAttachedToWindow() {
        mClient.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        mClient.onDetachedFromWindow();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {

    }

    @Override
    public void onHomeIntent(boolean internalStateHandled) {
        mClient.hideOverlay(mAlreadyOnHome /* animate */);
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public void onLauncherProviderChange() {
    }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) {
    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
        return false;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public LauncherClient getLauncherClient() {
        return mClient;
    }

    @Override
    public void onDestroy() {
        try {
            mClient.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Minus one client destroy failed", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    @Override
    public void onInteractionBegin() {
        mClient.requestHotwordDetection(false);
    }

    @Override
    public void onInteractionEnd() {
        mClient.requestHotwordDetection(true);
    }

    // Perry: Add gesture for some states: start
    @Override
    public void onRecentsDisplay() {
        if (mClient != null) {
            mClient.hideOverlay(mAlreadyOnHome /* animate */);
        }
    }
    // Perry: Add gesture for some states: end

    @Override
    public void onStart() {
        mStarted = true;
    }

    @Override
    public void onStop() {
        mStarted = false;
        if (!mResumed) {
            mAlreadyOnHome = false;
        }
    }

    @Override
    public void onValueChanged(SettingsPersistence.Persistence p) {
        mClient.setOverlayEnabled(isMinusOneEnable());
        mClient.reconnect();
    }

    private class MinusOneClientCallbacks implements LauncherClientCallbacks {

        private boolean mWasAttached = false;

        @Override
        public void onOverlayScrollChanged(float progress) {
            mLauncher.getWorkspace().onOverlayScrollChanged(progress);
        }

        @Override
        public void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive) {
            if (mWasAttached != overlayAttached) {
                mWasAttached = overlayAttached;
                mLauncher.setLauncherOverlay(overlayAttached ? mOverlay : null);
            }
        }
    }

    private boolean isMinusOneEnable() {
        return SettingsPersistence.MINUS_ONE_ENABLE.value();
    }
}
