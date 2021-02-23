package com.condor.launcher.transformer;

import android.view.View;

import static com.android.launcher3.anim.Interpolators.ACCEL_0_9;
import static com.android.launcher3.anim.Interpolators.DEACCEL_4;
import static com.android.launcher3.anim.Interpolators.Z_0_5;

/**
 * Created by Perry on 19-1-30
 */
public class Layered extends BaseTransformer {
    private final static float MIN_SCALE = 0.74f;

    @Override
    protected void onTransform(View view, float position) {
        if (position <= 0f) {
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(ACCEL_0_9.getInterpolation(1 + position));
        } else if (position <= 1f) {
            final float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Z_0_5.getInterpolation(Math.abs(position)));
            view.setAlpha(DEACCEL_4.getInterpolation(1 - position));
            view.setPivotX(0.5f * view.getWidth());
            view.setPivotY(0.5f * view.getHeight());
            view.setTranslationX(view.getWidth() * -position);
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
        }
    }

    @Override
    protected boolean isPagingEnabled() {
        return true;
    }

}
