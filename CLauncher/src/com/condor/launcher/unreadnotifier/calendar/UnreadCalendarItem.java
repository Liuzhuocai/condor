package com.condor.launcher.unreadnotifier.calendar;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract.CalendarAlerts;
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

public class UnreadCalendarItem extends UnreadBaseItem{
    private static final String TAG = "UnreadCalendarItem";
    private static final Uri CALENDARS_CONTENT_URI = CalendarAlerts.CONTENT_URI;

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.calendar",
            "com.android.calendar.AllInOneActivity");

//    private static final String PROP_DEFAULT_CALENDAR = "ro.launcher.unread.calendar";

    public UnreadCalendarItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, CALENDARS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_CALENDAR;
        mPrefKey = UnreadUtils.PREF_KEY_UNREAD_CALENDAR;
        mDefaultCn = /*SystemProperties.is_qmb_pk() ? */new ComponentName("com.google.android.calendar", "com.android.calendar.AllInOneActivity") /*: DEFAULT_CNAME*/;
        mType = UnreadInfoManager.TYPE_CALENDAR;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_calendar_enable);
        mDefaultState =  /*SystemPropertiesUtils.getBoolean(PROP_DEFAULT_CALENDAR, defaultState)*/ defaultState;
    }

    @Override
    public int readUnreadCount() {
        String[] ALERT_PROJECTION = new String[] { CalendarAlerts._ID, // 0
                CalendarAlerts.EVENT_ID, // 1
                CalendarAlerts.STATE, // 2
                CalendarAlerts.TITLE, // 3
                CalendarAlerts.EVENT_LOCATION, // 4
                CalendarAlerts.SELF_ATTENDEE_STATUS, // 5
                CalendarAlerts.ALL_DAY, // 6
                CalendarAlerts.ALARM_TIME, // 7
                CalendarAlerts.MINUTES, // 8
                CalendarAlerts.BEGIN, // 9
                CalendarAlerts.END, // 10
                CalendarAlerts.DESCRIPTION, // 11
        };
        int unreadEvents = 0;

        boolean result = checkPermission();
        if (!result) {
            return 0;
        }
        ContentResolver resolver = mContext.getContentResolver();
        Cursor alertCursor = null;
        try {
            alertCursor= resolver
                    .query(CALENDARS_CONTENT_URI,
                            ALERT_PROJECTION,
                            ("(" + CalendarAlerts.STATE + "=? OR "
                                    + CalendarAlerts.STATE + "=?) AND "
                                    + CalendarAlerts.ALARM_TIME + "<=" + System
                                    .currentTimeMillis()),
                            new String[] {
                                    Integer.toString(CalendarAlerts.STATE_FIRED),
                                    Integer.toString(CalendarAlerts.STATE_SCHEDULED) },
                            "begin DESC, end DESC");
            if(alertCursor != null) {
                unreadEvents = alertCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            UnreadUtils.closeCursorSilently(alertCursor);
        }

        Log.d(TAG, "readUnreadCount, unread Calendar num = "+unreadEvents);

        return unreadEvents;
    }

//    @Override
//    public String getUnreadHintString() {
//        String name = mContext.getString(R.string.unread_calendar);
//        return mContext.getString(R.string.unread_hint, name);
//    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] calLists = context.getResources().getStringArray(R.array.support_calendar_component_array);

        ArrayList<String> installedCalendarList = new ArrayList<>();
        for (String calList : calLists) {
            ComponentName componentName = ComponentName.unflattenFromString( calList );
            boolean isInstalled = UnreadUtils.isAppInstalled(context, componentName.getPackageName(), android.os.Process.myUserHandle());
            if (isInstalled) {
                installedCalendarList.add(componentName.flattenToShortString());
            }
        }

        return installedCalendarList;
    }
}
