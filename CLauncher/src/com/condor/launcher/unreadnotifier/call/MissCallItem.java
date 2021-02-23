package com.condor.launcher.unreadnotifier.call;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.util.Log;

import com.android.launcher3.R;
import com.condor.launcher.unreadnotifier.BaseContentObserver;
import com.condor.launcher.unreadnotifier.UnreadBaseItem;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;
import com.condor.launcher.unreadnotifier.UnreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bruce on 2019/1/30.
 */

public class MissCallItem extends UnreadBaseItem {
    private static final String TAG = "MissCallItem";
    private static final Uri CALLS_CONTENT_URI = CallLog.Calls.CONTENT_URI;
    private static final String MISSED_CALLS_SELECTION =
            CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.NEW + " = 1";

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.dialer",
            "com.android.dialer.app.DialtactsActivity");

    public MissCallItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, CALLS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_CALL_LOG;
        mPrefKey = UnreadUtils.PREF_KEY_MISS_CALL;
        mType = UnreadInfoManager.TYPE_CALL_LOG;
        mDefaultCn = DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_call_enable);
        mDefaultState = defaultState /*SystemPropertiesUtils.getBoolean(PROP_DEFAULT_CALL, defaultState)*/;
    }

    @Override
    public int readUnreadCount() {
        int missedCalls = 0;
        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(CALLS_CONTENT_URI, new String[]{BaseColumns._ID},
                    MISSED_CALLS_SELECTION, null, null);
            if(cursor != null) {
                missedCalls = cursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            UnreadUtils.closeCursorSilently(cursor);
        }

        Log.d(TAG, "readUnreadCount, missedCalls = "+missedCalls);

        return missedCalls;
    }

//    public String getUnreadHintString() {
//        String name = mContext.getString(R.string.unread_call);
//        return mContext.getString(R.string.unread_hint, name);
//    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        ArrayList<String> installedPhoneList = new ArrayList<>();
        /*vCollection<CallAppUtils.CallApplicationData> callApplications =
                CallAppUtils.getInstalledDialerApplications(context);


        for (CallAppUtils.CallApplicationData callApplication : callApplications) {
            ComponentName componentName = new ComponentName(callApplication.mPackageName, callApplication.callClassName);
            mDefaultCn = componentName;
            Log.d("liuzuo97","loadApps call componentName = "+componentName);
            String scName = componentName.flattenToShortString();
            installedPhoneList.add(scName);
        }*/
        Intent intent = new Intent(Intent.ACTION_CALL)
                /*.setData(ContactsContract.Contacts.CONTENT_URI)*/;
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        final List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(
                intent, PackageManager.MATCH_ALL);

        for (ResolveInfo info:list){
            if (info.activityInfo != null && info.activityInfo.applicationInfo != null) {
                if((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0){
                    String defaultPkg = info.activityInfo.applicationInfo.packageName;
                    String defaultcls = info.activityInfo.name;
                    ComponentName componentName = new ComponentName(defaultPkg, defaultcls);
                    mDefaultCn = componentName;
                    String scName = componentName.flattenToShortString();
                    installedPhoneList.add(scName);
                    Log.d("liuzuo97","loadApps call  componentName="+componentName);
                }
            }
        }
        if(installedPhoneList.size()==0){
            installedPhoneList.add(mDefaultCn.flattenToShortString());
        }
        return installedPhoneList;
    }
}