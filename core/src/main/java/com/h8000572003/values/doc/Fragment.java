package com.h8000572003.values.doc;

import java.util.List;

/**
 * Text appended to an accumulator, with the control structures around the append, outermost first.
 */
public record Fragment(String text, List<Level> path) {
}
