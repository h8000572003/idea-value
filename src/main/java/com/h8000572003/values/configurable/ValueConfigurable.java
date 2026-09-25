package com.h8000572003.values.configurable;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Tools | Values Settings: bind style and parameter collection name for the SQL injection fix.
 */
public class ValueConfigurable implements Configurable {

    private JPanel panel;
    private JRadioButton namedButton;
    private JRadioButton orderedButton;
    private JTextField parameterNameField;

    @Nls
    @Override
    public String getDisplayName() {
        return "Values Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            namedButton = new JRadioButton("Use Named");
            orderedButton = new JRadioButton("Use Ordered");
            ButtonGroup group = new ButtonGroup();
            group.add(namedButton);
            group.add(orderedButton);

            JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            typePanel.add(new JLabel("Choose use sql generate role:"));
            typePanel.add(namedButton);
            typePanel.add(orderedButton);

            parameterNameField = new JTextField(20);
            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            namePanel.add(new JLabel("Enter parameter name:"));
            namePanel.add(parameterNameField);

            panel = new JPanel(new BorderLayout());
            panel.add(typePanel, BorderLayout.NORTH);
            panel.add(namePanel, BorderLayout.CENTER);
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        MyPluginSettings.State state = state();
        return selectedType() != state.getFeatureEnabled()
                || !MyPluginSettings.State.effectiveName(parameterNameField.getText()).equals(state.getParameterName());
    }

    @Override
    public void apply() {
        MyPluginSettings.State state = state();
        state.setFeatureEnabled(selectedType());
        state.setParameterName(parameterNameField.getText());
    }

    @Override
    public void reset() {
        MyPluginSettings.State state = state();
        (state.getFeatureEnabled() == ParameterType.USE_ORDER ? orderedButton : namedButton).setSelected(true);
        parameterNameField.setText(state.getParameterName());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        namedButton = null;
        orderedButton = null;
        parameterNameField = null;
    }

    private ParameterType selectedType() {
        return orderedButton.isSelected() ? ParameterType.USE_ORDER : ParameterType.USE_NAMED;
    }

    private static MyPluginSettings.State state() {
        return MyPluginSettings.getInstance().getState();
    }

    JRadioButton namedButton() {
        return namedButton;
    }

    JRadioButton orderedButton() {
        return orderedButton;
    }

    JTextField parameterNameField() {
        return parameterNameField;
    }
}
