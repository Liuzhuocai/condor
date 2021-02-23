package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class Cube extends BaseTransformer {
    @Override
    protected void onTransform(View view, float position) {
        view.setPivotX(position < 0f ? view.getWidth() : 0f);
        view.setPivotY(view.getHeight() * 0.5f);
        view.setRotationY(90f * position);
    }

    @Override
    public boolean isPagingEnabled() {
        return true;
    }
}
