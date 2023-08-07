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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateAllSetterFieldNameAction extends PsiElementBaseIntentionAction {

    public static final String GENERATE_ALL_SETTERS = "generate all setters with name";
    private static final Logger log = Logger.getInstance(GenerateAllSetterFieldNameAction.class);
    private List<PsiMethod> methods;
    private PsiLocalVariable psiLocalVariable;

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final StringBuilder insertText = new StringBuilder("\n");
        this.methods.forEach(method -> appendCode(method, insertText));
        insertText(editor, insertText);

    }

    private void appendCode(PsiMethod method, StringBuilder insertText) {
        final String name = psiLocalVariable.getName();
        final String value = "\"" + method.getName().replace("set", "") + "\"";
        insertText.append("%s.%s(%s);\n".formatted(name, method.getName(), value));
    }

    private void insertText(Editor editor, StringBuilder insertText) {
        final Document document = editor.getDocument();
        document.insertString(//
                psiLocalVariable.getParent().getTextOffset() + psiLocalVariable.getParent().getText().length(),//
                insertText.toString());//
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        this.methods = null;
        this.psiLocalVariable = null;
        if (element instanceof PsiIdentifier) {
            this.psiLocalVariable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
            if (psiLocalVariable != null) {
                PsiClass psiClass = PsiTypesUtil.getPsiClass(psiLocalVariable.getType());
                if (psiClass != null) {
                    this.methods = Arrays.stream(psiClass.getMethods())//
                            .filter(m -> m.getName().startsWith("set"))//
                            .collect(Collectors.toList());//
                    return !this.methods.isEmpty();
                }

            }
        }
        return false;

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
