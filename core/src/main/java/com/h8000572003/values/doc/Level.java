package com.h8000572003.values.doc;

/**
 * One enclosing control structure of an accumulated fragment.
 *
 * @param chain  identity of the statement chain; branches of one if/else-if/else chain share it
 * @param branch identity of this branch
 * @param header rendered header, e.g. {@code if (a)}, {@code else}, {@code case 1:}
 * @param braces whether the level opens a {@code { }} block; labels such as {@code case} do not
 */
public record Level(Object chain, Object branch, String header, boolean braces) {

    public static Level block(Object chain, Object branch, String header) {
        return new Level(chain, branch, header, true);
    }

    public static Level label(Object branch, String header) {
        return new Level(branch, branch, header, false);
    }

    boolean sameBranch(Level other) {
        return branch.equals(other.branch);
    }
}
