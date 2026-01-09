package com.h8000572003.values;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class StringBuilderDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof PsiVariable) {
            PsiVariable variable = (PsiVariable) element;
            if (isAccumulator(variable.getType())) {
                return getAccumulatorContent(variable);
            }
        } else if (element instanceof PsiReferenceExpression) {
             PsiElement resolved = ((PsiReferenceExpression) element).resolve();
             if (resolved instanceof PsiVariable) {
                 PsiVariable variable = (PsiVariable) resolved;
                 if (isAccumulator(variable.getType())) {
                     return getAccumulatorContent(variable);
                 }
             }
        }
        return null;
    }

    private boolean isAccumulator(PsiType type) {
        if (type == null) return false;
        String canonicalText = type.getCanonicalText();

        return canonicalText.equals("java.lang.StringBuilder") || canonicalText.equals("java.lang.String") || canonicalText.equals("java.lang.StringBuffer")  ;
    }

    private boolean isBuilderType(PsiType type) {
        if (type == null) return false;
        String canonicalText = type.getCanonicalText();
        return canonicalText.equals("java.lang.StringBuilder") || canonicalText.equals("java.lang.StringBuffer");
    }

    private String getAccumulatorContent(PsiVariable variable) {
        PsiElement scope = PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class);
        if (scope == null) return null;

        StringBuilder content = new StringBuilder();
        boolean isBuilder = isBuilderType(variable.getType());

        // Handle initial value
        PsiExpression initializer = variable.getInitializer();
        if (isBuilder && initializer instanceof PsiNewExpression) {
            PsiExpressionList argumentList = ((PsiNewExpression) initializer).getArgumentList();
            if (argumentList != null && argumentList.getExpressions().length > 0) {
                content.append(evaluateExpression(argumentList.getExpressions()[0], initializer)).append("\n");
            }
        } else if (!isBuilder && initializer != null) {
            content.append(evaluateExpression(initializer, initializer)).append("\n");
        }

        java.util.List<IfBranch> activeIfs = new java.util.ArrayList<>();

        // Find all relevant expressions (method calls for StringBuilder, assignments for String)
        if (isBuilder) {
            Collection<PsiReferenceExpression> references = PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression.class);
            for (PsiReferenceExpression ref : references) {
                if (ref.isReferenceTo(variable)) {
                    PsiElement parent = ref.getParent();
                    if (parent instanceof PsiReferenceExpression && ((PsiReferenceExpression) parent).getQualifierExpression() == ref) {
                        PsiElement current = parent.getParent();

                        java.util.List<IfBranch> targetIfs = getParentIfs(current, scope);
                        activeIfs = updateIfContext(content, activeIfs, targetIfs);

                        while (current instanceof PsiMethodCallExpression || current instanceof PsiReferenceExpression) {
                            if (current instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) current;
                                if (isAppendCall(methodCall)) {
                                    PsiExpression[] args = methodCall.getArgumentList().getExpressions();
                                    if (args.length > 0) {
                                        appendIndented(content, activeIfs.size(), evaluateExpression(args[0], methodCall), activeIfs);
                                    }
                                } else if (!isBuilderType(methodCall.getType())) {
                                    break;
                                }
                            }
                            current = current.getParent();
                        }
                    }
                }
            }
        } else {
            // String +=
            Collection<PsiAssignmentExpression> assignments = PsiTreeUtil.findChildrenOfType(scope, PsiAssignmentExpression.class);
            for (PsiAssignmentExpression assignment : assignments) {
                PsiExpression lExpr = assignment.getLExpression();
                if (lExpr instanceof PsiReferenceExpression && ((PsiReferenceExpression) lExpr).isReferenceTo(variable)) {
                    if (assignment.getOperationTokenType() == JavaTokenType.PLUSEQ) {
                        PsiExpression rExpr = assignment.getRExpression();
                        if (rExpr != null) {
                            java.util.List<IfBranch> targetIfs = getParentIfs(assignment, scope);
                            activeIfs = updateIfContext(content, activeIfs, targetIfs);

                            appendIndented(content, activeIfs.size(), evaluateExpression(rExpr, assignment), activeIfs);
                        }
                    }
                }
            }
        }

        // Close all remaining ifs
        updateIfContext(content, activeIfs, new java.util.ArrayList<>());

        if (content.length() == 0) return null;
        return "Content: <pre><b>" + content.toString() + "</b></pre>";
    }

    private static class IfBranch {
        final PsiIfStatement ifStatement;
        final boolean isElse;

        IfBranch(PsiIfStatement ifStatement, boolean isElse) {
            this.ifStatement = ifStatement;
            this.isElse = isElse;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IfBranch ifBranch = (IfBranch) o;
            return isElse == ifBranch.isElse && ifStatement.equals(ifBranch.ifStatement);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ifStatement, isElse);
        }
    }

    private java.util.List<IfBranch> getParentIfs(PsiElement element, PsiElement scope) {
        java.util.List<IfBranch> ifs = new java.util.ArrayList<>();
        PsiElement current = element;
        while (current != null && current != scope) {
            PsiElement parent = current.getParent();
            if (parent instanceof PsiIfStatement) {
                PsiIfStatement ifStmt = (PsiIfStatement) parent;
                boolean isElse = (ifStmt.getElseBranch() == current);
                ifs.add(0, new IfBranch(ifStmt, isElse));
            }
            current = parent;
        }
        return ifs;
    }

    private java.util.List<IfBranch> updateIfContext(StringBuilder content, java.util.List<IfBranch> activeIfs, java.util.List<IfBranch> targetIfs) {
        int commonPrefix = 0;
        while (commonPrefix < activeIfs.size() && commonPrefix < targetIfs.size() && activeIfs.get(commonPrefix).equals(targetIfs.get(commonPrefix))) {
            commonPrefix++;
        }

        // Close ifs
        for (int i = activeIfs.size() - 1; i >= commonPrefix; i--) {
            IfBranch current = activeIfs.get(i);
            boolean isElseIfTransition = false;
            if (current.isElse && i + 1 < activeIfs.size()) {
                IfBranch next = activeIfs.get(i + 1);
                if (next.ifStatement.getParent() == current.ifStatement) {
                    isElseIfTransition = true;
                }
            }

            if (!isElseIfTransition) {
                int indent = getIndentLevel(activeIfs, i);
                for (int j = 0; j < indent; j++) content.append("\t");
                content.append("}\n");
            }
        }

        // Open new ifs
        boolean skipIndent = false;
        for (int i = commonPrefix; i < targetIfs.size(); i++) {
            IfBranch current = targetIfs.get(i);
            int indent = getIndentLevel(targetIfs, i);
            
            if (!skipIndent) {
                for (int j = 0; j < indent; j++) content.append("\t");
            }
            skipIndent = false;

            boolean isElseIf = false;
            if (current.isElse && i + 1 < targetIfs.size()) {
                IfBranch next = targetIfs.get(i + 1);
                if (next.ifStatement.getParent() == current.ifStatement) {
                    isElseIf = true;
                }
            }

            if (isElseIf) {
                content.append("else ");
                skipIndent = true;
            } else if (current.isElse) {
                content.append("else {\n");
            } else {
                PsiExpression condition = current.ifStatement.getCondition();
                String condText = condition != null ? condition.getText().replaceAll("\\s+", " ") : "";
                content.append("if(").append(condText).append("){\n");
            }
        }

        return targetIfs;
    }

    private int getIndentLevel(java.util.List<IfBranch> ifs, int index) {
        int indent = 0;
        for (int i = 0; i < index; i++) {
            IfBranch current = ifs.get(i);
            boolean isElseIfTransition = false;
            if (current.isElse && i + 1 < ifs.size()) {
                IfBranch next = ifs.get(i + 1);
                if (next.ifStatement.getParent() == current.ifStatement) {
                    isElseIfTransition = true;
                }
            }
            if (!isElseIfTransition) {
                indent++;
            }
        }
        return indent;
    }

    private void appendIndented(StringBuilder content, int listSize, String text, java.util.List<IfBranch> ifs) {
        int indentLevel = getIndentLevel(ifs, listSize);
        for (int i = 0; i < indentLevel; i++) {
            content.append("\t");
        }
        content.append(text).append("\n");
    }

    private boolean isAppendCall(PsiMethodCallExpression methodCall) {
        PsiReferenceExpression methodExpr = methodCall.getMethodExpression();
        return "append".equals(methodExpr.getReferenceName());
    }

    private String evaluateExpression(PsiExpression expression, PsiElement context) {
        if (expression instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteralExpression) expression).getValue();
            if (value != null) {
                String text = value.toString();
                if (text.endsWith("()")) {
                    return text.substring(0, text.length() - 2);
                }
                return text;
            }
            return "";
        } else if (expression instanceof PsiPolyadicExpression) {
            // Handle simple string concatenation like "a" + "b"
            StringBuilder sb = new StringBuilder();
            for (PsiExpression operand : ((PsiPolyadicExpression) expression).getOperands()) {
                sb.append(evaluateExpression(operand, context));
            }
            return sb.toString();
        } else if (expression instanceof PsiReferenceExpression) {
            PsiElement resolved = ((PsiReferenceExpression) expression).resolve();
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;
                return resolveVariableValue(variable, context);
            }
        } else if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
            return " ${" + methodCall.getMethodExpression().getReferenceName() + "}()";
        }
        return "";
    }

    private String resolveVariableValue(PsiVariable variable, PsiElement context) {
        PsiElement scope = PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class);
        if (scope == null) return " ${" + variable.getName() + "}";

        StringBuilder value = new StringBuilder();
        PsiExpression initializer = variable.getInitializer();
        if (initializer != null) {
            value.append(evaluateExpression(initializer, initializer));
        }

        Collection<PsiAssignmentExpression> assignments = PsiTreeUtil.findChildrenOfType(scope, PsiAssignmentExpression.class);
        for (PsiAssignmentExpression assignment : assignments) {
            // Check if assignment happens before the context
            if (assignment.getTextRange().getEndOffset() <= context.getTextRange().getStartOffset()) {
                PsiExpression lExpr = assignment.getLExpression();
                if (lExpr instanceof PsiReferenceExpression && ((PsiReferenceExpression) lExpr).isReferenceTo(variable)) {
                    PsiExpression rExpr = assignment.getRExpression();
                    if (rExpr != null) {
                        String rValue = evaluateExpression(rExpr, assignment);
                        if (assignment.getOperationTokenType() == JavaTokenType.EQ) {
                            value.setLength(0);
                            value.append(rValue);
                        } else if (assignment.getOperationTokenType() == JavaTokenType.PLUSEQ) {
                            value.append(rValue);
                        }
                    }
                }
            }
        }

        if (value.length() == 0) {
            return " ${" + variable.getName() + "}";
        }
        return value.toString();
    }
}
