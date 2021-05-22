package org.tera.plugins.livy.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.psi.search.GlobalSearchScopes

class LivyState(environment: ExecutionEnvironment): RunProfileState {

    private var myConsoleBuilder: TextConsoleBuilder
    private var myEnvironment: ExecutionEnvironment = environment

    init {
        val project = environment.project
        val searchScope = GlobalSearchScopes.executionScope(project, environment.runProfile)
        myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val console = myConsoleBuilder.console
        val runConfig = myEnvironment.runProfile as LivyConfiguration
        val handler = LivyProcessHandler(myEnvironment.project, runConfig)
        console.attachToProcess(handler)

        return DefaultExecutionResult(console, handler)
    }
}