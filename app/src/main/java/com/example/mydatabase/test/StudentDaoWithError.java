package com.example.mydatabase.test;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.mydatabase.MyApplication;
import com.example.mydatabase.simple.entity.Student;
import com.example.mydatabase.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

public class StudentDaoWithError {
    private static final String TAG = "StudentDao";
    private static final String INSERT = "INSERT INTO student(id, name, phone, address, age) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE student SET name = ?, phone = ?, address = ?, age = ? WHERE id = ?";
    private static final String DELETE = "delete from student WHERE id = ?";
    private static final String LOAD = "SELECT * FROM student WHERE phone = ?";
    private static final String QUERY_LIST = "SELECT * FROM student WHERE age < ?";

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("create table student(id integer primary key, name varchar(255), phone varchar(128), address varchar(255), age integer);");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("drop table if exists student;");
    }

    public static void clearTable(SQLiteDatabase db) {
        db.execSQL("delete from student;");
    }

    public void save(com.example.mydatabase.simple.entity.Student student) {
        DBOpenHelper helper = new DBOpenHelper(MyApplication.getContext());
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            db.execSQL(INSERT, new Object[] { student.getId(), student.getName(), student.getPhone(), student.getAddress(), student.getAge()});
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            IOUtils.close(db);
        }
    }

    public void update(com.example.mydatabase.simple.entity.Student student) {
        DBOpenHelper helper = new DBOpenHelper(MyApplication.getContext());
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            db.execSQL(UPDATE, new Object[] {  student.getName(), student.getPhone(), student.getAddress(), student.getAge(), student.getId()});
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            IOUtils.close(db);
        }
    }

    public void delete(int id) {
        DBOpenHelper helper = new DBOpenHelper(MyApplication.getContext());
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            db.execSQL(DELETE, new Object[] { id });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            IOUtils.close(db);
        }
    }

    public com.example.mydatabase.simple.entity.Student load(int id) {
        DBOpenHelper helper = new DBOpenHelper(MyApplication.getContext());
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getWritableDatabase();
            cursor = db.rawQuery(LOAD, new String[] { String.valueOf(id) });
            if (cursor.moveToFirst()) {
                return getStudent(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            IOUtils.close(cursor);
            IOUtils.close(db);
        }
        return null;
    }

    public List<com.example.mydatabase.simple.entity.Student> queryByAge(int age) {
        DBOpenHelper helper = new DBOpenHelper(MyApplication.getContext());
        List<com.example.mydatabase.simple.entity.Student> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getWritableDatabase();
            cursor = db.rawQuery(QUERY_LIST, new String[] { String.valueOf(age) });
            while (cursor.moveToNext()) {
                com.example.mydatabase.simple.entity.Student student = getStudent(cursor);
                list.add(student);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            IOUtils.close(cursor);
            IOUtils.close(db);
        }
        return list;
    }

    private com.example.mydatabase.simple.entity.Student getStudent(Cursor cursor) {
        com.example.mydatabase.simple.entity.Student student = new Student();
        student.setId(cursor.getInt(cursor.getColumnIndex("id")));
        student.setAddress(cursor.getString(cursor.getColumnIndex("address")));
        student.setName(cursor.getString(cursor.getColumnIndex("name")));
        student.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
        student.setAge(cursor.getInt(cursor.getColumnIndex("age")));
        return student;
    }
}
