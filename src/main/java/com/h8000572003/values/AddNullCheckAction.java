package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class AddNullCheckAction extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {

        PsiIfStatement ifStatement = PsiTreeUtil.getParentOfType(element, PsiIfStatement.class);

        if (ifStatement != null) {
            PsiExpression condition = ifStatement.getCondition();
            if (condition instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) condition;
                PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

                // 获取方法调用者（即 "value"）
                PsiExpression qualifierExpression = methodExpression.getQualifierExpression();

                if (qualifierExpression != null && qualifierExpression.getText().equals(element.getText())) {
                    String newConditionText = qualifierExpression.getText() + " != null && " + condition.getText();

                    // 创建新的条件表达式
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    PsiExpression newCondition = factory.createExpressionFromText(newConditionText, null);

                    // 替换旧的条件表达式
                    condition.replace(newCondition);
                }
            }
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, PsiIfStatement.class) != null;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Add 'value != null' check";
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return "Add 'value != null' check";
    }
}
