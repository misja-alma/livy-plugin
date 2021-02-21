package org.tera.plugins.livy.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class LivyConfigurationFactory: ConfigurationFactory(LivyConfigurationType()) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return LivyConfiguration(project, this, "Livy")
    }

    override fun getName(): String {
        return "Livy Configuration Factory"
    }

    // TODO
    //  The default implementation of ConfigurationFactory.getId is deprecated, you need to override it in org.tera.plugins.livy.run.LivyConfigurationFactory.
}