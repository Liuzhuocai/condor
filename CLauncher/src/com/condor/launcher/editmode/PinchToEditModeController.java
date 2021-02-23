package com.condor.launcher.editmode;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;
import com.android.launcher3.util.TouchController;

import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.LauncherState.NORMAL;

/**
 * Created by Perry on 19-1-18
 */
public class PinchToEditModeController extends ScaleGestureDetector.SimpleOnScaleGestureListener
        implements TouchController {
    private boolean mPinchStarted = false;
    private final Launcher mLauncher;
    private final ScaleGestureDetector mPinchDetector;
    private Workspace mWorkspace;

    public PinchToEditModeController(Launcher launcher) {
        mLauncher = launcher;
        mPinchDetector = new ScaleGestureDetector(launcher, this);
    }

    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        mPinchDetector.onTouchEvent(ev);
        return mPinchStarted;
    }

    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (mPinchStarted) {
            if (ev.getPointerCount() <= 2) {
                return mPinchDetector.onTouchEvent(ev);
            }
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!mLauncher.isInState(NORMAL)) {
            // Don't listen for the pinch gesture if on all apps, widget picker, -1, etc.
            return false;
        }
        if (mLauncher.isWorkspaceLocked()) {
            // Don't listen for the pinch gesture if the workspace isn't ready.
            return false;
        }
        if (mWorkspace == null) {
            mWorkspace = mLauncher.getWorkspace();
        }
        if (mWorkspace.isSwitchingState() || mWorkspace.mScrollInteractionBegan) {
            // Don't listen for the pinch gesture while switching state, as it will cause a jump
            // once the state switching animation is complete.
            return false;
        }
        mPinchStarted = true;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        super.onScaleEnd(detector);
        if (detector.getScaleFactor() < 1) {
            mLauncher.getStateManager().goToState(EDIT_MODE);
        }
        mPinchStarted = false;
    }
}
