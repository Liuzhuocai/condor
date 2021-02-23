package com.condor.launcher.unreadnotifier.sms;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;

import com.android.launcher3.R;
import com.condor.launcher.unreadnotifier.BaseContentObserver;
import com.condor.launcher.unreadnotifier.MMSAppUtils;
import com.condor.launcher.unreadnotifier.UnreadBaseItem;
import com.condor.launcher.unreadnotifier.UnreadInfoManager;
import com.condor.launcher.unreadnotifier.UnreadUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Bruce on 2019/1/30.
 */

public class UnreadMessageItem extends UnreadBaseItem {
    private static final String TAG = "MessageUnreadItem";
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms");

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.messaging",
            "com.android.messaging.ui.conversationlist.ConversationListActivity");

    private static final String PROP_DEFAULT_SMS = "ro.launcher.unread.sms";

    public UnreadMessageItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, MMSSMS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_SMS;
        mPrefKey = UnreadUtils.PREF_KEY_UNREAD_SMS;
        mType = UnreadInfoManager.TYPE_SMS;
        mDefaultCn = DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_sms_enable);
        mDefaultState = defaultState /*SystemPropertiesUtils.getBoolean(PROP_DEFAULT_SMS, defaultState)*/;
    }

    @Override
    public int readUnreadCount() {
        int unreadSms = 0;
        int unreadMms = 0;
        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            return 0;
        }

        Cursor smsCursor = null;
        try {
            smsCursor = resolver.query(SMS_CONTENT_URI, new String[]{BaseColumns._ID},
                    "type =1 AND read = 0", null, null);
            if (smsCursor != null) {
                unreadSms = smsCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            UnreadUtils.closeCursorSilently(smsCursor);
        }

        Cursor mmsCursor = null;
        try {
            mmsCursor = resolver.query(MMS_CONTENT_URI, new String[]{BaseColumns._ID},
                    "msg_box = 1 AND read = 0 AND ( m_type =130 OR m_type = 132 ) AND thread_id > 0",
                    null, null);
            if (mmsCursor != null) {
                unreadMms = mmsCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            UnreadUtils.closeCursorSilently(mmsCursor);
        }


        Log.d(TAG, "readUnreadCount: unread [sms : mms] = ["
                + unreadSms + " : " + unreadMms + "]");

        return unreadMms + unreadSms;
    }

//    @Override
//    public String getUnreadHintString() {
//        String name = mContext.getString(R.string.unread_sms);
//        return mContext.getString(R.string.unread_hint, name);
//    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        ArrayList<String> installedMsgList = new ArrayList<>();
        Collection<MMSAppUtils.SmsApplicationData> smsApplications =
                MMSAppUtils.getApplicationCollection(context);



        for (MMSAppUtils.SmsApplicationData smsApplicationData : smsApplications) {
            ComponentName componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.smsClassName);
            mDefaultCn = componentName;
            Log.d("liuzuo97","loadApps mms componentName = "+componentName);
            String scName = componentName.flattenToShortString();
            installedMsgList.add(scName);
        }

        /*Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:10010"));
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
                    installedMsgList.add(scName);
                    break;
                }
            }
        }*/
        if(installedMsgList.size()==0){
            installedMsgList.add(mDefaultCn.flattenToShortString());
        }
        return installedMsgList;
    }
}



