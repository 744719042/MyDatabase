package com.example.mydatabase.apt.dao;

import com.example.dbannotation.annotation.Dao;
import com.example.dbannotation.annotation.Delete;
import com.example.dbannotation.annotation.Insert;
import com.example.dbannotation.annotation.Load;
import com.example.dbannotation.annotation.Query;
import com.example.dbannotation.annotation.Update;
import com.example.mydatabase.apt.entity.Student;

import java.util.List;

@Dao
public interface StudentDao {
    @Insert
    void save(Student student);

    @Update
    void update(Student student);

    @Delete
    void delete(int id);

    @Load
    void load(int id);

    @Query("SELECT * FROM student WHERE age > ? and id > ?")
    List<Student> query(int age, int id);
}
