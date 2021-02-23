package com.condor.launcher.switcher.effect;

import android.app.Activity;
import android.content.Context;

import com.android.launcher3.SettingsActivity;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.Switcher;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by Perry on 19-1-18
 */
public class EffectSwitcher implements Switcher<Effect> {
    @Override
    public String getDefault() {
        return EffectManager.getInstance().getDefaultEffect().value;
    }

    @Override
    public CharSequence[] entryValues(Context context) {
        Collection<Effect> effects = EffectManager.getInstance().getEffects();
        return effects.stream().map((effect)-> effect.value).collect(Collectors.toList()).
                toArray(new String[effects.size()]);
    }

    @Override
    public CharSequence[] entries(Context context) {
        Collection<Effect> effects = EffectManager.getInstance().getEffects();
        return effects.stream().map((effect)-> context.getString(effect.getTitle())).
                collect(Collectors.toList()).toArray(new String[effects.size()]);
    }

    @Override
    public boolean doSwitch(Context context, String value) {
        getPersistence().save(value);
        if (context instanceof SettingsActivity) {
            ((Activity)context).finish();
        }
        return true;
    }

    @Override
    public Effect get() {
        return EffectManager.getInstance().getEffect(getPersistence().value());
    }

    @Override
    public SettingsPersistence.StringPersistence getPersistence() {
        return SettingsPersistence.EFFECT;
    }
}
