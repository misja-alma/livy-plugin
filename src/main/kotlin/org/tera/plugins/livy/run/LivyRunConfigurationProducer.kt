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

/**
 * The class responsible for showing the 'Run Livy Session ..' option in the action menu
 *
 * TODO Livy run config appears late in the context menu and sometimes disappears again later
 * This is due to how Intellij populates its lazy context popup.
 * See com.intellij.openapi.actionSystem.impl.Utils for initialisation logic.
 *
 * Registry.intValue("run.configuration.update.timeout") => initial 100 ms
 * this times out all the time and prevents the Livy action from showing so one solution is to increase it
 * Also during indexing the action will not show up.
*/
class LivyRunConfigurationProducer : LazyRunConfigurationProducer<LivyConfiguration>() {
    private val configFactory = LivyConfigurationFactory()

    override fun setupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        return doSetupConfigurationFromContext(configuration, context)
    }

    /**
     * This is called by IntelliJ when the user invokes the context menu; if this returns true then the livy
     * config option is shown in the popup.
     * The implementation checks if any text is selected and if it is, it sets up the config and returns true.
     */
    private fun doSetupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext
    ): Boolean {

        // TODO check if this can be done via context.psiElement, that might be faster
        val editors: Array<FileEditor> = FileEditorManager.getInstance(context.project).getSelectedEditors()
        val textEditor: TextEditor = editors.get(0) as TextEditor
        val caretModel: CaretModel = textEditor.editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        if (selectedText == null) {
            return false
        }

        configuration.code = selectedText
        if (Settings.activeSession != null) {
            configuration.name = "Livy session " + Settings.activeSession
        } else {
            configuration.setName("New Livy session")
        }

        configuration.sessionId = Settings.activeSession
        configuration.host = Settings.activeHost

        return true
    }

    /**
     * The idea is to always use the same run configuration regardless if the selected text changed. So we
     * only check that any text is selected at all.
     * As an alternative we could check if the selected text changed, maybe we could make this configurable.
     */
    override fun isConfigurationFromContext(configuration: LivyConfiguration, context: ConfigurationContext): Boolean {
        // TODO check if this can be done via context.psiElement
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
        return super.shouldReplace(self, other)
    }
}
