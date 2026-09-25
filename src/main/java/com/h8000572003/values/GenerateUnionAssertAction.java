package com.h8000572003.values;

import com.h8000572003.values.codegen.CodeGenerators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code assertEquals(expected.getX(), actual.getX());} for {@code void check(Type expected, Type actual)}.
 */
public class GenerateUnionAssertAction extends CaretStatementsIntention {

    public static final String TITLE = "Generated assert based on parameter 1 as parameter 2";

    public GenerateUnionAssertAction() {
        super(TITLE);
    }

    @Override
    protected List<String> statements(@NotNull PsiMethod method) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length != 2) {
            return List.of();
        }
        PsiClass expected = PsiAccessors.classOf(parameters[0].getType());
        if (expected == null) {
            return List.of();
        }
        return CodeGenerators.assertions(parameters[0].getName(), parameters[1].getName(), PsiAccessors.getters(expected));
    }
}
