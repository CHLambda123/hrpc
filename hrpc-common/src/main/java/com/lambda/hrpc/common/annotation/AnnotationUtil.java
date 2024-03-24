package com.lambda.hrpc.common.annotation;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AnnotationUtil {
    public static List<Class<?>> scanAnnotation(String packageName, Class<?> annotation) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> typeSet = reflections.getTypesAnnotatedWith((Class<? extends Annotation>) annotation);
        return new ArrayList<>(typeSet);
    }
    
}
