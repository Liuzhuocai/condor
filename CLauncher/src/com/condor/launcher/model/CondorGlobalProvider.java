package com.condor.launcher.model;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Process;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.Thunk;
import com.condor.launcher.CondorFeatureFlags;
import com.condor.launcher.util.DatabaseManager;
import com.condor.launcher.util.Logger;


import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by Perry on 19-1-24
 */
public class CondorGlobalProvider extends ContentProvider {
    private static final String TAG = "CondorGlobalProvider";
    private static final String PARAMETER_NOTIFY = "notify";
    public static final String AUTHORITY = CondorFeatureFlags.GLOBAL_AUTHORITY;

    private DatabaseManager mManager;

    @Override
    public boolean onCreate() {
        mManager = DatabaseManager.getInstance(new DatabaseHelper(getContext()));
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mManager.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        /// M. Check null pointer.
        if (result != null) {
            result.setNotificationUri(getContext().getContentResolver(), uri);
        }
        /// M.
        return result;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri);
        SQLiteDatabase db = mManager.getWritableDatabase();
        db.beginTransaction();
        try {
            int count = values.length;
            for (int i = 0; i < count; i++) {
                LauncherProvider.addModifiedTime(values[i]);
                final long rowId = dbInsertAndCheck(db, args.table, null, values[i]);
                if (rowId < 0) return 0;

                uri = ContentUris.withAppendedId(uri, rowId);
                sendNotify(uri);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return values.length;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri);

        SQLiteDatabase db = mManager.getWritableDatabase();
        LauncherProvider.addModifiedTime(values);
        final long rowId = dbInsertAndCheck(db, args.table, null, values);
        if (rowId < 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Thunk
    static long dbInsertAndCheck(SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        return db.insert(table, nullColumnHack, values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mManager.getWritableDatabase();

        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, selection, selectionArgs);

        LauncherProvider.addModifiedTime(values);
        SQLiteDatabase db = mManager.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public <T> boolean insertBatch(String sql, List<T> data,
                                   BiFunction<SQLiteStatement, T, SQLiteStatement> cb) {
        SQLiteDatabase db = null;
        try {
            db = mManager.getWritableDatabase();
            SQLiteStatement stat = db.compileStatement(sql);
            db.beginTransaction();
            for (T t : data) {
                long result = cb.apply(stat, t).executeInsert();
                if (result < 0) {
                    Logger.d(TAG, "insertBatch failed for " + sql);
                    return false;
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e(TAG, "insertBatch failed for " + sql + ", " + e.getMessage());
            return false;
        } finally {
            try {
                if (null != db) {
                    db.endTransaction();
                    mManager.close();
                }
            } catch (Exception e) {
                Logger.e(TAG, "insertBatch failed for " + sql + ", " + e.getMessage());
            }
        }

        return true;
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "global.db";
        private static final int DB_VERSION = 2;
        private final Context mContext;

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            addLockedTasksTable(db, false);
            // Perry: Add search recents for all apps: start
            addSearchRecentTable(db, false);
            // Perry: Add search recents for all apps: end
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Perry: Add search recents for all apps: start
            switch (oldVersion) {
                case 1:
                    addSearchRecentTable(db, false);
                    return;
            }

            createEmptyDB(db);
            // Perry: Add search recents for all apps: end
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            createEmptyDB(db);
        }

        public void createEmptyDB(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                db.execSQL("DROP TABLE IF EXISTS " + CondorLauncherSettings.LockedTasks.TABLE_NAME);
                onCreate(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        public long getDefaultUserSerial() {
            return UserManagerCompat.getInstance(mContext).getSerialNumberForUser(
                    Process.myUserHandle());
        }

        private void addLockedTasksTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + CondorLauncherSettings.LockedTasks.TABLE_NAME + " (" +
                    CondorLauncherSettings.LockedTasks._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CondorLauncherSettings.LockedTasks.COMPONENT + " TEXT," +
                    CondorLauncherSettings.LockedTasks.PROFILE_ID + " INTEGER DEFAULT " +  + getDefaultUserSerial() + "," +
                    LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }

        // Perry: Add search recents for all apps: start
        private void addSearchRecentTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + CondorLauncherSettings.SearchRecent.TABLE_NAME + " (" +
                    CondorLauncherSettings.SearchRecent._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CondorLauncherSettings.SearchRecent.ITEM_TYPE + " INTEGER," +
                    CondorLauncherSettings.SearchRecent.ITEM_ID + " INTEGER NOT NULL DEFAULT 0," +
                    CondorLauncherSettings.SearchRecent.COMPONENT + " TEXT," +
                    CondorLauncherSettings.SearchRecent.PROFILE_ID + " INTEGER DEFAULT " +  + getDefaultUserSerial() + "," +
                    LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }
        // Perry: Add search recents for all apps: end
    }


}
