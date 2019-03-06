package com.example.mydatabase.reflect.entity;

import com.example.mydatabase.reflect.annotation.GenerateType;
import com.example.mydatabase.reflect.annotation.GeneratedValue;
import com.example.mydatabase.reflect.annotation.Id;
import com.example.mydatabase.reflect.annotation.Table;

@Table(name = "t_student")
public class Student {
    @Id
    @GeneratedValue(generateType = GenerateType.AUTO_INCREMENT)
    private int id;
    private String name;
    private String phone;
    private String address;
    private int age;

    public Student() {
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
