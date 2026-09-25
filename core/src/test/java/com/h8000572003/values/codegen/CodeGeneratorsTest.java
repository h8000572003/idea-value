package com.h8000572003.values.codegen;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeGeneratorsTest {

    @Nested
    class SetterCalls {

        @Test
        void callsEverySetterWithSampleValue() {
            List<String> statements = CodeGenerators.setterCalls("user", List.of(
                    new Accessor("setName", "java.lang.String"),
                    new Accessor("setAge", "int"),
                    new Accessor("setScore", "long")), NumberSequences.sequential());

            assertEquals(List.of(
                    "user.setName(\"NAME\");",
                    "user.setAge(1);",
                    "user.setScore(2L);"), statements);
        }

        @Test
        void skipsMethodsThatAreNotSetters() {
            List<String> statements = CodeGenerators.setterCalls("user", List.of(
                    new Accessor("reset", "int"),
                    new Accessor("settle", "int"),
                    new Accessor("setAge", "int")), NumberSequences.sequential());

            assertEquals(List.of("user.setAge(1);"), statements);
        }
    }

    @Nested
    class CopyProperties {

        @Test
        void copiesPropertiesThatTargetCanSetAndSourceCanGet() {
            List<String> statements = CodeGenerators.copyProperties(
                    "target", List.of("setName", "setActive", "setOnlyOnTarget"),
                    "source", List.of("getName", "isActive", "getOnlyOnSource"));

            assertEquals(List.of(
                    "target.setName(source.getName());",
                    "target.setActive(source.isActive());"), statements);
        }

        @Test
        void prefersGetOverIsForSameProperty() {
            List<String> statements = CodeGenerators.copyProperties(
                    "t", List.of("setFlag"), "s", List.of("isFlag", "getFlag"));

            assertEquals(List.of("t.setFlag(s.getFlag());"), statements);
        }
    }

    @Nested
    class NewInstanceMapping {

        @Test
        void createsTargetCopiesAndReturns() {
            List<String> statements = CodeGenerators.newInstanceMapping(
                    "UserDto", List.of("setName"), "user", List.of("getName"));

            assertEquals(List.of(
                    "UserDto userDto = new UserDto();",
                    "userDto.setName(user.getName());",
                    "return userDto;"), statements);
        }

        @Test
        void avoidsClashWithSourceName() {
            List<String> statements = CodeGenerators.newInstanceMapping(
                    "User", List.of("setName"), "user", List.of("getName"));

            assertEquals(List.of(
                    "User result = new User();",
                    "result.setName(user.getName());",
                    "return result;"), statements);
        }

        @Test
        void usesSimpleNameForVariableOfGenericType() {
            List<String> statements = CodeGenerators.newInstanceMapping(
                    "Box<String>", List.of(), "s", List.of());

            assertEquals(List.of("Box<String> box = new Box<>();", "return box;"), statements);
        }
    }

    @Nested
    class Assertions {

        @Test
        void assertsEveryGetter() {
            List<String> statements = CodeGenerators.assertions("expected", "actual", List.of(
                    new Accessor("getName", "java.lang.String"),
                    new Accessor("isActive", "boolean"),
                    new Accessor("getCodes", "int[]"),
                    new Accessor("compute", "int")));

            assertEquals(List.of(
                    "assertEquals(expected.getName(), actual.getName());",
                    "assertEquals(expected.isActive(), actual.isActive());",
                    "assertArrayEquals(expected.getCodes(), actual.getCodes());"), statements);
        }
    }

    @Nested
    class FieldsFromGetters {

        @Test
        void declaresFieldPerGetterProperty() {
            List<String> fields = CodeGenerators.fieldsFromGetters(List.of(
                    new Accessor("getName", "String"),
                    new Accessor("isActive", "boolean"),
                    new Accessor("getIsbn", "String"),
                    new Accessor("island", "int")), Set.of());

            assertEquals(List.of(
                    "private String name;",
                    "private boolean active;",
                    "private String isbn;"), fields);
        }

        @Test
        void skipsExistingFieldsAndDuplicateProperties() {
            List<String> fields = CodeGenerators.fieldsFromGetters(List.of(
                    new Accessor("getName", "String"),
                    new Accessor("getFlag", "Boolean"),
                    new Accessor("isFlag", "boolean")), Set.of("name"));

            assertEquals(List.of("private Boolean flag;"), fields);
        }
    }
}
