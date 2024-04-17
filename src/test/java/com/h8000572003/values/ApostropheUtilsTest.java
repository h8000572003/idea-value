package com.h8000572003.values;

import com.h8000572003.values.commons.ApostropheUtils;

import static org.junit.Assert.assertEquals;

public class ApostropheUtilsTest {

    @org.junit.Test
    public void testStartNotion() {
        checkStartWith("'\"12345", " \"12345");
        checkStartWith(" '\"12345", " \"12345");
        checkStartWith("value= '\"12345", "value= \"12345");
    }

    private static void checkStartWith(String value1, String ans) {
        String x = ApostropheUtils.replaceStart(value1);
        assertEquals(ans, x);
    }

    @org.junit.Test
    public void test2() {
        checkEnd("12345\"'", "12345\" ");
        checkEnd("12345\"' ", "12345\" ");
        checkEnd("value=12345\"' ", "value=12345\" ");
    }

    private static void checkEnd(String value11, String ans) {
        String value1 = value11;

        String x = ApostropheUtils.replaceEnd(value1);
        assertEquals(ans, x);
    }

}
