package com.h8000572003.values;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class GenerateSqlInjectFixAction extends PsiElementBaseIntentionAction {
    final boolean isSqlDeclare;
    final String actionTip;

    public GenerateSqlInjectFixAction(boolean isSqlDeclaration, String actionTip) {
        this.isSqlDeclare = isSqlDeclaration;
        this.actionTip = actionTip;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final PsiElement parent = element.getParent();
        final PsiPolyadicExpression psiPolyadicExpression = PsiTreeUtil.getParentOfType(parent, PsiPolyadicExpression.class);


        //psiPolyadicExpression
        PsiElement parent1 = psiPolyadicExpression.getParent();

        String declarationName = null;
        if (parent1 instanceof PsiAssignmentExpression) {
            declarationName = parent1.getFirstChild().getText();
        } else if (parent1 instanceof PsiLocalVariable) {
            declarationName = ((PsiLocalVariable) parent1).getName();

        }
        if (parent1 != null) {
            int startOffsetInParent1 = parent1.getStartOffsetInParent();
            int textOffset = parent1.getTextOffset();
            editor.getDocument().insertString(textOffset - startOffsetInParent1 - 1, "//TODO remove this line use next line replace it .\n//");
        }


        PsiReferenceExpression assignRef = PsiTreeUtil.getParentOfType(parent1, PsiReferenceExpression.class);
        final List<PsiReferenceExpression> refs = PsiTreeUtil.getChildrenOfTypeAsList(psiPolyadicExpression, PsiReferenceExpression.class);
        if (psiPolyadicExpression != null) {
            final StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
            String text = psiPolyadicExpression.getText();
            for (PsiReferenceExpression ref : refs) {
                text = StringUtils.replace(text, ref.getText(), "\":" + ref.getText() + "\"");
            }
            String newSql = text.replaceAll("'", "");
            if (assignRef != null) {
                insertText.append(assignRef.getText());
            }
            if (declarationName != null) {
                insertText.append(declarationName);
            }
            insertText.append(" = " + newSql + ";\n");

            int currentOffset = editor.getCaretModel().getOffset();
            int currentLineNumber = editor.getDocument().getLineNumber(currentOffset);
            int nextLineStartOffset = editor.getDocument().getLineEndOffset(currentLineNumber) + 1;


            if (isSqlDeclare) {
                insertText.append("Map<String, Object>map = new HashMap<>();\n");
            }
            for (PsiReferenceExpression ref : refs) {
                insertText.append("map.put(\"" + ref.getText() + "\", " + ref.getText() + ");\n");
            }
            Document document = editor.getDocument();
            document.insertString(nextLineStartOffset, insertText.toString());
        }


    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        final PsiElement parent = element.getParent();
        return PsiTreeUtil.getParentOfType(parent, PsiPolyadicExpression.class) != null;
    }


    @NotNull
    @Override
    public String getFamilyName() {
        return "";
    }

    @NotNull
    @Override
    public String getText() {
        return actionTip;
    }
}
