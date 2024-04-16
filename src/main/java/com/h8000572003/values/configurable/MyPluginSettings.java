package com.h8000572003.values.configurable;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "MyPluginSettings",
        storages = {@Storage("MyPluginSettings.xml")}
)
public class MyPluginSettings implements PersistentStateComponent<MyPluginSettings.State> {

    private State myState = new State();

    public static MyPluginSettings getInstance() {
        return ServiceManager.getService(MyPluginSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public static class State {
        private ParameterType featureEnabled = ParameterType.USE_NAMED;
        private String parameterName = "";

        public ParameterType getFeatureEnabled() {
            return featureEnabled;
        }

        public String getParameterName() {
            return StringUtils.defaultIfEmpty(parameterName, "parameters");
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public void setFeatureEnabled(ParameterType featureEnabled) {
            this.featureEnabled = featureEnabled;
        }
    }

}
