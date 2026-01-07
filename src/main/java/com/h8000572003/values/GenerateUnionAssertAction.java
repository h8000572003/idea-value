package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class GenerateUnionAssertAction extends PsiElementBaseIntentionAction {
    public static final String TITLE = "Generated assert based on parameter 1 as parameter 2";
    private SmartPsiElementPointer<PsiParameterList> parameterListPointer;

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiParameterList parameterList = parameterListPointer.getElement();
        if (parameterList == null) return;
        Set<String> getMethods = getMethods(Objects.requireNonNull(parameterList.getParameter(0)), Contract.START_GET_OR_IS);

        insertCode(project, editor, getMethods, parameterList.getParameter(0), parameterList.getParameter(1));

    }

    private static void insertCode(@NotNull Project project, Editor editor, Set<String> get, PsiParameter parameter, PsiParameter parameter1) {
        StringBuilder insertText = new StringBuilder("\n");
        for (String method : get) {
            insertText.append("assertEquals(%s.%s(), %s.%s());\n".formatted(
                    parameter.getName(),
                    method,
                    parameter1.getName(),
                    method
            ));
        }
        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        document.insertString(editor.getCaretModel().getOffset(), insertText.toString());
        PsiDocumentManager.getInstance(project).commitDocument(document);
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile != null) {
            CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + insertText.length());
        }
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
            PsiParameterList parameterList = psiMethod.getParameterList();
            this.parameterListPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(parameterList);
            if (parameterList.getParametersCount() == 2) {
                PsiClass psiClass1 = getClassTypeFromParameter(0, parameterList);
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
