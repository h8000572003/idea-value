package com.h8000572003.values.codegen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generates Java statements and declarations from property accessors.
 */
public final class CodeGenerators {

    private CodeGenerators() {
    }

    /** {@code user.setName("NAME");} for every setter. */
    public static List<String> setterCalls(String variable, List<Accessor> setters, NumberSequence numbers) {
        List<String> statements = new ArrayList<>();
        for (Accessor setter : setters) {
            PropertyNames.fromSetter(setter.methodName()).ifPresent(property -> statements.add(
                    "%s.%s(%s);".formatted(variable, setter.methodName(), SampleValues.of(setter.type(), property, numbers))));
        }
        return statements;
    }

    /** {@code target.setX(source.getX());} for every property the target can set and the source can get. */
    public static List<String> copyProperties(String target, List<String> targetSetters, String source, List<String> sourceGetters) {
        Map<String, String> getterByProperty = gettersByProperty(sourceGetters);
        List<String> statements = new ArrayList<>();
        for (String setter : targetSetters) {
            Optional<String> getter = PropertyNames.fromSetter(setter).map(getterByProperty::get);
            getter.ifPresent(g -> statements.add("%s.%s(%s.%s());".formatted(target, setter, source, g)));
        }
        return statements;
    }

    /** Creates a new target, copies properties from the source and returns it. */
    public static List<String> newInstanceMapping(String targetType, List<String> targetSetters, String source, List<String> sourceGetters) {
        int typeArguments = targetType.indexOf('<');
        String rawType = typeArguments < 0 ? targetType : targetType.substring(0, typeArguments);
        String simpleName = rawType.substring(rawType.lastIndexOf('.') + 1);
        String variable = PropertyNames.decapitalize(simpleName);
        if (variable.equals(source)) {
            variable = "result";
        }
        List<String> statements = new ArrayList<>();
        statements.add("%s %s = new %s%s();".formatted(targetType, variable, rawType, typeArguments < 0 ? "" : "<>"));
        statements.addAll(copyProperties(variable, targetSetters, source, sourceGetters));
        statements.add("return %s;".formatted(variable));
        return statements;
    }

    /** {@code assertEquals(expected.getX(), actual.getX());} for every getter. */
    public static List<String> assertions(String expected, String actual, List<Accessor> getters) {
        List<String> statements = new ArrayList<>();
        for (Accessor getter : getters) {
            if (PropertyNames.fromGetter(getter.methodName()).isPresent()) {
                String assertion = getter.type().endsWith("[]") ? "assertArrayEquals" : "assertEquals";
                statements.add("%s(%s.%s(), %s.%s());".formatted(
                        assertion, expected, getter.methodName(), actual, getter.methodName()));
            }
        }
        return statements;
    }

    /** {@code private Type name;} for every getter property that has no field yet. */
    public static List<String> fieldsFromGetters(List<Accessor> getters, Set<String> existingFields) {
        Set<String> declared = new LinkedHashSet<>(existingFields);
        List<String> fields = new ArrayList<>();
        for (Accessor getter : getters) {
            PropertyNames.fromGetter(getter.methodName())
                    .filter(declared::add)
                    .ifPresent(property -> fields.add("private %s %s;".formatted(getter.type(), property)));
        }
        return fields;
    }

    private static Map<String, String> gettersByProperty(List<String> getters) {
        Map<String, String> byProperty = new LinkedHashMap<>();
        for (String getter : getters) {
            PropertyNames.fromGetter(getter).ifPresent(property -> byProperty.merge(property, getter,
                    (existing, candidate) -> candidate.startsWith("get") ? candidate : existing));
        }
        return byProperty;
    }
}
