package com.h8000572003.values.commons;

public class ApostropheUtils {


    public static String replaceEnd(String value) {
        return value.replaceAll("\"\\s*'\\s*$", "\" ");
    }

    public static String replaceStart(String value) {
        return value.replaceAll("\\s*'\\s*\"", " \"");
    }
}
