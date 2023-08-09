package com.h8000572003.values;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class KeppNameNumberValueStrategy implements Assignment.NumberValueStrategy {

    private Long startValue = 0l;
    private Map<String, Long> valuesMap = new HashMap<>();

    @Override
    public String getValue(String name) {
       return valuesMap.compute(name, (s1, aLong) -> {
            if (aLong == null) {
                return ++startValue;
            }
            return aLong;
        }) + StringUtils.EMPTY;
    }


}
