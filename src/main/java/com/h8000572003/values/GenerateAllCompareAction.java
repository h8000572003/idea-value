package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class GenerateAllCompareAction extends PsiElementBaseIntentionAction {

    public static final String TITLE = "Generate All Setters COMPARE";
    private static final Logger log = Logger.getInstance(GenerateAllCompareAction.class);

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
//        StringBuilder insertText = new StringBuilder();
//        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
//        if (psiMethod != null) {
//            PsiParameterList parameterList = psiMethod.getParameterList();
//            for (int i = 0; i < parameterList.getParametersCount(); i++) {
//                PsiParameter parameter = parameterList.getParameter(i);
//                if (parameter != null) {
//                    PsiClass psiClass = PsiTypesUtil.getPsiClass(parameter.getType());
//                    if (psiClass != null) {
//                        PsiMethod[] methods = psiClass.getMethods();
//                        for (PsiMethod method : methods) {
//                            boolean set = method.getName().startsWith("set");
//                            if (set) {
//                                PsiParameter parameter1 = method.getParameterList().getParameter(0);
//                                String canonicalText =  parameter1.getType().getCanonicalText();
//                                log.info("canonicalText = " + canonicalText);
//
//                                String name = parameter.getName();
//                                String value = "\"" + method.getName().replace("set", "") + "\"";
//                                insertText.append("%s.%s(%s);\n".formatted(name, method.getName(), value));
//                            }
//
//                        }
//
//                    }
//                }
//            }
//        }
//        Document document = editor.getDocument();
//        document.insertString(editor.getCaretModel().getOffset(), insertText.toString());
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiMethod parentOfType = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        return parentOfType != null;
    }


    @Override
    public @IntentionName @NotNull String getText() {
        return TITLE;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "";
    }


}
