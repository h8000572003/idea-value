package com.h8000572003.values.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns SQL built by string concatenation into a parameterized query.
 * <p>
 * The SQL text is scanned for quotes across all operands. A quoted SQL string that contains
 * untrusted expressions becomes one bind parameter whose value is the concatenated content of the
 * quotes (so {@code '%" + x + "%'} binds {@code "%" + x + "%"}). An expression outside quotes is
 * bound directly. Positions that cannot be bound (identifiers, IN lists, partial tokens) are rejected
 * with {@link UnsupportedSqlException} instead of producing broken or still-injectable SQL.
 */
public final class SqlParameterizer {

    private static final Pattern IDENTIFIER_BEFORE = Pattern.compile(
            "(?i)(?:\\b(?:ORDER\\s+BY|GROUP\\s+BY|FROM|JOIN|INTO|UPDATE|TABLE)|\\.)\\s*$");
    private static final Pattern IDENTIFIER_AFTER = Pattern.compile("^\\s*\\.");
    private static final Pattern IN_LIST_BEFORE = Pattern.compile("(?i)\\bIN\\s*\\(\\s*$");
    private static final Pattern IN_LIST_AFTER = Pattern.compile("^\\s*\\)");
    private static final Pattern NAME_SOURCE = Pattern.compile(
            "^(?:[A-Za-z_$][\\w$]*\\s*(?:\\(\\s*\\))?\\s*\\.\\s*)*([A-Za-z_$][\\w$]*)\\s*(\\(\\s*\\))?$");

    private enum Kind {CHAR, HOLE, VERBATIM}

    private record Atom(Kind kind, int part, char ch) {
    }

    private static final class Group {
        final int start;
        int end = -1;
        boolean hasHole;

        Group(int start) {
            this.start = start;
        }
    }

    private final List<SqlPart> parts;
    private final BindStyle style;
    private final List<Atom> atoms = new ArrayList<>();
    private final int[] firstAtomOfPart;
    private final int[] groupAtPartStart;
    private final List<Group> groups = new ArrayList<>();
    private int[] holeGroupOfAtom;

    private final List<String> tokens = new ArrayList<>();
    private StringBuilder segment;
    private final List<BindParameter> parameters = new ArrayList<>();
    private final Map<String, String> nameByValue = new HashMap<>();
    private final Map<String, String> valueByName = new HashMap<>();

    private SqlParameterizer(List<SqlPart> parts, BindStyle style) {
        this.parts = parts;
        this.style = style;
        this.firstAtomOfPart = new int[parts.size()];
        this.groupAtPartStart = new int[parts.size()];
    }

    public static SqlRewrite rewrite(List<SqlPart> parts, BindStyle style) {
        if (parts.stream().noneMatch(SqlPart.Expression.class::isInstance)) {
            throw new UnsupportedSqlException("Found no value to parameterize");
        }
        return new SqlParameterizer(parts, style).rewrite();
    }

    private SqlRewrite rewrite() {
        toAtoms();
        findQuoteGroups();
        emit();
        return new SqlRewrite(String.join(" + ", tokens), List.copyOf(parameters));
    }

    private void toAtoms() {
        for (int p = 0; p < parts.size(); p++) {
            firstAtomOfPart[p] = atoms.size();
            SqlPart part = parts.get(p);
            if (part instanceof SqlPart.Literal literal) {
                addChars(p, literal.value());
            } else if (part instanceof SqlPart.Constant constant) {
                addChars(p, constant.value());
            } else if (part instanceof SqlPart.Expression) {
                atoms.add(new Atom(Kind.HOLE, p, '\0'));
            } else {
                atoms.add(new Atom(Kind.VERBATIM, p, '\0'));
            }
        }
    }

    private void addChars(int part, String value) {
        for (char c : value.toCharArray()) {
            atoms.add(new Atom(Kind.CHAR, part, c));
        }
    }

    private void findQuoteGroups() {
        Arrays.fill(groupAtPartStart, -1);
        Group open = null;
        int nextPart = 0;
        for (int i = 0; i < atoms.size(); i++) {
            while (nextPart < parts.size() && firstAtomOfPart[nextPart] <= i) {
                groupAtPartStart[nextPart++] = open == null ? -1 : groups.size() - 1;
            }
            Atom atom = atoms.get(i);
            if (atom.kind() == Kind.VERBATIM && open != null) {
                throw new UnsupportedSqlException("Trusted SQL code cannot appear inside a quoted value");
            } else if (atom.kind() == Kind.HOLE && open != null) {
                open.hasHole = true;
            } else if (isQuote(i)) {
                if (open == null) {
                    open = new Group(i);
                    groups.add(open);
                } else if (isQuote(i + 1)) {
                    i++;
                } else {
                    open.end = i;
                    open = null;
                }
            }
        }
        while (nextPart < parts.size()) {
            groupAtPartStart[nextPart++] = open == null ? -1 : groups.size() - 1;
        }
        if (open != null) {
            throw new UnsupportedSqlException("Unbalanced quote in SQL");
        }

        holeGroupOfAtom = new int[atoms.size()];
        Arrays.fill(holeGroupOfAtom, -1);
        for (int g = 0; g < groups.size(); g++) {
            Group group = groups.get(g);
            if (!group.hasHole) {
                continue;
            }
            if (!(parts.get(atoms.get(group.start).part()) instanceof SqlPart.Literal)
                    || !(parts.get(atoms.get(group.end).part()) instanceof SqlPart.Literal)) {
                throw new UnsupportedSqlException("SQL quote starts or ends inside a constant");
            }
            Arrays.fill(holeGroupOfAtom, group.start, group.end + 1, g);
        }
    }

    private boolean isQuote(int i) {
        return i < atoms.size() && atoms.get(i).kind() == Kind.CHAR && atoms.get(i).ch() == '\'';
    }

    private boolean inHoleGroup(int part) {
        int g = groupAtPartStart[part];
        return g >= 0 && groups.get(g).hasHole;
    }

    private void emit() {
        for (int p = 0; p < parts.size(); p++) {
            SqlPart part = parts.get(p);
            if (part instanceof SqlPart.Literal) {
                if (!inHoleGroup(p)) {
                    flushSegment();
                }
                emitChars(p);
            } else if (inHoleGroup(p)) {
                // Constants and expressions inside quotes are part of the quoted parameter's value.
                continue;
            } else if (part instanceof SqlPart.Expression expression) {
                checkBindable(firstAtomOfPart[p], expression.code());
                appendToSegment(placeholder(expression.code(), expression.code()));
            } else if (part instanceof SqlPart.Constant constant) {
                flushSegment();
                tokens.add(constant.code());
            } else if (part instanceof SqlPart.Verbatim verbatim) {
                flushSegment();
                tokens.add(verbatim.code());
            }
        }
        flushSegment();
    }

    private void emitChars(int part) {
        int end = part + 1 < parts.size() ? firstAtomOfPart[part + 1] : atoms.size();
        for (int i = firstAtomOfPart[part]; i < end; i++) {
            int g = holeGroupOfAtom[i];
            if (g < 0) {
                appendToSegment(String.valueOf(atoms.get(i).ch()));
            } else if (groups.get(g).start == i) {
                emitQuotedParameter(groups.get(g));
            }
        }
    }

    private void emitQuotedParameter(Group group) {
        List<String> pieces = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        String nameSource = null;
        boolean firstTwoUntyped = true;
        for (int i = group.start + 1; i < group.end; i++) {
            Atom atom = atoms.get(i);
            SqlPart part = parts.get(atom.part());
            if (part instanceof SqlPart.Literal) {
                text.append(atom.ch());
                if (atom.ch() == '\'') {
                    i++;
                }
                continue;
            }
            if (atom.kind() == Kind.CHAR && firstAtomOfPart[atom.part()] != i) {
                continue;
            }
            if (!text.isEmpty()) {
                pieces.add(JavaStrings.literal(text.toString()));
                text.setLength(0);
            }
            String code;
            if (part instanceof SqlPart.Expression expression) {
                code = expression.code();
                if (nameSource == null) {
                    nameSource = code;
                }
            } else {
                code = ((SqlPart.Constant) part).code();
            }
            if (pieces.size() < 2 && !(part instanceof SqlPart.Expression)) {
                firstTwoUntyped = false;
            }
            pieces.add(code);
        }
        if (!text.isEmpty()) {
            pieces.add(JavaStrings.literal(text.toString()));
        }
        boolean needsStringStart = pieces.size() >= 2 && firstTwoUntyped
                && !pieces.get(0).startsWith("\"") && !pieces.get(1).startsWith("\"");
        String value = (needsStringStart ? "\"\" + " : "") + String.join(" + ", pieces);
        appendToSegment(placeholder(nameSource, value));
    }

    private void checkBindable(int hole, String code) {
        String before = textAround(hole, -1);
        String after = textAround(hole, +1);
        if (IDENTIFIER_BEFORE.matcher(before).find() || IDENTIFIER_AFTER.matcher(after).find()) {
            throw new UnsupportedSqlException(
                    "A table or column identifier cannot be a bind parameter; validate it against a whitelist instead: " + code);
        }
        if (IN_LIST_BEFORE.matcher(before).find() && IN_LIST_AFTER.matcher(after).find()) {
            throw new UnsupportedSqlException(
                    "A value list inside IN (...) cannot be bound as one parameter; use one placeholder per element: " + code);
        }
        if (isGlued(hole - 1) || isGlued(hole + 1)) {
            throw new UnsupportedSqlException("Expression is glued to an SQL token and cannot be bound: " + code);
        }
    }

    private boolean isGlued(int i) {
        if (i < 0 || i >= atoms.size()) {
            return false;
        }
        Atom atom = atoms.get(i);
        return atom.kind() == Kind.HOLE
                || atom.kind() == Kind.CHAR && (Character.isLetterOrDigit(atom.ch()) || atom.ch() == '_');
    }

    /** SQL text next to an atom, up to the nearest non-text atom. */
    private String textAround(int from, int direction) {
        StringBuilder text = new StringBuilder();
        for (int i = from + direction; i >= 0 && i < atoms.size() && atoms.get(i).kind() == Kind.CHAR; i += direction) {
            text.append(atoms.get(i).ch());
        }
        return direction < 0 ? text.reverse().toString() : text.toString();
    }

    private String placeholder(String nameSource, String value) {
        if (style == BindStyle.POSITIONAL) {
            parameters.add(new BindParameter(null, value));
            return "?";
        }
        String name = nameByValue.get(value);
        if (name == null) {
            name = uniqueName(nameFor(nameSource));
            nameByValue.put(value, name);
            valueByName.put(name, value);
            parameters.add(new BindParameter(name, value));
        }
        return ":" + name;
    }

    private String uniqueName(String base) {
        String name = base;
        for (int n = 2; valueByName.containsKey(name); n++) {
            name = base + n;
        }
        return name;
    }

    static String nameFor(String code) {
        Matcher matcher = code == null ? null : NAME_SOURCE.matcher(code.strip());
        if (matcher == null || !matcher.matches()) {
            return "param";
        }
        String name = matcher.group(1);
        if (matcher.group(2) != null) {
            name = stripAccessorPrefix(name);
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String stripAccessorPrefix(String name) {
        for (String prefix : new String[]{"get", "is"}) {
            if (name.length() > prefix.length() && name.startsWith(prefix)
                    && Character.isUpperCase(name.charAt(prefix.length()))) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    private void appendToSegment(String text) {
        if (segment == null) {
            segment = new StringBuilder();
        }
        segment.append(text);
    }

    private void flushSegment() {
        if (segment != null && !segment.isEmpty()) {
            tokens.add(JavaStrings.literal(segment.toString()));
        }
        segment = null;
    }
}
