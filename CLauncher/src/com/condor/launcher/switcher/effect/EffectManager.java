package com.condor.launcher.switcher.effect;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.android.launcher3.R;
import com.condor.launcher.transformer.BaseTransformer;
import com.condor.launcher.transformer.Cube;
import com.condor.launcher.transformer.Card;
import com.condor.launcher.transformer.Classic;
import com.condor.launcher.transformer.Compass;
import com.condor.launcher.transformer.Layered;
import com.condor.launcher.transformer.PageTurn;
import com.condor.launcher.transformer.Rotate;
import com.condor.launcher.transformer.Turntable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Created by Perry on 19-1-18
 */
public class EffectManager {
    private LinkedHashMap<String, Effect> mEffects = new LinkedHashMap<>();

    public Effect getDefaultEffect() {
        return mEffects.get(Classic.class.getSimpleName());
    }

    public static class SingletonHolder {
        public static final EffectManager INSTANCE = new EffectManager();
    }

    public static EffectManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private EffectManager() {
        // Perry: Implement sliding effect function: start
        put(R.string.effect_classic,    R.drawable.anim_classic_button, new Classic());
        put(R.string.effect_turntable,  R.drawable.anim_turntable_button, new Turntable());
        put(R.string.effect_layered,    R.drawable.anim_layered_button, new Layered());
        put(R.string.effect_rotate,     R.drawable.anim_rotate_button, new Rotate());
        put(R.string.effect_page_turn,  R.drawable.anim_page_turn_button, new PageTurn());
        put(R.string.effect_card,       R.drawable.anim_card_button, new Card());
        put(R.string.effect_compass,    R.drawable.anim_compass_button, new Compass());
        put(R.string.effect_cube,       R.drawable.anim_cube_button, new Cube());
        // Perry: Implement sliding effect function: end
    }
    
    private void put(@StringRes int titleResId,
                     @DrawableRes int iconResId,
                     BaseTransformer transformer) {
        Effect effect = new Effect(titleResId, iconResId, transformer);
        mEffects.put(effect.value, effect);
    }

    public Collection<Effect> getEffects() {
        return mEffects.values();
    }

    public Effect getEffect(String id) {
        return mEffects.getOrDefault(id, getDefaultEffect());
    }

    public Effect getEffect(int index) {
        Effect effect = new ArrayList<>(mEffects.values()).get(index);
        if (effect == null) {
            return getDefaultEffect();
        }
        return effect;
    }
                     
}
