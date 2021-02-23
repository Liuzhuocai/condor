package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class PageTurn extends BaseTransformer {
    @Override
    protected void onPreTransform(View view, float position) {
        super.onPreTransform(view, position);
        view.setCameraDistance(60000);
    }

    @Override
    protected void onTransform(View view, float position) {
        view.setPivotY(view.getHeight() * 0.5f);
        view.setRotationY(-90 * position);
        view.setAlpha(1 - Math.abs(position));
    }
}
