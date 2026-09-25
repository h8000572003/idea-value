package com.h8000572003.values.sql;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.h8000572003.values.sql.SqlPart.constant;
import static com.h8000572003.values.sql.SqlPart.expression;
import static com.h8000572003.values.sql.SqlPart.literal;
import static com.h8000572003.values.sql.SqlPart.verbatim;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlParameterizerTest {

    private static SqlRewrite named(SqlPart... parts) {
        return SqlParameterizer.rewrite(List.of(parts), BindStyle.NAMED);
    }

    private static SqlRewrite positional(SqlPart... parts) {
        return SqlParameterizer.rewrite(List.of(parts), BindStyle.POSITIONAL);
    }

    private static BindParameter param(String name, String value) {
        return new BindParameter(name, value);
    }

    @Nested
    class QuotedValues {

        @Test
        void quotedStringBecomesNamedParameter() {
            SqlRewrite rewrite = named(literal(" WHERE a = '"), expression("a"), literal("'"));

            assertEquals("\" WHERE a = :a\"", rewrite.expression());
            assertEquals(List.of(param("a", "a")), rewrite.parameters());
        }

        @Test
        void quotedStringBecomesPositionalParameter() {
            SqlRewrite rewrite = positional(literal(" WHERE a = '"), expression("a"), literal("'"));

            assertEquals("\" WHERE a = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "a")), rewrite.parameters());
        }

        @Test
        void literalAfterParameterKeepsItsOwnQuotedConstants() {
            SqlRewrite rewrite = positional(
                    literal(" WHERE a = '"), expression("a"), literal("' AND b = '2'"));

            assertEquals("\" WHERE a = ? AND b = '2'\"", rewrite.expression());
            assertEquals(List.of(param(null, "a")), rewrite.parameters());
        }

        @Test
        void literalBetweenTwoParametersClosesAndOpensQuotes() {
            SqlRewrite rewrite = positional(
                    literal("a = '"), expression("a"), literal("' AND b = '"), expression("b"), literal("'"));

            assertEquals("\"a = ? AND b = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "a"), param(null, "b")), rewrite.parameters());
        }

        @Test
        void escapedQuotesInSqlConstantsAreKept() {
            SqlRewrite rewrite = positional(
                    literal("a = 'it''s' AND b = '"), expression("b"), literal("'"));

            assertEquals("\"a = 'it''s' AND b = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "b")), rewrite.parameters());
        }

        @Test
        void likeWildcardsMoveIntoParameterValue() {
            SqlRewrite rewrite = positional(
                    literal(" WHERE name LIKE '%"), expression("name"), literal("%'"));

            assertEquals("\" WHERE name LIKE ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "\"%\" + name + \"%\"")), rewrite.parameters());
        }

        @Test
        void textInsideQuotesMovesIntoParameterValue() {
            SqlRewrite rewrite = positional(literal("a = 'X-"), expression("code"), literal("'"));

            assertEquals("\"a = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "\"X-\" + code")), rewrite.parameters());
        }

        @Test
        void escapedQuoteInsideParameterValueIsUnescaped() {
            SqlRewrite rewrite = positional(literal("a = 'it''s "), expression("x"), literal("'"));

            assertEquals(List.of(param(null, "\"it's \" + x")), rewrite.parameters());
        }

        @Test
        void severalExpressionsInOneQuoteBecomeOneParameter() {
            SqlRewrite rewrite = named(
                    literal("d = '"), expression("year"), literal("-"), expression("month"), literal("'"));

            assertEquals("\"d = :year\"", rewrite.expression());
            assertEquals(List.of(param("year", "year + \"-\" + month")), rewrite.parameters());
        }

        @Test
        void adjacentExpressionsKeepStringConcatenationSemantics() {
            SqlRewrite rewrite = positional(literal("a = '"), expression("x"), expression("y"), literal("'"));

            assertEquals(List.of(param(null, "\"\" + x + y")), rewrite.parameters());
        }

        @Test
        void quotedParameterSpanningLiteralsFromReportedBug() {
            SqlRewrite rewrite = named(
                    literal(" WHERE A.message_no='"), expression("msg_no"), literal("'"),
                    literal(" AND A.decl_type = "), literal(" '%"), expression("decl_type"),
                    literal("' and A.message_no ='2'"));

            assertEquals("\" WHERE A.message_no=:msg_no\" + \" AND A.decl_type = \" + \" :decl_type and A.message_no ='2'\"",
                    rewrite.expression());
            assertEquals(List.of(
                    param("msg_no", "msg_no"),
                    param("decl_type", "\"%\" + decl_type")), rewrite.parameters());
        }
    }

    @Nested
    class UnquotedValues {

        @Test
        void numericValueBecomesParameter() {
            SqlRewrite rewrite = positional(literal(" WHERE id = "), expression("id"));

            assertEquals("\" WHERE id = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "id")), rewrite.parameters());
        }

        @Test
        void literalAfterUnquotedParameterStaysSeparate() {
            SqlRewrite rewrite = positional(literal("id = "), expression("id"), literal(" AND x = 1"));

            assertEquals("\"id = ?\" + \" AND x = 1\"", rewrite.expression());
        }

        @Test
        void separateInListElementsAreBound() {
            SqlRewrite rewrite = positional(literal("id IN ("), expression("a"), literal(", "), expression("b"), literal(")"));

            assertEquals("\"id IN (?\" + \", ?\" + \")\"", rewrite.expression());
            assertEquals(List.of(param(null, "a"), param(null, "b")), rewrite.parameters());
        }

        @Test
        void methodCallsAreParameterizedToo() {
            SqlRewrite rewrite = named(literal("id = "), expression("dto.getUserId()"));

            assertEquals("\"id = :userId\"", rewrite.expression());
            assertEquals(List.of(param("userId", "dto.getUserId()")), rewrite.parameters());
        }
    }

    @Nested
    class ParameterNames {

        private String nameOf(String code) {
            return named(literal("a = "), expression(code)).parameters().get(0).name();
        }

        @Test
        void derivesNamesFromExpressions() {
            assertEquals("name", nameOf("name"));
            assertEquals("name", nameOf("this.name"));
            assertEquals("userId", nameOf("dto.getUserId()"));
            assertEquals("active", nameOf("dto.isActive()"));
            assertEquals("count", nameOf("count()"));
            assertEquals("get", nameOf("get()"));
            assertEquals("param", nameOf("arr[0]"));
            assertEquals("param", nameOf("a ? b : c"));
        }

        @Test
        void sameExpressionReusesNamedParameter() {
            SqlRewrite rewrite = named(literal("a = "), expression("id"), literal(" OR b = "), expression("id"));

            assertEquals("\"a = :id\" + \" OR b = :id\"", rewrite.expression());
            assertEquals(List.of(param("id", "id")), rewrite.parameters());
        }

        @Test
        void sameExpressionIsBoundTwiceForPositionalParameters() {
            SqlRewrite rewrite = positional(literal("a = "), expression("id"), literal(" OR b = "), expression("id"));

            assertEquals(List.of(param(null, "id"), param(null, "id")), rewrite.parameters());
        }

        @Test
        void differentExpressionsWithSameNameGetSuffix() {
            SqlRewrite rewrite = named(literal("a = "), expression("a.id"), literal(" AND b = "), expression("b.id"));

            assertEquals("\"a = :id\" + \" AND b = :id2\"", rewrite.expression());
            assertEquals(List.of(param("id", "a.id"), param("id2", "b.id")), rewrite.parameters());
        }
    }

    @Nested
    class TrustedCode {

        @Test
        void verbatimPrefixIsKeptAsCode() {
            SqlRewrite rewrite = positional(verbatim("sql"), literal(" AND a = '"), expression("a"), literal("'"));

            assertEquals("sql + \" AND a = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "a")), rewrite.parameters());
        }

        @Test
        void constantIsKeptAsCodeAndNotBound() {
            SqlRewrite rewrite = positional(
                    literal("SELECT * FROM "), constant("TABLE", "T_USER"), literal(" WHERE a = '"), expression("a"), literal("'"));

            assertEquals("\"SELECT * FROM \" + TABLE + \" WHERE a = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "a")), rewrite.parameters());
        }

        @Test
        void constantInsideQuotesJoinsParameterValue() {
            SqlRewrite rewrite = positional(literal("a = '"), constant("PREFIX", "X"), expression("a"), literal("'"));

            assertEquals("\"a = ?\"", rewrite.expression());
            assertEquals(List.of(param(null, "PREFIX + a")), rewrite.parameters());
        }

        @Test
        void quotedConstantValueIsTracked() {
            SqlRewrite rewrite = positional(
                    literal("a = "), constant("QUOTED", "'x'"), literal(" AND b = '"), expression("b"), literal("'"));

            assertEquals("\"a = \" + QUOTED + \" AND b = ?\"", rewrite.expression());
        }
    }

    @Nested
    class JavaEscaping {

        @Test
        void outputLiteralsAreEscaped() {
            SqlRewrite rewrite = positional(literal("SELECT \"COL\\1\"\nFROM t WHERE a = '"), expression("a"), literal("'"));

            assertEquals("\"SELECT \\\"COL\\\\1\\\"\\nFROM t WHERE a = ?\"", rewrite.expression());
        }

        @Test
        void parameterValueLiteralsAreEscaped() {
            SqlRewrite rewrite = positional(literal("a = '\""), expression("a"), literal("'"));

            assertEquals(List.of(param(null, "\"\\\"\" + a")), rewrite.parameters());
        }
    }

    @Nested
    class Unsupported {

        private void assertUnsupported(String reasonFragment, SqlPart... parts) {
            UnsupportedSqlException e = assertThrows(UnsupportedSqlException.class,
                    () -> SqlParameterizer.rewrite(List.of(parts), BindStyle.POSITIONAL));
            assertTrue(e.getMessage().contains(reasonFragment), e.getMessage());
        }

        @Test
        void nothingToParameterize() {
            assertUnsupported("no value", literal("SELECT 1"), constant("A", "x"));
        }

        @Test
        void orderByColumnCannotBeBound() {
            assertUnsupported("identifier", literal("SELECT * FROM t ORDER BY "), expression("column"));
        }

        @Test
        void tableNameCannotBeBound() {
            assertUnsupported("identifier", literal("SELECT * FROM "), expression("table"), literal(" WHERE 1 = 1"));
            assertUnsupported("identifier", literal("SELECT * FROM t JOIN "), expression("table"));
            assertUnsupported("identifier", literal("SELECT * FROM "), expression("schema"), literal(".t"));
            assertUnsupported("identifier", literal("SELECT t."), expression("column"), literal(" FROM t"));
        }

        @Test
        void inListCannotBeBoundToOneParameter() {
            assertUnsupported("IN", literal("WHERE id IN ("), expression("ids"), literal(")"));
        }

        @Test
        void expressionGluedToSqlToken() {
            assertUnsupported("SQL token", literal("WHERE col"), expression("suffix"), literal(" = 1"));
            assertUnsupported("SQL token", literal("WHERE col = "), expression("x"), literal("ABC"));
        }

        @Test
        void unbalancedQuote() {
            assertUnsupported("quote", literal("a = '"), expression("x"));
        }

        @Test
        void verbatimInsideQuotes() {
            assertUnsupported("quote", literal("a = '"), verbatim("sql"), expression("x"), literal("'"));
        }

        @Test
        void quoteOpenedInsideConstant() {
            assertUnsupported("quote", literal("a = "), constant("OPEN", "'"), expression("x"), literal("'"));
        }
    }
}
