package com.condor.launcher.locktask;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;

import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.condor.launcher.model.CondorLauncherSettings;
import com.condor.launcher.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Perry on 19-1-24
 */
public class LockTaskModel {
    private static final String TAG = "LockTaskModel";
    private static final Uri CONTENT_URI = CondorLauncherSettings.LockedTasks.CONTENT_URI;

    public static void registerObserver(Context context, ContentObserver observer) {
        final ContentResolver cr = context.getContentResolver();
        cr.registerContentObserver(CONTENT_URI, true, observer);
    }

    public static void unregisterObserver(Context context, ContentObserver observer) {
        final ContentResolver cr = context.getContentResolver();
        cr.unregisterContentObserver(observer);
    }

    public static List<ComponentKey> loadLockTasks(Context context) {
        List<ComponentKey> lockedTasks = new ArrayList<>();
        final ContentResolver cr = context.getContentResolver();
        final UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        Cursor c = cr.query(CONTENT_URI, null, null, null, null);
        try {
            final int componentIndex = c.getColumnIndexOrThrow(CondorLauncherSettings.LockedTasks.COMPONENT);
            final int profileIdIndex = c.getColumnIndexOrThrow(CondorLauncherSettings.LockedTasks.PROFILE_ID);
            while (c.moveToNext()) {
                ComponentName component = ComponentName.unflattenFromString(c.getString(componentIndex));
                int serialNumber = c.getInt(profileIdIndex);
                UserHandle user = userManager.getUserForSerialNumber(serialNumber);
                lockedTasks.add(new ComponentKey(component, user));
            }
        } catch (Exception e) {
            Logger.e(TAG, "Load hide apps failed", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return lockedTasks;
    }

    public static void addLockTask(Context context, ComponentKey componentKey) {
        final ContentResolver cr = context.getContentResolver();
        final ContentValues v = new ContentValues();
        v.put(CondorLauncherSettings.LockedTasks.COMPONENT, componentKey.componentName.flattenToString());
        v.put(CondorLauncherSettings.LockedTasks.PROFILE_ID, componentKey.user.getIdentifier());
        cr.insert(CONTENT_URI, v);
    }

    public static void removeLockTask(Context context, ComponentKey componentKey) {
        final ContentResolver cr = context.getContentResolver();
        String selection = CondorLauncherSettings.LockedTasks.COMPONENT + "=?" +
                " AND " + CondorLauncherSettings.LockedTasks.PROFILE_ID + "=?";
        String[] selectionArgs = new String[] {componentKey.componentName.flattenToString(),
                String.valueOf(componentKey.user.getIdentifier())};
        cr.delete(CONTENT_URI, selection, selectionArgs);
    }
}
