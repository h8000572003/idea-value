package com.h8000572003.values;

import com.h8000572003.values.commons.ApostropheUtils;

import static com.h8000572003.values.commons.ApostropheUtils.replaceStartWithPercentage;
import static org.junit.Assert.assertEquals;

public class ApostropheUtilsTest {

    @org.junit.Test
    public void testStartNotion() {
        checkStartWith("'\"12345", " \"12345");
        checkStartWith(" '\"12345", " \"12345");
        checkStartWith("value= '\"12345", "value= \"12345");
        checkStartWith("\" and p1=to_char(systime,'yyymmdd') and p2= '%\"", "\" and p1=to_char(systime,'yyymmdd') and p2= %\"");
    }

    private static void checkStartWith(String value1, String ans) {
        String x = ApostropheUtils.replaceAll(value1);
        assertEquals(ans, x);
    }

    @org.junit.Test
    public void test2() {
        checkEnd("12345\"'", "12345\" ");
        checkEnd("12345\"' ", "12345\" ");
        checkEnd("value=12345\"' ", "value=12345\" ");
        checkEnd("value=12345\"'%", "value=12345\"% ");
    }

    @org.junit.Test
    public void test3() {
        String x = replaceStartWithPercentage("='%\"");
        System.out.println(x);
    }

    @org.junit.Test
    public void test4() {
        String value = null;
        if ( value.isBlank()) {

        }
    }


    private static void checkEnd(String value11, String ans) {
        String value1 = value11;

        String x = ApostropheUtils.replaceAll(value1);
        assertEquals(ans, x);
    }

}
