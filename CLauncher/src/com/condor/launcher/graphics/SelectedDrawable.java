package com.condor.launcher.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

import com.android.launcher3.R;

/**
 * Created by Perry on 19-1-25
 */
public class SelectedDrawable extends BitmapDrawable {
    private boolean mSelected = true;

    public SelectedDrawable(Context context) {
        super(context.getResources(), ((BitmapDrawable) context.getDrawable(R.drawable.
                ic_condor_list_selected)).getBitmap());
    }

    @Override
    public void draw(Canvas canvas) {
        if (mSelected) {
            super.draw(canvas);
        }
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
        invalidateSelf();
    }
}
