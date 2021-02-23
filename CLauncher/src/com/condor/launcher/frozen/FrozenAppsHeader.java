package com.condor.launcher.frozen;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.android.launcher3.Insettable;
import com.condor.launcher.util.Utils;

import androidx.cardview.widget.CardView;


public class FrozenAppsHeader extends CardView implements Insettable {
    public FrozenAppsHeader(Context context) {
        this(context, null);
    }

    public FrozenAppsHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrozenAppsHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        lp.height = Utils.dp2px(getContext(), 56) + insets.top;
        setPadding(getPaddingLeft(), insets.top, getPaddingRight(), getPaddingBottom());
        requestLayout();
    }
}
