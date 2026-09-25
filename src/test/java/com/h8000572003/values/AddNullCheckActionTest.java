package com.h8000572003.values;

public class AddNullCheckActionTest extends JavaTestCase {

    private static final String TITLE = "Add 'value != null' check";

    public void testAddsCheckForMethodCallQualifier() {
        launch(TITLE, """
                class Test {
                    void run(String value) {
                        if (val<caret>ue.isEmpty()) {
                        }
                    }
                }
                """);

        assertCode("""
                class Test {
                    void run(String value) {
                        if (value != null && value.isEmpty()) {
                        }
                    }
                }
                """);
    }

    public void testQualifierInsideLargerCondition() {
        launch(TITLE, """
                class Test {
                    void run(String value, boolean b) {
                        if (b || val<caret>ue.isEmpty()) {
                        }
                    }
                }
                """);

        assertCode("""
                class Test {
                    void run(String value, boolean b) {
                        if (value != null && (b || value.isEmpty())) {
                        }
                    }
                }
                """);
    }

    public void testNotAvailableOutsideQualifier() {
        assertNotAvailable(TITLE, """
                class Test {
                    void run(String value) {
                        if (value.isEmpty()) {
                            Sys<caret>tem.out.println();
                        }
                    }
                }
                """);
    }

    public void testNotAvailableForPrimitive() {
        assertNotAvailable(TITLE, """
                class Test {
                    void run(int value) {
                        if (val<caret>ue > 1) {
                        }
                    }
                }
                """);
    }

    public void testNotAvailableWhenAlreadyChecked() {
        assertNotAvailable(TITLE, """
                class Test {
                    void run(String value) {
                        if (value != null && val<caret>ue.isEmpty()) {
                        }
                    }
                }
                """);
    }
}
