package org.tera.plugins.livy.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import org.json.JSONObject
import java.io.OutputStream
import java.lang.Exception
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

import okhttp3.RequestBody.Companion.toRequestBody
import org.tera.plugins.livy.Settings
import org.tera.plugins.livy.Utils


class LivyProcessHandler(project: Project, config: LivyConfiguration): ProcessHandler() {
    private val myProject = project
    private var isCanceled = false

    companion object {
        val MEDIA_TYPE_JSON = "application/json".toMediaType()
    }

    fun startLivySession(client: OkHttpClient, host: String, config: String): String? {
        return Utils.startLivySession(
            client,
            host,
            config,
            myProject,
            { isCanceled },
            {text: String, type: ConsoleViewContentType -> logText(text, type)})
    }


    fun post(client: OkHttpClient, url: String, postBody: String): Response {
        return Utils.post(client, url, postBody, { text: String, type: ConsoleViewContentType -> logText(text, type) })
    }


    private fun getUnsafeOkHttpClient(): OkHttpClient {
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
            .hostnameVerifier { _, _ -> true }.build()
    }

    init {
        val task = object:Task.Backgroundable(project, "Livy job", false) {
            override fun run(indicator: ProgressIndicator) {
                val client = getUnsafeOkHttpClient()
                val myProgress = ProgressIndicatorProvider.getGlobalProgressIndicator()

                val host = config.host
                var sessionId = config.sessionId
                if (sessionId == null || sessionId.isBlank()) {
                    sessionId = startLivySession(client, host, config.sessionConfig)
                    if (sessionId != null) {
                        Settings.activeSession = sessionId
                        config.sessionId = sessionId
                    } else {
                        return
                    }
                }

                val payload = JSONObject()
                payload.put("code", config.code)


                val url = "$host/sessions/$sessionId/statements"

                val response = post(client, url, payload.toString())
                var result = response.body!!.string()
                //logText(result + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                val callback = response.header("location")
                val callbackUrl = "$host$callback"
                var succes = response.isSuccessful
                response.close()

                while (succes && !isCanceled && !result.contains("available")) {

                    myProgress!!.setText("Waiting for Statement Result ..")

                    Thread.sleep(500)


                    val request = Request.Builder()
                        .url(callbackUrl)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        succes = response.isSuccessful
                        val responseText = response.body!!.string()
                        val jsonObject = JSONObject(responseText)
                        val state = jsonObject.getString("state")
                        val progress = jsonObject.getDouble("progress")
                        WindowManager.getInstance().getStatusBar(myProject).setInfo(state + ", progress: " + (Math.round(progress * 100)) + "%")

                        result = responseText
                    }
                }

                val jsonObject = JSONObject(result)
                val data = jsonObject.getJSONObject("output").getJSONObject("data").getString("text/plain")

                logText(data + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                myProgress!!.setText("Statement Finished")

                notifyProcessTerminated(0)
            }
        }

        ProgressManager.getInstance().run(task) // TODO or runProcess with a ProgressIndicator?
    }

    fun logText(text: String, type: ConsoleViewContentType) {
        // TODO find out what this Key should be used for
        notifyTextAvailable(text, com.intellij.openapi.util.Key.create<String>(type.toString()))
    }

    override fun destroyProcessImpl() {
        // TODO interrupt thread and throw terminate event?
        isCanceled = true
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        // TODO interrupt thread and throw terminate event?
        isCanceled = true
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean {
        return false
    }

    override fun getProcessInput(): OutputStream? {
        return null
    }
}