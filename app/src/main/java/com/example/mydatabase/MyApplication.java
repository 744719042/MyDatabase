package com.example.mydatabase;

import android.app.Application;

import com.example.mydatabase.simple.DBManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DBManager.init(this);
    }
}
