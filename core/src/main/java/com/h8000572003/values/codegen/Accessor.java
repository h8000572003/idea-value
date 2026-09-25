package com.h8000572003.values.codegen;

/**
 * A method used as a property accessor.
 *
 * @param methodName method name, e.g. {@code getName} or {@code setName}
 * @param type       getter return type or setter parameter type
 */
public record Accessor(String methodName, String type) {
}
