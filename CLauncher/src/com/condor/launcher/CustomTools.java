package com.condor.launcher;

import android.content.Context;

import com.android.launcher3.Utilities;

/**
 * add by Bruce for Customized tools
 */
public class CustomTools {
    private Context mContext;
    private static CustomTools instance = null;

    public static boolean isOpenNoticeDot = false;  //Whether the system setting notification switch is turned on
    public static boolean mSwitchUnreadPhone = false;
    public static boolean mSwitchUnreadSms = false;

    private CustomTools(Context context) {
        mContext = context;
    }

    public static synchronized CustomTools getInstance(Context context) {
        if (instance == null) {
            instance = new CustomTools(context);

        }
        return instance;
    }

    public void init() {
        isOpenNoticeDot = Utilities.getPrefs(mContext).getBoolean("ICON_BADGING_PREFERENCE_KEY", false);
        mSwitchUnreadPhone = Utilities.getPrefs(mContext).getBoolean("pref_unread_phone_settings", true);
        mSwitchUnreadSms = Utilities.getPrefs(mContext).getBoolean("pref_unread_sms_settings", true);
    }
}
