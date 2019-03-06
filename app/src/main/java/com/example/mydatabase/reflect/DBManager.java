package com.example.mydatabase.reflect;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.reflect.dao.StudentDao;
import com.example.mydatabase.reflect.dao.TeacherDao;
import com.example.mydatabase.utils.LogUtils;

public class DBManager {
    private static final String TAG = "DBManager";
    private static volatile DBManager sDBManager;

    private static Context sContext;
    private SQLiteDatabase mSQLite;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    private DBManager() {
        DBOpenHelper dbOpenHelper = new DBOpenHelper(sContext);
        try {
            mSQLite = dbOpenHelper.getWritableDatabase();
        } catch (Exception e) {
            LogUtils.printException(TAG, e);
        }
    }

    public static DBManager getInstance() {
        if (sDBManager == null) {
            synchronized (DBManager.class) {
                if (sDBManager == null) {
                    sDBManager = new DBManager();
                }
            }
        }
        return sDBManager;
    }

    public StudentDao getStudentDao() {
        return new StudentDao(mSQLite);
    }

    public TeacherDao getTeacherDao() {
        return new TeacherDao(mSQLite);
    }
}
