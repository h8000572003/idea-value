package com.h8000572003.values.codegen;

/**
 * Supplies numbers for generated sample values.
 */
public interface NumberSequence {

    long next(String property);

    /** Independent copy with the same state, e.g. for intention previews that must not advance the sequence. */
    NumberSequence copy();
}
