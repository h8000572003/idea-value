package com.h8000572003.values;

import com.h8000572003.values.codegen.CodeGenerators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code target.setX(source.getX());} for {@code void map(Target target, Source source)}.
 */
public class GenerateSourceMapperAction extends CaretStatementsIntention {

    public static final String TITLE = "Generated set/get based on parameter 1 as parameter 2";

    public GenerateSourceMapperAction() {
        super(TITLE);
    }

    @Override
    protected List<String> statements(@NotNull PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length != 2) {
            return List.of();
        }
        PsiClass target = PsiAccessors.classOf(parameters[0].getType());
        PsiClass source = PsiAccessors.classOf(parameters[1].getType());
        if (target == null || source == null) {
            return List.of();
        }
        return CodeGenerators.copyProperties(
                parameters[0].getName(), PsiAccessors.names(PsiAccessors.setters(target)),
                parameters[1].getName(), PsiAccessors.names(PsiAccessors.getters(source)));
    }
}
