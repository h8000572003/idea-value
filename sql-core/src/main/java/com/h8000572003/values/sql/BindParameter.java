package com.h8000572003.values.sql;

/**
 * A value to bind.
 *
 * @param name  parameter name for {@link BindStyle#NAMED}, {@code null} for {@link BindStyle#POSITIONAL}
 * @param value Java expression producing the value
 */
public record BindParameter(String name, String value) {
}
