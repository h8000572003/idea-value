package com.h8000572003.values;

import com.h8000572003.values.commons.Assignment;
import com.h8000572003.values.exception.GenerateException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BaseGenerateAllSetterFieldNameAction extends PsiElementBaseIntentionAction {

    private final String text;
    private List<SmartPsiElementPointer<PsiMethod>> methodPointers;
    private SmartPsiElementPointer<PsiLocalVariable> psiLocalVariablePointer;
    private final Assignment assignment;


    BaseGenerateAllSetterFieldNameAction(Assignment.NumberValueStrategy numberValueStrategy, String text) {
        this.text = text;
        this.assignment = new Assignment(numberValueStrategy);
    }

    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final StringBuilder insertText = new StringBuilder("\n");
        PsiLocalVariable psiLocalVariable = psiLocalVariablePointer.getElement();
        if (psiLocalVariable == null) return;
        this.methodPointers.forEach(methodPointer -> {
            PsiMethod method = methodPointer.getElement();
            if (method == null) return;
            final String name = psiLocalVariable.getName();
            PsiParameterList parameterList = method.getParameterList();
            if (parameterList.getParametersCount() == 0) {
                throw new GenerateException(name + "no parameters");
            }
            PsiType type = Objects.requireNonNull(parameterList.getParameter(0)).getType();
            final String value = this.assignment.getAssignment(type.getCanonicalText(), parameterList.getParameter(0));
            insertText.append("%s.%s(%s);\n".formatted(name, method.getName(), value));
        });
        int offset = editor.getCaretModel().getOffset();
        final Document document = editor.getDocument();
        insertText(editor, insertText);
        PsiDocumentManager.getInstance(project).commitDocument(document);
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile != null) {
            CodeStyleManager.getInstance(project).reformatText(psiFile, offset, offset + insertText.length());
        }
    }

    private void insertText(Editor editor, StringBuilder insertText) {
        final Document document = editor.getDocument();
        PsiLocalVariable psiLocalVariable = psiLocalVariablePointer.getElement();
        if (psiLocalVariable == null) return;
        document.insertString(//
                psiLocalVariable.getParent().getTextOffset() + psiLocalVariable.getParent().getText().length(),//
                insertText.toString());//
    }


    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        this.methodPointers = null;
        this.psiLocalVariablePointer = null;
        if (element instanceof PsiIdentifier) {
            PsiLocalVariable psiLocalVariable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
            if (psiLocalVariable != null) {
                PsiType type = psiLocalVariable.getType();
                PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
                if (psiClass != null) {
                    PsiMethod[] methods1 = psiClass.getMethods();
                    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(project);
                    this.methodPointers = Arrays.stream(methods1)//
                            .filter(m -> m.getName().matches(Contract.START_SET))//
                            .map(smartPointerManager::createSmartPsiElementPointer)
                            .toList();
                    if (!this.methodPointers.isEmpty()) {
                        this.psiLocalVariablePointer = smartPointerManager.createSmartPsiElementPointer(psiLocalVariable);
                        return true;
                    }
                }

            }
        }
        return false;

    }


    @Override
    public @IntentionName @NotNull String getText() {
        return text;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "";
    }
}
