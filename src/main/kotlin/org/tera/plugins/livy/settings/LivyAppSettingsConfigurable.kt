package org.tera.plugins.livy.settings

import com.intellij.openapi.options.Configurable;
import javax.swing.JComponent

/**
 * Manages the persistent settings of the Livy plugin
 */
class LivyAppSettingsConfigurable: Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String {
        return "Livy Settings"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings: AppSettingsState = AppSettingsState.instance
        var modified: Boolean = !mySettingsComponent!!.hostNameText.equals(settings.livyHost)
        modified = modified or (mySettingsComponent!!.sessionPrefix != settings.sessionPrefix)
        modified = modified or (mySettingsComponent!!.sessionConfig != settings.sessionConfig)
        return modified
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.instance
        settings.livyHost = mySettingsComponent!!.hostNameText
        settings.sessionPrefix = mySettingsComponent!!.sessionPrefix
        settings.sessionConfig = mySettingsComponent!!.sessionConfig
    }

    override fun reset() {
        val settings: AppSettingsState = AppSettingsState.instance
        mySettingsComponent?.hostNameText = settings.livyHost
        mySettingsComponent?.sessionPrefix = settings.sessionPrefix
        mySettingsComponent?.sessionConfig = settings.sessionConfig
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}