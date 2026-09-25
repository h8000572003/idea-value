package com.h8000572003.values;

import com.h8000572003.values.configurable.MyPluginSettings;
import com.h8000572003.values.configurable.ParameterType;
import com.h8000572003.values.sql.BindStyle;
import com.h8000572003.values.sql.ParameterCodeGenerator;
import com.h8000572003.values.sql.SqlParameterizer;
import com.h8000572003.values.sql.SqlRewrite;
import com.h8000572003.values.sql.UnsupportedSqlException;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Replaces values concatenated into SQL with bind parameters and inserts the statements
 * that collect the parameter values before the current statement.
 */
public abstract class SqlInjectionFixIntention extends PsiElementBaseIntentionAction {

    private final boolean declare;
    private final String title;

    protected SqlInjectionFixIntention(boolean declare, String title) {
        this.declare = declare;
        this.title = title;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiPolyadicExpression concatenation = SqlConcatenation.find(element);
        return concatenation != null && SqlConcatenation.anchor(concatenation) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiPolyadicExpression concatenation = SqlConcatenation.find(element);
        PsiStatement anchor = concatenation == null ? null : SqlConcatenation.anchor(concatenation);
        if (anchor == null) {
            return;
        }
        MyPluginSettings.State settings = MyPluginSettings.getInstance().getState();
        BindStyle style = settings.getFeatureEnabled() == ParameterType.USE_ORDER ? BindStyle.POSITIONAL : BindStyle.NAMED;

        SqlRewrite rewrite;
        try {
            rewrite = SqlParameterizer.rewrite(SqlConcatenation.parts(concatenation), style);
        } catch (UnsupportedSqlException e) {
            if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
                CommonRefactoringUtil.showErrorHint(project, editor, e.getMessage(), title, null);
            }
            return;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiElement block = anchor.getParent();
        for (String text : ParameterCodeGenerator.statements(style, settings.getParameterName(), rewrite.parameters(), declare)) {
            PsiElement added = block.addBefore(factory.createStatementFromText(text, anchor), anchor);
            added = JavaCodeStyleManager.getInstance(project).shortenClassReferences(added);
            CodeStyleManager.getInstance(project).reformat(added);
        }
        concatenation.replace(factory.createExpressionFromText(rewrite.expression(), concatenation));
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return title;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return title;
    }
}
