package com.haleywang.putty.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * @author haley
 */
public class FieldUtils {
    private FieldUtils() {
    }


    /**
     * Find fields of a class by its annotation
     *
     * @param classs
     * @param ann
     * @return null safe set
     */
    public static Set<Field> findFields(Class<?> classs, Class<? extends Annotation> ann) {
        Set<Field> set = new HashSet<>();
        if (ann == null) {
            return set;
        }
        Class<?> c = classs;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(ann)) {
                    set.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return set;
    }


    /**
     * Find fields of a class by its annotation except fields of superclass
     *
     * @param classs
     * @param ann
     * @return null safe set
     */
    public static Set<Field> findSelfFields(Class<?> classs, Class<? extends Annotation> ann) {
        Set<Field> set = new HashSet<>();
        if (classs == null || ann == null) {
            return set;
        }


        for (Field field : classs.getDeclaredFields()) {
            if (field.isAnnotationPresent(ann)) {
                set.add(field);
            }
        }


        return set;
    }
}