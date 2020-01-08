package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.klesun.lang.Opt;

import static org.klesun.lang.Lang.opt;
import static org.klesun.lang.Lang.som;

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
    public Integer explicitDepthLimit = 55;
    public Integer implicitDepthLimit = 30;
    public Integer totalExpressionLimit = 7500;
    public Integer usageBasedCompletionDepthLimit = 3;
    public Boolean removeUnusedImportsOnSaveEnabled = false;
    public Boolean passArgsToImplementations = false;
    public Boolean enableMemberCompletion = true;

    public static DeepSettings inst(Project project) {
        return Opt.fst(
            () -> opt(ServiceManager.getService(project, DeepSettings.class)),
            () -> som(new DeepSettings())
        ).unw();
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
