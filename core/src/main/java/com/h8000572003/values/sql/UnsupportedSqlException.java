package com.h8000572003.values.sql;

/**
 * The concatenation cannot be turned into a safe parameterized query automatically.
 */
public class UnsupportedSqlException extends RuntimeException {
    public UnsupportedSqlException(String message) {
        super(message);
    }
}
