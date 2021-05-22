package org.tera.plugins.livy.run

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.registry.Registry
import org.tera.plugins.livy.settings.AppSettingsState

class NewLivySession: AnAction() {
    override fun update(e: AnActionEvent) {
        // Only show the item if there is already an action session. If not, then the run producer will already take its place.
        e.presentation.isEnabledAndVisible = AppSettingsState.activeSession != null
    }

    // See RunContextAction.perform
    override fun actionPerformed(e: AnActionEvent) {
        val context = ConfigurationContext.getFromContext(e.dataContext)
        val runManager = context.getRunManager() as RunManagerImpl

        val config = LivyConfiguration(e.project!!, LivyConfigurationFactory(), LivyConfiguration.defaultName)
        val editor = CommonDataKeys.EDITOR.getData(context.dataContext)
        val selectedText = editor?.caretModel?.currentCaret?.selectedText
        if (selectedText == null) {
            return
        }
        config.code = selectedText

        val configuration = RunnerAndConfigurationSettingsImpl(runManager, config)

        runManager.setTemporaryConfiguration(configuration) // TODO check what does this do?

        if (Registry.`is`("select.run.configuration.from.context")) {
            runManager.selectedConfiguration = configuration // TODO check what does this do?
        }
        val myExecutor: Executor = DefaultRunExecutor()

        ExecutionUtil.doRunConfiguration(configuration, myExecutor, null, null, e.getDataContext())
    }
}

