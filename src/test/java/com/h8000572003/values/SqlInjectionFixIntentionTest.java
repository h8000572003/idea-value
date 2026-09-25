package com.h8000572003.values;

import com.h8000572003.values.configurable.MyPluginSettings;
import com.h8000572003.values.configurable.ParameterType;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class SqlInjectionFixIntentionTest extends BasePlatformTestCase {

    private static final String DECLARE = GenerateMapFromStringWithDeclareAction.TITLE;
    private static final String NO_DECLARE = GenerateMapFromStringWithoutDeclareAction.TITLE;

    private ParameterType savedType;
    private String savedName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MyPluginSettings.State state = MyPluginSettings.getInstance().getState();
        savedType = state.getFeatureEnabled();
        savedName = state.getParameterName();
        state.setFeatureEnabled(ParameterType.USE_NAMED);
        state.setParameterName("params");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            MyPluginSettings.State state = MyPluginSettings.getInstance().getState();
            state.setFeatureEnabled(savedType);
            state.setParameterName(savedName);
        } finally {
            super.tearDown();
        }
    }

    private String launch(String title, String body) {
        myFixture.configureByText("Dao.java", "class Dao {\n" + body + "\n}\n");
        myFixture.launchAction(myFixture.findSingleIntention(title));
        return myFixture.getEditor().getDocument().getText();
    }

    private static void assertInOrder(String text, String... fragments) {
        int from = 0;
        for (String fragment : fragments) {
            int index = text.indexOf(fragment, from);
            assertTrue("missing in order: " + fragment + "\n" + text, index >= 0);
            from = index + fragment.length();
        }
    }

    public void testNamedParametersWithDeclaration() {
        String text = launch(DECLARE, """
                    void find(String name, int id) {
                        String sql = "SELECT * FROM T WHERE NAME = '" + na<caret>me + "' AND ID = " + id;
                    }
                """);

        assertInOrder(text,
                "params = new ",
                "params.put(\"name\", name);",
                "params.put(\"id\", id);",
                "String sql = \"SELECT * FROM T WHERE NAME = :name AND ID = :id\";");
    }

    public void testPositionalParametersWithoutDeclaration() {
        MyPluginSettings.getInstance().getState().setFeatureEnabled(ParameterType.USE_ORDER);

        String text = launch(NO_DECLARE, """
                    void find(String name) {
                        String sql = "SELECT * FROM T WHERE NAME LIKE '%" + na<caret>me + "%'";
                    }
                """);

        assertFalse(text, text.contains("new "));
        assertInOrder(text,
                "params.add(\"%\" + name + \"%\");",
                "String sql = \"SELECT * FROM T WHERE NAME LIKE ?\";");
    }

    public void testMethodCallIsBound() {
        String text = launch(NO_DECLARE, """
                    void find(Dto dto) {
                        String sql = "SELECT * FROM T WHERE ID = '" + dto.get<caret>Id() + "'";
                    }
                """);

        assertInOrder(text,
                "params.put(\"id\", dto.getId());",
                "String sql = \"SELECT * FROM T WHERE ID = :id\";");
    }

    public void testAccumulatedSqlVariableIsKept() {
        String text = launch(NO_DECLARE, """
                    void find(String sql, String name) {
                        sql = sql + " AND NAME = '" + na<caret>me + "'";
                    }
                """);

        assertInOrder(text,
                "params.put(\"name\", name);",
                "sql = sql + \" AND NAME = :name\";");
        assertFalse(text, text.contains("params.put(\"sql\""));
    }

    public void testCompoundAssignment() {
        String text = launch(NO_DECLARE, """
                    void find(String sql, String name) {
                        sql += " AND NAME = '" + na<caret>me + "'";
                    }
                """);

        assertInOrder(text,
                "params.put(\"name\", name);",
                "sql += \" AND NAME = :name\";");
    }

    public void testConstantIsNotBound() {
        String text = launch(NO_DECLARE, """
                    static final String TABLE = "T_USER";
                    void find(String name) {
                        String sql = "SELECT * FROM " + TABLE + " WHERE NAME = '" + na<caret>me + "'";
                    }
                """);

        assertInOrder(text,
                "params.put(\"name\", name);",
                "String sql = \"SELECT * FROM \" + TABLE + \" WHERE NAME = :name\";");
        assertFalse(text, text.contains("params.put(\"TABLE\""));
    }

    public void testStatementInsideNestedBlock() {
        String text = launch(NO_DECLARE, """
                    void find(boolean b, String name) {
                        if (b) {
                            run("SELECT * FROM T WHERE NAME = '" + na<caret>me + "'");
                        }
                    }
                    void run(String sql) {}
                """);

        assertInOrder(text,
                "if (b) {",
                "params.put(\"name\", name);",
                "run(\"SELECT * FROM T WHERE NAME = :name\");");
    }

    public void testUnsupportedSqlShowsError() {
        myFixture.configureByText("Dao.java", """
                class Dao {
                    void find(String column) {
                        String sql = "SELECT * FROM T ORDER BY " + col<caret>umn;
                    }
                }
                """);

        try {
            myFixture.launchAction(myFixture.findSingleIntention(NO_DECLARE));
            fail("expected an error hint");
        } catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("identifier"));
        }
        assertTrue(myFixture.getEditor().getDocument().getText().contains("ORDER BY \" + column"));
    }

    public void testNotAvailableForNumericAddition() {
        myFixture.configureByText("Dao.java", """
                class Dao {
                    int sum(int a, int b) {
                        return a <caret>+ b;
                    }
                }
                """);

        assertNull(myFixture.getAvailableIntention(NO_DECLARE));
        assertNull(myFixture.getAvailableIntention(DECLARE));
    }

    public void testNotAvailableForFieldInitializer() {
        myFixture.configureByText("Dao.java", """
                class Dao {
                    String name;
                    String sql = "SELECT * FROM T WHERE NAME = '" + na<caret>me + "'";
                }
                """);

        assertNull(myFixture.getAvailableIntention(NO_DECLARE));
    }
}
