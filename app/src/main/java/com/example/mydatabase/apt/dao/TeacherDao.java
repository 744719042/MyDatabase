package com.example.mydatabase.apt.dao;

import com.example.dbannotation.annotation.Dao;
import com.example.dbannotation.annotation.Delete;
import com.example.dbannotation.annotation.Insert;
import com.example.dbannotation.annotation.Load;
import com.example.dbannotation.annotation.Query;
import com.example.dbannotation.annotation.Update;
import com.example.mydatabase.apt.entity.Teacher;

import java.util.List;

@Dao
public interface TeacherDao {
    @Insert
    void save(Teacher teacher);

    @Update
    void update(Teacher teacher);

    @Delete
    void delete(int id);

    @Load
    Teacher load(int id);

    @Query("SELECT * FROM teacher WHERE name = ?")
    List<Teacher> query(String name);
}
