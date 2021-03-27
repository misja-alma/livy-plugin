package org.tera.plugins.livy.run

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.tera.plugins.livy.settings.AppSettingsState

/**
 * The class responsible for showing the 'Run Livy Session ..' option in the action menu
 *
 * Note Livy run config appears late in the context menu and sometimes disappears again later
 * This is due to how Intellij populates its lazy context popup.
 * See com.intellij.openapi.actionSystem.impl.Utils for initialisation logic.
 * Intellij bug ticket: https://youtrack.jetbrains.com/issue/IDEA-206889?_ga=2.182916198.1332934996.1615203001-488187979.1603277938
 *
 * Registry.intValue("run.configuration.update.timeout") => initial 100 ms
 * This times out all the time and prevents the Livy action from showing so one solution is to increase it but it
 * would have to be increased quite a lot.
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
        val editor = CommonDataKeys.EDITOR.getData(context.dataContext)
        val selectedText = editor?.caretModel?.currentCaret?.selectedText
        if (selectedText == null) {
            // Currently, if returning true, the Livy Config somehow takes precedence over other run configs
            // in some places where it shouldn't, such as e.g. in the sidebar.
            // Therefore as a workaround Livy configs are only generated when any text is selected.
            return false
        }

        configuration.code = selectedText
        configuration.sessionId = AppSettingsState.activeSession
        configuration.name = configuration.suggestedName()
        configuration.host = AppSettingsState.instance.livyHost

        return true
    }

    override fun isConfigurationFromContext(configuration: LivyConfiguration, context: ConfigurationContext): Boolean {
        val editor = CommonDataKeys.EDITOR.getData(context.dataContext)
        val selectedText = editor?.caretModel?.currentCaret?.selectedText

        return selectedText != null
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return configFactory
    }

    /**
     * Let any other runConfiguration take precedence
     */
    override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
        if (self == null) return false
        if (other == null) return true
        return self.configuration !is LivyConfiguration
    }

    override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
        val result = super.findExistingConfiguration(context)
        if (result != null && result.configuration is LivyConfiguration) {
            doSetupConfigurationFromContext(result.configuration as LivyConfiguration, context)
        }

        return result
    }
}
