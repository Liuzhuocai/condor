package com.condor.launcher.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.launcher3.Utilities;
import com.condor.launcher.switcher.desktopmode.StartPageHelper;
import com.condor.launcher.switcher.effect.EffectManager;
import com.condor.launcher.util.Constants;
import com.condor.launcher.util.Utils;

import java.io.PrintWriter;
import java.util.function.Supplier;

import static com.condor.launcher.switcher.Switcher.DESKTOP_LAYOUT_SWITCHER;
import static com.condor.launcher.switcher.Switcher.DESKTOP_MODE_SWITCHER;
import static com.condor.launcher.switcher.Switcher.EFFECT_SWITCHER;
import static com.condor.launcher.switcher.Switcher.WALLPAPER_LAYOUT_SWITCHER;

/**
 * Created by Perry on 19-1-11
 */
public class SettingsPersistence {
    private static final String TAG = "SettingsPersistence";

    public static final BooleanPersistence DEFAULT_LOADED = new BooleanPersistence(SettingsKey.KEY_FIRST_LOADED,
            false);
    // Perry: Cyclic sliding of desktop pages: start
    public static final BooleanPersistence DESKTOP_LOOP_OPEN = new BooleanPersistence(SettingsKey.KEY_DESKTOP_LOOP_OPEN,
            false);
    // Perry: Cyclic sliding of desktop pages: end
    public static final BooleanPersistence MINUS_ONE_ENABLE = new BooleanPersistence(SettingsKey.KEY_MINUS_ONE_ENABLE,
            Utils.hasGmsVersion());
    // Perry: Fixed screen function: start
    public static final BooleanPersistence SCREEN_LOCKED = new BooleanPersistence(SettingsKey.KEY_SCREEN_LOCKED,
            false);
    // Perry: Fixed screen function: end
    public static final StringPersistence DESKTOP_MODE = new StringPersistence(SettingsKey.KEY_DESKTOP_MODE,
            DESKTOP_MODE_SWITCHER.getDefault());
    public static final IntegerPersistence START_PAGE = new IntegerPersistence(SettingsKey.KEY_START_PAGE,
            StartPageHelper.INVALID_START_PAGE);
    // Perry: desktop layout switch function: start
    public static final StringPersistence DESKTOP_LAYOUT = new StringPersistence(DESKTOP_MODE::value,
            DESKTOP_LAYOUT_SWITCHER.getDefault());
    // Perry: desktop layout switch function: end

    // Perry: Implement sliding effect function: start
    public static final StringPersistence EFFECT = new StringPersistence(SettingsKey.KEY_EFFECT,
            EFFECT_SWITCHER.getDefault());
    // Perry: Implement sliding effect function: end

    // Perry: Add predicted applications: start
    public static final BooleanPersistence SHOW_PREDICTIONS = new BooleanPersistence(SettingsKey.KEY_SHOW_PREDICTIONS,
            true);
    // Perry: Add predicted applications: end

    // Perry: Add live icon: start
    public static final BooleanPersistence LIVE_ICON_ENABLE = new BooleanPersistence(SettingsKey.KEY_LIVE_ICON_ENABLE,
            false);
    // Perry: Add live icon: end

    // liuzuo: add for wallpaper layout: start
    public static final StringPersistence WALLPAPER_LAYOUT = new StringPersistence(SettingsKey.KEY_WALLPAPER_SETTINGS,
            WALLPAPER_LAYOUT_SWITCHER.getDefault());
    // liuzuo: add for wallpaper layout: end

    private static SharedPreferences prefs;

    public static void load(Context context) {
        prefs = Utilities.getPrefs(context);
        DEFAULT_LOADED.load();
        if (DEFAULT_LOADED.value()) {
            loadDefaultValuesFromPrefs(context);
        } else {
            loadDefaultValuesFromFile(context);
            DEFAULT_LOADED.save(true);
        }

        // Perry: convert old version settings: start
        convertOldVersionSettings();
        // Perry: convert old version settings: end
    }

    // Perry: convert old version settings: start
    private static void convertOldVersionSettings() {
        if (prefs.contains(Constants.OLD_VERSION_SCREEN_LOCK_SETTINGS)) {
            SCREEN_LOCKED.save(prefs.getBoolean(Constants.OLD_VERSION_SCREEN_LOCK_SETTINGS,
                    SCREEN_LOCKED.value()));
            prefs.edit().remove(Constants.OLD_VERSION_SCREEN_LOCK_SETTINGS).apply();
        }
        if (prefs.contains(Constants.OLD_VERSION_EFFECT_TYPE)) {
            int effectIndex = prefs.getInt(Constants.OLD_VERSION_EFFECT_TYPE, 0);
            EFFECT.save(EffectManager.getInstance().getEffect(effectIndex).value);
            prefs.edit().remove(Constants.OLD_VERSION_EFFECT_TYPE).apply();
        }
        if (prefs.contains(Constants.OLD_VERSION_MINUS_ONE_SETTINGS)) {
            MINUS_ONE_ENABLE.save(prefs.getBoolean(Constants.OLD_VERSION_MINUS_ONE_SETTINGS,
                    MINUS_ONE_ENABLE.value()));
            prefs.edit().remove(Constants.OLD_VERSION_MINUS_ONE_SETTINGS).apply();
        }
    }
    // Perry: convert old version settings: end

    public static void dump(PrintWriter pw) {
        StringBuilder sb = new StringBuilder("Settings = {");
        sb.append(DESKTOP_LOOP_OPEN).append(',');
        sb.append(MINUS_ONE_ENABLE).append(',');
        sb.append(SCREEN_LOCKED).append(",");
        sb.append(DESKTOP_MODE).append(',');
        sb.append(EFFECT).append(',');
        sb.append(LIVE_ICON_ENABLE).append(',');
        sb.append(START_PAGE);
        sb.append('}');
        pw.println(sb.toString());
    }

    private static void loadDefaultValuesFromPrefs(Context context) {
        DESKTOP_LOOP_OPEN.load();
        MINUS_ONE_ENABLE.load();
        SCREEN_LOCKED.load();
        DESKTOP_MODE.load();
        EFFECT.load();
        LIVE_ICON_ENABLE.load();
        START_PAGE.load();
        WALLPAPER_LAYOUT.load();
    }

    private static void loadDefaultValuesFromFile(Context context) {
        DefaultValuesParser parser = new DefaultValuesParser(context);
        parser.saveBooleanValue(DefaultKey.DESKTOP_LOOP_OPEN, DESKTOP_LOOP_OPEN);
        parser.saveBooleanValue(DefaultKey.MINUS_ONE_ENABLE, MINUS_ONE_ENABLE);
        parser.saveBooleanValue(DefaultKey.SCREEN_LOCKED, SCREEN_LOCKED);
        parser.saveBooleanValue(DefaultKey.LIVE_ICON_ENABLE, LIVE_ICON_ENABLE);
        parser.saveStringValue(DefaultKey.DESKTOP_MODE, DESKTOP_MODE);
        parser.saveStringValue(DefaultKey.EFFECT, EFFECT);
        parser.saveIntegerValue(DefaultKey.START_PAGE, START_PAGE);
    }

    public static SharedPreferences getPrefs() {
        return prefs;
    }

    public interface OnValueChangedListener {
        void onValueChanged(Persistence p);
    }

    public static abstract class Persistence<T> {
        private OnValueChangedListener listener;
        private Supplier<String> key;
        private T defaultValue;
        private T value;

        public Persistence(String key, T defaultValue) {
            this(()-> key, defaultValue);
        }

        public Persistence(Supplier<String> key, T defaultValue) {
            this.key = key;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public void registerValueChangedListener(OnValueChangedListener listener) {
            this.listener = listener;
        }

        public void unregisterValueChangedListener() {
            listener = null;
        }

        public T value() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }

        public T load() {
            this.value = loadValue(key.get(), value);
            return value;
        }

        public void save(T value) {
            save(key.get(), value);
        }

        public void save(String key, T value) {
            this.value = value;
            saveValue(key, value);
            if (listener != null) {
                listener.onValueChanged(this);
            }
        }

        public void remove() {
            this.value = defaultValue;
            prefs.edit().remove(key.get()).apply();
        }

        public boolean exist() {
            return prefs.contains(key.get());
        }

        public String key() {
            return key.get();
        }

        public T defaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return key.get() + ":" + value;
        }

        protected abstract T loadValue(String key, T defaultValue);
        protected abstract void saveValue(String key, T value);
    }

    public static class BooleanPersistence extends Persistence<Boolean> {

        public BooleanPersistence(String key, Boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Boolean loadValue(String key, Boolean defaultValue) {
            return prefs.getBoolean(key, defaultValue);
        }

        @Override
        protected void saveValue(String key, Boolean value) {
            prefs.edit().putBoolean(key, value).apply();
        }
    }

    public static class StringPersistence extends Persistence<String> {

        public StringPersistence(String key, String defaultValue) {
            super(key, defaultValue);
        }

        public StringPersistence(Supplier<String> key, String defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected String loadValue(String key, String defaultValue) {
            return prefs.getString(key, defaultValue);
        }

        @Override
        protected void saveValue(String key, String value) {
            prefs.edit().putString(key, value).apply();
        }
    }

    public static class IntegerPersistence extends Persistence<Integer> {

        public IntegerPersistence(String key, Integer defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Integer loadValue(String key, Integer defaultValue) {
            return prefs.getInt(key, defaultValue);
        }

        @Override
        protected void saveValue(String key, Integer value) {
            prefs.edit().putInt(key, value).apply();
        }
    }
}