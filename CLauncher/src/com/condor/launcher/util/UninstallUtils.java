package com.condor.launcher.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.Toast;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.compat.LauncherAppsCompat;

/**
 * Created by Perry on 19-1-14
 */
public class UninstallUtils {
    public static boolean supportUninstall(Context context, ItemInfo info) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bundle restrictions = userManager.getUserRestrictions();
        if (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)) {
            return false;
        }

        return getUninstallTarget(context, info) != null;
    }

    public static boolean startUninstallActivity(
            final Context context, ItemInfo info) {
        final ComponentName cn = getUninstallTarget(context, info);

        final boolean isUninstallable;
        if (cn == null) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            Toast.makeText(context, R.string.uninstall_system_app_text, Toast.LENGTH_SHORT).show();
            isUninstallable = false;
        } else {
            Intent intent = new Intent(Intent.ACTION_DELETE,
                    Uri.fromParts("package", cn.getPackageName(), cn.getClassName()))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtra(Intent.EXTRA_USER, info.user);
            context.startActivity(intent);
            isUninstallable = true;
        }

        return isUninstallable;
    }

    /**
     * @return the component name that should be uninstalled or null.
     */
    public static ComponentName getUninstallTarget(Context context, ItemInfo item) {
        Intent intent = null;
        UserHandle user = null;
        if (item != null &&
                item.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION) {
            intent = item.getIntent();
            user = item.user;
        }
        if (intent != null) {
            LauncherActivityInfo info = LauncherAppsCompat.getInstance(context)
                    .resolveActivity(intent, user);
            if (info != null
                    && (info.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return info.getComponentName();
            }
        }
        return null;
    }
}
