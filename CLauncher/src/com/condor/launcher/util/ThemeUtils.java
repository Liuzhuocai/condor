package com.condor.launcher.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.util.LooperExecutor;
import com.condor.launcher.theme.bean.ThemeConfigBean;

/**
 * author：liuzuo on 19-1-14 16:50
 */
public class ThemeUtils {

    //liuzuo:get theme config:begin
    public static ThemeConfigBean getThemeConfigBean() {
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if(instanceNoCreate!=null){
            return instanceNoCreate.getIconCache().getThemeConfigBean();
        }
        return null;
    }
    public static Bitmap getBitmapByName(String name) {
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if(instanceNoCreate!=null){
            return instanceNoCreate.getIconCache().getBitmapByName(name);
        }
        return null;
    }
    public static void normalizatiionThemeConfigBean(Context context, ThemeConfigBean themeConfigBean) {
        float density = 3f/*context.getResources().getDisplayMetrics().density*/;
        themeConfigBean.setCoordinate_x(themeConfigBean.getCoordinate_x()*density);
        themeConfigBean.setCoordinate_y(themeConfigBean.getCoordinate_y()*density);
        themeConfigBean.setPadding_left(themeConfigBean.getPadding_left()*density);
        themeConfigBean.setPadding_right(themeConfigBean.getPadding_right()*density);
        themeConfigBean.setPadding_top(themeConfigBean.getPadding_top()*density);
        themeConfigBean.setPadding_bottom(themeConfigBean.getPadding_bottom()*density);
        try {
            Bitmap bitmapByName = ThemeUtils.getBitmapByName("folder_icon_bg.png");
            if(bitmapByName!=null){
                themeConfigBean.setFolder_bg_size(bitmapByName.getWidth());
            }else {
                themeConfigBean.setFolder_bg_size(186);
            }
        }catch (Exception e){
            themeConfigBean.setFolder_bg_size(186);
        }
    }

    public static void restart(final Context context) {
        new LooperExecutor(LauncherModel.getWorkerLooper()).execute(new Runnable() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (Exception e) {
                }

                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setPackage(context.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 50, pendingIntent);

                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }
    //liuzuo:get theme config:end
}
