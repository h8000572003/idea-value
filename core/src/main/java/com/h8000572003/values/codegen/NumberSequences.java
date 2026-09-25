package com.h8000572003.values.codegen;

import java.util.HashMap;
import java.util.Map;

public final class NumberSequences {

    private NumberSequences() {
    }

    /** A new number every time. */
    public static NumberSequence sequential() {
        return new Sequential(0);
    }

    /** The same number for the same property name, so separately generated objects get equal values. */
    public static NumberSequence perProperty() {
        return new PerProperty(new Sequential(0), new HashMap<>());
    }

    private static final class Sequential implements NumberSequence {
        private long last;

        Sequential(long last) {
            this.last = last;
        }

        @Override
        public synchronized long next(String property) {
            return ++last;
        }

        @Override
        public synchronized NumberSequence copy() {
            return new Sequential(last);
        }
    }

    private static final class PerProperty implements NumberSequence {
        private final Sequential counter;
        private final Map<String, Long> numbers;

        PerProperty(Sequential counter, Map<String, Long> numbers) {
            this.counter = counter;
            this.numbers = numbers;
        }

        @Override
        public synchronized long next(String property) {
            return numbers.computeIfAbsent(property, counter::next);
        }

        @Override
        public synchronized NumberSequence copy() {
            return new PerProperty((Sequential) counter.copy(), new HashMap<>(numbers));
        }
    }
}
