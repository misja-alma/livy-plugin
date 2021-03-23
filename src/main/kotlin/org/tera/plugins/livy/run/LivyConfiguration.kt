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
        val defaultName = "New Livy Session"
    }
    var host: String = AppSettingsState.instance.livyHost
    var sessionId: Int? = AppSettingsState.activeSession
    var code: String = ""
    var kind: String = "spark"
    var driverMemory: String = "20G"
    var executorMemory: String = "15G"
    var executorCores: Int = 3
    var numExecutors: Int = 2
    var sessionName: String = AppSettingsState.instance.generateSessionName()
    var statementTimeout: Int = 36000
    var showRawOutput: Boolean = false
    var sessionConfig: String =
        """{        
"spark.kubernetes.executor.request.cores": "5",
"spark.kubernetes.container.image": "nexus-docker.local/spark-aws:v2.4.5-20200326-20200526",
"spark.jars.packages": "net.teralytics:home-work-assembly:21.1.3+35-273efd21",
"spark.sql.broadcastTimeout": "1200"
}"""

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
        return if (sessionId == null) defaultName else "Livy Session $sessionId"
    }

    override fun readExternal(element: Element) {
        super<LocatableConfigurationBase>.readExternal(element)

        element.getAttributeValue("host")?.let { host = it }
        element.getAttributeValue("sessionId")?.let { sessionId = it.toInt() }
        element.getAttributeValue("code")?.let { code = it }
        element.getAttributeValue("kind")?.let { kind = it }
        element.getAttributeValue("driverMemory")?.let { driverMemory = it }
        element.getAttributeValue("executorMemory")?.let { executorMemory = it }
        element.getAttributeValue("executorCores")?.let { executorCores = it.toInt() }
        element.getAttributeValue("numExecutors")?.let { numExecutors = it.toInt() }
        element.getAttributeValue("sessionName")?.let { sessionName = it }
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

        super<LocatableConfigurationBase>.writeExternal(element)
    }

    override fun clone(): RunConfiguration {
        val result = super.clone() as LivyConfiguration
        // Make sure new sessions get a unique session name
        if (result.sessionId == null) result.sessionName = AppSettingsState.instance.generateSessionName()
        return result
    }
}

