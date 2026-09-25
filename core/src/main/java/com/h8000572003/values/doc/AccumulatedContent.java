package com.h8000572003.values.doc;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the fragments appended to a String/StringBuilder as pseudo code, keeping if/else/switch structure.
 */
public final class AccumulatedContent {

    private static final String INDENT = "    ";

    private AccumulatedContent() {
    }

    public static String render(List<Fragment> fragments) {
        List<String> lines = new ArrayList<>();
        List<Level> open = List.of();
        for (Fragment fragment : fragments) {
            open = transition(lines, open, fragment.path());
            for (String line : fragment.text().split("\n", -1)) {
                lines.add(INDENT.repeat(open.size()) + line);
            }
        }
        transition(lines, open, List.of());
        return String.join("\n", lines);
    }

    public static String toHtml(String content) {
        return "Content: <pre><b>" + escape(content) + "</b></pre>";
    }

    private static List<Level> transition(List<String> lines, List<Level> open, List<Level> target) {
        int common = 0;
        while (common < open.size() && common < target.size() && open.get(common).sameBranch(target.get(common))) {
            common++;
        }
        for (int i = open.size() - 1; i > common; i--) {
            close(lines, open, i);
        }
        boolean continueChain = common < open.size() && common < target.size()
                && open.get(common).braces() && target.get(common).braces()
                && open.get(common).chain().equals(target.get(common).chain());
        if (common < open.size() && !continueChain) {
            close(lines, open, common);
        }
        for (int i = common; i < target.size(); i++) {
            Level level = target.get(i);
            String indent = INDENT.repeat(i);
            if (i == common && continueChain) {
                lines.add(indent + "} " + level.header() + " {");
            } else {
                lines.add(indent + level.header() + (level.braces() ? " {" : ""));
            }
        }
        return target;
    }

    private static void close(List<String> lines, List<Level> open, int index) {
        if (open.get(index).braces()) {
            lines.add(INDENT.repeat(index) + "}");
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
