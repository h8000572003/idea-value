package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class GenerateUnionAssertAction extends PsiElementBaseIntentionAction {

    public static final String TITLE = "Generate all source1 assertion sources2";
    private PsiParameterList parameterList;

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        Set<String> getMethods = getMethods(Objects.requireNonNull(parameterList.getParameter(0)), Contract.START_GET_OR_IS);
        insertCode(editor, getMethods, parameterList.getParameter(0), parameterList.getParameter(1));


    }

    private static void insertCode(Editor editor, Set<String> get, PsiParameter parameter, PsiParameter parameter1) {
        StringBuilder insertText = new StringBuilder("\n");
        for (String method : get) {
            insertText.append("assertEquals(%s.%s(), %s.%s());\n".formatted(
                    parameter.getName(),
                    method,
                    parameter1.getName(),
                    method
            ));
        }
        Document document = editor.getDocument();
        document.insertString(editor.getCaretModel().getOffset(), insertText.toString());
    }

    private static Set<String> getMethods(PsiParameter psiParameter, String startWith) {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(psiParameter.getType());
        Set<String> methodNames = new LinkedHashSet<>();
        if (psiClass != null) {
            for (PsiMethod method : psiClass.getMethods()) {
                if(method.getName().matches(startWith)){
                    methodNames.add(method.getName());
                }
            }
        }
        return methodNames;
    }



    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (element instanceof PsiWhiteSpace) {
            PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (psiMethod == null) {
                return false;
            }
            this.parameterList = psiMethod.getParameterList();
            if (parameterList.getParametersCount() == 2) {
                PsiClass psiClass1 = getClassTypeFromParameter(0, this.parameterList);
                return psiClass1 != null;
            }
        }

        return false;
    }

    private PsiClass getClassTypeFromParameter(int index, PsiParameterList parameterList) {
        PsiParameter parameter1 = parameterList.getParameter(index);
        if (parameter1 != null) {
            return PsiTypesUtil.getPsiClass(parameter1.getType());
        }
        return null;
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
