package com.h8000572003.values;

import com.h8000572003.values.doc.AccumulatedContent;
import com.h8000572003.values.doc.Fragment;
import com.h8000572003.values.doc.Level;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quick documentation for a String/StringBuilder/StringBuffer variable: the text appended to it,
 * with the surrounding if/else/switch/loop structure.
 */
public class StringBuilderDocumentationProvider extends AbstractDocumentationProvider {

    private static final int MAX_RESOLVE_DEPTH = 5;

    private enum Kind {STRING, BUILDER}

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (!(element instanceof PsiVariable variable)) {
            return null;
        }
        Kind kind = kindOf(variable.getType());
        PsiElement scope = scopeOf(variable);
        if (kind == null || scope == null) {
            return null;
        }
        List<Fragment> fragments = new ArrayList<>();
        PsiExpression initial = initialContent(variable, kind);
        if (initial != null) {
            fragments.add(new Fragment(evaluate(initial, 0), path(variable, scope)));
        }
        for (PsiReferenceExpression reference : PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression.class)) {
            if (reference.isReferenceTo(variable)) {
                String appended = kind == Kind.BUILDER ? appendedByChain(reference) : appendedByAssignment(reference, variable);
                if (appended != null) {
                    fragments.add(new Fragment(appended, path(reference, scope)));
                }
            }
        }
        return fragments.isEmpty() ? null : AccumulatedContent.toHtml(AccumulatedContent.render(fragments));
    }

    private static @Nullable Kind kindOf(PsiType type) {
        if (type == null) {
            return null;
        }
        return switch (type.getCanonicalText()) {
            case CommonClassNames.JAVA_LANG_STRING -> Kind.STRING;
            case CommonClassNames.JAVA_LANG_STRING_BUILDER, CommonClassNames.JAVA_LANG_STRING_BUFFER -> Kind.BUILDER;
            default -> null;
        };
    }

    private static @Nullable PsiElement scopeOf(PsiVariable variable) {
        if (variable instanceof PsiParameter parameter) {
            return parameter.getDeclarationScope();
        }
        return variable instanceof PsiLocalVariable ? PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class) : null;
    }

    /** {@code "a"} of {@code String s = "a"} or of {@code new StringBuilder("a")}, but not a capacity. */
    private static @Nullable PsiExpression initialContent(PsiVariable variable, Kind kind) {
        PsiExpression initializer = variable.getInitializer();
        if (kind == Kind.STRING || initializer == null) {
            return initializer;
        }
        if (initializer instanceof PsiNewExpression newExpression && newExpression.getArgumentList() != null) {
            PsiExpression[] arguments = newExpression.getArgumentList().getExpressions();
            if (arguments.length == 1 && !(arguments[0].getType() instanceof PsiPrimitiveType)) {
                return arguments[0];
            }
        }
        return null;
    }

    /** Arguments of {@code sb.append(a).append(b)} starting at {@code sb}, joined. */
    private static @Nullable String appendedByChain(PsiReferenceExpression reference) {
        StringBuilder text = new StringBuilder();
        boolean appended = false;
        PsiElement current = reference;
        while (current.getParent() instanceof PsiReferenceExpression method
                && method.getQualifierExpression() == current
                && method.getParent() instanceof PsiMethodCallExpression call) {
            if (!"append".equals(method.getReferenceName())) {
                break;
            }
            PsiExpression[] arguments = call.getArgumentList().getExpressions();
            if (arguments.length > 0) {
                text.append(evaluate(arguments[0], 0));
                appended = true;
            }
            current = call;
        }
        return appended ? text.toString() : null;
    }

    /** Right side of {@code s += x} or {@code s = s + x}. */
    private static @Nullable String appendedByAssignment(PsiReferenceExpression reference, PsiVariable variable) {
        if (!(reference.getParent() instanceof PsiAssignmentExpression assignment)
                || assignment.getLExpression() != reference || assignment.getRExpression() == null) {
            return null;
        }
        PsiExpression right = assignment.getRExpression();
        if (assignment.getOperationTokenType() == JavaTokenType.PLUSEQ) {
            return evaluate(right, 0);
        }
        if (assignment.getOperationTokenType() == JavaTokenType.EQ
                && PsiUtil.skipParenthesizedExprDown(right) instanceof PsiPolyadicExpression concatenation
                && concatenation.getOperationTokenType() == JavaTokenType.PLUS
                && concatenation.getOperands()[0] instanceof PsiReferenceExpression first
                && first.isReferenceTo(variable)) {
            StringBuilder text = new StringBuilder();
            PsiExpression[] operands = concatenation.getOperands();
            for (int i = 1; i < operands.length; i++) {
                text.append(evaluate(operands[i], 0));
            }
            return text.toString();
        }
        return null;
    }

    /** Text an expression contributes; values that cannot be known are shown as {@code ${expression}}. */
    private static String evaluate(PsiExpression expression, int depth) {
        expression = PsiUtil.skipParenthesizedExprDown(expression);
        if (expression == null) {
            return "";
        }
        Object constant = JavaPsiFacade.getInstance(expression.getProject())
                .getConstantEvaluationHelper().computeConstantExpression(expression);
        if (constant != null) {
            return String.valueOf(constant);
        }
        if (expression instanceof PsiPolyadicExpression concatenation
                && concatenation.getOperationTokenType() == JavaTokenType.PLUS
                && kindOf(concatenation.getType()) == Kind.STRING) {
            StringBuilder text = new StringBuilder();
            for (PsiExpression operand : concatenation.getOperands()) {
                text.append(evaluate(operand, depth));
            }
            return text.toString();
        }
        if (expression instanceof PsiReferenceExpression reference && reference.resolve() instanceof PsiLocalVariable local
                && kindOf(local.getType()) == Kind.STRING && depth < MAX_RESOLVE_DEPTH) {
            String value = localValue(local, reference, depth + 1);
            if (value != null) {
                return value;
            }
        }
        return "${" + expression.getText() + "}";
    }

    /** Value of a local String before {@code usage}: its initializer and the assignments before it. */
    private static @Nullable String localValue(PsiLocalVariable local, PsiElement usage, int depth) {
        PsiElement scope = PsiTreeUtil.getParentOfType(local, PsiCodeBlock.class);
        if (scope == null) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        boolean known = false;
        if (local.getInitializer() != null) {
            value.append(evaluate(local.getInitializer(), depth));
            known = true;
        }
        int usageOffset = usage.getTextRange().getStartOffset();
        for (PsiAssignmentExpression assignment : PsiTreeUtil.findChildrenOfType(scope, PsiAssignmentExpression.class)) {
            if (assignment.getTextRange().getEndOffset() > usageOffset
                    || !(assignment.getLExpression() instanceof PsiReferenceExpression target)
                    || !target.isReferenceTo(local) || assignment.getRExpression() == null) {
                continue;
            }
            String right = evaluate(assignment.getRExpression(), depth);
            if (assignment.getOperationTokenType() == JavaTokenType.EQ) {
                value.setLength(0);
                value.append(right);
                known = true;
            } else if (assignment.getOperationTokenType() == JavaTokenType.PLUSEQ) {
                value.append(right);
            }
        }
        return known ? value.toString() : null;
    }

    /** Control structures between the scope and the element, outermost first. */
    private static List<Level> path(PsiElement element, PsiElement scope) {
        List<Level> levels = new ArrayList<>();
        PsiElement current = element;
        while (current != null && current != scope) {
            PsiElement parent = current.getParent();
            if (parent instanceof PsiIfStatement statement) {
                if (current == statement.getThenBranch()) {
                    String keyword = isElseIf(statement) ? "else if (" : "if (";
                    levels.add(Level.block(chainRoot(statement), List.of(statement, "then"),
                            keyword + text(statement.getCondition()) + ")"));
                } else if (current == statement.getElseBranch() && !(current instanceof PsiIfStatement)) {
                    levels.add(Level.block(chainRoot(statement), List.of(statement, "else"), "else"));
                }
            } else if (parent instanceof PsiSwitchLabeledRuleStatement rule && current == rule.getBody()) {
                String header = rule.getText().substring(0, current.getStartOffsetInParent()).trim();
                levels.add(Level.label(rule, header));
            } else if (parent instanceof PsiCodeBlock block && block.getParent() instanceof PsiSwitchBlock switchBlock) {
                if (!(current instanceof PsiSwitchLabeledRuleStatement)) {
                    PsiSwitchLabelStatement label = PsiTreeUtil.getPrevSiblingOfType(current, PsiSwitchLabelStatement.class);
                    if (label != null) {
                        levels.add(Level.label(label, label.getText().trim()));
                    }
                }
                levels.add(Level.block(switchBlock, switchBlock, "switch (" + text(switchBlock.getExpression()) + ")"));
            } else if (parent instanceof PsiLoopStatement loop && !(loop instanceof PsiDoWhileStatement)
                    && current == loop.getBody()) {
                String header = loop.getText().substring(0, current.getStartOffsetInParent());
                levels.add(Level.block(loop, loop, header.replaceAll("\\s+", " ").trim()));
            }
            current = parent;
        }
        Collections.reverse(levels);
        return levels;
    }

    private static boolean isElseIf(PsiIfStatement statement) {
        return statement.getParent() instanceof PsiIfStatement parent && parent.getElseBranch() == statement;
    }

    private static PsiIfStatement chainRoot(PsiIfStatement statement) {
        while (isElseIf(statement)) {
            statement = (PsiIfStatement) statement.getParent();
        }
        return statement;
    }

    private static String text(@Nullable PsiElement element) {
        return element == null ? "" : element.getText().replaceAll("\\s+", " ");
    }
}
