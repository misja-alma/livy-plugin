package org.tera.plugins.livy

object Settings {
    var activeSession: String? = null
    var activeHost: String = "https://livy-dev.service.ckd.dns.teralytics.net"
    var lastNameIndex = 2
    var sessionName = "malma_rest_test"

    fun newSessionName(): String {
        val currentTime = System.currentTimeMillis()
        return sessionName + "_" + currentTime
    }
}