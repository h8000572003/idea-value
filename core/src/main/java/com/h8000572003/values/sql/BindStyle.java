package com.h8000572003.values.sql;

/**
 * How bind parameters are written into the SQL.
 */
public enum BindStyle {
    /** {@code :name}, values collected into a {@code Map}. */
    NAMED,
    /** {@code ?}, values collected into a {@code List} in order. */
    POSITIONAL
}
