package com.example.mydatabase.reflect.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.example.mydatabase.reflect.annotation.GenerateType;
import com.example.mydatabase.reflect.annotation.GeneratedValue;
import com.example.mydatabase.reflect.annotation.Id;
import com.example.mydatabase.reflect.annotation.Table;
import com.example.mydatabase.utils.CollectionUtils;
import com.example.mydatabase.utils.LogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseDao<T> {
    private static final String TAG = "BaseDao";
    private SQLiteDatabase mDb;
    protected Class<?> entityClass;

    private String mInsertSql;
    private String mUpdateSql;
    private String mDeleteSql;
    private String mLoadSql;
    private String mTableName;

    // 主键相关内容
    private String mIdName;
    private GenerateType mIdGenerateType = GenerateType.PROVIDED;
    private Field mIdField;

    public BaseDao(SQLiteDatabase db) {
        this.mDb = db;
    }

    private T createEntity(Cursor cursor) throws InstantiationException, IllegalAccessException {
        Field[] fields = getEntityClass().getDeclaredFields();
        if (!CollectionUtils.isEmpty(fields)) {
            return null;
        }
        T entity = (T) getEntityClass().newInstance();
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(entity, getFieldValue(field, cursor));
        }
        return entity;
    }

    private Object getFieldValue(Field field, Cursor cursor) {
        Class<?> fieldType = field.getType();
        Object value = null;
        if (fieldType == int.class || fieldType == Integer.class) {
            value = cursor.getInt(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == short.class || fieldType == Short.class) {
            value = (short) cursor.getInt(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            value = (byte) cursor.getInt(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == long.class || fieldType == Long.class) {
            value = cursor.getLong(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == char.class || fieldType == Character.class) {
            String str = cursor.getString(cursor.getColumnIndex(field.getName()));
            if (str.length() > 0) {
                value = str.charAt(0);
            }
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            String str = cursor.getString(cursor.getColumnIndex(field.getName()));
            value = "true".equals(str);
        } else if (fieldType == long.class || fieldType == Long.class) {
            value = cursor.getLong(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == float.class || fieldType == Float.class) {
            value = (float) cursor.getDouble(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == double.class || fieldType == Double.class) {
            value = cursor.getDouble(cursor.getColumnIndex(field.getName()));
        } else if (fieldType == String.class) {
            value = cursor.getString(cursor.getColumnIndex(field.getName()));
        }
        return value;
    }

    private String getTableName() {
        if (TextUtils.isEmpty(mTableName)) {
            Class<?> entityClass = getEntityClass();
            if (entityClass.isAnnotationPresent(Table.class)) {
                Table table = entityClass.getAnnotation(Table.class);
                mTableName = table.name();
            } else {
                mTableName = entityClass.getName();
            }
        }

        return mTableName;
    }

    private String getIdName() {
        parseIdInfo();
        return mTableName;
    }

    private void parseIdInfo() {
        if (mIdField == null) {
            Class<?> entityClass = getEntityClass();
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    mIdField = field;
                    Id id = field.getAnnotation(Id.class);
                    mIdName = id.value();
                    if (TextUtils.isEmpty(mIdName)) {
                        mIdName = field.getName();
                    }
                    break;
                }
            }

            if (mIdField == null) {
                throw new RuntimeException("primary key must define");
            }

            mIdField.setAccessible(true);
            if (mIdField.isAnnotationPresent(GeneratedValue.class)) {
                GeneratedValue generatedValue = mIdField.getAnnotation(GeneratedValue.class);
                mIdGenerateType = generatedValue.generateType();
            }
        }
    }

    private Field getIdField() {
        parseIdInfo();
        return mIdField;
    }

    private GenerateType getIdGenerateType() {
        parseIdInfo();
        return mIdGenerateType;
    }

    public void save(T t) {
        Field[] fields = getEntityClass().getDeclaredFields();
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        if (TextUtils.isEmpty(mInsertSql)) {
            generateInsertSQL(fields);
        }

        Log.e(TAG, mInsertSql);
        List<Object> list = new ArrayList<>(fields.length);
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.getName().equals(getIdField().getName())) {
                    if (getIdGenerateType() == GenerateType.UUID) {
                        field.set(t, UUID.randomUUID());
                    }
                }
                list.add(field.get(t));
            } catch (IllegalAccessException e) {
                LogUtils.printException(TAG, e);
            }
        }
        mDb.execSQL(mInsertSql, list.toArray());
    }

    private void generateInsertSQL(Field[] fields) {
        StringBuilder builder = new StringBuilder("INSERT INTO ");
        builder.append(getTableName());
        builder.append("(");
        for (Field field : fields) {
            builder.append(field.getName());
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(")");
        builder.append("VALUES(");
        for (Field field : fields) {
            builder.append("?,");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        mInsertSql = builder.toString();
    }

    public void update(T t) {
        Field[] fields = getEntityClass().getDeclaredFields();
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        if (TextUtils.isEmpty(mUpdateSql)) {
            generateUpdateSQL(fields);
        }

        List<Object> list = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (field != getIdField()) {
                field.setAccessible(true);
                try {
                    list.add(field.get(t));
                } catch (IllegalAccessException e) {
                    LogUtils.printException(TAG, e);
                }
            }
        }
        try {
            list.add(getIdField().get(t));
        } catch (IllegalAccessException e) {
            LogUtils.printException(TAG, e);
        }
        mDb.execSQL(mUpdateSql, list.toArray());
    }

    private void generateUpdateSQL(Field[] fields) {
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(getTableName());
        builder.append(" SET ");
        for (Field field : fields) {
            // 如果不是主键
            if (!field.getName().equals(getIdField().getName())) {
                builder.append(field.getName())
                        .append(" = ?,");
            }
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(" WHERE ");
        builder.append(getIdName());
        builder.append(" = ?");
        mUpdateSql = builder.toString();
        Log.e(TAG, "mUpdateSql = " + mUpdateSql);
    }

    public void delete(Object key) {
        Field[] fields = getEntityClass().getDeclaredFields();
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        if (TextUtils.isEmpty(mDeleteSql)) {
            generateDeleteSQL(fields);
        }

        mDb.execSQL(mDeleteSql, new Object[] { key });
    }

    private void generateDeleteSQL(Field[] fields) {
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(getTableName());
        builder.append(" WHERE ");
        builder.append(getIdName());
        builder.append(" = ?;");
        mDeleteSql = builder.toString();
        Log.e(TAG, mDeleteSql);
    }

    public T load(Object key) {
        Field[] fields = getEntityClass().getDeclaredFields();
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }

        if (TextUtils.isEmpty(mLoadSql)) {
            generateLoadSQL(fields);
        }

        Cursor cursor = mDb.rawQuery(mLoadSql, new String[] { String.valueOf(key) });
        if (cursor.moveToNext()) {
            try {
                return createEntity(cursor);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return null;
    }

    private void generateLoadSQL(Field[] fields) {
        StringBuilder builder = new StringBuilder("SELECT * FROM ");
        builder.append(getTableName());
        builder.append(" WHERE ");
        builder.append(getIdName());
        builder.append(" = ?;");
        mLoadSql = builder.toString();
        Log.e(TAG, mLoadSql);
    }

    public List<T> queryList(String where, String[] args) {
        List<T> list = new ArrayList<>();
        Field[] fields = getEntityClass().getDeclaredFields();
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }

        String sql = "SELECT * FROM " + getTableName() + " WHERE " + where + ";";
        Cursor cursor = mDb.rawQuery(sql, args);
        while (cursor.moveToNext()) {
            try {
                list.add(createEntity(cursor));
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return list;
    }

    public Class<?> getEntityClass() {
        if (entityClass == null) {
            Class<?> current = getClass();
            while (!(current.getGenericSuperclass() instanceof ParameterizedType)) {
                current = current.getSuperclass();
            }

            ParameterizedType type = (ParameterizedType) current.getGenericSuperclass();
            entityClass = (Class<?>) type.getActualTypeArguments()[0];
        }
        return entityClass;
    }
}
