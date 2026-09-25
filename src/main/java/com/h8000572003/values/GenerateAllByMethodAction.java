package com.h8000572003.values;

import com.h8000572003.values.codegen.CodeGenerators;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * On an empty spot in a class body, declares a field for every getter of the class.
 */
public class GenerateAllByMethodAction extends PsiElementBaseIntentionAction {

    public static final String TEXT = "Generate fields by get/is method name";

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace && element.getParent() instanceof PsiClass psiClass
                && !fields(psiClass).isEmpty();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        if (element.getParent() instanceof PsiClass psiClass) {
            CodeInserter.fieldsAfter(psiClass, element.getPrevSibling(), fields(psiClass));
        }
    }

    private static List<String> fields(PsiClass psiClass) {
        Set<String> existing = Arrays.stream(psiClass.getFields()).map(PsiField::getName).collect(Collectors.toSet());
        return CodeGenerators.fieldsFromGetters(PsiAccessors.ownGetters(psiClass), existing);
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
