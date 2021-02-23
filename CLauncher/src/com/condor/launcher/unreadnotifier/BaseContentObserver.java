package com.condor.launcher.unreadnotifier;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;


/**
 * Created by Bruce on 2019/1/30.
 */

public class BaseContentObserver extends ContentObserver {
    private static final String TAG = "BaseContentObserver";
    private Uri mUri;
    private Context mContext;
    private UnreadBaseItem mItem;
    private Handler mHandler = new Handler();
    private static final int OBSERVER_HANDLER_DELAY = 1000;

    public BaseContentObserver(Handler handler, Context context, Uri uri, UnreadBaseItem item) {
        super(handler);
        mContext = context;
        mUri = uri;
        mItem = item;
    }

    void registerContentObserver() {
        mContext.getContentResolver().registerContentObserver(mUri, true, this);
    }

    void unregisterContentObserver() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    private Runnable changeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mItem != null) {
                mItem.updateUIFromDatabase();
            }
        }
    };

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        mHandler.removeCallbacks(changeRunnable);
        mHandler.postDelayed(changeRunnable, OBSERVER_HANDLER_DELAY);
    }

}
