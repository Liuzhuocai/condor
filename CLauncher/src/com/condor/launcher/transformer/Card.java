package com.condor.launcher.transformer;

import android.view.View;

/**
 * Created by Perry on 19-1-30
 */
public class Card extends BaseTransformer {
    @Override
    protected void onTransform(View view, float position) {
        view.setRotation(-90 * position);
        view.setAlpha(1 - Math.abs(position));
    }
}
