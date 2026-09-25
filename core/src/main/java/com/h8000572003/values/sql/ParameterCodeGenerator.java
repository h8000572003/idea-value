package com.h8000572003.values.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Java statements that collect bind parameter values.
 * Types are fully qualified; the caller is expected to shorten them.
 */
public final class ParameterCodeGenerator {

    private ParameterCodeGenerator() {
    }

    public static List<String> statements(BindStyle style, String variable, List<BindParameter> parameters, boolean declare) {
        List<String> statements = new ArrayList<>();
        if (declare) {
            statements.add(style == BindStyle.NAMED
                    ? "java.util.Map<java.lang.String, java.lang.Object> " + variable + " = new java.util.HashMap<>();"
                    : "java.util.List<java.lang.Object> " + variable + " = new java.util.ArrayList<>();");
        }
        for (BindParameter parameter : parameters) {
            statements.add(style == BindStyle.NAMED
                    ? variable + ".put(" + JavaStrings.literal(parameter.name()) + ", " + parameter.value() + ");"
                    : variable + ".add(" + parameter.value() + ");");
        }
        return statements;
    }
}
