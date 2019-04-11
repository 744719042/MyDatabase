package com.example.mydatabase;

import android.app.Application;
import android.content.Context;

import com.example.mydatabase.simple.DBManager;

public class MyApplication extends Application {
    private static Context sContext;
    @Override
    public void onCreate() {
        super.onCreate();
        DBManager.init(this);
        com.example.mydatabase.test.DBManager.init(this);
        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }
}
