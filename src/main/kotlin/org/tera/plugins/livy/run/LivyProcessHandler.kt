package org.tera.plugins.livy.run

import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.tera.plugins.livy.Settings
import org.tera.plugins.livy.Utils
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * This class is instantiated by Idea when a Livy run config should be executed:
 * this class sends a statement request to Livy and creates a new session if no active session is selected in the config.
 */
class LivyProcessHandler(project: Project, config: LivyConfiguration) : ProcessHandler() {
    private val myProject = project
    private var isCanceled = false

    companion object {
        val MEDIA_TYPE_JSON = "application/json".toMediaType()
    }

    fun startLivySession(client: OkHttpClient, config: LivyConfiguration): Int? {
        return Utils.startLivySession (
            client,
            config,
            myProject,
            { isCanceled },
            { text: String, type: ConsoleViewContentType -> logText(text, type) }
        )
    }

    fun post(client: OkHttpClient, url: String, postBody: String): Response {
        return Utils.post(client, url, postBody) { text: String, type: ConsoleViewContentType -> logText(text, type) }
    }

    init {
        val task = object:Task.Backgroundable(project, "Livy job", true) {

            override fun run(indicator : ProgressIndicator) {
                try {
                    doRun(indicator)
                } catch (ex: Exception) {
                    Utils.eventLog("Uncaught Exception", ex.toString(), NotificationType.ERROR)
                    notifyProcessTerminated(1)
                }
            }

            private fun doRun(indicator : ProgressIndicator) {
                val client = Utils.getUnsafeOkHttpClient()
                val myProgress = ProgressIndicatorProvider.getGlobalProgressIndicator()

                val host = config.host
                var sessionId = config.sessionId
                if (sessionId == null) {
                    sessionId = startLivySession(client, config)
                    val oldSessionName = config.name
                    if (sessionId != null) {
                        if (config.name == LivyConfiguration.defaultName) {
                            config.name = "${LivyConfiguration.defaultName} $sessionId"
                        }
                        Settings.activeSession = sessionId
                        config.sessionId = sessionId
                    } else {
                        logText("Could not start new Livy session", ConsoleViewContentType.LOG_ERROR_OUTPUT)
                        notifyProcessTerminated(1)
                        return
                    }

                    val runManager = RunManagerImpl.getInstanceImpl(project)
                    val runConfig = runManager.findConfigurationByName(oldSessionName)
                    if (runConfig != null) {
                        runManager.fireRunConfigurationChanged(runConfig)
                    }
                } else {
                    // TODO we could still check here that the old session is not dead
                    logText("Using existing session $sessionId\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                }

                val payload = JSONObject()
                payload.put("code", config.code)


                val url = "$host/sessions/$sessionId/statements"

                val response = post(client, url, payload.toString())
                var result = response.body!!.string()

                val callback = response.header("location")
                val callbackUrl = "$host$callback"
                var succes = response.isSuccessful
                response.close()

                while (succes && !isCanceled && !result.contains("available")) {
                    myProgress!!.checkCanceled() // Will throw a ProcessCanceledException if cancel button was pressed
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
                        WindowManager.getInstance().getStatusBar(myProject).setInfo(state + ", progress: " + ((progress * 100).roundToInt()) + "%")

                        result = responseText
                    }
                }

                if (succes) {
                    val jsonObject = JSONObject(result)
                    val data = jsonObject.getJSONObject("output").getJSONObject("data").getString("text/plain")

                    logText(data + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                    myProgress?.let { it.text = "Statement Finished" }
                } else {
                    logText("Error while fetching Livy statement result", ConsoleViewContentType.LOG_ERROR_OUTPUT)
                    Utils.eventLog("Livy Error", result, NotificationType.ERROR)
                    myProgress?.let { it.text = "Error" }
                }

                notifyProcessTerminated(0)
            }
        }

        ProgressManager.getInstance().run(task) // TODO or runProcess with a ProgressIndicator?
    }

    fun logText(text: String, type: ConsoleViewContentType) {
        // TODO find out what this Key is used for
        notifyTextAvailable(text, com.intellij.openapi.util.Key.create<String>(type.toString()))
    }

    override fun destroyProcessImpl() {
        isCanceled = true
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
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
