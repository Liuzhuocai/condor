package com.condor.launcher.blur;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;


public class BlurImageView extends ImageView {
    public BlurImageView(Context context) {
        super(context);
    }

    public BlurImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlurImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
