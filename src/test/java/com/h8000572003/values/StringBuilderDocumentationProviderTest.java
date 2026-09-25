package com.h8000572003.values;

import com.intellij.psi.PsiElement;

public class StringBuilderDocumentationProviderTest extends JavaTestCase {

    private String doc(String methodBody) {
        myFixture.configureByText("Test.java", "class Test {\n" + methodBody + "\n}\n");
        PsiElement variable = myFixture.getElementAtCaret();
        return new StringBuilderDocumentationProvider().generateDoc(variable, null);
    }

    private static String html(String content) {
        return "Content: <pre><b>" + content + "</b></pre>";
    }

    public void testStringBuilderAppends() {
        assertEquals(html("SELECT *\n FROM t WHERE a = ${a}"), doc("""
                    void run(String a) {
                        StringBuilder sql = new StringBuilder("SELECT *");
                        sql.append(" FROM t").append(" WHERE a = ").append(a);
                        s<caret>ql.toString();
                    }
                """));
    }

    public void testCapacityConstructorIsNotContent() {
        assertEquals(html("A"), doc("""
                    void run() {
                        StringBuilder sb = new StringBuilder(100);
                        sb.append("A");
                        s<caret>b.toString();
                    }
                """));
    }

    public void testIfElseStructure() {
        assertEquals(html("""
                SELECT
                if (a) {
                    X
                } else if (b) {
                    Y
                } else {
                    Z
                }"""), doc("""
                    void run(boolean a, boolean b) {
                        StringBuilder sb = new StringBuilder("SELECT");
                        if (a) {
                            sb.append("X");
                        } else if (b) {
                            sb.append("Y");
                        } else {
                            sb.append("Z");
                        }
                        s<caret>b.toString();
                    }
                """));
    }

    public void testSwitchStructure() {
        assertEquals(html("""
                switch (t) {
                    case 1:
                        A
                    default:
                        B
                }"""), doc("""
                    void run(int t) {
                        StringBuilder sb = new StringBuilder();
                        switch (t) {
                            case 1:
                                sb.append("A");
                                break;
                            default:
                                sb.append("B");
                        }
                        s<caret>b.toString();
                    }
                """));
    }

    public void testStringConcatenationAssignments() {
        assertEquals(html("SELECT\n FROM t\n WHERE x = ${x}"), doc("""
                    void run(String x) {
                        String sql = "SELECT";
                        sql += " FROM t";
                        sql = sql + " WHERE x = " + x;
                        s<caret>ql.length();
                    }
                """));
    }

    public void testConstantsAndLocalValuesAreResolved() {
        assertEquals(html("SELECT * FROM T_USER\n WHERE id = ${dto.getId()}"), doc("""
                    static final String TABLE = "T_USER";
                    void run(Dto dto) {
                        String from = " FROM " + TABLE;
                        StringBuilder sb = new StringBuilder("SELECT *" + from);
                        sb.append(" WHERE id = " + dto.getId());
                        s<caret>b.toString();
                    }
                    interface Dto { int getId(); }
                """));
    }

    public void testContentIsHtmlEscaped() {
        assertEquals(html("a &lt; b"), doc("""
                    void run() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("a < b");
                        s<caret>b.toString();
                    }
                """));
    }

    public void testNoDocForOtherTypes() {
        assertNull(doc("""
                    void run() {
                        int count = 1;
                        cou<caret>nt++;
                    }
                """));
    }
}
