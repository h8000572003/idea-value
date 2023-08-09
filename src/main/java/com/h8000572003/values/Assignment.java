package com.h8000572003.values;

import com.intellij.psi.PsiParameter;
import com.intellij.util.Function;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Assignment {
    private Map<String, Function<PsiParameter, String>> canonicalMap = new HashMap<>();
    private final static Function<PsiParameter, String> fileNameValue = s -> "\"" + s.getName().replace("set", "").toUpperCase() + "\"";
    private final static Function<PsiParameter, String> unknow = s -> "";

    private Long startValue = 0l;
    private Map<String, Long> valuesMap = new HashMap<>();
    private final Function<PsiParameter, String> values = s ->
            valuesMap.compute(s.getName(), (s1, aLong) -> {
                if (aLong == null) {
                    return ++startValue;
                }
                return aLong;
            }) + "";

    public Assignment() {

        this.canonicalMap.put("int", values);
        this.canonicalMap.put(Integer.class.getName(), values);


        this.canonicalMap.put(String.class.getName(), fileNameValue);

        this.canonicalMap.put("short", s -> "(short)" + values.fun(s) + "");
        this.canonicalMap.put(Short.class.getName(), s -> "(short)" + values.fun(s) + "");

        this.canonicalMap.put(BigDecimal.class.getName(), s -> "BigDecimal.valueOf(" + ++startValue + ")");

        this.canonicalMap.put("long", values);
        this.canonicalMap.put(Long.class.getName(), values);

        this.canonicalMap.put("float", values);
        this.canonicalMap.put(Float.class.getName(), values);


        this.canonicalMap.put("double", values);
        this.canonicalMap.put(Double.class.getName(), values);

        this.canonicalMap.put("boolean", s -> "Boolean.TRUE");
        this.canonicalMap.put(Boolean.class.getName(), s -> "Boolean.TRUE");

        this.canonicalMap.put(Date.class.getName(), s -> "new Date()");
        this.canonicalMap.put(Timestamp.class.getName(), s -> "Timestamp.valueOf(LocalDateTime.now())");


    }

    public String getAssignment(String canonicalText, PsiParameter parameter) {
        return canonicalMap.getOrDefault(canonicalText, unknow).fun(parameter);
    }
}
