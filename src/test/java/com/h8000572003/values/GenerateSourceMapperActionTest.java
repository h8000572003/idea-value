package com.h8000572003.values;

public class GenerateSourceMapperActionTest extends JavaTestCase {

    private static final String TITLE = "Generated set/get based on parameter 1 as parameter 2";

    public void testCopiesMatchingProperties() {
        launch(TITLE, """
                class Target {
                    public void setName(String name) {}
                    public void setActive(boolean active) {}
                    public void setOnlyTarget(int x) {}
                }
                class Source {
                    public String getName() { return null; }
                    public boolean isActive() { return true; }
                    public int getOnlySource() { return 0; }
                }
                class Mapper {
                    void map(Target target, Source source) {
                        <caret>
                    }
                }
                """);

        assertCode("""
                class Target {
                    public void setName(String name) {}
                    public void setActive(boolean active) {}
                    public void setOnlyTarget(int x) {}
                }
                class Source {
                    public String getName() { return null; }
                    public boolean isActive() { return true; }
                    public int getOnlySource() { return 0; }
                }
                class Mapper {
                    void map(Target target, Source source) {
                        target.setName(source.getName());
                        target.setActive(source.isActive());
                    }
                }
                """);
    }

    public void testNotAvailableWithOneParameter() {
        assertNotAvailable(TITLE, """
                class Target {
                    public void setName(String name) {}
                }
                class Mapper {
                    void map(Target target) {
                        <caret>
                    }
                }
                """);
    }
}
