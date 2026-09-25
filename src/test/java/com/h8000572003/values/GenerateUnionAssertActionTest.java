package com.h8000572003.values;

public class GenerateUnionAssertActionTest extends JavaTestCase {

    private static final String TITLE = "Generated assert based on parameter 1 as parameter 2";

    public void testAssertsEveryGetter() {
        launch(TITLE, """
                class User {
                    public String getName() { return null; }
                    public boolean isActive() { return true; }
                    public int[] getCodes() { return null; }
                    public void setName(String name) {}
                    public String getAddress(int index) { return null; }
                    public static User getDefault() { return null; }
                }
                class UserTest {
                    void check(User expected, User actual) {
                        <caret>
                    }
                }
                """);

        assertCode("""
                class User {
                    public String getName() { return null; }
                    public boolean isActive() { return true; }
                    public int[] getCodes() { return null; }
                    public void setName(String name) {}
                    public String getAddress(int index) { return null; }
                    public static User getDefault() { return null; }
                }
                class UserTest {
                    void check(User expected, User actual) {
                        assertEquals(expected.getName(), actual.getName());
                        assertEquals(expected.isActive(), actual.isActive());
                        assertArrayEquals(expected.getCodes(), actual.getCodes());
                    }
                }
                """);
    }

    public void testNotAvailableForPrimitiveParameter() {
        assertNotAvailable(TITLE, """
                class UserTest {
                    void check(int expected, int actual) {
                        <caret>
                    }
                }
                """);
    }
}
