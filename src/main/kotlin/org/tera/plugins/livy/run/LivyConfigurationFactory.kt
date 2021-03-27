package org.tera.plugins.livy.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class LivyConfigurationFactory : ConfigurationFactory(LivyConfigurationType()) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return LivyConfiguration(project, this, LivyConfiguration.defaultName)
    }

    override fun getName(): String {
        return "Livy Configuration Factory"
    }

    override fun getId(): String {
        return "Livy Configuration Factory"
    }

    override fun createConfiguration(name: String?, template: RunConfiguration): RunConfiguration {
        return LivyConfiguration(template.project, this, name)
    }
}
