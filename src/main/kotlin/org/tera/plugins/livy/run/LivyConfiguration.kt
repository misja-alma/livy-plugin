package org.tera.plugins.livy.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.tera.plugins.livy.settings.AppSettingsState

class LivyConfiguration(project: Project, factory: ConfigurationFactory, name: String?) : LocatableConfigurationBase<LivyOptions>(project, factory, name) {
    companion object {
        const val defaultName = "New Livy Session"
    }
    var host: String = AppSettingsState.instance.livyHost
    var sessionId: Int? = null
    var code: String = ""
    var kind: String = "spark"
    var driverMemory: String = "20G"
    var executorMemory: String = "15G"
    var executorCores: Int = 3
    var numExecutors: Int = 2
    var sessionName: String = AppSettingsState.instance.generateSessionName()
    var statementTimeout: Int = 36000
    var showRawOutput: Boolean = false
    var sessionConfig: String = AppSettingsState.instance.sessionConfig

    init {
        isAllowRunningInParallel = true
        setGeneratedName()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return LivyState(environment)
    }

    override fun getConfigurationEditor(): LivySettingsEditor {
        return LivySettingsEditor()
    }

    override fun getId(): String {
        return this.hashCode().toString() // overridden to make sure that name changes don't change the id
    }

    override fun suggestedName(): String {
        return sessionName
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        element.getAttributeValue("host")?.let { host = it }
        element.getAttributeValue("sessionId")?.let { sessionId = it.toInt() }
        element.getAttributeValue("code")?.let { code = it }
        element.getAttributeValue("kind")?.let { kind = it }
        element.getAttributeValue("driverMemory")?.let { driverMemory = it }
        element.getAttributeValue("executorMemory")?.let { executorMemory = it }
        element.getAttributeValue("executorCores")?.let { executorCores = it.toInt() }
        element.getAttributeValue("numExecutors")?.let { numExecutors = it.toInt() }
        element.getAttributeValue("sessionName")?.let {
            sessionName = it
            name = it
        }
        element.getAttributeValue("statementTimeout")?.let { statementTimeout = it.toInt() }
        element.getAttributeValue("showRawOutput")?.let { showRawOutput = it.toBoolean() }
        element.getAttributeValue("sessionConfig")?.let { sessionConfig = it }
    }

    override fun writeExternal(element: Element) {
        element.setAttribute("host", host)
        sessionId?.let { element.setAttribute("sessionId", it.toString()) }
        element.setAttribute("code", code)
        element.setAttribute("kind", kind)
        element.setAttribute("driverMemory", driverMemory)
        element.setAttribute("executorMemory", executorMemory)
        element.setAttribute("executorCores", executorCores.toString())
        element.setAttribute("numExecutors", numExecutors.toString())
        element.setAttribute("sessionName", sessionName)
        element.setAttribute("statementTimeout", statementTimeout.toString())
        element.setAttribute("showRawOutput", showRawOutput.toString())
        element.setAttribute("sessionConfig", sessionConfig)

        super.writeExternal(element)
    }


    // TODO this is called when the run config editor is opened in the top bar or in the drop down with 'edit'. But this is not correct,
    // we're not editing the same config this way?!
    override fun clone(): RunConfiguration {
        return this
    }

    // Note that setGeneratedName has to be called to let this have effect
    override fun getActionName(): String? {
        if (sessionId == null) return defaultName else return super.getActionName()
    }
}

