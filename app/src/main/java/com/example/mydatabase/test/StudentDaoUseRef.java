package com.example.mydatabase.test;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mydatabase.simple.entity.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentDaoUseRef {
    private static final String INSERT = "INSERT INTO student(id, name, phone, address, age) VALUES(?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE student SET name = ?, phone = ?, address = ?, age = ? WHERE id = ?";
    private static final String DELETE = "delete from student WHERE id = ?";
    private static final String LOAD = "SELECT * FROM student WHERE id = ?";
    private static final String QUERY_LIST = "SELECT * FROM student WHERE age < ?";

    public StudentDaoUseRef() {
    }

    public static void createTable() {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            db.execSQL("create table student(id integer primary key, name varchar(255), phone varchar(128), address varchar(255), age integer);");
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
    }

    public static void clearTable() {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            db.execSQL("delete from student;");
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
    }

    public void save(Student student) {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            db.execSQL(INSERT, new Object[] { student.getId(), student.getName(), student.getPhone(), student.getAddress(), student.getAge()});
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
    }

    public void update(Student student) {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            db.execSQL(UPDATE, new Object[] {  student.getName(), student.getPhone(), student.getAddress(), student.getAge(), student.getId()});
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
    }

    public void delete(int id) {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            db.execSQL(DELETE, new Object[] { id });
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
    }

    public Student load(int id) {
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            Cursor cursor = db.rawQuery(LOAD, new String[] { String.valueOf(id) });
            if (cursor.moveToFirst()) {
                return getStudent(cursor);
            }
        } finally {
            DBReferenceManager.getInstance().closeConnection();
        }
        return null;
    }

    public List<Student> queryByAge(int age) {
        List<Student> list = new ArrayList<>();
        try {
            SQLiteDatabase db = DBReferenceManager.getInstance().openConnection();
            Cursor cursor = db.rawQuery(QUERY_LIST, new String[] { String.valueOf(age) });
            while (cursor.moveToNext()) {
                Student student = getStudent(cursor);
                list.add(student);
            }
        } finally {
            DBReferenceManager.getInstance().closeConnection();
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
