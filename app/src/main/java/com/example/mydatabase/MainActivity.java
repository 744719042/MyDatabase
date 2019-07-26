package com.example.mydatabase;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.mydatabase.simple.entity.Student;
import com.example.mydatabase.test.DBManager;
import com.example.mydatabase.test.DBOpenHelper;
import com.example.mydatabase.test.DBReferenceManager;
import com.example.mydatabase.test.StudentDao;
import com.example.mydatabase.test.StudentDaoUseRef;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startTest(View view) {
        for (int i = 0; i < 10; i++) {
            new UserThread(i).start();
        }
    }

    public void initTest(View view) {
        new DBOpenHelper(this).getWritableDatabase();
        for (int i = 0; i < 5; i++) {
            new InitThread(i).start();
        }
    }

    public void deleteTest(View view) {
        StudentDao.clearTable(new DBOpenHelper(this).getWritableDatabase());
    }

    private class InitThread extends Thread {
        private int index;

        public InitThread(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            int start = index * 10000;
//            final StudentDaoWithError dao =  new StudentDaoWithError();
//            final StudentDao dao = DBManager.getInstance().getStudentDao();
            final StudentDaoUseRef dao = DBReferenceManager.getInstance().getStudentDao();
            for (int i = start; i < start + 30; i++) {
                Student student = new Student();
                student.setId(i);
                student.setAddress("Beijing");
                student.setAge(30);
                student.setName("zhangsan");
                student.setPhone("23233232");
                dao.save(student);
                Log.e(TAG, "save student");
            }
        }
    }

    private class UserThread extends Thread {
        private int index;
        public UserThread(int index) {
            this.index = index;
        }

        @Override
        public void run() {
//            final StudentDaoWithError dao =  new StudentDaoWithError();
//            final StudentDao dao = DBManager.getInstance().getStudentDao();
            final StudentDaoUseRef dao = DBReferenceManager.getInstance().getStudentDao();
            final int start = index / 2 * 10000;
            if (index % 2 == 0) {
                for (int i = start; i < start + 40; i++) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            dao.load(start + new Random().nextInt(30));
                            Log.e(TAG, "query student");
                        }
                    }).start();
                }
            } else {
                for (int i = start; i < start + 40; i++) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Student student = new Student();
                            student.setId(start + new Random().nextInt(30));
                            student.setAddress("Shanghai");
                            student.setAge(20);
                            student.setName("lisi");
                            student.setPhone("88888888");
                            dao.update(student);
                            Log.e(TAG, "update student");
                        }
                    }).start();
                }
            }
        }
    }
}
