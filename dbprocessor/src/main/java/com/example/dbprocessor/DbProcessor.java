package com.example.dbprocessor;

import com.example.dbannotation.annotation.Dao;
import com.example.dbannotation.annotation.Database;
import com.example.dbannotation.annotation.Delete;
import com.example.dbannotation.annotation.GenerateType;
import com.example.dbannotation.annotation.GeneratedValue;
import com.example.dbannotation.annotation.Id;
import com.example.dbannotation.annotation.Insert;
import com.example.dbannotation.annotation.Load;
import com.example.dbannotation.annotation.Query;
import com.example.dbannotation.annotation.Table;
import com.example.dbannotation.annotation.Update;

import java.io.IOException;
import java.io.Writer;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

// https://docs.oracle.com/javase/8/docs/api/
// https://docs.oracle.com/javase/7/docs/technotes/guides/apt/GettingStarted.html
public class DbProcessor extends AbstractProcessor {
    private Elements mElements;
    private Types mTypes;
    private Filer mFiler;
    private Messager mMessager;
    private Set<String> mSet;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        mElements = env.getElementUtils();
        mTypes = env.getTypeUtils();
        mFiler = env.getFiler();
        mMessager = env.getMessager();
        mSet = new HashSet<>();
        mSet.add(Database.class.getCanonicalName());
        mSet.add(Table.class.getCanonicalName());
        mSet.add(Dao.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Set<? extends Element> database = env.getElementsAnnotatedWith(Database.class);
        if (CollectionUtils.isEmpty(database)) {
            return true;
        }

        Set<? extends Element> tables = env.getElementsAnnotatedWith(Table.class);
        if (CollectionUtils.isEmpty(tables)) {
            return true;
        }

        Set<? extends Element> daos = env.getElementsAnnotatedWith(Dao.class);
        if (CollectionUtils.isEmpty(daos)) {
            return true;
        }

        for (Element element : database) {
            if (element instanceof TypeElement) {
                TypeElement type = (TypeElement) element;
                Database dbAnnotation = type.getAnnotation(Database.class);
                String dbName = dbAnnotation.name();
                int dbVersion = dbAnnotation.version();
                List<TypeElement> tableElements = parseTables(type);
                if (CollectionUtils.isEmpty(tableElements)) {
                    continue;
                }

                PackageElement packageElement = (PackageElement) type.getEnclosingElement();
                Map<TypeElement, TypeElement> daoMap = filterDao(tableElements, daos, packageElement);
                if (CollectionUtils.isEmpty(daoMap)) {
                    continue;
                }

                generateDaos(type, dbName, dbVersion, packageElement, daoMap);
            }
        }
        return true;
    }

    private void generateDaos(TypeElement database, String dbName, int dbVersion, PackageElement packageElement, Map<TypeElement, TypeElement> daoMap) {
        Set<String> generateDaos = new HashSet<>();
        for (Map.Entry<TypeElement, TypeElement> entry : daoMap.entrySet()) {
            TypeElement table = entry.getKey();
            TypeElement dao = entry.getValue();
            String qualifiedDaoName = generateDao(packageElement, table, dao);
            generateDaos.add(qualifiedDaoName);
        }

        generateDatabase(database, dbName, dbVersion, daoMap.keySet(), generateDaos);
    }

    private void generateDatabase(TypeElement database, String dbName, int dbVersion, Set<TypeElement> tables, Set<String> generateDaos) {
        PackageElement packageElement = (PackageElement) database.getEnclosingElement();

        List<? extends Element> elementList = database.getEnclosedElements();
        List<ExecutableElement> methodList = new ArrayList<>();
        for (Element element :elementList) {
            if (element instanceof ExecutableElement) {
                ExecutableElement executableElement = (ExecutableElement) element;
                if (executableElement.getSimpleName().contentEquals("onDBCreate") ||
                        executableElement.getSimpleName().contentEquals("onDBUpgrade")) {
                    continue;
                }

                Set<Modifier> modifiers = executableElement.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (Modifier.ABSTRACT.name().equals(modifier.name())) {
                        methodList.add(executableElement);
                        break;
                    }
                }
            }
        }

        String packageName = packageElement.getQualifiedName().toString();
        String className = database.getSimpleName().toString() + "Impl";
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(packageName).append(";").append(LINE_SEPARATOR);
        builder.append("public class ").append(className).append(" extends ").append(database.getSimpleName().toString()).append(" {").append(LINE_SEPARATOR);
        builder.append("private android.database.sqlite.SQLiteDatabase mDb;").append(LINE_SEPARATOR);
        builder.append("private static android.content.Context sContext;").append(LINE_SEPARATOR);
        builder.append("private static ").append(packageName).append(".").append(className).append(" sInstance;").append(LINE_SEPARATOR);
        builder.append("public static void init(android.content.Context context) {").append(LINE_SEPARATOR);
        builder.append("sContext = context;").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);
        builder.append("private ").append(className).append("() {").append(LINE_SEPARATOR);
        builder.append("DBOpenHelper helper = new DBOpenHelper(sContext, \"").append(dbName).append("\", ").append(dbVersion).append(");").append(LINE_SEPARATOR);
        builder.append("try {").append(LINE_SEPARATOR);
        builder.append("mDb = helper.getWritableDatabase();").append(LINE_SEPARATOR);
        builder.append("} catch (Exception e) {").append(LINE_SEPARATOR);
        builder.append("e.printStackTrace();").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);

        builder.append("class DBOpenHelper extends android.database.sqlite.SQLiteOpenHelper {").append(LINE_SEPARATOR);
        builder.append("public DBOpenHelper(android.content.Context context, String dbName, int version) {").append(LINE_SEPARATOR);
        builder.append("super(context, dbName, null, version);").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);

        builder.append("public void onCreate(android.database.sqlite.SQLiteDatabase db) {").append(LINE_SEPARATOR);
        for (TypeElement typeElement : tables) {
            String createSql = generateCreateSql(typeElement);
            builder.append("db.execSQL(\"").append(createSql).append("\");").append(LINE_SEPARATOR);
        }
        builder.append("onDBCreate(db);");
        builder.append("}").append(LINE_SEPARATOR);

        builder.append("public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {").append(LINE_SEPARATOR);
        builder.append("onDBUpgrade(db, oldVersion, newVersion);").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);

        builder.append("}").append(LINE_SEPARATOR);

        for (ExecutableElement executableElement : methodList) {
            TypeElement typeElement = (TypeElement) mTypes.asElement(executableElement.getReturnType());
            for (String name : generateDaos) {
                if (name.contains(typeElement.getSimpleName().toString())) {
                    Set<Modifier> modifierSet = executableElement.getModifiers();
                    for (Modifier modifier : modifierSet) {
                        if (!modifier.name().equals(Modifier.ABSTRACT.name())) {
                            builder.append(modifier.toString()).append(" ");
                        }
                    }

                    builder.append(typeElement.getQualifiedName().toString()).append(" ").append(executableElement.getSimpleName().toString()).append("() {").append(LINE_SEPARATOR);
                    builder.append("return new ").append(name).append("(mDb);").append(LINE_SEPARATOR);
                    builder.append("}").append(LINE_SEPARATOR);
                }
            }
        }

        builder.append("}").append(LINE_SEPARATOR);
        System.out.println(builder.toString());
        writeToFile(packageName + "." + className, builder.toString());
    }

    private void writeToFile(String fullName, String source) {
        try {
            JavaFileObject javaFileObject = mFiler.createSourceFile(fullName, (Element[]) null);
            Writer writer = javaFileObject.openWriter();
            writer.append(source);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateCreateSql(TypeElement table) {
        List<? extends Element> elements = table.getEnclosedElements();
        List<VariableElement> columns = new ArrayList<>();
        VariableElement key = null;
        Id keyId = null;
        GeneratedValue generatedValue = null;
        for (Element variable : elements) {
            if (variable instanceof VariableElement) {
                VariableElement variableElement = (VariableElement) variable;
                Id id = variableElement.getAnnotation(Id.class);
                if (id != null) {
                    keyId = id;
                    key = variableElement;
                    generatedValue = variableElement.getAnnotation(GeneratedValue.class);
                    continue;
                }
                columns.add(variableElement);
            }
        }

        StringBuilder builder = new StringBuilder();
        Table t = table.getAnnotation(Table.class);
        String tableName = TextUtils.isEmpty(t.name()) ? table.getSimpleName().toString() : t.name();
        String idName = TextUtils.isEmpty(keyId.value()) ? key.getSimpleName().toString() : keyId.value();
        String generateType = generatedValue == null ? "" : generatedValue.generateType() == GenerateType.AUTO_INCREMENT ? "autoincrement" : "";
        builder.append("CREATE TABLE ")
                .append(tableName)
                .append("(")
                .append(idName).append(" ").append(getSQLType(key.asType())).append(" primary key ").append(generateType).append(",");
        for (VariableElement variableElement : columns) {
            builder.append(variableElement.getSimpleName().toString()).append(" ").append(getSQLType(variableElement.asType())).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        return builder.toString();
    }

    public String getSQLType(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.INT || typeMirror.getKind() == TypeKind.LONG || typeMirror.getKind() == TypeKind.SHORT) {
            return "integer";
        } else if (typeMirror.getKind() == TypeKind.DOUBLE || typeMirror.getKind() == TypeKind.FLOAT) {
            return "real";
        } else if (typeMirror.getKind() == TypeKind.BOOLEAN) {
            return "varchar(10)";
        } else {
            return "varchar(255)";
        }
    }

    private String generateDao(PackageElement packageElement, TypeElement table, TypeElement dao) {
        String packageName = packageElement.getQualifiedName().toString() + ".impl";
        String className = dao.getSimpleName().toString() + "Impl";
        StringBuilder builder = new StringBuilder();
        builder.append("package ");
        builder.append(packageName).append(";").append(LINE_SEPARATOR);
        builder.append("public class ").append(className);
        builder.append(" implements ").append(dao.getQualifiedName().toString()).append(" {").append(LINE_SEPARATOR);
        builder.append("protected android.database.sqlite.SQLiteDatabase mDb;").append(LINE_SEPARATOR);
        builder.append("public ").append(className).append("(android.database.sqlite.SQLiteDatabase db) {").append(LINE_SEPARATOR);
        builder.append("this.mDb = db;").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);

        List<? extends Element> elements = table.getEnclosedElements();
        List<VariableElement> columns = new ArrayList<>();
        for (Element variable : elements) {
            if (variable instanceof VariableElement) {
                columns.add((VariableElement) variable);
            }
        }

        for (Element element : dao.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) element;
                Insert insert = method.getAnnotation(Insert.class);
                if (insert != null) {
                    List<? extends VariableElement> params = method.getParameters();
                    if (!CollectionUtils.isEmpty(params) && params.size() == 1) {
                        String paramName = params.get(0).getSimpleName().toString();
                        String paramType = params.get(0).asType().toString();
                        Set<Modifier> modifierSet = method.getModifiers();
                        String methodName = method.getSimpleName().toString();
                        for (Modifier modifier : modifierSet) {
                            if (modifier != Modifier.ABSTRACT) {
                                builder.append(modifier.toString()).append(" ");
                            }
                        }

                        String insertSql = generateInsertSql(table, columns);
                        builder.append("void ").append(methodName).append("(").append(paramType).append(" ").append(paramName).append(")").append(" {").append(LINE_SEPARATOR);
                        builder.append("mDb.execSQL(\"").append(insertSql).append("\"").append(", new Object[] { ");
                        for (VariableElement variable : columns) {
                            builder.append(paramName).append(".get").append(TextUtils.capital(variable.getSimpleName().toString())).append("(),");
                        }
                        builder.deleteCharAt(builder.length() - 1);
                        builder.append("});").append(LINE_SEPARATOR);
                        builder.append("}").append(LINE_SEPARATOR);
                    }
                    continue;
                }
                Delete delete = method.getAnnotation(Delete.class);
                if (delete != null) {
                    List<? extends VariableElement> params = method.getParameters();
                    if (!CollectionUtils.isEmpty(params) && params.size() == 1) {
                        String paramName = params.get(0).getSimpleName().toString();
                        String paramType = params.get(0).asType().toString();
                        Set<Modifier> modifierSet = method.getModifiers();
                        String methodName = method.getSimpleName().toString();
                        for (Modifier modifier : modifierSet) {
                            if (modifier != Modifier.ABSTRACT) {
                                builder.append(modifier.toString()).append(" ");
                            }
                        }

                        String deleteSql = generateDeleteSql(table, columns);
                        builder.append("void ").append(methodName).append("(").append(paramType).append(" ").append(paramName).append(")").append(" {").append(LINE_SEPARATOR);
                        builder.append("mDb.execSQL(\"").append(deleteSql).append("\"").append(", new Object[] { ");;
                        for (VariableElement variable : columns) {
                            Id id = variable.getAnnotation(Id.class);
                            if (id != null) {
                                builder.append(paramName);
                                break;
                            }
                        }
                        builder.append("});").append(LINE_SEPARATOR);
                        builder.append("}").append(LINE_SEPARATOR);
                    }
                    continue;
                }
                Update update = method.getAnnotation(Update.class);
                if (update != null) {
                    List<? extends VariableElement> params = method.getParameters();
                    if (!CollectionUtils.isEmpty(params) && params.size() == 1) {
                        String paramName = params.get(0).getSimpleName().toString();
                        String paramType = params.get(0).asType().toString();
                        Set<Modifier> modifierSet = method.getModifiers();
                        String methodName = method.getSimpleName().toString();
                        for (Modifier modifier : modifierSet) {
                            if (modifier != Modifier.ABSTRACT) {
                                builder.append(modifier.toString()).append(" ");
                            }
                        }

                        String updateSql = generateUpdateSql(table, columns);
                        builder.append("void ").append(methodName).append("(").append(paramType).append(" ").append(paramName).append(")").append(" {").append(LINE_SEPARATOR);
                        builder.append("mDb.execSQL(\"").append(updateSql).append("\"").append(", new Object[] { ");
                        for (VariableElement variable : columns) {
                            Id id = variable.getAnnotation(Id.class);
                            if (id == null) {
                                builder.append(paramName).append(".get").append(TextUtils.capital(variable.getSimpleName().toString())).append("(),");
                            }
                        }

                        for (VariableElement variable : columns) {
                            Id id = variable.getAnnotation(Id.class);
                            if (id != null) {
                                builder.append(paramName).append(".get").append(TextUtils.capital(variable.getSimpleName().toString())).append("()");
                            }
                        }
                        builder.append("});").append(LINE_SEPARATOR);
                        builder.append("}").append(LINE_SEPARATOR);
                    }
                    continue;
                }
                Load load = method.getAnnotation(Load.class);
                if (load != null) {
                    List<? extends VariableElement> params = method.getParameters();
                    if (!CollectionUtils.isEmpty(params) && params.size() == 1) {
                        String paramName = params.get(0).getSimpleName().toString();
                        String paramType = params.get(0).asType().toString();
                        Set<Modifier> modifierSet = method.getModifiers();
                        String methodName = method.getSimpleName().toString();
                        for (Modifier modifier : modifierSet) {
                            if (modifier != Modifier.ABSTRACT) {
                                builder.append(modifier.toString()).append(" ");
                            }
                        }

                        String loadSql = generateLoadSql(table, columns);
                        builder.append(table.getQualifiedName().toString()).append(" ").append(methodName).append("(").append(paramType).append(" ").append(paramName).append(")").append(" {").append(LINE_SEPARATOR);
                        builder.append("android.database.Cursor cursor = mDb.rawQuery(\"").append(loadSql).append("\"").append(", new String[] { String.valueOf(");
                        for (VariableElement variable : columns) {
                            Id id = variable.getAnnotation(Id.class);
                            if (id != null) {
                                builder.append(paramName).append(")}");
                            }
                        }
                        builder.append(");").append(LINE_SEPARATOR);
                        builder.append("if (cursor.moveToFirst()) {").append(LINE_SEPARATOR);
                        builder.append(" return new").append(table.getSimpleName().toString()).append("(cursor);").append(LINE_SEPARATOR);
                        builder.append("}").append(LINE_SEPARATOR);
                        builder.append("return null;").append(LINE_SEPARATOR);
                        builder.append("}").append(LINE_SEPARATOR);
                    }
                    continue;
                }
                Query query = method.getAnnotation(Query.class);
                if (query != null) {
                    Set<Modifier> modifierSet = method.getModifiers();
                    String methodName = method.getSimpleName().toString();
                    for (Modifier modifier : modifierSet) {
                        if (modifier != Modifier.ABSTRACT) {
                            builder.append(modifier.toString()).append(" ");
                        }
                    }

                    String loadSql = query.value();
                    List<? extends VariableElement> params = method.getParameters();
                    builder.append("java.util.List<").append(table.getQualifiedName().toString()).append("> ").append(methodName).append("(");
                    if (!CollectionUtils.isEmpty(params)) {
                        for (VariableElement variableElement : params) {
                            builder.append(variableElement.asType().toString()).append(" ").append(variableElement.getSimpleName().toString()).append(",");
                        }
                        builder.deleteCharAt(builder.length() - 1);
                    }
                    builder.append(")").append(" {").append(LINE_SEPARATOR);
                    builder.append("android.database.Cursor cursor = mDb.rawQuery(\"").append(loadSql).append("\", new String[] { ");
                    if (!CollectionUtils.isEmpty(params)) {
                        for (VariableElement variableElement : params) {
                            builder.append("String.valueOf(").append(variableElement.getSimpleName().toString()).append("),");
                        }
                        builder.deleteCharAt(builder.length() - 1);
                    }
                    builder.append("});").append(LINE_SEPARATOR);
                    builder.append("java.util.List<").append(table.getQualifiedName().toString()).append("> list = new java.util.ArrayList<>();").append(LINE_SEPARATOR);
                    builder.append("while (cursor.moveToNext()) {").append(LINE_SEPARATOR);
                    builder.append(table.getQualifiedName().toString()).append(" obj = ").append("new").append(table.getSimpleName().toString()).append("(cursor);").append(LINE_SEPARATOR);
                    builder.append("list.add(obj);").append(LINE_SEPARATOR);
                    builder.append("}").append(LINE_SEPARATOR);
                    builder.append("return list;").append(LINE_SEPARATOR);
                    builder.append("}").append(LINE_SEPARATOR);
                    continue;
                }
            }
        }

        generateNewMethod(builder, table, columns);
        builder.append("}");
        String fullName = packageName + "." + className;
        writeToFile(fullName, builder.toString());
        return fullName;
    }

    private void generateNewMethod(StringBuilder builder, TypeElement table, List<VariableElement> columns) {
        builder.append("private ").append(table.getQualifiedName().toString()).append(" new");
        builder.append(table.getSimpleName().toString()).append("(android.database.Cursor cursor) {").append(LINE_SEPARATOR);
        builder.append(table.getQualifiedName().toString()).append(" obj = new ").append(table.getQualifiedName().toString()).append("();").append(LINE_SEPARATOR);
        for (VariableElement variableElement : columns) {
            TypeKind typeKind = variableElement.asType().getKind();
            if (typeKind == TypeKind.INT || typeKind == TypeKind.SHORT || typeKind == TypeKind.BYTE) {
                String type = typeKind == TypeKind.INT ? "int" : typeKind == TypeKind.SHORT ? "short" : "byte";
                builder.append("int ").append(variableElement.getSimpleName().toString()).append(" = cursor.getInt");
                builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\"));").append(LINE_SEPARATOR);
                builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append("(").append(type).append(")").append(variableElement.getSimpleName().toString()).append(");").append(LINE_SEPARATOR);;

            } else if (typeKind == TypeKind.LONG) {
                builder.append("long ").append(variableElement.getSimpleName().toString()).append(" = cursor.getLong");
                builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\")));").append(LINE_SEPARATOR);
                builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append(variableElement.getSimpleName().toString()).append(");").append(LINE_SEPARATOR);;
            } else if (typeKind == TypeKind.FLOAT) {
                builder.append("float ").append(variableElement.getSimpleName().toString()).append(" = cursor.getFloat");
                builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\")));").append(LINE_SEPARATOR);
                builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append(variableElement.getSimpleName().toString()).append(");").append(LINE_SEPARATOR);;
            } else if (typeKind == TypeKind.DOUBLE) {
                builder.append("double ").append(variableElement.getSimpleName().toString()).append(" = cursor.getDouble");
                builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\")));").append(LINE_SEPARATOR);
                builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append(variableElement.getSimpleName().toString()).append(");").append(LINE_SEPARATOR);;
            } else {
                if (typeKind == TypeKind.BOOLEAN) {
                    builder.append("String ").append(variableElement.getSimpleName().toString()).append(" = cursor.getString");
                    builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\")));").append(LINE_SEPARATOR);
                    builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append("\"true\".equals(").append(variableElement.getSimpleName().toString()).append("));").append(LINE_SEPARATOR);;
                } else {
                    builder.append("String ").append(variableElement.getSimpleName().toString()).append(" = cursor.getString");
                    builder.append("(cursor.getColumnIndex(\"").append(variableElement.getSimpleName().toString()).append("\"));").append(LINE_SEPARATOR);
                    builder.append("obj.set").append(TextUtils.capital(variableElement.getSimpleName().toString())).append("(").append(variableElement.getSimpleName().toString()).append(");").append(LINE_SEPARATOR);;
                }
            }
        }
        builder.append("return obj;").append(LINE_SEPARATOR);
        builder.append("}").append(LINE_SEPARATOR);
    }

    private String generateLoadSql(TypeElement table, List<VariableElement> columns) {
        StringBuilder builder = new StringBuilder();
        Table t = table.getAnnotation(Table.class);

        String tableName = TextUtils.isEmpty(t.name()) ? table.getSimpleName().toString() : t.name();
        builder.append("SELECT * FROM ").append(tableName).append(" WHERE ");
        for (VariableElement element : columns) {
            Id id = element.getAnnotation(Id.class);
            String name = element.getSimpleName().toString();
            if (id != null) {
                name = TextUtils.isEmpty(id.value()) ? name : id.value();
                builder.append(name).append("=?;");
                break;
            }
        }
        return builder.toString();
    }

    private String generateUpdateSql(TypeElement table, List<VariableElement> columns) {
        StringBuilder builder = new StringBuilder();
        Table t = table.getAnnotation(Table.class);

        String tableName = TextUtils.isEmpty(t.name()) ? table.getSimpleName().toString() : t.name();
        builder.append("UPDATE ").append(tableName).append(" SET ");
        for (VariableElement element : columns) {
            Id id = element.getAnnotation(Id.class);
            String name = element.getSimpleName().toString();
            if (id == null) {
                builder.append(name).append("=?,");
            }
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(" WHERE ");
        for (VariableElement element : columns) {
            Id id = element.getAnnotation(Id.class);
            String name = element.getSimpleName().toString();
            if (id != null) {
                name = TextUtils.isEmpty(id.value()) ? name : id.value();
                builder.append(name).append("=?;");
                break;
            }
        }
        return builder.toString();
    }

    private String generateDeleteSql(TypeElement table, List<VariableElement> columns) {
        StringBuilder builder = new StringBuilder();
        Table t = table.getAnnotation(Table.class);
        String tableName = TextUtils.isEmpty(t.name()) ? table.getSimpleName().toString() : t.name();
        builder.append("DELETE FROM ").append(tableName).append(" WHERE ");
        for (VariableElement element : columns) {
            Id id = element.getAnnotation(Id.class);
            String name = element.getSimpleName().toString();
            if (id != null) {
                name = TextUtils.isEmpty(id.value()) ? name : id.value();
                builder.append(name).append("=?;");
                break;
            }
        }
        return builder.toString();
    }

    private String generateInsertSql(TypeElement table, List<VariableElement> columns) {
        StringBuilder builder = new StringBuilder();
        Table t = table.getAnnotation(Table.class);

        String tableName = TextUtils.isEmpty(t.name()) ? table.getSimpleName().toString() : t.name();
        builder.append("INSERT INTO ").append(tableName).append("(");
        for (VariableElement element : columns) {
            Id id = element.getAnnotation(Id.class);
            String name = element.getSimpleName().toString();
            if (id != null) {
                name = TextUtils.isEmpty(id.value()) ? name : id.value();
            }
            builder.append(name).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(") VALUES(");
        for (VariableElement element : columns) {
            builder.append("?,");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        return builder.toString();
    }

    private Map<TypeElement, TypeElement> filterDao(List<TypeElement> tableElements, Set<? extends Element> daos, PackageElement packageElement) {
        Map<TypeElement, TypeElement> daoMap = new HashMap<>();

        for (TypeElement typeElement : tableElements) {
            for (Element element : daos) {
                if (element instanceof TypeElement) {
                    TypeElement daoElement = (TypeElement) element;
                    if (daoElement.getQualifiedName().toString().startsWith(packageElement.getQualifiedName().toString())) {
                        if (daoElement.getSimpleName().contentEquals(typeElement.getSimpleName().toString() + "Dao")) {
                            daoMap.put(typeElement, daoElement);
                        }
                    }
                }
            }
        }
        return daoMap;
    }

    private List<TypeElement> parseTables(TypeElement type) {
        List<TypeElement> tables = new ArrayList<>();
        List<? extends AnnotationMirror> mirrors = type.getAnnotationMirrors();
        if (CollectionUtils.isEmpty(mirrors) || mirrors.size() != 1) {
            return tables;
        }

        AnnotationMirror dbAnnotationMirror = null;
        AnnotationMirror annotationMirror = mirrors.get(0);
        TypeElement typeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
        if (typeElement.getQualifiedName().contentEquals(Database.class.getCanonicalName())) {
            dbAnnotationMirror = annotationMirror;
        }

        if (dbAnnotationMirror == null) {
            return tables;
        }

        Map<? extends ExecutableElement, ? extends AnnotationValue> map = dbAnnotationMirror.getElementValues();
        if (CollectionUtils.isEmpty(map)) {
            return tables;
        }

        List<AnnotationValue> annotationValues = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : map.entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals("tables")) {
                AnnotationValue value = entry.getValue();
                annotationValues = (List<AnnotationValue>) value.getValue();
                break;
            }
        }

        if (CollectionUtils.isEmpty(annotationValues)) {
            return tables;
        }

        for (AnnotationValue annotationValue : annotationValues) {
            DeclaredType declaredType = (DeclaredType) annotationValue.getValue();
            tables.add((TypeElement) declaredType.asElement());
        }

        return tables;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return mSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }
}
