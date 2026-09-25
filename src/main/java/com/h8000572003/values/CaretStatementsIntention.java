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

import java.util.List;

/**
 * Generates statements from the enclosing method's signature and inserts them at the caret,
 * which must be on an empty spot inside a code block.
 */
abstract class CaretStatementsIntention extends PsiElementBaseIntentionAction {

    private final String text;

    CaretStatementsIntention(String text) {
        this.text = text;
    }

    /** Statements to insert, or an empty list when the intention does not apply. */
    protected abstract List<String> statements(@NotNull PsiMethod method);

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return !statementsAt(element).isEmpty();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        List<String> statements = statementsAt(element);
        if (!statements.isEmpty()) {
            CodeInserter.statementsAt((PsiWhiteSpace) element, statements);
        }
    }

    private List<String> statementsAt(PsiElement element) {
        PsiCodeBlock block = CodeInserter.blockAtCaret(element);
        PsiMethod method = block == null ? null : PsiTreeUtil.getParentOfType(block, PsiMethod.class, true, PsiClass.class);
        return method == null ? List.of() : statements(method);
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return text;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return text;
    }
}
