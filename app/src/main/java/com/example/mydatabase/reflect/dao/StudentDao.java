package com.example.mydatabase.reflect.dao;

import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.simple.entity.Student;

public class StudentDao extends BaseDao<Student> {

    public StudentDao(SQLiteDatabase db) {
        super(db);
    }

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("create table student(id integer primary key, name varchar(255), phone varchar(128), address varchar(255), age integer);");
    }
}
