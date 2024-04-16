package com.h8000572003.values;

import com.h8000572003.values.configurable.MyPluginSettings;
import com.h8000572003.values.configurable.ParameterType;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GenerateSqlInjectFixAction extends PsiElementBaseIntentionAction {
    final boolean isSqlDeclare;
    final String actionTip;
    private Map<ParameterType, UseParametersTypeService> useParametersTypeServiceMap = new HashMap<>();
    private UseParametersTypeService useParametersTypeService;

    public interface UseParametersTypeService {

        String getParameterValues(List<PsiReferenceExpression> refs, boolean isDeclare);

        String getSql(List<PsiReferenceExpression> refs, String originalSql);
    }


    private void addName() {
        this.useParametersTypeServiceMap.put(ParameterType.USE_NAMED, new UseParametersTypeService() {
            @Override
            public String getParameterValues(List<PsiReferenceExpression> refs, boolean isDeclare) {
                StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
                if (isSqlDeclare) {
                    insertText.append("Map<String, Object>" + MyPluginSettings.getInstance().getState().getParameterName() + " = new HashMap<>();\n");
                }
                for (PsiReferenceExpression ref : refs) {
                    insertText.append(MyPluginSettings.getInstance().getState().getParameterName() + ".put(\"" + ref.getText() + "\", " + ref.getText() + ");\n");
                }
                return insertText.toString();
            }

            @Override
            public String getSql(List<PsiReferenceExpression> refs, String originalSql) {
                for (PsiReferenceExpression ref : refs) {
                    originalSql = StringUtils.replace(originalSql, ref.getText(), "\":" + ref.getText() + "\"");
                }
                return originalSql.replaceAll("'", "") + ";\n";
            }
        });
    }

    private void addOrder() {
        this.useParametersTypeServiceMap.put(ParameterType.USE_ORDER, new UseParametersTypeService() {
            @Override
            public String getParameterValues(List<PsiReferenceExpression> refs, boolean isDeclare) {
                StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
                if (isDeclare) {
                    insertText.append("List<Object> " + MyPluginSettings.getInstance().getState().getParameterName() + "=new ArrayList<>();\n");
                }
                for (PsiReferenceExpression ref : refs) {
                    insertText.append(MyPluginSettings.getInstance().getState().getParameterName() + ".add(" + ref.getText() + ");\n");
                }
                return insertText.toString();
            }

            @Override
            public String getSql(List<PsiReferenceExpression> refs, String originalSql) {
                for (PsiReferenceExpression ref : refs) {
                    originalSql = StringUtils.replace(originalSql, ref.getText(), "\"" + "?" + "\"");
                }
                return originalSql.replaceAll("'", "") + ";\n";
            }
        });
    }


    public GenerateSqlInjectFixAction(boolean isSqlDeclaration, String actionTip) {
        this.isSqlDeclare = isSqlDeclaration;
        this.actionTip = actionTip;
        this.addName();
        this.addOrder();


    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        final PsiElement parent = element.getParent();
        final PsiPolyadicExpression psiPolyadicExpression = PsiTreeUtil.getParentOfType(parent, PsiPolyadicExpression.class);
        this.useParametersTypeService = useParametersTypeServiceMap.get(MyPluginSettings.getInstance().getState().getFeatureEnabled());
        if (parent != null) {
            int startOffsetInParent1 = parent.getStartOffsetInParent();
            int textOffset = parent.getTextOffset();
            editor.getDocument().insertString(textOffset - startOffsetInParent1, "//");
        }
        final List<PsiReferenceExpression> refs = PsiTreeUtil.getChildrenOfTypeAsList(psiPolyadicExpression, PsiReferenceExpression.class);
        if (psiPolyadicExpression != null) {

            int currentOffset = editor.getCaretModel().getOffset();
            int currentLineNumber = editor.getDocument().getLineNumber(currentOffset);
            int nextLineStartOffset = editor.getDocument().getLineEndOffset(currentLineNumber) + 1;

            final StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
            String text = psiPolyadicExpression.getText();
            insertText.append(this.useParametersTypeService.getSql(refs, text));
            insertText.append(this.useParametersTypeService.getParameterValues(refs, isSqlDeclare));


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
