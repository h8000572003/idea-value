package com.h8000572003.values;

import com.h8000572003.values.sql.SqlPart;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds a SQL string concatenation in Java PSI and converts its operands to {@link SqlPart}s.
 */
final class SqlConcatenation {

    private SqlConcatenation() {
    }

    /** Innermost string concatenation around the element, or {@code null}. */
    static @Nullable PsiPolyadicExpression find(@NotNull PsiElement element) {
        PsiPolyadicExpression expression = PsiTreeUtil.getParentOfType(element, PsiPolyadicExpression.class, false);
        while (expression != null && !isStringConcatenation(expression)) {
            expression = PsiTreeUtil.getParentOfType(expression, PsiPolyadicExpression.class);
        }
        return expression;
    }

    /** Statement in a code block before which parameter statements are inserted, or {@code null}. */
    static @Nullable PsiStatement anchor(@NotNull PsiExpression expression) {
        PsiStatement statement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class, true,
                PsiLambdaExpression.class, PsiClass.class);
        return statement != null && statement.getParent() instanceof PsiCodeBlock ? statement : null;
    }

    static List<SqlPart> parts(@NotNull PsiPolyadicExpression expression) {
        PsiExpression[] operands = expression.getOperands();
        List<SqlPart> parts = new ArrayList<>();
        for (int i = 0; i < operands.length; i++) {
            PsiExpression operand = operands[i];
            if (operand instanceof PsiLiteralExpression literal && literal.getValue() instanceof String value) {
                parts.add(SqlPart.literal(value));
            } else if (i == 0 && isAssignedVariable(operand, expression)) {
                parts.add(SqlPart.verbatim(operand.getText()));
            } else {
                Object constant = constantValue(operand);
                parts.add(constant != null
                        ? SqlPart.constant(operand.getText(), String.valueOf(constant))
                        : SqlPart.expression(operand.getText()));
            }
        }
        return parts;
    }

    private static boolean isStringConcatenation(PsiPolyadicExpression expression) {
        if (expression.getOperationTokenType() != JavaTokenType.PLUS) {
            return false;
        }
        for (PsiExpression operand : expression.getOperands()) {
            if (operand instanceof PsiLiteralExpression literal && literal.getValue() instanceof String) {
                return true;
            }
        }
        return false;
    }

    /** {@code sql = sql + ...}: the variable that accumulates the SQL is trusted. */
    private static boolean isAssignedVariable(PsiExpression operand, PsiPolyadicExpression expression) {
        if (!(PsiUtil.skipParenthesizedExprUp(expression.getParent()) instanceof PsiAssignmentExpression assignment)
                || !(PsiUtil.skipParenthesizedExprDown(operand) instanceof PsiReferenceExpression reference)
                || !(PsiUtil.skipParenthesizedExprDown(assignment.getLExpression()) instanceof PsiReferenceExpression target)) {
            return false;
        }
        PsiElement variable = reference.resolve();
        return variable != null && variable.equals(target.resolve());
    }

    private static @Nullable Object constantValue(PsiExpression operand) {
        PsiConstantEvaluationHelper helper = JavaPsiFacade.getInstance(operand.getProject()).getConstantEvaluationHelper();
        Object value = helper.computeConstantExpression(operand);
        if (value == null
                && PsiUtil.skipParenthesizedExprDown(operand) instanceof PsiReferenceExpression reference
                && reference.resolve() instanceof PsiVariable variable
                && variable.hasModifierProperty(PsiModifier.FINAL)
                && variable.getInitializer() != null) {
            value = helper.computeConstantExpression(variable.getInitializer());
        }
        return value;
    }
}
