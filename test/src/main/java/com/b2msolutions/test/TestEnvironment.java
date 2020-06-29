package com.b2msolutions.test;

public class TestEnvironment {

    public static TestEnvironmentContext build(Object t) {
        TestEnvironmentContext testContext = new TestEnvironmentContext();
        testContext.build(t);
        return testContext;
    }

    public static TestEnvironmentContext exclude(String... excludedFields){
        TestEnvironmentContext testContext = new TestEnvironmentContext();
        return testContext.exclude(excludedFields);
    }
}
