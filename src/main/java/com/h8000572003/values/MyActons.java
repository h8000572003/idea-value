package com.h8000572003.values;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtilEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class MyActons extends AnAction {


    private Logger log = Logger.getInstance(MyActons.class);

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (editor == null || psiFile == null) {
            return;
        }
        int offset = editor.getCaretModel().getOffset();

        PsiElement element = psiFile.findElementAt(offset);


        if (element == null) {
            return;
        }


        StringBuilder insertText = new StringBuilder();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (psiMethod != null) {
            PsiParameterList parameterList = psiMethod.getParameterList();
            for (int i = 0; i < parameterList.getParametersCount(); i++) {
                PsiParameter parameter = parameterList.getParameter(i);
                if (parameter != null) {
                    PsiClass psiClass = PsiTypesUtil.getPsiClass(parameter.getType());
                    if (psiClass != null) {
                        PsiMethod[] methods = psiClass.getMethods();
                        for (PsiMethod method : methods) {
                            boolean set = method.getName().startsWith("set");
                            if (set) {
                                String name = parameter.getName();
                                String value ="\""+ method.getName().replace("set", "")+"\"";
                                insertText.append("%s.%s(%s);\n".formatted(name, method.getName(),value));
                            }

                        }

                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(insertText.toString())) {
            Document document = editor.getDocument();
            Runnable r = () -> EditorModificationUtilEx.insertStringAtCaret(editor, insertText.toString());
            ApplicationManager.getApplication().runWriteAction(
                    () -> CommandProcessor.getInstance().executeCommand(project, r, null, null, UndoConfirmationPolicy.DEFAULT, document));
            Messages.showMessageDialog(anActionEvent.getProject(), "OK", "PSI Info", null);
        }


    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile != null);


    }


}
