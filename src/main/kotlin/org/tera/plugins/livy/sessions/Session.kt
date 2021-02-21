package org.tera.plugins.livy.sessions

data class Session(val id: Int, val name: String?, val state: String, val appId: String, val sparkUIUrl: String?) {
}