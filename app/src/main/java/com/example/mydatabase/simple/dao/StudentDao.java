package com.example.mydatabase.simple.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.simple.entity.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentDao {
    private static final String INSERT = "INSERT INTO student(id, name, phone, address, age) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE student SET name = ?, phone = ?, address = ?, age = ? WHERE id = ?";
    private static final String DELETE = "delete from student WHERE id = ?";
    private static final String LOAD = "SELECT * FROM student WHERE phone = ?";
    private static final String QUERY_LIST = "SELECT * FROM student WHERE age < ?";

    private SQLiteDatabase mDb;

    public StudentDao(SQLiteDatabase db) {
        this.mDb = db;
    }

    public static void createTable(SQLiteDatabase db) {
        db.execSQL("create table student(id integer primary key, name varchar(255), phone varchar(128), address varchar(255), age integer);");
    }

    public void save(Student student) {
        mDb.execSQL(INSERT, new Object[] { student.getId(), student.getName(), student.getPhone(), student.getAddress(), student.getAge()});
    }

    public void update(Student student) {
        mDb.execSQL(UPDATE, new Object[] {  student.getName(), student.getPhone(), student.getAddress(), student.getAge(), student.getId()});
    }

    public void delete(int id) {
        mDb.execSQL(DELETE, new Object[] { id });
    }

    public Student load(String phone) {
        Cursor cursor = mDb.rawQuery(LOAD, new String[] { phone });
        if (cursor.moveToFirst()) {
            return getStudent(cursor);
        }

        return null;
    }

    public List<Student> queryByAge(int age) {
        Cursor cursor = mDb.rawQuery(QUERY_LIST, new String[] { String.valueOf(age) });
        List<Student> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            Student student = getStudent(cursor);
            list.add(student);
        }
        return list;
    }

    private Student getStudent(Cursor cursor) {
        Student student = new Student();
        student.setId(cursor.getInt(cursor.getColumnIndex("id")));
        student.setAddress(cursor.getString(cursor.getColumnIndex("address")));
        student.setName(cursor.getString(cursor.getColumnIndex("name")));
        student.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
        student.setAge(cursor.getInt(cursor.getColumnIndex("age")));
        return student;
    }
}
