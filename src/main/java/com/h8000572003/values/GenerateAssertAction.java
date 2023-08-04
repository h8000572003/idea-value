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

import java.util.LinkedHashSet;
import java.util.Set;

public class GenerateAssertAction extends PsiElementBaseIntentionAction {

    public static final String TITLE = "Generate source1 assertion sources2";
    private static final Logger log = Logger.getInstance(GenerateAssertAction.class);

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {

        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        PsiParameterList parameterList = psiMethod.getParameterList();


        PsiParameter parameter = parameterList.getParameter(0);
        Set<String> get = getMethods(parameter, "get");

        PsiParameter parameter1 = parameterList.getParameter(1);
        Set<String> get1 = getMethods(parameter1, "get");
        Set<String> intersect = new LinkedHashSet<>(get);
        intersect.retainAll(get1);

        StringBuilder insertText = new StringBuilder("\n");
        for (String method : intersect) {
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
                if(method.getName().startsWith(startWith)){
                    methodNames.add(method.getName());
                }
            }
        }
        return methodNames;
    }



    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (psiMethod == null) {
            return false;
        }
        PsiParameterList parameterList = psiMethod.getParameterList();
        if (parameterList.getParametersCount() != 2) {
            return false;
        }
        return true;
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
