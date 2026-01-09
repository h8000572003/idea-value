package com.h8000572003.values;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

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

        java.util.List<ControlBranch> activeBranches = new java.util.ArrayList<>();

        // Find all relevant expressions (method calls for StringBuilder, assignments for String)
        if (isBuilder) {
            Collection<PsiReferenceExpression> references = PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression.class);
            for (PsiReferenceExpression ref : references) {
                if (ref.isReferenceTo(variable)) {
                    PsiElement parent = ref.getParent();
                    if (parent instanceof PsiReferenceExpression && ((PsiReferenceExpression) parent).getQualifierExpression() == ref) {
                        PsiElement current = parent.getParent();

                        java.util.List<ControlBranch> targetBranches = getParentBranches(current, scope);
                        activeBranches = updateControlContext(content, activeBranches, targetBranches);

                        while (current instanceof PsiMethodCallExpression || current instanceof PsiReferenceExpression) {
                            if (current instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) current;
                                if (isAppendCall(methodCall)) {
                                    PsiExpression[] args = methodCall.getArgumentList().getExpressions();
                                    if (args.length > 0) {
                                        appendIndented(content, activeBranches.size(), evaluateExpression(args[0], methodCall), activeBranches);
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
                            java.util.List<ControlBranch> targetBranches = getParentBranches(assignment, scope);
                            activeBranches = updateControlContext(content, activeBranches, targetBranches);

                            appendIndented(content, activeBranches.size(), evaluateExpression(rExpr, assignment), activeBranches);
                        }
                    }
                }
            }
        }

        // Close all remaining branches
        updateControlContext(content, activeBranches, new java.util.ArrayList<>());

        if (content.length() == 0) return null;
        return "Content: <pre><b>" + content.toString() + "</b></pre>";
    }

    private static class ControlBranch {
        final PsiElement statement; // PsiIfStatement or PsiSwitchStatement or PsiSwitchLabelStatementBase
        final boolean isElse; // for if

        ControlBranch(PsiElement statement, boolean isElse) {
            this.statement = statement;
            this.isElse = isElse;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ControlBranch that = (ControlBranch) o;
            return isElse == that.isElse && Objects.equals(statement, that.statement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statement, isElse);
        }
    }

    private java.util.List<ControlBranch> getParentBranches(PsiElement element, PsiElement scope) {
        java.util.List<ControlBranch> branches = new java.util.ArrayList<>();
        PsiElement current = element;
        while (current != null && current != scope) {
            PsiElement parent = current.getParent();
            if (parent instanceof PsiIfStatement) {
                PsiIfStatement ifStmt = (PsiIfStatement) parent;
                boolean isElse = (ifStmt.getElseBranch() == current);
                branches.add(0, new ControlBranch(ifStmt, isElse));
            } else if (parent instanceof PsiSwitchStatement || (parent instanceof PsiCodeBlock && parent.getParent() instanceof PsiSwitchStatement)) {
                PsiSwitchStatement switchStmt = (parent instanceof PsiSwitchStatement) ? (PsiSwitchStatement) parent : (PsiSwitchStatement) parent.getParent();
                
                // Only add the switch branch if we haven't already processed it for the same switchStmt
                boolean alreadyAdded = false;
                for (ControlBranch branch : branches) {
                    if (branch.statement == switchStmt) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (!alreadyAdded) {
                    branches.add(0, new ControlBranch(switchStmt, false));

                    // Also find the case/default label if we are inside one
                    PsiElement runner = current;
                    while (runner != null && runner.getParent() != switchStmt.getBody()) {
                        runner = runner.getParent();
                    }

                    // Search backwards from runner to find the nearest switch label
                    PsiElement prev = runner;
                    while (prev != null) {
                        if (prev instanceof PsiSwitchLabelStatementBase) {
                            branches.add(1, new ControlBranch(prev, false));
                            break;
                        }
                        prev = prev.getPrevSibling();
                    }
                }
            }
            current = parent;
        }
        return branches;
    }

    private java.util.List<ControlBranch> updateControlContext(StringBuilder content, java.util.List<ControlBranch> activeBranches, java.util.List<ControlBranch> targetBranches) {
        int commonPrefix = 0;
        while (commonPrefix < activeBranches.size() && commonPrefix < targetBranches.size() && activeBranches.get(commonPrefix).equals(targetBranches.get(commonPrefix))) {
            commonPrefix++;
        }

        // Close branches
        for (int i = activeBranches.size() - 1; i >= commonPrefix; i--) {
            ControlBranch current = activeBranches.get(i);
            if (current.statement instanceof PsiIfStatement) {
                boolean isElseIfTransition = false;
                if (current.isElse && i + 1 < activeBranches.size()) {
                    ControlBranch next = activeBranches.get(i + 1);
                    if (next.statement instanceof PsiIfStatement && next.statement.getParent() == current.statement) {
                        isElseIfTransition = true;
                    }
                }

                if (!isElseIfTransition) {
                    int indent = getIndentLevel(activeBranches, i);
                    for (int j = 0; j < indent; j++) content.append("\t");
                    content.append("}\n");
                }
            } else if (current.statement instanceof PsiSwitchStatement) {
                int indent = getIndentLevel(activeBranches, i);
                for (int j = 0; j < indent; j++) content.append("\t");
                content.append("}\n");
            }
            // PsiSwitchLabelStatementBase doesn't have closing brace
        }

        // Open new branches
        boolean skipIndent = false;
        for (int i = commonPrefix; i < targetBranches.size(); i++) {
            ControlBranch current = targetBranches.get(i);
            int indent = getIndentLevel(targetBranches, i);

            if (!skipIndent) {
                for (int j = 0; j < indent; j++) content.append("\t");
            }
            skipIndent = false;

            if (current.statement instanceof PsiIfStatement) {
                PsiIfStatement ifStmt = (PsiIfStatement) current.statement;
                boolean isElseIf = false;
                if (current.isElse && i + 1 < targetBranches.size()) {
                    ControlBranch next = targetBranches.get(i + 1);
                    if (next.statement instanceof PsiIfStatement && next.statement.getParent() == ifStmt) {
                        isElseIf = true;
                    }
                }

                if (isElseIf) {
                    content.append("else ");
                    skipIndent = true;
                } else if (current.isElse) {
                    content.append("else {\n");
                } else {
                    PsiExpression condition = ifStmt.getCondition();
                    String condText = condition != null ? condition.getText().replaceAll("\\s+", " ") : "";
                    content.append("if(").append(condText).append("){\n");
                }
            } else if (current.statement instanceof PsiSwitchStatement) {
                PsiSwitchStatement switchStmt = (PsiSwitchStatement) current.statement;
                PsiExpression expression = switchStmt.getExpression();
                String exprText = expression != null ? expression.getText() : "";
                content.append("switch(").append(exprText).append("){\n");
            } else if (current.statement instanceof PsiSwitchLabelStatementBase) {
                PsiSwitchLabelStatementBase label = (PsiSwitchLabelStatementBase) current.statement;
                content.append(label.getText().trim()).append("\n");
            }
        }

        return targetBranches;
    }

    private int getIndentLevel(java.util.List<ControlBranch> branches, int index) {
        int indent = 0;
        for (int i = 0; i < index; i++) {
            ControlBranch current = branches.get(i);
            if (current.statement instanceof PsiIfStatement) {
                boolean isElseIfTransition = false;
                if (current.isElse && i + 1 < branches.size()) {
                    ControlBranch next = branches.get(i + 1);
                    if (next.statement instanceof PsiIfStatement && next.statement.getParent() == current.statement) {
                        isElseIfTransition = true;
                    }
                }
                if (!isElseIfTransition) {
                    indent++;
                }
            } else if (current.statement instanceof PsiSwitchStatement) {
                indent++;
            } else if (current.statement instanceof PsiSwitchLabelStatementBase) {
                indent++;
            }
        }
        return indent;
    }

    private void appendIndented(StringBuilder content, int listSize, String text, java.util.List<ControlBranch> branches) {
        int indentLevel = getIndentLevel(branches, listSize);
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
