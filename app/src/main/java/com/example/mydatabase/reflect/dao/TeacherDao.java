package com.example.mydatabase.reflect.dao;

import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.simple.entity.Teacher;

public class TeacherDao extends BaseDao<Teacher> {
    private static String CREATE_TABLE;

    public TeacherDao(SQLiteDatabase db) {
        super(db);
    }

    public static void createTable(SQLiteDatabase db) {

        db.execSQL("create table teacher(id integer primary key, name varchar(255), title varchar(128), course varchar(128), gender varchar(10));");
    }
}
