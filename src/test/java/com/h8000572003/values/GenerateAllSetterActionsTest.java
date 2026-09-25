package com.h8000572003.values;

public class GenerateAllSetterActionsTest extends JavaTestCase {

    private static final String CACHE = "Generate all setters cache name values";
    private static final String NO_CACHE = "Generate all setters no cache values";

    private static final String USER = """
            class Base {
                public void setId(long id) {}
            }
            class User extends Base {
                public void setName(String name) {}
                public void setAge(int age) {}
                public void setAddress(java.util.List<String> address) {}
                public void settle(int x) {}
                public static void setDefault(int x) {}
                public void setBoth(int a, int b) {}
            }
            """;

    public void testGeneratesSetterCallsAfterDeclaration() {
        launch(CACHE, USER + """
                class Test {
                    void run() {
                        User us<caret>er = new User();
                    }
                }
                """);

        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("User user = new User();\n        user.setName(\"NAME\");\n        user.setAge("));
        assertTrue(text, text.contains("user.setAddress(null);"));
        assertTrue(text, text.contains("user.setId("));
        assertFalse(text, text.contains("user.settle("));
        assertFalse(text, text.contains("user.setDefault("));
        assertFalse(text, text.contains("user.setBoth("));
    }

    public void testCacheReusesNumbersForSameProperty() {
        String first = generateAgeValue(CACHE);
        String second = generateAgeValue(CACHE);
        assertEquals(first, second);
    }

    public void testNoCacheUsesNewNumbers() {
        String first = generateAgeValue(NO_CACHE);
        String second = generateAgeValue(NO_CACHE);
        assertFalse(first + " vs " + second, first.equals(second));
    }

    private String generateAgeValue(String intention) {
        launch(intention, USER + """
                class Test {
                    void run() {
                        User us<caret>er = new User();
                    }
                }
                """);
        String text = myFixture.getEditor().getDocument().getText();
        int start = text.indexOf("user.setAge(") + "user.setAge(".length();
        return text.substring(start, text.indexOf(')', start));
    }

    public void testNotAvailableWithoutSetters() {
        assertNotAvailable(CACHE, """
                class Empty {
                    public int getName() { return 0; }
                }
                class Test {
                    void run() {
                        Empty em<caret>pty = new Empty();
                    }
                }
                """);
    }

    public void testNotAvailableOnField() {
        assertNotAvailable(CACHE, USER + """
                class Test {
                    User us<caret>er = new User();
                }
                """);
    }
}
