package com.h8000572003.values.codegen;

import java.util.Locale;
import java.util.Map;
import java.util.function.LongFunction;

/**
 * Java source of a sample value for a property of the given type.
 */
public final class SampleValues {

    private static final Map<String, LongFunction<String>> NUMBERS = Map.ofEntries(
            Map.entry("int", n -> Long.toString(n)),
            Map.entry("java.lang.Integer", n -> Long.toString(n)),
            Map.entry("long", n -> n + "L"),
            Map.entry("java.lang.Long", n -> n + "L"),
            Map.entry("short", n -> "(short) " + n),
            Map.entry("java.lang.Short", n -> "(short) " + n),
            Map.entry("byte", n -> "(byte) " + n),
            Map.entry("java.lang.Byte", n -> "(byte) " + n),
            Map.entry("float", n -> n + "f"),
            Map.entry("java.lang.Float", n -> n + "f"),
            Map.entry("double", n -> n + "d"),
            Map.entry("java.lang.Double", n -> n + "d"),
            Map.entry("java.math.BigDecimal", n -> "java.math.BigDecimal.valueOf(" + n + ")"),
            Map.entry("java.math.BigInteger", n -> "java.math.BigInteger.valueOf(" + n + ")"));

    private static final Map<String, String> CONSTANTS = Map.ofEntries(
            Map.entry("boolean", "true"),
            Map.entry("java.lang.Boolean", "Boolean.TRUE"),
            Map.entry("char", "'A'"),
            Map.entry("java.lang.Character", "'A'"),
            Map.entry("java.util.Date", "new java.util.Date()"),
            Map.entry("java.sql.Timestamp", "java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())"),
            Map.entry("java.time.LocalDate", "java.time.LocalDate.now()"),
            Map.entry("java.time.LocalDateTime", "java.time.LocalDateTime.now()"),
            Map.entry("java.time.LocalTime", "java.time.LocalTime.now()"));

    private SampleValues() {
    }

    public static String of(String type, String property, NumberSequence numbers) {
        if ("java.lang.String".equals(type)) {
            return '"' + property.toUpperCase(Locale.ROOT) + '"';
        }
        LongFunction<String> number = NUMBERS.get(type);
        if (number != null) {
            return number.apply(numbers.next(property));
        }
        return CONSTANTS.getOrDefault(type, "null");
    }
}
