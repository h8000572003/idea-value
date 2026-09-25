package com.h8000572003.values;

import com.h8000572003.values.codegen.NumberSequences;

public class GenerateAllSetterNoCacheNameAction extends BaseGenerateAllSetterFieldNameAction {

    GenerateAllSetterNoCacheNameAction() {
        super(NumberSequences.sequential(), "Generate all setters no cache values");
    }
}
