package com.h8000572003.values.configurable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(
        name = "MyPluginSettings",
        storages = {@Storage("MyPluginSettings.xml")}
)
public final class MyPluginSettings implements PersistentStateComponent<MyPluginSettings.State> {

    private State myState = new State();

    public static MyPluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(MyPluginSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public static class State {
        static final String DEFAULT_PARAMETER_NAME = "parameters";

        private ParameterType featureEnabled = ParameterType.USE_NAMED;
        private String parameterName = "";

        /** The parameter collection name used for the given input: blank means the default. */
        public static String effectiveName(String parameterName) {
            return parameterName == null || parameterName.isBlank() ? DEFAULT_PARAMETER_NAME : parameterName.strip();
        }

        public ParameterType getFeatureEnabled() {
            return featureEnabled;
        }

        public String getParameterName() {
            return effectiveName(parameterName);
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public void setFeatureEnabled(ParameterType featureEnabled) {
            this.featureEnabled = featureEnabled;
        }
    }
}
