package org.tera.plugins.livy

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.tera.plugins.livy.run.LivyConfiguration
import org.tera.plugins.livy.run.LivyProcessHandler
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Utils {
    val log = Logger.getInstance(Utils.javaClass)

    private fun parseSessionConfig(config: String): MutableMap<String, Any> {
        val json = JSONObject(config)
        return json.toMap()
    }

    fun deleteSession(client: OkHttpClient,
                      host: String,
                      id: Int): Boolean {
        val request = Request.Builder()
            .url("$host/sessions/$id")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    fun startLivySession(client: OkHttpClient,
                         config: LivyConfiguration,
                         myProject: Project,
                         isCanceled: () -> Boolean,
                         logText: (String, ConsoleViewContentType) -> Unit): Int? {
        val sparkConfig = parseSessionConfig(config.sessionConfig)

        val myProgress = ProgressIndicatorProvider.getGlobalProgressIndicator()
        try {
            myProgress?.text = "Starting Livy Session .."

            val payload = JSONObject()
            payload.put("kind", config.kind)
            payload.put("driverMemory", config.driverMemory)
            payload.put("executorMemory", config.executorMemory)
            payload.put("executorCores", config.executorCores)
            payload.put("numExecutors", config.numExecutors)
            payload.put("name", config.sessionName)
            payload.put("conf", sparkConfig)

            logText("Creating session ...\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
            var (result, sessionLocation, success) =
                post(client, config.host + "/sessions", payload.toString(), logText).use { response ->
                    val result = response.body!!.string()
                    log.debug(result)
                    if (!response.isSuccessful) {
                        return null
                    }

                    val sessionLocation = response.header("location")!!

                    Triple(result, sessionLocation, response.isSuccessful)
                }

            val callbackUrl = config.host + sessionLocation

            while (success && !isCanceled() && !result.contains("idle") && !result.contains("dead")) {

                Thread.sleep(500)

                val request = Request.Builder()
                    .url(callbackUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    success = response.isSuccessful
                    val responseText = response.body!!.string()

                    val jsonObject = JSONObject(responseText)
                    val state = jsonObject.getString("state")
                    val id = jsonObject.getInt("id")
                    WindowManager.getInstance().getStatusBar(myProject).info = "Session $id $state"

                    result = responseText
                }
            }

            return if (!isCanceled() && result.contains("idle")) {
                myProgress?.let { it.text = "Session started" }
                val session = sessionLocation.split("/").last()
                session.toInt()
            } else {
                if (!isCanceled()) {
                    myProgress?.let { it.text = "Error while starting session" }
                }
                null
            }
        } catch (ex: Exception) {
            eventLog("Livy connection error", ex.toString(), NotificationType.ERROR)
            myProgress?.let { it.text = ex.message }
            return null
        }
    }

    fun post(client: OkHttpClient, url: String, postBody: String, logText: (String, ConsoleViewContentType) -> Unit): Response {

        val request = Request.Builder()
            .url(url)
            .post(postBody.toRequestBody(LivyProcessHandler.MEDIA_TYPE_JSON))
            .build()

        val response = client.newCall(request).execute()
        val msg = "Sent 'POST' request: $postBody to URL : $url; Response Code : ${response.code} \n"
        log.debug(msg)
        return response
    }

    @Suppress("UnresolvedPluginConfigReference")
    fun eventLog(title: String, msg: String, notificationType: NotificationType) {
        val notification = Notification(
            "Livy",
            title,
            msg,
            notificationType
        )
        Notifications.Bus.notify(notification)
//        notification.getBalloon()?.hide() // TODO this doesn't work
    }

    fun getUnsafeOkHttpClient(callTimeoutSec: Int): OkHttpClient {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .callTimeout(Duration.ofSeconds(callTimeoutSec.toLong()))
            .hostnameVerifier { _, _ -> true }.build()
    }
}
