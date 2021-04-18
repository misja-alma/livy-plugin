package org.tera.plugins.livy.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

class LivyConfigurationType : ConfigurationType {
    private val version = "0.0.3"

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
        return "LivyRunConfiguration-$version"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(LivyConfigurationFactory())
    }

    override fun isManaged(): Boolean {
        // Note but because this is false, user can't change the name.
        // Overridden to prevent that name change affects config id. This goes together with the getId override in LivyConfiguration
        // Now that it is true, the effect is that a new config that changes to config for session, is not selected in the run configs dropdown
        // Apparently for managed (i.e. user defined) names, Idea tries to find the runconfig by name in the dropdown
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LivyConfigurationType) return false
        return other.version == version
    }

    override fun hashCode(): Int {
        return version.hashCode()
    }
}
