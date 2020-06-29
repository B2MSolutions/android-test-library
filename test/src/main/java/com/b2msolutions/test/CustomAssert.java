package com.b2msolutions.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

public class CustomAssert {
    public static <T extends Exception> T assertThrows(
            final Class<T> expected,
            final Testable codeUnderTest) throws Exception {
        T result = null;
        try {
            codeUnderTest.run();
            throw new AssertException("Expecting exception but none was thrown.");
        } catch (final Exception actual) {
            if (expected.isInstance(actual)) {
                result = expected.cast(actual);
            } else {
                throw actual;
            }
        }
        return result;
    }

    public static void assertConstructed(Object o, String... ignoredFields) {
        HashSet<String> fieldsToIgnore = new HashSet<>();
        for (String ignoredField:  ignoredFields) {
            fieldsToIgnore.add(ignoredField);
        }
        ArrayList<Field> fields = ReflectionTools.getAllClassFields(o.getClass());
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                continue;
            }

            try {
                field.setAccessible(true);
                if (field.get(o) != null) {
                    continue;
                }
                if(field.getType().isPrimitive()){
                    continue;
                }
                if(fieldsToIgnore.contains(field.getName())){
                    continue;
                }
            } catch (IllegalAccessException e) {
                continue;
            }

            field.setAccessible(true);
            throw new AssertException(field.getName() + " field is not initialised");
        }
    }
}
