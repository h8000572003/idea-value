package com.h8000572003.values.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Map;

public class ValueConfigurable implements Configurable {

    private JPanel panel;
    private JLabel label = new JLabel("Choose use sql generate role:");
    private JRadioButton mapCheckBox1;
    private JRadioButton listCheckBox2;
    private ButtonGroup buttonGroup;

    private Map<JRadioButton, ParameterType> ui2Type;
    private Map<ParameterType, JRadioButton> type2Ui;
    private JTextField parameterTextField;

    @Nls
    @Override
    public String getDisplayName() {
        return "values Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null) {

            JPanel panel = new JPanel(new BorderLayout());
            this.mapCheckBox1 = new JRadioButton("Use Named");
            this.listCheckBox2 = new JRadioButton("Use Ordered");


            this.ui2Type = Map.of(
                    mapCheckBox1, ParameterType.USE_NAMED,
                    listCheckBox2, ParameterType.USE_ORDER
            );

            this.type2Ui = Map.of(
                    ParameterType.USE_NAMED, mapCheckBox1,
                    ParameterType.USE_ORDER, listCheckBox2
            );

            this.buttonGroup = new ButtonGroup();
            this.buttonGroup.add(mapCheckBox1);
            this.buttonGroup.add(listCheckBox2);

            JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


            radioPanel.add(label);
            radioPanel.add(mapCheckBox1);
            radioPanel.add(listCheckBox2);


            panel.add(radioPanel, BorderLayout.NORTH);


            parameterTextField = new JTextField(20);
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            inputPanel.add(new JLabel("Enter parameter name:"));
            inputPanel.add(parameterTextField);
            panel.add(inputPanel, BorderLayout.CENTER);

            this.panel = panel;
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        return !(getParameterType().equals(MyPluginSettings.getInstance().getState().getFeatureEnabled())) ||
                !parameterTextField.getText().equals(MyPluginSettings.getInstance().getState().getParameterName());
    }

    @Override
    public void apply() throws ConfigurationException {
        MyPluginSettings.getInstance().getState().setFeatureEnabled(getParameterType());
        MyPluginSettings.getInstance().getState().setParameterName(parameterTextField.getText());
    }

    @Override
    public void reset() {
        ParameterType featureEnabled = MyPluginSettings.getInstance().getState().getFeatureEnabled();
        type2Ui.get(featureEnabled).setSelected(true);
        String parameterName = MyPluginSettings.getInstance().getState().getParameterName();
        parameterTextField.setText(parameterName);
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        buttonGroup = null;
        mapCheckBox1 = null;
        listCheckBox2 = null;
        parameterTextField = null;
    }

    public ParameterType getParameterType() {

        Enumeration<AbstractButton> elements = buttonGroup.getElements();
        while (elements.hasMoreElements()) {
            AbstractButton abstractButton = elements.nextElement();
            if (abstractButton.isSelected()) {
                return ui2Type.get(abstractButton);
            }
        }
        return ParameterType.USE_NAMED;

    }
}
