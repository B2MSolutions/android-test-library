package com.b2msolutions.test;

import android.content.Context;
import android.os.Build;
import android.util.Pair;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.b2msolutions.test.ReflectionTools.getAllClassFields;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestEnvironmentContext {

    Object testClassObject = null;
    ArrayList<Object> availableMocks = new ArrayList<>();
    HashMap<String, Object> targetDependencies = new HashMap<>();
    HashSet<String> excludedFields = new HashSet<>();

    public TestEnvironmentContext build(Object t) {
        MockitoAnnotations.initMocks(t);
        this.testClassObject = t;
        this.availableMocks = getAllMocks(t);
        this.mapTargetDependencies();
        this.mapDefaultTestTarget();
        return this;
    }

    public TestEnvironmentContext exclude(String... excludedFields) {
        for (String field : excludedFields) {
            this.excludedFields.add(field);
        }
        return this;
    }

    public TestEnvironmentContext withMocks(Object... testDependencies) {
        ArrayList<Object> mergedList = new ArrayList<Object>();
        for (Object dependency : testDependencies) {
            mergedList.add(dependency);
        }
        mergedList.addAll(this.availableMocks);
        this.availableMocks = mergedList;
        return this;
    }

    public TestEnvironmentContext explicit() {
        this.availableMocks = new ArrayList<>();
        return this;
    }

    public TestEnvironmentContext withTestTarget(Object testTargetObject) {
        if (testTargetObject == null)
            throw new RuntimeException("Provide freshly constructed object under test");
        replaceTestTargetFields(testTargetObject);
        setTestTarget(this.testClassObject, testTargetObject);
        return this;
    }

    public TestEnvironmentContext addContextServicesMocks(Pair<String, Class>... services) {
        Context context = findContext(this.testClassObject);
        if (context == null) {
            context = mock(Context.class);
        }

        if (!MockUtil.isMock(context)) {
            return this;
        }

        for (Pair<String, Class> item : services) {
            Object serviceMock = findAppropriateMock(availableMocks, item.second);
            if (serviceMock == null) {
                serviceMock = mock(item.second);
                this.availableMocks.add(serviceMock);
            }

            when(context.getSystemService(item.first)).thenReturn(serviceMock);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when(context.getSystemService(item.second)).thenReturn(serviceMock);
            }
        }

        return this;
    }

    public TestEnvironmentContext addContextServicesMocks(String serviceName, Class className) {
        addContextServicesMocks(new Pair<String, Class>(serviceName, className));
        return this;
    }

    private void replaceTestTargetFields(Object testTargetObject) {
        ArrayList<Field> fields = getAllClassFields(testTargetObject.getClass());

        for (Field field : fields) {
            if (excludedFields.contains(field.getName())) continue;
            if (this.targetDependencies.containsKey(field.getName())) {
                try {
                    field.setAccessible(true);
                    field.set(testTargetObject, this.targetDependencies.get(field.getName()));
                    continue;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            Class fieldType = field.getType();
            if (fieldType.isPrimitive()
                    || fieldType.isAssignableFrom(String.class)
                    || fieldType.isAssignableFrom(Collection.class)
                    || fieldType.isAssignableFrom(Map.class)) continue;

            if (fieldType.getName().startsWith("java.")) {
                continue;
            }

            try {
                Object readyMock = findAppropriateMock(availableMocks, field.getType());
                if (readyMock != null) {
                    field.setAccessible(true);
                    field.set(testTargetObject, readyMock);
                } else {
                    try {
                        field.setAccessible(true);
                        field.set(testTargetObject, mock(field.getType()));
                    } catch (Exception e) {
                        System.out.println("Failed to replace dependency: " + e.getMessage());
                        //failed to init mock for field type
                    }
                }
            } catch (IllegalAccessException e) {
            }
        }
    }

    private static void setTestTarget(Object testClassObject, Object testTarget) {
        Field field = findTestTargetField(testClassObject);
        if (field == null) throw new RuntimeException("No field annotated as @TestTarget");
        try {
            field.setAccessible(true);
            field.set(testClassObject, testTarget);
        } catch (Exception e) {
            throw new RuntimeException("failed to set property", e);
        }
    }

    private static Field findTestTargetField(Object testClassObject) {
        Field[] fields = testClassObject.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(TestTarget.class) != null) return field;
        }
        return null;
    }

    private static ArrayList<Object> getAllMocks(Object testClassObject) {
        ArrayList<Object> list = new ArrayList<>();
        Field[] fields = testClassObject.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Mock.class) != null && field.getAnnotation(TargetField.class) == null) {
                try {
                    field.setAccessible(true);
                    list.add(field.get(testClassObject));
                    if (Context.class.isAssignableFrom(field.getType())) {
                        addApplicationContext(field.get(testClassObject));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

    private void mapTargetDependencies() {
        Field[] fields = testClassObject.getClass().getDeclaredFields();
        for (Field field : fields) {
            TargetField targetField = field.getAnnotation(TargetField.class);
            if (targetField != null) {
                try {
                    field.setAccessible(true);
                    this.targetDependencies.put(targetField.value(), field.get(testClassObject));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void mapDefaultTestTarget() {
        Field field = findTestTargetField(testClassObject);
        if (field == null) return;

        try {
            Object instance = spy(field.getType().getConstructor().newInstance());
            replaceTestTargetFields(instance);
            field.setAccessible(true);
            field.set(testClassObject, instance);
        } catch (Exception e) {
            return;
        }
    }

    private static Object findAppropriateMock(List<Object> readyMocks, Class c) {
        for (Object m : readyMocks) {
            if (c.isInstance(m)) return m;
        }
        return null;
    }

    private static Context findContext(Object t) {
        for (Field field : getAllClassFields(t.getClass())) {
            if (!field.getType().isAssignableFrom(Context.class)) continue;
            try {
                field.setAccessible(true);
                return (Context) field.get(t);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static void addApplicationContext(Object context) {
        if (MockUtil.isMock(context)) {
            when(((Context) context).getApplicationContext()).thenReturn((Context) context);
        }
    }
}
