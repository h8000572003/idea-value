package com.h8000572003.values;

import com.h8000572003.values.commons.ApostropheUtils;
import com.h8000572003.values.configurable.MyPluginSettings;
import com.h8000572003.values.configurable.ParameterType;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.stream.Collectors;


public abstract class GenerateSqlInjectFixAction extends PsiElementBaseIntentionAction {
    final boolean isSqlDeclare;
    final String actionTip;
    private Map<ParameterType, UseParametersTypeService> useParametersTypeServiceMap = new HashMap<>();
    private UseParametersTypeService useParametersTypeService;

    public interface UseParametersTypeService {

        String getParameterValues(List<PsiReferenceExpression> refs, boolean isDeclare);

        String getSql(List<PsiReferenceExpression> refs, String originalSql, PsiPolyadicExpression psiPolyadicExpression);
    }


    private void addName() {
        this.useParametersTypeServiceMap.put(ParameterType.USE_NAMED, new UseParametersTypeService() {
            @Override
            public String getParameterValues(List<PsiReferenceExpression> refs, boolean isDeclare) {
                StringBuilder insertText = new StringBuilder(StringUtils.EMPTY);
                if (isSqlDeclare) {
                    insertText.append("Map<String, Object>" + MyPluginSettings.getInstance().getState().getParameterName() + " = new HashMap<>();\n");
                }
                Set<String> set = refs.stream().map(PsiReferenceExpression::getText).collect(Collectors.toSet());
                for (String key : set) {
                    insertText.append(MyPluginSettings.getInstance().getState().getParameterName() + ".put(\"" + key + "\", " + key + ");\n");
                }
                return insertText.toString();
            }

            @Override
            public String getSql(List<PsiReferenceExpression> refs, String originalSql, PsiPolyadicExpression psiPolyadicExpression) {

                return getNewString(psiPolyadicExpression, true);
            }
        });
    }

    private static @NotNull String getNewString(PsiPolyadicExpression psiPolyadicExpression, boolean useName) {
        PsiElement[] children = psiPolyadicExpression.getChildren();
        Set<Integer> refsValuedSet = new LinkedHashSet<>();
        for (int i = 0; i < children.length; i++) {
            PsiElement psiElement = children[i];
            if (psiElement instanceof PsiReferenceExpression) {
                int pre = searchValue(children, i, -1);
                if (pre >= 0) {
                    refsValuedSet.add(pre);
                }
                int next = searchValue(children, i, +1);
                if (next >= 0) {
                    refsValuedSet.add(next);
                }
            }
        }

        String allSql = new String();
        for (int i = 0; i < children.length; i++) {
            PsiElement operand = children[i];
            String text = operand.getText();
            if (refsValuedSet.contains(i)) {
                text = ApostropheUtils.replaceAll(text);
            }
            if (useName) {
                if (operand instanceof PsiReferenceExpression) {
                    String replaceValue = ":" + operand.getText();
                    text = StringUtils.replace(text, operand.getText(), "\"" + replaceValue + "\"");
                }
            } else {
                if (operand instanceof PsiReferenceExpression) {
                    String replaceValue = "?";
                    text = StringUtils.replace(text, operand.getText(), "\"" + replaceValue + "\"");
                }
            }

            allSql += text;
        }
        return allSql;
    }

    private static int searchValue(PsiElement[] children, int index, int shift) {
        int newIndex = index + shift;
        if (newIndex >= 0) {
            if (children[newIndex] instanceof PsiLiteralExpression) {
                return newIndex;
            } else {
                return searchValue(children, newIndex, shift);
            }
        }

        return -1;
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
            public String getSql(List<PsiReferenceExpression> refs, String originalSql, PsiPolyadicExpression psiPolyadicExpression) {
                return getNewString(psiPolyadicExpression, false);
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

        final List<PsiReferenceExpression> refs = PsiTreeUtil.getChildrenOfTypeAsList(psiPolyadicExpression, PsiReferenceExpression.class);
        if (psiPolyadicExpression != null) {
            String text = psiPolyadicExpression.getText();
            String sql = this.useParametersTypeService.getSql(refs, text, psiPolyadicExpression);
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            String parameterValues = this.useParametersTypeService.getParameterValues(refs, isSqlDeclare);
            PsiExpression newExpression = factory.createExpressionFromText(sql  , null);
            psiPolyadicExpression.replace(newExpression);


            CopyPasteManager.getInstance().setContents(new StringSelection(parameterValues));
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
