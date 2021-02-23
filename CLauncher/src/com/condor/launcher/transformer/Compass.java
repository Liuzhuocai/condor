package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class Compass extends BaseTransformer {
    @Override
    protected void onTransform(View view, float position) {
        view.setPivotX(view.getWidth() * 0.5f);
        view.setPivotY(view.getHeight() * 0.5f);
        view.setRotation(-90 * position);
        view.setAlpha(1 - Math.abs(position));
    }
}
