package com.h8000572003.values;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * Light Java fixture whose project JDK is the JDK running the tests, so {@code String} and other
 * JDK classes resolve like they do in a real project.
 */
public abstract class JavaTestCase extends LightJavaCodeInsightFixtureTestCase {

    private static final LightProjectDescriptor DESCRIPTOR = new DefaultLightProjectDescriptor(
            () -> JavaSdk.getInstance().createJdk("test-jdk", System.getProperty("java.home"), false));

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return DESCRIPTOR;
    }

    protected void launch(String intention, String before) {
        myFixture.configureByText("Test.java", before);
        myFixture.launchAction(myFixture.findSingleIntention(intention));
    }

    /** Compares code, ignoring blank lines and trailing whitespace left by the formatter. */
    protected void assertCode(String expected) {
        assertEquals(normalize(expected), normalize(myFixture.getEditor().getDocument().getText()));
    }

    private static String normalize(String text) {
        return text.lines().map(String::stripTrailing).filter(line -> !line.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    protected void assertNotAvailable(String intention, String text) {
        myFixture.configureByText("Test.java", text);
        assertNull(myFixture.getAvailableIntention(intention));
    }
}
