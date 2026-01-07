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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class GenerateSourceMapperV2Action extends PsiElementBaseIntentionAction {

    public static final String TITLE = "Generated set/get based on parameter 1 return value";
    private PsiParameterList parameterList;
    private PsiClass psiClass1;
    private PsiType returnType;


    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(returnType);
        final Set<String> set1 = getMethods1(Contract.START_GET_OR_IS, psiClass);


        String name1 = psiClass.getName();
        String target = "target";

        StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
        String name = Objects.requireNonNull(parameterList.getParameter(0)).getName();


        insertText.append("%s %s=new %s();\n".formatted(
                name1,
                target,
                name1
        ));
        for (String method : set1) {
            insertText.append("%s.%s(%s.%s());\n".formatted(
                    target,
                    method.replaceFirst("get", "set").replaceFirst("is", "set"),
                    name,
                    method
            ));
        }
        insertText.append("return %s;\n".formatted(target));
        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        document.insertString(editor.getCaretModel().getOffset(), insertText.toString());

        PsiDocumentManager.getInstance(project).commitDocument(document);
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile != null) {
            CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + insertText.length());
        }
    }

    private static Set<String> getMethods1(String startWith, PsiClass psiClass) {
        Set<String> methodNames = new LinkedHashSet<>();
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.hasModifierProperty(PsiModifier.PUBLIC) &&
                    !method.hasModifierProperty(PsiModifier.STATIC) &&
                    method.getName().matches(startWith)) {
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
            this.returnType = psiMethod.getReturnType();
            this.parameterList = psiMethod.getParameterList();
            if (parameterList.getParametersCount() >= 1 &&
                    !PsiTypes.voidType().equals(psiMethod.getReturnType())) {
                if ((returnType instanceof PsiPrimitiveType)) {
                    return false;
                }
                this.psiClass1 = getClassTypeFromParameter(0, this.parameterList);

                return this.psiClass1 != null && returnType != null;
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
