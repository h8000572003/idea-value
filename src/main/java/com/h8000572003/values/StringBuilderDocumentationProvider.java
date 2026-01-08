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

        PsiIfStatement lastIfStatement = null;

        // Find all relevant expressions (method calls for StringBuilder, assignments for String)
        if (isBuilder) {
            Collection<PsiReferenceExpression> references = PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression.class);
            for (PsiReferenceExpression ref : references) {
                if (ref.isReferenceTo(variable)) {
                    PsiElement parent = ref.getParent();
                    if (parent instanceof PsiReferenceExpression && ((PsiReferenceExpression) parent).getQualifierExpression() == ref) {
                        PsiElement current = parent.getParent();
                        
                        PsiIfStatement currentIf = PsiTreeUtil.getParentOfType(current, PsiIfStatement.class);
                        if (currentIf != lastIfStatement) {
                            if (lastIfStatement != null) content.append("fi\n");
                            if (currentIf != null) {
                                PsiExpression condition = currentIf.getCondition();
                                String condText = condition != null ? condition.getText().replaceAll("\\s+", " ") : "";
                                content.append("=== if(").append(condText).append("){\n");
                            }
                            lastIfStatement = currentIf;
                        }

                        while (current instanceof PsiMethodCallExpression || current instanceof PsiReferenceExpression) {
                            if (current instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) current;
                                if (isAppendCall(methodCall)) {
                                    PsiExpression[] args = methodCall.getArgumentList().getExpressions();
                                    if (args.length > 0) {
                                        if (currentIf != null) content.append("\t");
                                        content.append(evaluateExpression(args[0], methodCall)).append("\n");
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
                            PsiIfStatement currentIf = PsiTreeUtil.getParentOfType(assignment, PsiIfStatement.class);
                            if (currentIf != lastIfStatement) {
                                if (lastIfStatement != null) content.append("fi\n");
                                if (currentIf != null) {
                                    PsiExpression condition = currentIf.getCondition();
                                    String condText = condition != null ? condition.getText().replaceAll("\\s+", " ") : "";
                                    content.append("=== if(").append(condText).append("){\n");
                                }
                                lastIfStatement = currentIf;
                            }

                            if (currentIf != null) content.append("\t");
                            content.append(evaluateExpression(rExpr, assignment)).append("\n");
                        }
                    }
                }
            }
        }

        if (lastIfStatement != null) {
            content.append("fi\n");
        }

        if (content.length() == 0) return null;
        return "Content: <pre><b>" + content.toString() + "</b></pre>";
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
            return " $" + methodCall.getMethodExpression().getReferenceName() + "() ";
        }
        return "";
    }

    private String resolveVariableValue(PsiVariable variable, PsiElement context) {
        PsiElement scope = PsiTreeUtil.getParentOfType(variable, PsiCodeBlock.class);
        if (scope == null) return " $" + variable.getName() + " ";

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
            return " $" + variable.getName() + " ";
        }
        return value.toString();
    }
}
