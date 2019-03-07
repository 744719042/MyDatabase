package com.example.sqlitelib;

import android.database.sqlite.SQLiteDatabase;

public abstract class AbsDatabase {

    protected void onDBCreate(SQLiteDatabase db) {

    }

    protected void onDBUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
