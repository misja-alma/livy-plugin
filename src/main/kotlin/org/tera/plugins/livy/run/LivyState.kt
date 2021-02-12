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
    //private var selectedText: String?
    private var myEnvironment: ExecutionEnvironment

    init {
        myEnvironment = environment
        val project = environment.getProject()
        val searchScope = GlobalSearchScopes.executionScope(project, environment.getRunProfile())
        myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope)

        // TODO move this code to a new Action, 'run selection in Livy'
//        val editors: Array<FileEditor> = FileEditorManager.getInstance(project).getSelectedEditors()
//        val textEditor: TextEditor = editors.get(0) as TextEditor
//        val caretModel: CaretModel = textEditor.editor.getCaretModel()
//        selectedText = caretModel.currentCaret.selectedText
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val console = myConsoleBuilder.getConsole()
        // TODO in the process handler, start the command on startNotify?
        // Send events for each text output and for termination
        //if (selectedText != null) {
            val runConfig = myEnvironment.runProfile as LivyConfiguration
            val handler = LivyProcessHandler(myEnvironment.getProject(), runConfig)
            console.attachToProcess(handler)

            return DefaultExecutionResult(console, handler)
//        } else {
//            throw RuntimeException("No code selected!")
//        }
    }
}