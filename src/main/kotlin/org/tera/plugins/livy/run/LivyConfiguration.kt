package org.tera.plugins.livy.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.tera.plugins.livy.settings.AppSettingsState
import org.tera.plugins.livy.settings.Settings

class LivyConfiguration(project: Project, factory: ConfigurationFactory, name: String?) : LocatableConfigurationBase<LivyOptions>(project, factory, name) {
    companion object {
        val defaultName = "Livy"
    }
    var host: String = AppSettingsState.instance.livyHost
    var sessionId: Int? = Settings.activeSession
    var code: String = ""
    // TODO maybe simply always store last configuration and copy its values
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
        return "$defaultName $sessionId"
    }
}

