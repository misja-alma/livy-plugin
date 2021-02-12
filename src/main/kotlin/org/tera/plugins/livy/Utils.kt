package org.tera.plugins.livy

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.tera.plugins.livy.run.LivyProcessHandler
import java.lang.Exception
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Utils {
    // * %%configure -f
    //{
    //    "driverMemory": "20G",
    //    "executorMemory": "22G",
    //    "executorCores": 5,
    //    "numExecutors": 20,
    //    "name": "malma_sbb",
    //    "conf": {
    //        "spark.kubernetes.executor.request.cores": "5",
    //        "spark.kubernetes.container.image": "nexus-docker.local/spark-aws:v2.4.5-20200326-20200526",
    //        "spark.jars.packages": "net.teralytics:home-work-assembly:21.1.3+35-273efd21",
    //        "spark.sql.broadcastTimeout": "1200"
    //    }
    //}
    //             val sparkConfig: MutableMap<String, out Comparable<*>> = mutableMapOf(
    //                "spark.kubernetes.executor.request.cores" to 5,
    //                "spark.kubernetes.container.image" to "nexus-docker.local/spark-aws:v2.4.5-20200326-20200526",
    //                "nexus-docker.local/spark-aws:v2.4.5-20200326-20200526" to "net.teralytics:home-work-assembly:21.1.3+35-273efd21",
    //                "spark.sql.broadcastTimeout" to "1200"
    //            )

    fun parseSessionConfig(config: String): MutableMap<String, Any> {
        val json = JSONObject(config)
        return json.toMap()
    }

    fun startLivySession(client: OkHttpClient,
                         host: String,
                         config: String,
                         myProject: Project,
                         isCanceled: () -> Boolean,
                         logText: (String, ConsoleViewContentType) -> Unit): String? {
        val sparkConfig = parseSessionConfig(config)

        val myProgress = ProgressIndicatorProvider.getGlobalProgressIndicator()
        try {
            myProgress!!.setText("Starting Livy Session ..")

            val payload = JSONObject()
            // TODO get these from run config!
            payload.put("kind", "spark")
            payload.put("driverMemory", "20G")
            payload.put("executorMemory", "15G")
            payload.put("executorCores", 3)
            payload.put("numExecutors", 2)
            payload.put("name", Settings.newSessionName())
            payload.put("conf", sparkConfig)
            logText("Creating session ...\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
            val response = post(client, host + "/sessions", payload.toString(), logText)
            var result = response.body!!.string()
            logText(result + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
            if (!response.isSuccessful) {
                return null
            }

            val sessionLocation = response.header("location")!!
            var succes = response.isSuccessful
            response.close()

            val callbackUrl = host + sessionLocation

            // TODO reuse this polling loop
            while (succes && !isCanceled() && !result.contains("idle")) {

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
                    val id = jsonObject.getInt("id")
                    WindowManager.getInstance().getStatusBar(myProject).setInfo("Session " + id + " " + state)

                    result = responseText
                }
            }

            if (!isCanceled()) {
                myProgress!!.setText("Session started")
                val session = sessionLocation.split("/").last()
                return session
            } else {
                return null
            }
        } catch (ex: Exception) {
            myProgress!!.setText(ex.message)
            return null
        }
    }

    fun post(client: OkHttpClient, url: String, postBody: String, logText: (String, ConsoleViewContentType) -> Unit): Response {

        val request = Request.Builder()
            .url(url)
            .post(postBody.toRequestBody(LivyProcessHandler.MEDIA_TYPE_JSON))
            .build()

        val response = client.newCall(request).execute()
        logText("Sent 'POST' request: $postBody to URL : $url; Response Code : ${response.code} \n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        return response
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
}