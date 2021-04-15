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
    var sessionConfig =
        """{        
"spark.kubernetes.executor.request.cores": "5",
"spark.kubernetes.container.image": "nexus-docker.local/spark-aws:v2.4.5-20200326-20200526",
"spark.jars.packages": "net.teralytics:home-work-assembly:21.1.3+35-273efd21",
"spark.sql.broadcastTimeout": "1200"
}"""

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
        var activeSession: Int? = null

        val instance: AppSettingsState
            get() = ServiceManager.getService(AppSettingsState::class.java)
    }
}