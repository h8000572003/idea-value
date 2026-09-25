package com.h8000572003.values.codegen;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyNamesTest {

    @Test
    void propertyFromSetter() {
        assertEquals(Optional.of("userId"), PropertyNames.fromSetter("setUserId"));
        assertEquals(Optional.of("x"), PropertyNames.fromSetter("setX"));
        assertEquals(Optional.of("URL"), PropertyNames.fromSetter("setURL"));
    }

    @Test
    void notASetter() {
        assertEquals(Optional.empty(), PropertyNames.fromSetter("set"));
        assertEquals(Optional.empty(), PropertyNames.fromSetter("settle"));
        assertEquals(Optional.empty(), PropertyNames.fromSetter("reset"));
    }

    @Test
    void propertyFromGetter() {
        assertEquals(Optional.of("name"), PropertyNames.fromGetter("getName"));
        assertEquals(Optional.of("active"), PropertyNames.fromGetter("isActive"));
        assertEquals(Optional.of("URL"), PropertyNames.fromGetter("getURL"));
    }

    @Test
    void notAGetter() {
        assertEquals(Optional.empty(), PropertyNames.fromGetter("get"));
        assertEquals(Optional.empty(), PropertyNames.fromGetter("island"));
        assertEquals(Optional.empty(), PropertyNames.fromGetter("getter"));
        assertEquals(Optional.empty(), PropertyNames.fromGetter("name"));
    }

    @Test
    void decapitalizeFollowsJavaBeans() {
        assertEquals("userDto", PropertyNames.decapitalize("UserDto"));
        assertEquals("URL", PropertyNames.decapitalize("URL"));
        assertEquals("", PropertyNames.decapitalize(""));
    }

    @Test
    void setterForProperty() {
        assertEquals("setUserId", PropertyNames.setterFor("userId"));
        assertEquals("setURL", PropertyNames.setterFor("URL"));
    }
}
