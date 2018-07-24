package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

@State(
    name = "DeepAssocCompletionPluginSettings",
    storages = {
        @Storage(StoragePathMacros.WORKSPACE_FILE),
    }
)
public class DeepSettings implements PersistentStateComponent<DeepSettings> {
    public Boolean bgTypePvdrEnabled = false;
    public Integer bgTypePvdrDepthLimit = 5;
    public Integer bgTypePvdrTimeout = 100; // milliseconds
    public Integer explicitDepthLimit = 40;
    public Integer implicitDepthLimit = 25;
    public Integer totalExpressionLimit = 10000;
    public Boolean removeUnusedImportsOnSaveEnabled = false;

    public static DeepSettings inst(Project project) {
        return ServiceManager.getService(project, DeepSettings.class);
    }

    @Nullable
    @Override
    public DeepSettings getState() {
        return this;
    }

    @Override
    public void loadState(DeepSettings deepSettings) {
        XmlSerializerUtil.copyBean(deepSettings, this);
    }
}
