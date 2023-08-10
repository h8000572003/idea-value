package com.h8000572003.values.commons;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class KeppNameNumberValueStrategy implements Assignment.NumberValueStrategy {

    private Long startValue = 0L;
    private final Map<String, Long> valuesMap = new HashMap<>();

    @Override
    public String getValue(String name) {
       return valuesMap.compute(name, (s1, aLong) -> {
            if (aLong == null) {
                return getIncrease();
            }
            return aLong;
        }) + StringUtils.EMPTY;
    }

    private Long getIncrease() {
         ++startValue;
         if(startValue>=Long.MAX_VALUE){
             startValue=0L;
         }
         return startValue;
    }


}
