package com.h8000572003.values;

import com.h8000572003.values.codegen.Accessor;
import com.h8000572003.values.codegen.PropertyNames;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds JavaBeans accessors on classes.
 */
final class PsiAccessors {

    private PsiAccessors() {
    }

    static @Nullable PsiClass classOf(@Nullable PsiType type) {
        return type instanceof PsiClassType ? PsiTypesUtil.getPsiClass(type) : null;
    }

    /** Public instance setters with one parameter, including inherited ones; own class first. */
    static List<Accessor> setters(@NotNull PsiClass psiClass) {
        Map<String, Accessor> setters = new LinkedHashMap<>();
        for (PsiMethod method : psiClass.getAllMethods()) {
            if (isPublicInstance(method) && method.getParameterList().getParametersCount() == 1
                    && PropertyNames.fromSetter(method.getName()).isPresent()) {
                PsiType type = method.getParameterList().getParameters()[0].getType();
                setters.putIfAbsent(method.getName(), new Accessor(method.getName(), type.getCanonicalText()));
            }
        }
        return new ArrayList<>(setters.values());
    }

    /** Public instance getters without parameters, including inherited ones; own class first. */
    static List<Accessor> getters(@NotNull PsiClass psiClass) {
        return getters(psiClass.getAllMethods());
    }

    /** Public instance getters declared in the class itself. */
    static List<Accessor> ownGetters(@NotNull PsiClass psiClass) {
        return getters(psiClass.getMethods());
    }

    static List<String> names(List<Accessor> accessors) {
        return accessors.stream().map(Accessor::methodName).toList();
    }

    private static List<Accessor> getters(PsiMethod[] methods) {
        Map<String, Accessor> getters = new LinkedHashMap<>();
        Arrays.stream(methods)
                .filter(method -> isPublicInstance(method) && method.getParameterList().isEmpty()
                        && method.getReturnType() != null && !PsiTypes.voidType().equals(method.getReturnType())
                        && PropertyNames.fromGetter(method.getName()).isPresent())
                .forEach(method -> getters.putIfAbsent(method.getName(),
                        new Accessor(method.getName(), method.getReturnType().getCanonicalText())));
        return new ArrayList<>(getters.values());
    }

    private static boolean isPublicInstance(PsiMethod method) {
        PsiClass owner = method.getContainingClass();
        return method.hasModifierProperty(PsiModifier.PUBLIC)
                && !method.hasModifierProperty(PsiModifier.STATIC)
                && !method.isConstructor()
                && (owner == null || !CommonClassNames.JAVA_LANG_OBJECT.equals(owner.getQualifiedName()));
    }
}
