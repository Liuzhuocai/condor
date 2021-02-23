package com.condor.launcher.unreadnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.LauncherAppsCompat;

public class UnreadUtils {
    private final static String TAG = "UnreadUtils";

    public static final String PREF_KEY_MISS_CALL = "pref_missed_call_count";
    public static final String PREF_KEY_UNREAD_SMS = "pref_unread_sms_count";
    public static final String PREF_KEY_UNREAD_EMAIL = "pref_unread_email_count";
    public static final String PREF_KEY_UNREAD_CALENDAR = "pref_unread_calendar_count";

    private static final int MAX_UNREAD_COUNT = 99;

    public static void closeCursorSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable t) {
            Log.w(TAG, "fail to close", t);
        }
    }

    public static boolean isAppInstalled(Context context, String pkgName, UserHandle user) {
        return LauncherAppsCompat.getInstance(context).isPackageEnabledForProfile(pkgName, user);
    }

    private static final String CHECKBOX_KEY = "_checked";
    public static boolean isPreferenceChecked(Context context, String prefKey, boolean defValue) {
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharePref.getBoolean(prefKey + CHECKBOX_KEY, defValue);
    }

    public static Point getTextDrawPoint(Rect targetRect, Paint.FontMetrics fm) {
        Point p = new Point();
        int fontHeight = Math.round(fm.descent - fm.ascent);
        int paddingY = (targetRect.height() - fontHeight) >> 1;
        p.x = targetRect.centerX();
        p.y = targetRect.top + paddingY + Math.abs(Math.round(fm.ascent));
        return p;
    }

    /**
     * Draw unread number for the given icon.
     *
     * @param canvas
     * @param icon
     * @return
     */
    public static void drawUnreadEventIfNeed(Canvas canvas, View icon, int count) {
        if (icon.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) icon.getTag();
            if (count > 0 && isUnreadItemType(info.itemType)) {
                String unreadText;
                if (count > MAX_UNREAD_COUNT) {
                    unreadText = Integer.toString(MAX_UNREAD_COUNT) + "+";
                } else {
                    unreadText = Integer.toString(count);
                }
                BadgeUtils.drawBadge(canvas, icon, unreadText);
            }
        }
    }

    public static boolean isUnreadItemType(int itemType) {
        boolean ret = false;

        switch (itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                ret = true;
                break;
            default:
                break;
        }
        return ret;
    }

}
