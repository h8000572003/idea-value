package com.h8000572003.values.configurable;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ValueConfigurableTest extends BasePlatformTestCase {

    private MyPluginSettings.State state;
    private ParameterType savedType;
    private String savedName;
    private ValueConfigurable configurable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        state = MyPluginSettings.getInstance().getState();
        savedType = state.getFeatureEnabled();
        savedName = state.getParameterName();
        configurable = new ValueConfigurable();
        configurable.createComponent();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            configurable.disposeUIResources();
            state.setFeatureEnabled(savedType);
            state.setParameterName(savedName);
        } finally {
            super.tearDown();
        }
    }

    public void testResetShowsStoredSettings() {
        state.setFeatureEnabled(ParameterType.USE_ORDER);
        state.setParameterName("list");

        configurable.reset();

        assertTrue(configurable.orderedButton().isSelected());
        assertFalse(configurable.namedButton().isSelected());
        assertEquals("list", configurable.parameterNameField().getText());
        assertFalse(configurable.isModified());
    }

    public void testApplyStoresChanges() {
        state.setFeatureEnabled(ParameterType.USE_ORDER);
        state.setParameterName("list");
        configurable.reset();

        configurable.namedButton().setSelected(true);
        configurable.parameterNameField().setText("map");
        assertTrue(configurable.isModified());

        configurable.apply();

        assertEquals(ParameterType.USE_NAMED, state.getFeatureEnabled());
        assertEquals("map", state.getParameterName());
        assertFalse(configurable.isModified());
    }

    public void testBlankNameFallsBackToDefault() {
        configurable.reset();
        configurable.parameterNameField().setText("  ");

        configurable.apply();

        assertEquals("parameters", state.getParameterName());
        assertFalse(configurable.isModified());
    }
}
