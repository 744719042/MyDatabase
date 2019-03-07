package com.example.mydatabase.apt;

import com.example.sqlitelib.AbsDatabase;
import com.example.dbannotation.annotation.Database;
import com.example.mydatabase.apt.dao.StudentDao;
import com.example.mydatabase.apt.entity.Student;
import com.example.mydatabase.apt.entity.Teacher;
import com.example.mydatabase.simple.dao.TeacherDao;

@Database(tables = { Student.class, Teacher.class}, version = 1, name = "grade.db")
public abstract class DbManager extends AbsDatabase {
    public abstract StudentDao getStudentDao();
    public abstract TeacherDao getTeacherDao();
}
