package com.condor.launcher.unreadnotifier.email;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.android.launcher3.R;
import com.condor.launcher.unreadnotifier.BaseContentObserver;
import com.condor.launcher.unreadnotifier.UnreadBaseItem;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;
import com.condor.launcher.unreadnotifier.UnreadUtils;

import java.util.ArrayList;

/**
 * Created by Bruce on 1/30/19.
 */

public class UnreadEmailItem extends UnreadBaseItem {
    private static final String TAG = "UnreadEmailItem";
    private static final Uri EMAILS_CONTENT_URI = Uri.parse("content://com.android.email.provider/mailbox");
    private static final Uri EMAILS_NOTIFY_URI = Uri.parse("content://com.android.email.notifier");

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.email",
            "com.android.email.activity.Welcome");

    private static final String PROP_DEFAULT_EMAIL = "ro.launcher.unread.email";

    public UnreadEmailItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, EMAILS_NOTIFY_URI, this);
        mPermission = /*false && SystemProperties.is_qmb_pk() ? "com.google.android.gm.email.permission.ACCESS_PROVIDER" :*/ "com.android.email.permission.ACCESS_PROVIDER";
        mPrefKey = UnreadUtils.PREF_KEY_UNREAD_EMAIL;
        mType = UnreadInfoManager.TYPE_EMAIL;
        mDefaultCn = /*false && SystemProperties.is_qmb_pk() ? new ComponentName("com.google.android.gm.lite", "com.google.android.gm.ConversationListActivityGmail") :*/ DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_email_enable);
        mDefaultState = defaultState /*SystemPropertiesUtils.getBoolean(PROP_DEFAULT_EMAIL, defaultState)*/;
    }

    @Override
    public int readUnreadCount() {
        int unreadEmail = 0;
        int unRead;

        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(EMAILS_CONTENT_URI, new String[]{"unreadCount"},
                    "type = ?", new String[]{"0"}, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    unRead = cursor.getInt(0);
                    if (unRead > 0) {
                        unreadEmail += unRead;
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            UnreadUtils.closeCursorSilently(cursor);
        }

        Log.d(TAG, "readUnreadCount: unreadEmail = " + unreadEmail);

        return unreadEmail;
    }

//    @Override
//    public String getUnreadHintString() {
//        String name = mContext.getString(R.string.unread_email);
//        return mContext.getString(R.string.unread_hint, name);
//    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] emailLists = context.getResources().getStringArray(R.array.support_email_component_array);

        ArrayList<String> installEmailList = new ArrayList<>();
        for (String emailList : emailLists) {
            ComponentName componentName = ComponentName.unflattenFromString(emailList);
            boolean isInstalled = UnreadUtils.isAppInstalled(context, componentName.getPackageName(), android.os.Process.myUserHandle());
            if (isInstalled) {
                installEmailList.add(componentName.flattenToShortString());
            }
        }

        return installEmailList;
    }
}
