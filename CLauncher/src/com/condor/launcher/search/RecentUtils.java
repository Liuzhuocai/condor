package com.condor.launcher.search;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.UserHandle;


import com.android.launcher3.AppInfo;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.model.CondorLauncherSettings;

import java.util.ArrayList;

/**
 * Created by Perry on 19-1-24
 */
public class RecentUtils {
    public static void addRecentItem(Context context, SearchKey key) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues v = new ContentValues();
        v.put(CondorLauncherSettings.SearchRecent.ITEM_TYPE, key.getType());
        v.put(CondorLauncherSettings.SearchRecent.ITEM_ID, key.getItemId());
        if (key instanceof ComponentKey) {
            v.put(CondorLauncherSettings.SearchRecent.COMPONENT, ((ComponentKey) key).componentName.flattenToString());
            v.put(CondorLauncherSettings.SearchRecent.PROFILE_ID, UserManagerCompat.getInstance(context).
                    getSerialNumberForUser(((ComponentKey) key).user));
        }
        cr.insert(CondorLauncherSettings.SearchRecent.CONTENT_URI, v);
    }

    public static void addRecentItem(Context context, AppInfo info) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues v = new ContentValues();
        v.put(CondorLauncherSettings.SearchRecent.ITEM_TYPE, SearchKey.SEARCH_TYPE_APP);
        v.put(CondorLauncherSettings.SearchRecent.COMPONENT, info.getTargetComponent().flattenToString());
        v.put(CondorLauncherSettings.SearchRecent.PROFILE_ID, UserManagerCompat.getInstance(context).
                getSerialNumberForUser(info.user));
        cr.insert(CondorLauncherSettings.SearchRecent.CONTENT_URI, v);
    }

    public static void updateRecentItem(Context context, SearchKey key) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues v = new ContentValues();
        String where = CondorLauncherSettings.SearchRecent.ITEM_TYPE + "=" + key.getItemId() +
                " AND " + CondorLauncherSettings.SearchRecent.ITEM_ID + "=" + key.getType();
        if (key instanceof ComponentKey) {
            where = CondorLauncherSettings.SearchRecent.ITEM_TYPE + "=" + key.getItemId() +
                    " AND " + CondorLauncherSettings.SearchRecent.COMPONENT + "='" +
                    ((ComponentKey) key).componentName.flattenToString() +
                    "' AND " + CondorLauncherSettings.SearchRecent.PROFILE_ID + "=" +
                    UserManagerCompat.getInstance(context).
                            getSerialNumberForUser(((ComponentKey) key).user);
        }
        cr.update(CondorLauncherSettings.SearchRecent.CONTENT_URI, v, where, null);
    }

    public static void updateRecentItem(Context context, AppInfo info) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues v = new ContentValues();
        String where = CondorLauncherSettings.SearchRecent.ITEM_TYPE + "=" + SearchKey.SEARCH_TYPE_APP +
                " AND " + CondorLauncherSettings.SearchRecent.COMPONENT + "='" +
                info.getTargetComponent().flattenToString() +
                "' AND " + CondorLauncherSettings.SearchRecent.PROFILE_ID + "=" +
                UserManagerCompat.getInstance(context).
                        getSerialNumberForUser(info.user);
        cr.update(CondorLauncherSettings.SearchRecent.CONTENT_URI, v, where, null);
    }

    public static void clearRecentItems(Context context) {
        final ContentResolver cr = context.getContentResolver();
        cr.delete(CondorLauncherSettings.SearchRecent.CONTENT_URI, null, null);
    }

    public static ArrayList<SearchKey> loadRecentItems(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final ArrayList<SearchKey> result = new ArrayList<>();
        final UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        Cursor c = null;
        try {
            c = cr.query(CondorLauncherSettings.SearchRecent.CONTENT_URI,
                    null, null, null, CondorLauncherSettings.SearchRecent.MODIFIED + " DESC");
            if (c == null || c.getCount() <= 0) {
                return result;
            }

            final int indexOfItemType = c.getColumnIndexOrThrow(CondorLauncherSettings.SearchRecent.ITEM_TYPE);
            final int indexOfItemId = c.getColumnIndexOrThrow(CondorLauncherSettings.SearchRecent.ITEM_ID);
            final int indexOfComponent = c.getColumnIndexOrThrow(CondorLauncherSettings.SearchRecent.COMPONENT);
            final int indexOfUserId = c.getColumnIndexOrThrow(CondorLauncherSettings.SearchRecent.PROFILE_ID);

            while (c.moveToNext()) {
                final int itemType = c.getInt(indexOfItemType);
                if (itemType == SearchKey.SEARCH_TYPE_APP) {
                    final ComponentName component = ComponentName.unflattenFromString(c.getString(indexOfComponent));
                    final int serialNumber = c.getInt(indexOfUserId);
                    UserHandle user = userManager.getUserForSerialNumber(serialNumber);
                    result.add(new ComponentKey(component, user));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }
}
