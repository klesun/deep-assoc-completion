package org.klesun.deep_assoc_completion.entry;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DeepSettingsForm implements Configurable
{
    final private Project project;

    public DeepSettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    /** generated by GUI form editor: */
    private JPanel rootPanel;
    private JCheckBox bgTypePvdrEnabled;
    private JFormattedTextField bgTypePvdrDepthLimit;
    private JFormattedTextField bgTypePvdrTimeout;
    private JFormattedTextField explicitDepthLimit;
    private JFormattedTextField implicitDepthLimit;
    private JCheckBox removeUnusedImportsOnSaveEnabled;
    private JFormattedTextField totalExpressionLimit;
    private JCheckBox passArgsToImplementations;

    @Nls
    @Override
    public String getDisplayName() {
        return "deep-assoc-completion";
    }

    @Nullable
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return rootPanel;
    }

    @Override
    public boolean isModified() {
        return !getSettings().bgTypePvdrEnabled == bgTypePvdrEnabled.isSelected()
            || !getSettings().removeUnusedImportsOnSaveEnabled == removeUnusedImportsOnSaveEnabled.isSelected()
            || !getSettings().passArgsToImplementations == passArgsToImplementations.isSelected()
            || !getSettings().bgTypePvdrDepthLimit.toString().equals(bgTypePvdrDepthLimit.getText())
            || !getSettings().bgTypePvdrTimeout.toString().equals(bgTypePvdrTimeout.getText())
            || !getSettings().explicitDepthLimit.toString().equals(explicitDepthLimit.getText())
            || !getSettings().implicitDepthLimit.toString().equals(implicitDepthLimit.getText())
            || !getSettings().totalExpressionLimit.toString().equals(totalExpressionLimit.getText())
            ;
    }

    private Integer validateInt(JFormattedTextField field, int min, int max) throws ConfigurationException {
        Integer value = null;
        try {
            value = Integer.parseInt(field.getText());
        } catch (NumberFormatException numExc) {
            String msg = "Invalid integer value: " + field.getText() + " - " + numExc.getMessage();
            throw new ConfigurationException(msg);
        }
        if (value < min || value > max) {
            String msg = "Value " + field.getText() + " is out of range, it should be between " + min + " and " + max + " inclusive";
            throw new ConfigurationException(msg);
        } else {
            return value;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().bgTypePvdrEnabled = bgTypePvdrEnabled.isSelected();
        getSettings().passArgsToImplementations = passArgsToImplementations.isSelected();
        getSettings().bgTypePvdrDepthLimit = validateInt(bgTypePvdrDepthLimit, 0, 100);
        getSettings().bgTypePvdrTimeout = validateInt(bgTypePvdrTimeout, 5, 10000);
        getSettings().explicitDepthLimit = validateInt(explicitDepthLimit, 0, 100);
        getSettings().implicitDepthLimit = validateInt(implicitDepthLimit, 0, 100);
        getSettings().totalExpressionLimit = validateInt(totalExpressionLimit, 0, 1000000);
    }

    @Override
    public void reset() {
        bgTypePvdrEnabled.setSelected(getSettings().bgTypePvdrEnabled);
        removeUnusedImportsOnSaveEnabled.setSelected(getSettings().removeUnusedImportsOnSaveEnabled);
        passArgsToImplementations.setSelected(getSettings().passArgsToImplementations);
        bgTypePvdrDepthLimit.setText(getSettings().bgTypePvdrDepthLimit.toString());
        bgTypePvdrTimeout.setText(getSettings().bgTypePvdrTimeout.toString());
        explicitDepthLimit.setText(getSettings().explicitDepthLimit.toString());
        implicitDepthLimit.setText(getSettings().implicitDepthLimit.toString());
        totalExpressionLimit.setText(getSettings().totalExpressionLimit.toString());
    }

    public void disposeUIResources() {

    }

    private DeepSettings getSettings() {
        return DeepSettings.inst(this.project);
    }
}
