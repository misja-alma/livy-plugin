package org.tera.plugins.livy.settings

// TODO remove and use AppSettingsState. Maybe also for activeSession?
object Settings {
    var activeSession: Int? = null
   // var activeHost: String = "https://livy-dev.service.ckd.dns.teralytics.net"
   // var sessionPrefix = "${System.getProperty("user.name")}_idea"

//    fun generateSessionName(): String {
//        val currentTime = System.currentTimeMillis()
//        return sessionPrefix + "_" + currentTime
//    }
}