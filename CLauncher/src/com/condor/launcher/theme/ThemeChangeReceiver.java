package com.condor.launcher.theme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.launcher3.LauncherAppState;
import com.condor.launcher.util.ThemeUtils;

public class ThemeChangeReceiver extends BroadcastReceiver {
    public static final String action ="com.cyee.intent.action.theme.change";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("c","theme changed:"+intent.getAction());
        Intent intentw = new Intent(Intent.ACTION_MAIN);
        intentw.addCategory(Intent.CATEGORY_HOME);
        intentw.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentw.setPackage(context.getApplicationContext().getPackageName());
        //Log.d("c","theme changed:"+intent.getAction()+" ,  "+context.getApplicationContext().getPackageName());
        context.getApplicationContext().startActivity(intentw);
        LauncherAppState.setInstanceNull();
        LauncherAppState.getInstance(context.getApplicationContext()).clearIcons();
        LauncherAppState.getInstance(context.getApplicationContext()).isThemeChanged();
        LauncherAppState.getInstance(context.getApplicationContext()).getModel().forceReload();
        ThemeUtils.restart(context);
        /*SharedPreferences sharedPreferences = context.getSharedPreferences("com.android.launcher3.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean isDefault = !ThemeIconProvider.isThemeAvailableOnExternal();
        editor.putBoolean("isDefaultTheme",isDefault);
        editor.putLong("modifyTime", isDefault?0:ThemeIconProvider.getThemeModifyTime());
        editor.apply();*/
    }
}
