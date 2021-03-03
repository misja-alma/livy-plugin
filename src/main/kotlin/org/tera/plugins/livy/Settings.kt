package org.tera.plugins.livy

object Settings {
    var activeSession: Int? = null
    // TODO persist and reload these settings, maybe add panel
    var activeHost: String = "https://livy-dev.service.ckd.dns.teralytics.net"
    var sessionPrefix = "malma_idea"

    fun generateSessionName(): String {
        val currentTime = System.currentTimeMillis()
        return sessionPrefix + "_" + currentTime
    }
}