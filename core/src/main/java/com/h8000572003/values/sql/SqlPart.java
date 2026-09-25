package com.h8000572003.values.sql;

/**
 * One operand of a Java string concatenation that builds SQL.
 */
public sealed interface SqlPart {

    /** SQL text from a Java string literal (already unescaped). */
    record Literal(String value) implements SqlPart {
    }

    /** Untrusted value; it will be bound as a parameter. */
    record Expression(String code) implements SqlPart {
    }

    /** Compile-time constant, kept as code; its value is used to track SQL quotes. */
    record Constant(String code, String value) implements SqlPart {
    }

    /** Trusted SQL code of unknown content, such as the SQL variable being appended to; kept as code. */
    record Verbatim(String code) implements SqlPart {
    }

    static SqlPart literal(String value) {
        return new Literal(value);
    }

    static SqlPart expression(String code) {
        return new Expression(code);
    }

    static SqlPart constant(String code, String value) {
        return new Constant(code, value);
    }

    static SqlPart verbatim(String code) {
        return new Verbatim(code);
    }
}
