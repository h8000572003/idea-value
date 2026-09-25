package com.h8000572003.values.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberSequencesTest {

    @Test
    void sequentialAlwaysIncreases() {
        NumberSequence numbers = NumberSequences.sequential();
        assertEquals(1, numbers.next("a"));
        assertEquals(2, numbers.next("a"));
        assertEquals(3, numbers.next("b"));
    }

    @Test
    void perPropertyReusesNumberForSameName() {
        NumberSequence numbers = NumberSequences.perProperty();
        assertEquals(1, numbers.next("a"));
        assertEquals(2, numbers.next("b"));
        assertEquals(1, numbers.next("a"));
    }

    @Test
    void copyIsIndependent() {
        NumberSequence numbers = NumberSequences.sequential();
        numbers.next("a");
        NumberSequence copy = numbers.copy();
        assertEquals(2, copy.next("a"));
        assertEquals(3, copy.next("a"));
        assertEquals(2, numbers.next("a"));

        NumberSequence perProperty = NumberSequences.perProperty();
        perProperty.next("a");
        NumberSequence perPropertyCopy = perProperty.copy();
        assertEquals(2, perPropertyCopy.next("b"));
        assertEquals(1, perPropertyCopy.next("a"));
        assertEquals(2, perProperty.next("c"));
    }
}
