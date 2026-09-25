package com.h8000572003.values;

import com.h8000572003.values.codegen.CodeGenerators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Creates the return value and copies properties from the first parameter.
 */
public class GenerateSourceMapperV2Action extends CaretStatementsIntention {

    public static final String TITLE = "Generated set/get based on parameter 1 return value";

    public GenerateSourceMapperV2Action() {
        super(TITLE);
    }

    @Override
    protected List<String> statements(@NotNull PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        PsiType returnType = method.getReturnType();
        PsiClass target = PsiAccessors.classOf(returnType);
        if (parameters.length == 0 || target == null || target.isInterface() || target.isEnum()
                || target.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return List.of();
        }
        PsiClass source = PsiAccessors.classOf(parameters[0].getType());
        if (source == null) {
            return List.of();
        }
        return CodeGenerators.newInstanceMapping(returnType.getCanonicalText(),
                PsiAccessors.names(PsiAccessors.setters(target)),
                parameters[0].getName(), PsiAccessors.names(PsiAccessors.getters(source)));
    }
}
