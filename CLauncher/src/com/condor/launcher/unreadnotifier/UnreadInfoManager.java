package com.condor.launcher.unreadnotifier;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import com.condor.launcher.unreadnotifier.calendar.UnreadCalendarItem;
import com.condor.launcher.unreadnotifier.call.MissCallItem;
import com.condor.launcher.unreadnotifier.email.UnreadEmailItem;
import com.condor.launcher.unreadnotifier.sms.UnreadMessageItem;

import java.util.ArrayList;

/**
 * Created by Bruce on 1/30/19.
 */

public class UnreadInfoManager {
    private static final String TAG = "UnreadInfoManager";

    @SuppressLint("StaticFieldLeak")
    private static UnreadInfoManager mInstance;
    private Context mContext;

    public static final int TYPE_CALL_LOG = 101;
    public static final int TYPE_SMS = 102;
    public static final int TYPE_EMAIL = 103;
    public static final int TYPE_CALENDAR = 104;

    private UnreadMessageItem mMessageUnreadItem;
    private MissCallItem mMissCallItem;
    private UnreadEmailItem mUnreadEmailItem;
    private UnreadCalendarItem mUnreadCalendarItem;

    private ArrayList<UnreadBaseItem> mUnreadItems = new ArrayList<>();
    private ArrayList<UnreadBaseItem> mGrantedPermissionItems = new ArrayList<>();
    private ArrayList<UnreadBaseItem> mDeniedpermissionItems = new ArrayList<>();

    private UnreadInfoInterface mUnreadInfoInterface;

    private UnreadInfoManager(Context context) {
        mContext = context;
    }

    public static UnreadInfoManager getInstance(Context context) {
        synchronized (UnreadInfoManager.class) {
            if (mInstance == null) {
                mInstance = new UnreadInfoManager(context);
            }
        }
        return mInstance;
    }


    public synchronized void init() {
        if (mUnreadItems.isEmpty()) {
            createItems();
        }
        initAppsAndPermissionList();
        initUnreadInfo();
    }


    private void createItems() {
        mUnreadItems.clear();

        //init begin
        if (mMessageUnreadItem == null) {
            mMessageUnreadItem = new UnreadMessageItem(mContext);
            mUnreadItems.add(mMessageUnreadItem);

        }

        if (mMissCallItem == null) {
            mMissCallItem = new MissCallItem(mContext);
            mUnreadItems.add(mMissCallItem);
        }


//        if (mUnreadEmailItem == null) {
//            mUnreadEmailItem = new UnreadEmailItem(mContext);
//            mUnreadItems.add(mUnreadEmailItem);
//        }
//
//        if (mUnreadCalendarItem == null) {
//            mUnreadCalendarItem = new UnreadCalendarItem(mContext);
//            mUnreadItems.add(mUnreadCalendarItem);
//        }
    }

    private void initAppsAndPermissionList() {
        int N = mUnreadItems.size();

        mGrantedPermissionItems.clear();
        mDeniedpermissionItems.clear();

        for (int i = 0; i < N; i++) {
            UnreadBaseItem item = mUnreadItems.get(i);
            //verify ComponentName
            ArrayList<String> listValues = item.loadApps(item.mContext);
            //Log.d("liuzuo98","initAppsAndPermissionList i="+i+"  ,item.mDefaultCn= "+item.mDefaultCn+"  ,listValues="+listValues.size());
            item.verifyDefaultCN(listValues, item.mDefaultCn);
            item.setInstalledList(listValues);

            //init permission List
            if (item.checkPermission()) {
                mGrantedPermissionItems.add(item);
            } else {
                mDeniedpermissionItems.add(item);
            }
        }
    }

    private void initUnreadInfo() {
        int N = mDeniedpermissionItems.size();

        String[] deniedString = new String[N];
        for (int i = 0; i < N; i++) {
            deniedString[i] = mDeniedpermissionItems.get(i).mPermission;
        }
//        if (N > 0) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (mUnreadInfoInterface != null) {
//                    mUnreadInfoInterface.requestUnreadPermise(deniedString, PERMISSIONS_REQUEST_CODE);
//                }
//            }
//        }

        N = mGrantedPermissionItems.size();
        for (int i = 0; i < N; i++) {
            UnreadBaseItem item = mGrantedPermissionItems.get(i);
            if (item != null) {
                try {
                    item.mContentObserver.registerContentObserver();
                }catch (SecurityException e){
                    e.printStackTrace();
                }
                item.updateUIFromDatabase();
            }
        }
    }

    private boolean isDeniedPermissionItem(String key) {
        int N = mDeniedpermissionItems.size();
        for (int i = 0; i < N; i++) {
            UnreadBaseItem item = mDeniedpermissionItems.get(i);
            if (item != null && item.getCurrentComponentName() != null) {
                String value = item.getCurrentComponentName().flattenToShortString();
                if (value.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public UnreadBaseItem getItemByType(int type) {
        for (int i = 0; i < mUnreadItems.size(); i++) {
            UnreadBaseItem item = mUnreadItems.get(i);
            if (item.mType == type) {
                return item;
            }
        }
        return null;
    }

    UnreadBaseItem getItemByKey(String key) {
        for (int i = 0; i < mUnreadItems.size(); i++) {
            UnreadBaseItem item = mUnreadItems.get(i);
            if (item.mPrefKey.equals(key)) {
                return item;
            }
        }
        return null;
    }


    public void updateUI(final Context context, final String desComponentName, int type) {
        //Log.d("liuzuo98","updateUI  desComponentName="+desComponentName+",type="+type);
        if (!TextUtils.isEmpty(desComponentName) && null != mUnreadInfoInterface) {
            ComponentName cmpName = ComponentName.unflattenFromString(desComponentName);
            int unreadCount = getUnreadCountForDesComponent(cmpName);
            mUnreadInfoInterface.updateUnreadBadges(cmpName, unreadCount, type);
        }
    }

    public int getUnreadCountForDesComponent(final ComponentName desComponentName) {
        int result = 0;
        for (UnreadBaseItem item : mUnreadItems) {
            if (!TextUtils.isEmpty(item.mCurrentCn)) {
                ComponentName cmpName = ComponentName.unflattenFromString(item.mCurrentCn);
                if (cmpName.equals(desComponentName)) {
                    result += item.getUnreadCount();
                }
            }
        }
        return result;
    }

    public int getUnreadCountByType(final int type) {
        int result = 0;
        for (UnreadBaseItem item : mUnreadItems) {
            if (!TextUtils.isEmpty(item.mCurrentCn) && item.mType == type) {
//                ComponentName cmpName = ComponentName.unflattenFromString(item.mCurrentCn);
                result += item.getUnreadCount();
            }
        }
        return result;
    }

    public UnreadInfoInterface getUnreadInfoInterface() {
        return mUnreadInfoInterface;
    }

    public void setUnreadInfoInterface(UnreadInfoInterface unreadInfoInterface) {
        this.mUnreadInfoInterface = unreadInfoInterface;
    }

    public void releaseRes() {
        if (mUnreadItems.isEmpty()) {

        } else {
            for (UnreadBaseItem baseItem : mUnreadItems) {
                if (baseItem != null) {
                    try {
                        baseItem.mContentObserver.unregisterContentObserver();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public interface UnreadInfoInterface {
//        void requestUnreadPermise(String[] deniedString, int requestCode);
        void updateUnreadBadges(ComponentName componentName, int count, int type);
    }

}
