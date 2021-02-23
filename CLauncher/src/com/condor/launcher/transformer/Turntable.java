package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class Turntable extends BaseTransformer {
    private static final float TRANSITION_MAX_ROTATION = 24;
    @Override
    protected void onTransform(View view, float position) {
        view.setRotation(TRANSITION_MAX_ROTATION * position);
        view.setPivotX(view.getWidth() * 0.5f);
        view.setPivotY(view.getHeight());
    }

    @Override
    protected boolean isPagingEnabled() {
        return true;
    }
}
