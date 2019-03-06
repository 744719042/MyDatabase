package com.example.mydatabase.reflect.entity;

import com.example.mydatabase.reflect.annotation.GenerateType;
import com.example.mydatabase.reflect.annotation.GeneratedValue;
import com.example.mydatabase.reflect.annotation.Id;
import com.example.mydatabase.reflect.annotation.Table;

@Table(name = "t_teacher")
public class Teacher {
    @Id
    @GeneratedValue(generateType = GenerateType.UUID)
    private int id;
    private String name;
    private String title;
    private String course;
    private String gender;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
