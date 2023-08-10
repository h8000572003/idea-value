package com.h8000572003.values.commons;


public class NotKeepValueStrategy implements Assignment.NumberValueStrategy {

    private Long startValue = 0L;

    @Override
    public String getValue(String name) {
        return getIncrease() + "";
    }


    private Long getIncrease() {
        ++startValue;
        if(startValue>=Long.MAX_VALUE){
            startValue=0L;
        }
        return startValue;
    }
}
