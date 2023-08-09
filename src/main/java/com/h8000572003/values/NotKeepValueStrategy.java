package com.h8000572003.values;


public class NotKeepValueStrategy implements Assignment.NumberValueStrategy {

    private Long startValue = 0l;

    @Override
    public String getValue(String name) {
        return ++startValue + "";
    }


}
