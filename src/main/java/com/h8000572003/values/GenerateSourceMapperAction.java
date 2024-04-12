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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class GenerateSourceMapperAction extends PsiElementBaseIntentionAction {

    public static final String TITLE = "Generated set/get based on parameter 1 as parameter 2";
    private PsiParameterList parameterList;
    private PsiClass psiClass1;



    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final Set<String> get1 = getMethods1(Contract.START_GET_OR_IS, psiClass1);

        StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
        String name = Objects.requireNonNull(parameterList.getParameter(0)).getName();
        String name1 = Objects.requireNonNull(parameterList.getParameter(1)).getName();
        for (String method : get1) {
            insertText.append("%s.%s(%s.%s());\n".formatted(
                    name,
                    method.replaceFirst("get", "set").replaceFirst("is", "set"),
                    name1,
                    method
            ));
        }
        Document document = editor.getDocument();
        document.insertString(editor.getCaretModel().getOffset(), insertText.toString());


    }

    private static Set<String> getMethods1(String startWith, PsiClass psiClass) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.getName().matches(startWith)) {
                methodNames.add(method.getName());
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
                this.psiClass1 = getClassTypeFromParameter(0, this.parameterList);
                return this.psiClass1 != null;
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
