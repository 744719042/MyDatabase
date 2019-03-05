package com.example.mydatabase.simple.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.entity.Teacher;

import java.util.ArrayList;
import java.util.List;

public class TeacherDao {
    private static final String INSERT = "INSERT INTO teacher(id, name, title, course, gender) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE teacher SET name = ?, title = ?, course = ?, gender = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM teacher WHERE id = ?";
    private static final String LOAD = "SELECT * FROM teacher WHERE name = ?";
    private static final String QUERY_LIST = "SELECT * FROM teacher WHERE title = ?";

    private SQLiteDatabase mDb;
    public TeacherDao(SQLiteDatabase db) {
        this.mDb = db;
    }

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("create table teacher(id integer primary key, name varchar(255), title varchar(128), course varchar(128), gender varchar(10));");
    }

    public void save(Teacher teacher) {
        mDb.execSQL(INSERT, new Object[] { teacher.getId(), teacher.getName(), teacher.getTitle(), teacher.getCourse(), teacher.getGender()});
    }

    public void update(Teacher teacher) {
        mDb.execSQL(UPDATE, new Object[] {  teacher.getName(), teacher.getTitle(), teacher.getCourse(), teacher.getGender(), teacher.getId()});
    }

    public void delete(int id) {
        mDb.execSQL(DELETE, new Object[] { id });
    }

    public Teacher load(String phone) {
        Cursor cursor = mDb.rawQuery(LOAD, new String[] { phone });
        if (cursor.moveToFirst()) {
            return getTeacher(cursor);
        }

        return null;
    }

    public List<Teacher> queryByTitle(String title) {
        Cursor cursor = mDb.rawQuery(QUERY_LIST, new String[] { String.valueOf(title) });
        List<Teacher> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            Teacher teacher = getTeacher(cursor);
            list.add(teacher);
        }
        return list;
    }

    private Teacher getTeacher(Cursor cursor) {
        Teacher teacher = new Teacher();
        teacher.setId(cursor.getInt(cursor.getColumnIndex("id")));
        teacher.setCourse(cursor.getString(cursor.getColumnIndex("course")));
        teacher.setName(cursor.getString(cursor.getColumnIndex("name")));
        teacher.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        teacher.setGender(cursor.getString(cursor.getColumnIndex("gender")));
        return teacher;
    }
}
