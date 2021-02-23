package com.condor.launcher.views;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Perry on 19-1-31
 */
public class CondorSwitchPreference extends SwitchPreference {
    public CondorSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CondorSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CondorSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CondorSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setOnClickListener((v)-> onClick());
    }
}
