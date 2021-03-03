package org.tera.plugins.livy.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.tera.plugins.livy.Settings

// TODO Livy run config appears late in the context menu and sometimes disappears again later
// See com.intellij.openapi.actionSystem.impl.Utils for initialisation logic.
// Registry.intValue("run.configuration.update.timeout") => initial 100!
// so maybe this times out all the time and prevents the Livy action from showing
class LivyRunConfigurationProducer : LazyRunConfigurationProducer<LivyConfiguration>() {
    private val configFactory = LivyConfigurationFactory()

    override fun setupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        return doSetupConfigurationFromContext(configuration, context)
    }

    private fun doSetupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val editors: Array<FileEditor> = FileEditorManager.getInstance(context.project).getSelectedEditors()
        val textEditor: TextEditor = editors.get(0) as TextEditor
        val caretModel: CaretModel = textEditor.editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        if (selectedText == null) {
            return false
        }

        // TODO this doesn't belong here! We only need to take care of this when something is actually run
        //var configurationChanged = false

        configuration.code = selectedText
        // TODO find out how to make sure the run configs all are grouped under the 'Livy' folder in the run configs
        //      Or are new folders only created for new versions of the plugin?
        if (Settings.activeSession != null) {
            configuration.name = "Livy session " + Settings.activeSession
//            if (configuration.sessionId != Settings.activeSession) {
//                configurationChanged = true
//            }
        } else {
            configuration.setName("New Livy session")
        }

        configuration.sessionId = Settings.activeSession
        configuration.host = Settings.activeHost

//
//        if (configurationChanged) {
//            val runManager = RunManagerImpl.getInstanceImpl(context.project)
//            val config = runManager.findConfigurationByName(configuration.name)
//            if (config != null) {
//                runManager.fireRunConfigurationChanged(config)
//            }
//        }

        return true
    }

    /**
     * The idea is to always use the same run configuration regardless if the selected text changed. So we
     * only check that any text is selected at all.
     * As an alternative we could check if the selected text changed, maybe we could make this configurable.
     * TODO maybe we should make the file type be configurable for which Livy configs can be run
     */
    override fun isConfigurationFromContext(configuration: LivyConfiguration, context: ConfigurationContext): Boolean {
        val editors: Array<FileEditor> = FileEditorManager.getInstance(context.project).getSelectedEditors()
        val textEditor: TextEditor = editors.get(0) as TextEditor
        val caretModel: CaretModel = textEditor.editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        return selectedText != null
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return configFactory
    }

    override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
        if (self == null) return false
        if (other == null) return true
        return self.configuration !is LivyConfiguration
        // Trying to let other configs always take precedence. Maybe make this configurable?
    }

    override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
        val result = super.findExistingConfiguration(context)
        if (result != null && result.configuration is LivyConfiguration) {
            doSetupConfigurationFromContext(result.configuration as LivyConfiguration, context)
        }

        // TODO check these! This one keeps the list of run configs!
        // Also we add add configs here.
        val runManager = RunManagerImpl.getInstanceImpl(context.project)
        // See also:
        // runManager.fireRunConfigurationChanged()
        // runManager.addConfiguration()
        // Each config in the list seem to be of type RunnerAndConfigurationsettings
        // Check where the folder structure is kept

        return result
    }

    override fun findOrCreateConfigurationFromContext(context: ConfigurationContext): ConfigurationFromContext? {
        return super.findOrCreateConfigurationFromContext(context)
    }

    override fun getConfigurationSettingsList(runManager: RunManager): MutableList<RunnerAndConfigurationSettings> {
        return super.getConfigurationSettingsList(runManager)
    }

    override fun onFirstRun(
        configuration: ConfigurationFromContext,
        context: ConfigurationContext,
        startRunnable: Runnable
    ) {
        super.onFirstRun(configuration, context, startRunnable)
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        // TODO override?
        return super.shouldReplace(self, other)
    }
}
