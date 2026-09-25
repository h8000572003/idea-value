package com.h8000572003.values;

import com.h8000572003.values.codegen.Accessor;
import com.h8000572003.values.codegen.CodeGenerators;
import com.h8000572003.values.codegen.NumberSequence;
import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * On a local variable, calls every setter of its class with a sample value.
 */
public abstract class BaseGenerateAllSetterFieldNameAction extends PsiElementBaseIntentionAction {

    private final String text;
    @FileModifier.SafeFieldForPreview
    private final NumberSequence numbers;

    protected BaseGenerateAllSetterFieldNameAction(NumberSequence numbers, String text) {
        this.numbers = numbers;
        this.text = text;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiLocalVariable variable = variableAt(element);
        return variable != null && !setters(variable).isEmpty();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiLocalVariable variable = variableAt(element);
        if (variable == null) {
            return;
        }
        // The preview must not advance the shared number sequence.
        NumberSequence sequence = IntentionPreviewUtils.isIntentionPreviewActive() ? numbers.copy() : numbers;
        List<String> statements = CodeGenerators.setterCalls(variable.getName(), setters(variable), sequence);
        CodeInserter.statementsAfter(variable.getParent(), statements);
    }

    private static @Nullable PsiLocalVariable variableAt(PsiElement element) {
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiLocalVariable variable
                && variable.getParent() instanceof PsiDeclarationStatement statement
                && statement.getParent() instanceof PsiCodeBlock) {
            return variable;
        }
        return null;
    }

    private static List<Accessor> setters(PsiLocalVariable variable) {
        PsiClass psiClass = PsiAccessors.classOf(variable.getType());
        return psiClass == null ? List.of() : PsiAccessors.setters(psiClass);
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
