package com.example.dbprocessor;

import com.example.dbannotation.annotation.Dao;
import com.example.dbannotation.annotation.Database;
import com.example.dbannotation.annotation.Table;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
// 测试专用
public class CheckProcessor extends AbstractProcessor {

    private Set<String> mSet;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        mSet = new HashSet<>();
        mSet.add(Database.class.getCanonicalName());
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
        Set<? extends Element> database = env.getElementsAnnotatedWith(Database.class);
        for (Iterator<? extends Element> i = database.iterator(); i.hasNext();) {
            TypeElement element = (TypeElement) i.next();
            System.out.println(element);
            List<? extends Element> elements = element.getEnclosedElements();
            for (Iterator<? extends Element> eli = elements.iterator(); eli.hasNext(); ) {
                Element el = eli.next();
                System.out.println(el);
            }

            Element enclosingElement = element.getEnclosingElement();
            System.out.println(enclosingElement);

            List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
            for (Iterator<? extends AnnotationMirror> mirrorIter = annotationMirrors.iterator(); mirrorIter.hasNext(); ) {
                AnnotationMirror annotationMirror = mirrorIter.next();
                System.out.println(annotationMirror);

                Map<? extends Element, ? extends AnnotationValue> annotationValues = annotationMirror.getElementValues();
                for (Map.Entry<? extends Element, ? extends AnnotationValue> entry : annotationValues.entrySet()) {
                    System.out.println(entry.getKey() + " = " + entry.getValue());
                }
            }

            Database db = element.getAnnotation(Database.class);
            System.out.println(db.name());
            // System.out.println(db.tables()); // 抛出异常
        }
        return true;
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
