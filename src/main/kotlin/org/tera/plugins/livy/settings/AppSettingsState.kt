package org.tera.plugins.livy.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(name = "org.tera.plugins.livy.settings.AppSettingsState", storages = [Storage("LivyPlugin.xml")])
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var livyHost = "https://livy-dev.service.ckd.dns.teralytics.net"
    var sessionPrefix = "${System.getProperty("user.name")}_idea"

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun generateSessionName(): String {
        val currentTime = System.currentTimeMillis()
        return sessionPrefix + "_" + currentTime
    }

    companion object {
        val instance: AppSettingsState
            get() = ServiceManager.getService(AppSettingsState::class.java)
    }
}