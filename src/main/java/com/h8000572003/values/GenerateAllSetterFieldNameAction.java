package com.h8000572003.values;

import com.h8000572003.values.commons.KeppNameNumberValueStrategy;

public class GenerateAllSetterFieldNameAction extends BaseGenerateAllSetterFieldNameAction {

    GenerateAllSetterFieldNameAction() {
        super(new KeppNameNumberValueStrategy(), "generate all setters cache name values");

    }

}
