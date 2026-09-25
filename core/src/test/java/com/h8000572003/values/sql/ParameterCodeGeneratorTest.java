package com.h8000572003.values.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterCodeGeneratorTest {

    @Test
    void namedParametersWithDeclaration() {
        List<String> statements = ParameterCodeGenerator.statements(BindStyle.NAMED, "params",
                List.of(new BindParameter("id", "dto.getId()"), new BindParameter("name", "\"%\" + name")), true);

        assertEquals(List.of(
                "java.util.Map<java.lang.String, java.lang.Object> params = new java.util.HashMap<>();",
                "params.put(\"id\", dto.getId());",
                "params.put(\"name\", \"%\" + name);"), statements);
    }

    @Test
    void namedParametersWithoutDeclaration() {
        List<String> statements = ParameterCodeGenerator.statements(BindStyle.NAMED, "map",
                List.of(new BindParameter("id", "id")), false);

        assertEquals(List.of("map.put(\"id\", id);"), statements);
    }

    @Test
    void positionalParametersWithDeclaration() {
        List<String> statements = ParameterCodeGenerator.statements(BindStyle.POSITIONAL, "params",
                List.of(new BindParameter(null, "a"), new BindParameter(null, "a")), true);

        assertEquals(List.of(
                "java.util.List<java.lang.Object> params = new java.util.ArrayList<>();",
                "params.add(a);",
                "params.add(a);"), statements);
    }

    @Test
    void positionalParametersWithoutDeclaration() {
        List<String> statements = ParameterCodeGenerator.statements(BindStyle.POSITIONAL, "list",
                List.of(new BindParameter(null, "a")), false);

        assertEquals(List.of("list.add(a);"), statements);
    }
}
