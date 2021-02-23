package com.condor.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import com.android.launcher3.R;
import com.android.launcher3.SessionCommitReceiver;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.graphics.IconShapeOverride;
import com.condor.launcher.liveicon.LiveIconsManager;
import com.condor.launcher.settings.SettingsKey;
import com.condor.launcher.settings.SettingsPersistence;
import com.condor.launcher.switcher.Switcher;
import com.condor.launcher.switcher.desktopmode.DesktopModeHelper;
import com.condor.launcher.switcher.effect.EffectManager;
import com.condor.launcher.util.Utils;

import java.lang.reflect.Field;

import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.condor.launcher.settings.SettingsPersistence.DESKTOP_LOOP_OPEN;
import static com.condor.launcher.settings.SettingsPersistence.EFFECT;
import static com.condor.launcher.settings.SettingsPersistence.LIVE_ICON_ENABLE;
import static com.condor.launcher.settings.SettingsPersistence.MINUS_ONE_ENABLE;
import static com.condor.launcher.settings.SettingsPersistence.SCREEN_LOCKED;
import static com.condor.launcher.switcher.Switcher.DESKTOP_LAYOUT_SWITCHER;
import static com.condor.launcher.switcher.Switcher.DESKTOP_MODE_SWITCHER;
import static com.condor.launcher.switcher.Switcher.WALLPAPER_LAYOUT_SWITCHER;

/**
 * Created by Perry on 19-1-11
 */
public class CLauncherSettingsActivity extends SettingsActivity {
    // Perry: adjust settings UI: start
    public static final int FAST_ACTIVITY = 1;
    public static final int FULLSCREEN_ACTIVITY = 1 << 1;
    public static final int ALL = FAST_ACTIVITY | FULLSCREEN_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Perry: Set traslucent to statusbar and navigationbar: start
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setElevation(0);
        // Perry: Set traslucent to statusbar and navigationbar: end
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    // Perry: adjust settings UI: end

    @Override
    protected PreferenceFragment getNewFragment() {
        // Perry: adjust settings UI: start
        return CLauncherSettingsFragment.create(FULLSCREEN_ACTIVITY);
        // Perry: adjust settings UI: end
    }

    public static class CLauncherSettingsFragment extends SettingsActivity.LauncherSettingsFragment {
        // Perry: adjust settings UI: start
        private int mState;

        public static CLauncherSettingsFragment create(int state) {
            CLauncherSettingsFragment fragment = new CLauncherSettingsFragment();
            Bundle arguments = new Bundle();
            arguments.putInt("state", state);
            fragment.setArguments(arguments);
            return fragment;
        }
        // Perry: adjust settings UI: end

        private interface PreferenceValid {
            boolean apply();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Perry: adjust settings UI: start
            mState = getArguments().getInt("state", ALL);

            PreferenceCategory quickOptionsCategory = (PreferenceCategory)
                    findPreference(SettingsKey.KEY_QUICK_OPTIONS_CATEGORY);
            PreferenceCategory moreFunctionCategory = (PreferenceCategory)
                    findPreference(SettingsKey.KEY_MORE_FUNCTION_CATEGORY);
            // Perry: Cyclic sliding of desktop pages: start
            initSwitchPreference(quickOptionsCategory, DESKTOP_LOOP_OPEN,
                    ()-> isInState(ALL));
            // Perry: Cyclic sliding of desktop pages: end
            // Perry: Add live icon: start
            initSwitchPreference(quickOptionsCategory, LIVE_ICON_ENABLE,
                    ()-> LiveIconsManager.obtain().supportLiveIcons() &&
                            isInState(FAST_ACTIVITY));
            // Perry: Add live icon: end
            initSwitchPreference(quickOptionsCategory, MINUS_ONE_ENABLE,
                    ()-> Utils.hasGmsVersion() && isInState(ALL));
            // Perry: Fixed screen function: start
            initSwitchPreference(quickOptionsCategory, SCREEN_LOCKED,
                    ()-> isInState(ALL));
            // Perry: Fixed screen function: end
            initListPreference(moreFunctionCategory, DESKTOP_MODE_SWITCHER,
                    ()-> CondorFeatureFlags.SUPPORT_DESKTOP_MODE_SWITCH && isInState(FULLSCREEN_ACTIVITY));
            initListPreference(moreFunctionCategory, SettingsKey.KEY_DESKTOP_LAYOUT,
                    DESKTOP_LAYOUT_SWITCHER, ()-> isInState(FULLSCREEN_ACTIVITY));
            // Perry: Implement sliding effect function: start
            // liuzuo: add for wallpaper layout: start
            initListPreference(moreFunctionCategory, WALLPAPER_LAYOUT_SWITCHER,
                    ()-> isInState(FULLSCREEN_ACTIVITY));
            // liuzuo: add for wallpaper layout: end
            initClickPreference(moreFunctionCategory, EFFECT.key(),
                    (p)-> {
                        Action.create(Action.ACTION_ENTER_EFFECT_PANEL).setHandler(launcher -> {
                            if (!launcher.isInState(EDIT_MODE)) {
                                launcher.getStateManager().goToState(EDIT_MODE, 0, ()->
                                        launcher.getEditModePanel().switchToEffectPanel());
                            } else {
                                launcher.getEditModePanel().switchToEffectPanel();
                            }
                        }).attach(getContext());
                        // Perry: Optimizing settings: start
                        getActivity().finish();
                        // Perry: Optimizing settings: end
                        return true;
                    },
                    () -> isInState(ALL));
            // Perry: Implement sliding effect function: end
            initClickPreference(moreFunctionCategory, SettingsKey.KEY_MORE_SETTINGS,
                    (p)-> {
                        startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                                .setPackage(getContext().getPackageName())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        return true;
                    },
                    ()-> isInState(FAST_ACTIVITY));
            // Perry: Implement frozen apps: start
            initClickPreference(moreFunctionCategory, SettingsKey.KEY_APP_FROZEN,
                    (p)-> {
                        Action.create(Action.ACTION_ENTER_FROZEN_PANEL).setHandler(launcher -> {
                            launcher.getAppsView().setState(AllAppsContainerView.State.FROZEN);
                            launcher.getStateManager().goToState(ALL_APPS);
                        }).attach(getContext());
                        getActivity().finish();
                        return true;
                    },
                    ()-> isInState(ALL));
            // Perry: Implement frozen apps: end
            if (isInState(FAST_ACTIVITY)) {
                quickOptionsCategory.removePreference(findPreference(ICON_BADGING_PREFERENCE_KEY));
                quickOptionsCategory.removePreference(findPreference(ICON_BADGING_PREFERENCE_KEY_GO));
                quickOptionsCategory.removePreference(findPreference(ALLOW_ROTATION_PREFERENCE_KEY));
                quickOptionsCategory.removePreference(findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                moreFunctionCategory.removePreference(findPreference(IconShapeOverride.KEY_PREFERENCE));

                addAllPreferenceToScreen(quickOptionsCategory);
                addAllPreferenceToScreen(moreFunctionCategory);

                // Perry: Hide icon in fast settings: start
                for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                    Preference preference = getPreferenceScreen().getPreference(i);
                    hidePreferenceIcon(preference);
                    // Perry: adjust settings UI: start
                    hidePreferenceSummary(preference);
                    // Perry: adjust settings UI: end
                }
                // Perry: Hide icon in fast settings: end
            } else {
                // Perry: adjust settings UI: start
                if (DesktopModeHelper.isDefaultMode()) {
                    quickOptionsCategory.removePreference(findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                    // Perry: Icon shape override for different desktop modes: start
                    moreFunctionCategory.removePreference(findPreference(IconShapeOverride.KEY_PREFERENCE));
                    // Perry: Icon shape override for different desktop modes: end
                } else{
                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("com.android.launcher3.prefs", Context.MODE_PRIVATE);
                    if( !sharedPreferences.getBoolean("isDefaultTheme",true)){
                        quickOptionsCategory.removePreference(findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                        // Perry: Icon shape override for different desktop modes: start
                        moreFunctionCategory.removePreference(findPreference(IconShapeOverride.KEY_PREFERENCE));
                        // Perry: Icon shape override for different desktop modes: end
                    }
                }
                // Perry: adjust settings UI: end
                if(FeatureFlags.UNREAD_MESSAGE){
                    quickOptionsCategory.removePreference(findPreference(ICON_BADGING_PREFERENCE_KEY));
                }else {
                    quickOptionsCategory.removePreference(findPreference(ICON_BADGING_PREFERENCE_KEY_GO));
                }
            }
            // Perry: adjust settings UI: end


        }

        // Perry: Hide icon in fast settings: start
        private void hidePreferenceIcon(Preference preference) {
            preference.setIcon(null);
            try {
                Field field = Preference.class.getDeclaredField("mIconResId");
                field.setAccessible(true);
                field.set(preference, 0);
            }catch (Exception e) {
            }
        }
        // Perry: Hide icon in fast settings: end

        private void hidePreferenceSummary(Preference preference) {
            preference.setSummary(null);
        }

        // Perry: adjust settings UI: start
        private void addAllPreferenceToScreen(PreferenceCategory category) {
            int count = category.getPreferenceCount();
            for (int i =0; i < count; i++) {
                Preference preference = category.getPreference(i);
                getPreferenceScreen().addPreference(preference);
            }
            category.removeAll();
            getPreferenceScreen().removePreference(category);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            View rootView = getView();
            ListView list = rootView.findViewById(android.R.id.list);
            // Perry: Hide icon in fast settings: start
            Resources res = getResources();
            Drawable bg = res.getDrawable(R.drawable.condor_settings_divider_bg,
                    getContext().getTheme());
            Drawable divider;
            if (isInState(FAST_ACTIVITY)) {
                divider = new InsetDrawable(bg, res.getDimensionPixelSize(R.dimen.condor_settings_padding_left),
                        0, 0, 0);
            } else {
                divider = new InsetDrawable(bg, Utils.dp2px(getContext(), 57),
                        0, 0, 0);
            }

            list.setDivider(divider);
            // Perry: Hide icon in fast settings: end
        }

        // Perry: Show current effect type in fullscreen settings: start
        @Override
        public void onResume() {
            super.onResume();
            if (isInState(FULLSCREEN_ACTIVITY)) {
                Preference preference = findPreference(EFFECT.key());
                if (preference != null) {
                    // Perry: Effect summary error: start
                    preference.setSummary(EffectManager.getInstance().
                            getEffect(EFFECT.value()).getTitle());
                    // Perry: Effect summary error: end
                }
            }
        }
        // Perry: Show current effect type in fullscreen settings: end

        private boolean isInState(int state) {
            return (mState & state) != 0;
        }
        // Perry: adjust settings UI: end

        // Perry: Implement sliding effect function: start
        private void initClickPreference(PreferenceCategory category,
                                         String key,
                                         Preference.OnPreferenceClickListener listener,
                                         PreferenceValid valid) {
            Preference preference = findPreference(key);
            if (valid.apply()) {
                preference.setOnPreferenceClickListener(listener);
            } else {
                category.removePreference(preference);
            }
        }
        // Perry: Implement sliding effect function: end

        private void initSwitchPreference(PreferenceCategory category,
                                    SettingsPersistence.BooleanPersistence persistence,
                                    PreferenceValid valid) {
            SwitchPreference preference = (SwitchPreference) findPreference(persistence.key());
            if (valid.apply()) {
                preference.setDefaultValue(persistence.value());
                preference.setChecked(persistence.value());
                preference.setOnPreferenceChangeListener((pref, newValue) -> {
                    persistence.save((Boolean)newValue);
                    return true;
                });
            } else {
                category.removePreference(preference);
            }
        }

        private void initListPreference(PreferenceCategory category,
                                        Switcher switcher, PreferenceValid valid) {
            initListPreference(category, switcher.getPersistence().key(), switcher, valid);
        }

        private void initListPreference(PreferenceCategory category,
                                        String key, Switcher switcher, PreferenceValid valid) {
            SettingsPersistence.StringPersistence persistence = switcher.getPersistence();
            ListPreference preference = (ListPreference) findPreference(key);
            if (valid.apply()) {
                preference.setEntryValues(switcher.entryValues(getContext()));
                preference.setEntries(switcher.entries(getContext()));
                preference.setValue(persistence.value());
                preference.setOnPreferenceChangeListener((pref, newValue) ->
                        switcher.doSwitch(getContext(), (String)newValue));
            } else {
                category.removePreference(preference);
            }
        }

    }
}
