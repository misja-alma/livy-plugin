package org.tera.plugins.livy.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

class LivyConfigurationType: ConfigurationType {
    override fun getDisplayName(): String {
        return "Livy"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Livy Configuration Type"
    }

    override fun getIcon(): Icon {
        return AllIcons.RunConfigurations.Remote
    }

    override fun getId(): String {
        return "LivyRunConfiguration"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(LivyConfigurationFactory())
    }

    override fun isManaged(): Boolean {
        return false // Override to prevent that name change affects config id. This goes together with the getId override in LivyConfiguration
    }
}