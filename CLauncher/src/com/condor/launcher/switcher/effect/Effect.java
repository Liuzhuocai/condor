package com.condor.launcher.switcher.effect;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.condor.launcher.switcher.SwitchEntry;
import com.condor.launcher.transformer.BaseTransformer;

/**
 * Created by Perry on 19-1-18
 */
public class Effect extends SwitchEntry {
    private final @StringRes int title;
    private final @DrawableRes int icon;
    private final BaseTransformer transformer;

    public Effect(@StringRes int title, @DrawableRes int icon, @NonNull BaseTransformer transformer) {
        super(transformer.getClass().getSimpleName());
        this.title = title;
        this.icon = icon;
        this.transformer = transformer;
    }

    public @StringRes int getTitle() {
        return title;
    }

    public @DrawableRes int getIcon() {
        return icon;
    }

    public BaseTransformer getTransformer() {
        return transformer;
    }
}
