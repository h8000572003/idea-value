package com.h8000572003.values.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SampleValuesTest {

    private static String value(String type, String property) {
        return SampleValues.of(type, property, NumberSequences.sequential());
    }

    @Test
    void stringUsesUpperCasePropertyName() {
        assertEquals("\"USERNAME\"", value("java.lang.String", "userName"));
    }

    @Test
    void numericTypesUseTypedLiterals() {
        assertEquals("1", value("int", "a"));
        assertEquals("1", value("java.lang.Integer", "a"));
        assertEquals("1L", value("long", "a"));
        assertEquals("1L", value("java.lang.Long", "a"));
        assertEquals("(short) 1", value("short", "a"));
        assertEquals("(short) 1", value("java.lang.Short", "a"));
        assertEquals("(byte) 1", value("byte", "a"));
        assertEquals("(byte) 1", value("java.lang.Byte", "a"));
        assertEquals("1f", value("float", "a"));
        assertEquals("1f", value("java.lang.Float", "a"));
        assertEquals("1d", value("double", "a"));
        assertEquals("1d", value("java.lang.Double", "a"));
        assertEquals("java.math.BigDecimal.valueOf(1)", value("java.math.BigDecimal", "a"));
        assertEquals("java.math.BigInteger.valueOf(1)", value("java.math.BigInteger", "a"));
    }

    @Test
    void otherKnownTypes() {
        assertEquals("true", value("boolean", "a"));
        assertEquals("Boolean.TRUE", value("java.lang.Boolean", "a"));
        assertEquals("'A'", value("char", "a"));
        assertEquals("'A'", value("java.lang.Character", "a"));
        assertEquals("new java.util.Date()", value("java.util.Date", "a"));
        assertEquals("java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())", value("java.sql.Timestamp", "a"));
        assertEquals("java.time.LocalDate.now()", value("java.time.LocalDate", "a"));
        assertEquals("java.time.LocalDateTime.now()", value("java.time.LocalDateTime", "a"));
        assertEquals("java.time.LocalTime.now()", value("java.time.LocalTime", "a"));
    }

    @Test
    void unknownTypeIsNull() {
        assertEquals("null", value("com.example.Address", "a"));
        assertEquals("null", value("java.util.List<java.lang.String>", "a"));
    }

    @Test
    void onlyNumericTypesConsumeNumbers() {
        NumberSequence numbers = NumberSequences.sequential();
        assertEquals("\"A\"", SampleValues.of("java.lang.String", "a", numbers));
        assertEquals("1", SampleValues.of("int", "b", numbers));
        assertEquals("2L", SampleValues.of("long", "c", numbers));
    }
}
