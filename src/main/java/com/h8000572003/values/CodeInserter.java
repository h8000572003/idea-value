package com.h8000572003.values;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Inserts generated Java code into PSI, then shortens class references and reformats it.
 */
final class CodeInserter {

    private CodeInserter() {
    }

    /** Whitespace directly inside a code block, where the caret marks the insertion point. */
    static @Nullable PsiCodeBlock blockAtCaret(@NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace && element.getParent() instanceof PsiCodeBlock block ? block : null;
    }

    /** Inserts statements at the caret whitespace inside a code block. */
    static void statementsAt(@NotNull PsiWhiteSpace caret, @NotNull List<String> statements) {
        statementsAfter(caret.getPrevSibling(), statements);
    }

    /** Inserts statements after an element in a code block, in order. */
    static void statementsAfter(@NotNull PsiElement anchor, @NotNull List<String> statements) {
        PsiElement parent = anchor.getParent();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(anchor.getProject());
        for (String text : statements) {
            anchor = parent.addAfter(factory.createStatementFromText(text, parent), anchor);
            anchor = tidy(anchor);
        }
    }

    /** Inserts field declarations after an element in a class body, in order. */
    static void fieldsAfter(@NotNull PsiClass psiClass, @NotNull PsiElement anchor, @NotNull List<String> fields) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        for (String text : fields) {
            anchor = psiClass.addAfter(factory.createFieldFromText(text, psiClass), anchor);
            anchor = tidy(anchor);
        }
    }

    private static PsiElement tidy(PsiElement element) {
        element = JavaCodeStyleManager.getInstance(element.getProject()).shortenClassReferences(element);
        return CodeStyleManager.getInstance(element.getProject()).reformat(element);
    }
}
