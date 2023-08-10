package com.h8000572003.values.commons;

import com.intellij.psi.PsiParameter;
import com.intellij.util.Function;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Assignment {
    private final Map<String, Function<PsiParameter, String>> canonicalMap = new HashMap<>();
    private static final Function<PsiParameter, String> fileNameValue = s -> "\"" + s.getName().replace("set", "").toUpperCase() + "\"";
    private static final Function<PsiParameter, String> unknown = s -> "";


    public interface NumberValueStrategy {
        String getValue(String name);
    }


    public Assignment(NumberValueStrategy valueStrategy) {

        this.canonicalMap.put("int", i -> valueStrategy.getValue(i.getName()));
        this.canonicalMap.put(Integer.class.getName(), i -> valueStrategy.getValue(i.getName()));


        this.canonicalMap.put(String.class.getName(), fileNameValue);

        this.canonicalMap.put("short", s -> "(short)" + valueStrategy.getValue(s.getName()));
        this.canonicalMap.put(Short.class.getName(), s -> "(short)" + valueStrategy.getValue(s.getName()));

        this.canonicalMap.put(BigDecimal.class.getName(), s -> "BigDecimal.valueOf(" + valueStrategy.getValue(s.getName()) + ")");

        this.canonicalMap.put("long", i -> valueStrategy.getValue(i.getName()) + "L");
        this.canonicalMap.put(Long.class.getName(), i -> valueStrategy.getValue(i.getName()) + "L");

        this.canonicalMap.put("float", i -> valueStrategy.getValue(i.getName()) + "f");
        this.canonicalMap.put(Float.class.getName(), i -> valueStrategy.getValue(i.getName()) + "f");


        this.canonicalMap.put("double", i -> valueStrategy.getValue(i.getName()) + "d");
        this.canonicalMap.put(Double.class.getName(), i -> valueStrategy.getValue(i.getName()) + "d");

        this.canonicalMap.put("boolean", s -> "Boolean.TRUE");
        this.canonicalMap.put(Boolean.class.getName(), s -> "Boolean.TRUE");

        this.canonicalMap.put(Date.class.getName(), s -> "new Date()");
        this.canonicalMap.put(Timestamp.class.getName(), s -> "Timestamp.valueOf(LocalDateTime.now())");


    }

    public String getAssignment(String canonicalText, PsiParameter parameter) {
        return canonicalMap.getOrDefault(canonicalText, unknown).fun(parameter);
    }
}
