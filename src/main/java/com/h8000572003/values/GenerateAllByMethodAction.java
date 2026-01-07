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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GenerateAllByMethodAction extends PsiElementBaseIntentionAction {

    public static final String TEXT = "Generate fields by get/is method name";
    private final List<SmartPsiElementPointer<PsiMethod>> methods = new ArrayList<>();

    private SmartPsiElementPointer<PsiElement> elementPointer;

    GenerateAllByMethodAction() {

    }

    private String name;

    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final StringBuilder insertText = new StringBuilder("\n");
        this.methods.forEach(methodPointer -> {
            PsiMethod method = methodPointer.getElement();
            if (method == null) return;
            PsiType returnType = method.getReturnType();
            String canonicalText = getClassName(Objects.requireNonNull(returnType));
            String uncapitalize = StringUtils.uncapitalize(method.getName().replace("get", "").replace("is", ""));
            insertText.append("private %s %s;\n".formatted(canonicalText, uncapitalize));
        });
        insertText(editor, insertText);

    }

    @NotNull
    private static String getClassName(PsiType returnType) {
        String canonicalText = returnType.getCanonicalText();
        PsiClass psiClass1 = PsiTypesUtil.getPsiClass(returnType);
        if (psiClass1 != null) {
            String qualifiedName = psiClass1.getName();
            if (StringUtils.isNotBlank(qualifiedName)) {
                canonicalText = qualifiedName;
            }
        }
        return canonicalText;
    }

    private void insertText(Editor editor, StringBuilder insertText) {
        final Document document = editor.getDocument();
        PsiElement element = elementPointer.getElement();
        if (element == null) return;
        document.insertString(//
                element.getTextOffset() + element.getText().length(),//
                insertText.toString());//
    }


    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        this.methods.clear();
        if (!(element instanceof PsiWhiteSpace)) {
            return false;
        }
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass == null) {
            return false;
        }
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(project);
        this.elementPointer = smartPointerManager.createSmartPsiElementPointer(element);
        for (PsiMethod method : psiClass.getMethods()) {
            if (isMatch(method)) {
                this.methods.add(smartPointerManager.createSmartPsiElementPointer(method));
            }
        }
        return !methods.isEmpty();

    }

    private static boolean isMatch(PsiMethod method) {
        return method.getName().matches("(get|is)\\w+") &&
                method.getParameterList().getParametersCount() == 0 &&
                method.hasModifierProperty(PsiModifier.PUBLIC) &&
                !method.hasModifierProperty(PsiModifier.STATIC) &&
                !Objects.equals(method.getReturnType(), PsiTypes.voidType());
    }


    @Override
    public @IntentionName @NotNull String getText() {
        return TEXT;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "";
    }
}
