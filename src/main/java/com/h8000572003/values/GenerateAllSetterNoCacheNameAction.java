package com.h8000572003.values;

import com.h8000572003.values.commons.NotKeepValueStrategy;

public class GenerateAllSetterNoCacheNameAction extends BaseGenerateAllSetterFieldNameAction {

    GenerateAllSetterNoCacheNameAction() {
        super(new NotKeepValueStrategy(), "generate all setters no cache values");

    }
}
