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

class LivyRunConfigurationProducer: LazyRunConfigurationProducer<LivyConfiguration>() {
    private val configFactory = LivyConfigurationFactory()

    override fun setupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        return doSetupConfigurationFromContext(configuration, context)
    }

    fun doSetupConfigurationFromContext(
        configuration: LivyConfiguration,
        context: ConfigurationContext
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
        val result = super.findExistingConfiguration(context)
        if (result != null && result.configuration is LivyConfiguration) {
            doSetupConfigurationFromContext(result.configuration as LivyConfiguration, context)
        }

        // TODO check these! This one keeps the list of run configs!
        // Also we add add configs here.
        val runManager = RunManagerImpl.getInstanceImpl(context.project)
        //runManager.fireRunConfigurationChanged()
        // Each config in the list seem to be of type RunnerAndConfigurationsettings
        //runManager.addConfiguration()

        // TODO
        // What seems to happen is that Idea checks after this method is:
        // If it is null, Idea adds the config to the dropdown under a new 'Livy' folder. Check how to reuse this folder!
        // If it is not null, Idea will try to select it in the dropdown. But if the name changed, it can't do it
        // (does it compare by reference?) so then 'new configuration..' is shown.

        // When renaming the livy name, now that the id is the name, this error is shown:
        // java.lang.Throwable: Livy.Livy session 572 must be added before selecting
        //	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:146)
        //	at com.intellij.execution.impl.RunManagerImpl.setSelectedConfiguration(RunManagerImpl.kt:528)
        // which probably means that when renaming, we have to remove the old one first and add the new renamed one
        
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