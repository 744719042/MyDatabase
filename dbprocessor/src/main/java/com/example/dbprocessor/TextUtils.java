package com.example.dbprocessor;

public class TextUtils {
    public static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    public static String capital(String s) {
        if (isEmpty(s)) {
            return "";
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
