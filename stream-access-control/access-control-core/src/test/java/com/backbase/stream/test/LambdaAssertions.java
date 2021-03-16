package com.backbase.stream.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

public class LambdaAssertions {

    private LambdaAssertions() {
    }

    public static <T> Consumer<T> assertEqualsTo(T expected) {
        return actual -> assertEquals(expected, actual);
    }
}
