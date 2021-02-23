package com.condor.launcher.util;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Perry on 19-1-24
 */
public class DatabaseManager {
    private AtomicInteger mOpenCounter = new AtomicInteger();
    private static DatabaseManager sInstance;
    private SQLiteOpenHelper mOpenHelper;
    private SQLiteDatabase mDb;

    private DatabaseManager(SQLiteOpenHelper helper) {
        mOpenHelper = helper;
    }

    public static synchronized DatabaseManager getInstance(SQLiteOpenHelper helper) {
        if (sInstance == null) {
            sInstance = new DatabaseManager(helper);
        }
        return sInstance;
    }

    public synchronized SQLiteDatabase getWritableDatabase() {
        if (mOpenCounter.incrementAndGet() == 1) {
            mDb = mOpenHelper.getWritableDatabase();
        }
        return mDb;
    }

    public synchronized SQLiteDatabase getReadableDatabase() {
        if (mOpenCounter.incrementAndGet() == 1) {
            mDb = mOpenHelper.getReadableDatabase();
        }
        return mDb;
    }

    public synchronized void close() {
        if (mOpenCounter.decrementAndGet() == 0) {
            mDb.close();
        }
    }
}