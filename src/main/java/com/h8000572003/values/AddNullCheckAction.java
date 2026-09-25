package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * On {@code value} in {@code if (value.isEmpty())}, prepends {@code value != null &&} to the condition.
 */
public class AddNullCheckAction extends PsiElementBaseIntentionAction {

    public static final String TEXT = "Add 'value != null' check";

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return qualifierAt(element) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiReferenceExpression qualifier = qualifierAt(element);
        PsiExpression condition = qualifier == null ? null : conditionOf(qualifier);
        if (condition == null) {
            return;
        }
        String conditionText = needsParentheses(condition) ? "(" + condition.getText() + ")" : condition.getText();
        PsiExpression checked = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(qualifier.getText() + " != null && " + conditionText, condition);
        condition.replace(checked);
    }

    /** A variable used as qualifier of a call or field access inside an if condition, not yet null-checked. */
    private static @Nullable PsiReferenceExpression qualifierAt(PsiElement element) {
        if (!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiReferenceExpression reference)
                || !(reference.getParent() instanceof PsiReferenceExpression outer)
                || outer.getQualifierExpression() != reference
                || !(reference.resolve() instanceof PsiVariable)
                || reference.getType() == null || reference.getType() instanceof PsiPrimitiveType) {
            return null;
        }
        PsiExpression condition = conditionOf(reference);
        return condition != null && !isNullChecked(condition, reference) ? reference : null;
    }

    private static @Nullable PsiExpression conditionOf(PsiReferenceExpression reference) {
        PsiIfStatement statement = PsiTreeUtil.getParentOfType(reference, PsiIfStatement.class);
        PsiExpression condition = statement == null ? null : statement.getCondition();
        return condition != null && PsiTreeUtil.isAncestor(condition, reference, true) ? condition : null;
    }

    private static boolean isNullChecked(PsiExpression condition, PsiReferenceExpression reference) {
        for (PsiBinaryExpression binary : SyntaxTraverser.psiTraverser(condition).filter(PsiBinaryExpression.class)) {
            if (binary.getOperationTokenType() == JavaTokenType.NE && binary.getROperand() != null
                    && (isNullComparison(binary.getLOperand(), binary.getROperand(), reference)
                    || isNullComparison(binary.getROperand(), binary.getLOperand(), reference))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNullComparison(PsiExpression value, PsiExpression other, PsiReferenceExpression reference) {
        return PsiUtil.skipParenthesizedExprDown(other) instanceof PsiLiteralExpression literal
                && literal.getValue() == null && "null".equals(literal.getText())
                && value.getText().equals(reference.getText());
    }

    private static boolean needsParentheses(PsiExpression condition) {
        if (condition instanceof PsiPolyadicExpression polyadic) {
            IElementType operator = polyadic.getOperationTokenType();
            return operator == JavaTokenType.OROR;
        }
        return condition instanceof PsiConditionalExpression || condition instanceof PsiAssignmentExpression
                || condition instanceof PsiLambdaExpression;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return TEXT;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return TEXT;
    }
}
