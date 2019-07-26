package com.example.mydatabase.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBReferenceManager {
    private int referenceCount = 0;

    private static final String TAG = "DBManager";
    private static volatile DBReferenceManager sDBManager;

    private static Context sContext;
    private DBOpenHelper dbOpenHelper;
    private SQLiteDatabase sqLiteDatabase;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    private DBReferenceManager() {
        dbOpenHelper = new DBOpenHelper(sContext);
    }

    public static DBReferenceManager getInstance() {
        if (sDBManager == null) {
            synchronized (DBReferenceManager.class) {
                if (sDBManager == null) {
                    sDBManager = new DBReferenceManager();
                }
            }
        }
        return sDBManager;
    }

    public StudentDaoUseRef getStudentDao() {
        return new StudentDaoUseRef();
    }

    public synchronized SQLiteDatabase openConnection() {
        Log.i(TAG, "openConnection() referenceCount = " + referenceCount);
        if (referenceCount == 0) {
            sqLiteDatabase = dbOpenHelper.getWritableDatabase();
        }
        referenceCount++;
        return sqLiteDatabase;
    }

    public synchronized void closeConnection() {
        if (referenceCount <= 0) {
            return;
        }
        referenceCount--;
        Log.i(TAG, "closeConnection() referenceCount = " + referenceCount);
        if (referenceCount == 0) {
            sqLiteDatabase.close();
            sqLiteDatabase = null;
        }
    }
}
