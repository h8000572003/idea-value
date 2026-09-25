package com.h8000572003.values;

public class GenerateAllByMethodActionTest extends JavaTestCase {

    private static final String TITLE = "Generate fields by get/is method name";

    public void testDeclaresFieldsForGetters() {
        launch(TITLE, """
                class User {
                    private int age;
                    <caret>
                    public String getName() { return null; }
                    public int getAge() { return age; }
                    public boolean isActive() { return true; }
                    public String getIsbn() { return null; }
                    public java.util.List<String> getTags() { return null; }
                    public static String getDefault() { return null; }
                    private String getSecret() { return null; }
                    public void getNothing() {}
                }
                """);

        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text, text.contains("""
                    private int age;
                    private String name;
                    private boolean active;
                    private String isbn;
                    private List<String> tags;
                """));
        assertTrue(text, text.contains("import java.util.List;"));
        assertEquals(text, 1, text.split("private int age;", -1).length - 1);
        assertFalse(text, text.contains("private String default;"));
        assertFalse(text, text.contains("private String secret;"));
        assertFalse(text, text.contains("nothing;"));
    }

    public void testNotAvailableInsideMethod() {
        assertNotAvailable(TITLE, """
                class User {
                    public String getName() {
                        <caret>
                        return null;
                    }
                }
                """);
    }
}
