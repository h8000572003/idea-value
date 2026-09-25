package com.h8000572003.values.doc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccumulatedContentTest {

    private static final Level IF_A = Level.block("chain-a", "a-then", "if (a)");
    private static final Level ELSE_IF_B = Level.block("chain-a", "b-then", "else if (b)");
    private static final Level ELSE_A = Level.block("chain-a", "a-else", "else");
    private static final Level IF_C = Level.block("chain-c", "c-then", "if (c)");
    private static final Level SWITCH = Level.block("switch", "switch", "switch (t)");
    private static final Level CASE_1 = Level.label("case-1", "case 1:");
    private static final Level CASE_2 = Level.label("case-2", "case 2:");

    private static Fragment fragment(String text, Level... path) {
        return new Fragment(text, List.of(path));
    }

    @Test
    void plainFragmentsAreLines() {
        assertEquals("SELECT *\nFROM t", AccumulatedContent.render(List.of(
                fragment("SELECT *"), fragment("FROM t"))));
    }

    @Test
    void ifBlockIsIndentedAndClosed() {
        assertEquals("A\nif (a) {\n    B\n    C\n}\nD", AccumulatedContent.render(List.of(
                fragment("A"), fragment("B", IF_A), fragment("C", IF_A), fragment("D"))));
    }

    @Test
    void ifElseChain() {
        assertEquals("if (a) {\n    X\n} else if (b) {\n    Y\n} else {\n    Z\n}", AccumulatedContent.render(List.of(
                fragment("X", IF_A), fragment("Y", ELSE_IF_B), fragment("Z", ELSE_A))));
    }

    @Test
    void separateIfStatementsAreNotChained() {
        assertEquals("if (a) {\n    X\n}\nif (c) {\n    Y\n}", AccumulatedContent.render(List.of(
                fragment("X", IF_A), fragment("Y", IF_C))));
    }

    @Test
    void nestedBlocks() {
        assertEquals("if (a) {\n    X\n    if (c) {\n        Y\n    }\n}", AccumulatedContent.render(List.of(
                fragment("X", IF_A), fragment("Y", IF_A, IF_C))));
    }

    @Test
    void switchCases() {
        assertEquals("switch (t) {\n    case 1:\n        X\n    case 2:\n        Y\n}", AccumulatedContent.render(List.of(
                fragment("X", SWITCH, CASE_1), fragment("Y", SWITCH, CASE_2))));
    }

    @Test
    void multiLineFragmentIsIndentedPerLine() {
        assertEquals("if (a) {\n    X\n    Y\n}", AccumulatedContent.render(List.of(fragment("X\nY", IF_A))));
    }

    @Test
    void htmlEscapesContent() {
        assertEquals("Content: <pre><b>a &lt; b &amp;&amp; c &gt; d</b></pre>",
                AccumulatedContent.toHtml("a < b && c > d"));
    }
}
