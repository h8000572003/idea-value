package com.h8000572003.values;

import com.h8000572003.values.codegen.NumberSequences;

public class GenerateAllSetterFieldNameAction extends BaseGenerateAllSetterFieldNameAction {

    GenerateAllSetterFieldNameAction() {
        super(NumberSequences.perProperty(), "Generate all setters cache name values");
    }
}
