package com.h8000572003.values.codegen;

import java.util.Optional;

/**
 * JavaBeans property naming.
 */
public final class PropertyNames {

    private PropertyNames() {
    }

    public static Optional<String> fromSetter(String methodName) {
        return strip(methodName, "set");
    }

    public static Optional<String> fromGetter(String methodName) {
        return strip(methodName, "get").or(() -> strip(methodName, "is"));
    }

    public static String setterFor(String property) {
        return "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    }

    /** {@link java.beans.Introspector#decapitalize} rules: {@code URL} stays, {@code UserDto} becomes {@code userDto}. */
    public static String decapitalize(String name) {
        if (name.isEmpty() || name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static Optional<String> strip(String methodName, String prefix) {
        if (methodName.length() > prefix.length() && methodName.startsWith(prefix)
                && Character.isUpperCase(methodName.charAt(prefix.length()))) {
            return Optional.of(decapitalize(methodName.substring(prefix.length())));
        }
        return Optional.empty();
    }
}
