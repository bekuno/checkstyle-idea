package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginConfigurationManager {
    public static final String LEGACY_PROJECT_DIR = "$PROJECT_DIR$";
    public static final String PROJECT_DIR = "$PRJ_DIR$";

    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;
    private final ProjectConfigurationState projectConfigurationState;
    private final WorkspaceConfigurationState workspaceConfigurationState;

    /**
     * mock instance which may be set and used by unit tests
     */
    private static PluginConfigurationManager testInstance = null;


    public PluginConfigurationManager(@NotNull final Project project,
                                      @NotNull final ProjectConfigurationState projectConfigurationState,
                                      @NotNull final WorkspaceConfigurationState workspaceConfigurationState) {
        this.project = project;
        this.projectConfigurationState = projectConfigurationState;
        this.workspaceConfigurationState = workspaceConfigurationState;
    }


    public void addConfigurationListener(final ConfigurationListener configurationListener) {
        if (configurationListener != null) {
            configurationListeners.add(configurationListener);
        }
    }

    private void fireConfigurationChanged() {
        synchronized (configurationListeners) {
            for (ConfigurationListener configurationListener : configurationListeners) {
                configurationListener.configurationChanged();
            }
        }
    }

    public void disableActiveConfiguration() {
        setCurrent(PluginConfigurationBuilder.from(getCurrent())
                .withActiveLocation(null)
                .build(), true);
    }

    @NotNull
    public PluginConfiguration getCurrent() {
        final PluginConfigurationBuilder defaultConfig = PluginConfigurationBuilder.defaultConfiguration(project);
        return workspaceConfigurationState
                .populate(projectConfigurationState.populate(defaultConfig))
                .build();
    }

    public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration, final boolean fireEvents) {
        projectConfigurationState.setCurrentConfig(updatedConfiguration);
        workspaceConfigurationState.setCurrentConfig(updatedConfiguration);
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }

    public static PluginConfigurationManager getInstance(@NotNull final Project project) {
        PluginConfigurationManager result = testInstance;
        if (result == null) {
            result = ServiceManager.getService(project, PluginConfigurationManager.class);
        }
        return result;
    }

    public static void activateMock4UnitTesting(@Nullable final PluginConfigurationManager testingInstance) {
        PluginConfigurationManager.testInstance = testingInstance;
    }
}
