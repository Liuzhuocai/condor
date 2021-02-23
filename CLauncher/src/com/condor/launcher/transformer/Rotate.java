package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class Rotate extends BaseTransformer {
    @Override
    protected void onPreTransform(View view, float position) {
        super.onPreTransform(view, position);
        view.setCameraDistance(20000);
    }

    @Override
    protected void onTransform(View view, float position) {
        view.setPivotX(view.getWidth() * 0.5f);
        view.setPivotY(view.getHeight());
        view.setRotationY(-90 * position);
        view.setAlpha(1 - Math.abs(position));
    }
}
