package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class GenerateAllSetterFieldNameAction extends PsiElementBaseIntentionAction {

    public static final String GENERATE_ALL_SETTERS = "Generate All Setters By FieldName";
    private static final Logger log = Logger.getInstance(GenerateAllSetterFieldNameAction.class);

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        int textOffset = element.getTextOffset();
        log.info("textOffset = " + textOffset);
        StringBuilder insertText = new StringBuilder("\n");
//        PsiVariable psiVariable = PsiTreeUtil.getParentOfType(element, PsiVariable.class);
//        if (element instanceof PsiWhiteSpace) {

        PsiLocalVariable psiLocalVariable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);

        PsiElement parent1 = psiLocalVariable.getParent();
        PsiClass psiClass = PsiTypesUtil.getPsiClass(psiLocalVariable.getType());
        if (psiClass != null) {
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                boolean set = method.getName().startsWith("set");
                if (set) {
                    PsiParameter parameter1 = method.getParameterList().getParameter(0);
                    String canonicalText = parameter1.getType().getCanonicalText();
                    log.info("canonicalText = " + canonicalText);
                    String name = psiLocalVariable.getName();
                    String value = "\"" + method.getName().replace("set", "") + "\"";
                    insertText.append("%s.%s(%s);\n".formatted(name, method.getName(), value));
                }

            }

        }


        Document document = editor.getDocument();
        document.insertString(parent1.getTextOffset() + parent1.getText().length(), insertText.toString());
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return element instanceof PsiIdentifier;
    }


    @Override
    public @IntentionName @NotNull String getText() {
        return GENERATE_ALL_SETTERS;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "";
    }


}
