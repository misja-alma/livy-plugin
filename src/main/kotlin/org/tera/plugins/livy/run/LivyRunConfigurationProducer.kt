package org.tera.plugins.livy.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.tera.plugins.livy.Settings

class LivyRunConfigurationProducer: LazyRunConfigurationProducer<LivyConfiguration>() {
    private val configFactory = LivyConfigurationFactory()

    // Note: this method is called already before the action is performed. I guess to be able to show run config name?
    // Because of this putting a breakpoint here or some blocking UI action will for some reason prevent the Run option to show up
    override fun createConfigurationFromContext(context: ConfigurationContext): ConfigurationFromContext? {
        val configuration = super.createConfigurationFromContext(context)
        if (configuration == null) {
            return null
        }

        // TODO find out how to make sure the run configs all are grouped under the 'Livy' folder in the run configs
        // TODO think of a better way to show a unique name. Or change name at the last moment?
        //val nextIndex = Settings.lastNameIndex + 1
        configuration!!.configuration.setName("Livy Run Selection")
        //Settings.lastNameIndex = nextIndex
        (configuration!!.configuration as LivyConfiguration).sessionId = Settings.activeSession
        (configuration!!.configuration as LivyConfiguration).host = Settings.activeHost

        return configuration
    }

    // Note: this method is called already before the action is performed. I guess to be able to show run config name?
    // Because of this putting a breakpoint here or some blocking UI action will for some reason prevent the Run option to show up
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
        //val nextIndex = Settings.lastNameIndex + 1
        // TODO find out how to make sure the run configs all are grouped under the 'Livy' folder in the run configs
        // TODO this doesn't work?!
        configuration.setName("Livy Run Selection")
        //Settings.lastNameIndex = nextIndex

        configuration.setGeneratedName()
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
        return false // Let 'normal' run configs come first
    }
}