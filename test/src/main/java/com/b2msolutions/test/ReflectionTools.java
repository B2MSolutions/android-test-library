package com.b2msolutions.test;

import com.b2msolutions.dependency.Dependency;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class ReflectionTools {
    public static ArrayList<Field> getAllClassFields(Class c) {
        ArrayList<Field> fields = new ArrayList<>();
        Class type = c;
        do {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isPrivate(modifiers) && field.getAnnotation(Dependency.class) == null) {
                    continue;
                } else {
                    fields.add(field);
                }
            }
            type = type.getSuperclass();
        }
        while (type != null);
        return fields;
    }
}
