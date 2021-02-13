package org.tera.plugins.livy.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.tera.plugins.livy.Settings

class LivyRunConfigurationProducer: LazyRunConfigurationProducer<LivyConfiguration>() {
    private val configFactory = LivyConfigurationFactory()

    override fun setupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val editors: Array<FileEditor> = FileEditorManager.getInstance(context.project).getSelectedEditors()
        val textEditor: TextEditor = editors.get(0) as TextEditor
        val caretModel: CaretModel = textEditor.editor.getCaretModel()
        val selectedText = caretModel.currentCaret.selectedText
        if (selectedText == null) {
            return false
        }

        configuration.code = selectedText
        // TODO find out how to make sure the run configs all are grouped under the 'Livy' folder in the run configs
        if (Settings.activeSession != null) {
            configuration.setName("Livy session " + Settings.activeSession)
        } else {
            configuration.setName("New Livy session")
        }

        configuration.sessionId = Settings.activeSession
        configuration.host = Settings.activeHost

        return true
    }

    override fun isConfigurationFromContext(configuration: LivyConfiguration, context: ConfigurationContext): Boolean {
        val editors: Array<FileEditor> = FileEditorManager.getInstance(context.project).getSelectedEditors()
        val textEditor: TextEditor = editors.get(0) as TextEditor
        val caretModel: CaretModel = textEditor.editor.getCaretModel()
        val selectedText = caretModel.currentCaret.selectedText
        return selectedText != null
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return configFactory
    }

    override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
        if (self == null) return false
        if (other == null) return true
        if (other.configuration is LivyConfiguration) return true
        return false // Let 'normal' run configs come first
    }

    // TODO check what to do with these ..

    override fun cloneTemplateConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings {
        return super.cloneTemplateConfiguration(context)
    }

    override fun createLightConfiguration(context: ConfigurationContext): RunConfiguration? {
        return super.createLightConfiguration(context)
    }

    override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
        // TODO check if exactly the same config already exists? Or should isPreferredConfiguration handle that?
        return null // Had to be overridden otherwise the name would never change
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