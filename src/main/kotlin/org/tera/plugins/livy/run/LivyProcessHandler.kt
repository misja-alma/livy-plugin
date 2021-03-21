package org.tera.plugins.livy.run

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
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
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.json.JSONObject
import org.tera.plugins.livy.Utils
import org.tera.plugins.livy.settings.AppSettingsState
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

    private fun post(client: OkHttpClient, url: String, postBody: String): Response {
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
                val client = Utils.getUnsafeOkHttpClient(config.statementTimeout)
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
                        AppSettingsState.activeSession = sessionId
                        config.sessionId = sessionId
                    } else {
                        logText("Could not start new Livy session", ConsoleViewContentType.LOG_ERROR_OUTPUT)
                        notifyProcessTerminated(1)
                        return
                    }

                    updateRunConfiguration(project, oldSessionName, config)
                } else {
                    // TODO we could still check here that the old session is not dead
                    logText("Using existing session $sessionId\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                }

                logText("Executing statements ..\n", ConsoleViewContentType.LOG_INFO_OUTPUT)

                executeStatement(config, host, sessionId, client, myProgress)

                notifyProcessTerminated(0)
            }
        }

        ProgressManager.getInstance().run(task)
    }

    private fun updateRunConfiguration(
        project: Project,
        oldSessionName: @NotNull String,
        newConfig: RunConfiguration,
    ) {
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val oldRunConfig: RunnerAndConfigurationSettings? = runManager.findConfigurationByName(oldSessionName)
        if (oldRunConfig != null) {
            // TODO this sometimes takes effect really late ..
            // This is probably because the below method should be called when there is really a new runconfig!
            // In our case the run config is the same, and this is checked in the Idea UI
            runManager.removeConfiguration(oldRunConfig)
            runManager.fireRunConfigurationChanged(oldRunConfig)
        }
        val newRunConfig = RunnerAndConfigurationSettingsImpl(runManager, newConfig)
        runManager.addConfiguration(newRunConfig)
        runManager.fireRunConfigurationChanged(newRunConfig)
    }

    private fun executeStatement(
        config: LivyConfiguration,
        host: String,
        sessionId: Int?,
        client: OkHttpClient,
        myProgress: @Nullable ProgressIndicator?
    ) {
        val payload = JSONObject()
        payload.put("code", config.code)

        val url = "$host/sessions/$sessionId/statements"

        val response = post(client, url, payload.toString())
        var result = response.body!!.string()
        if (config.showRawOutput) {
            logText(result + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        }

        val callback = response.header("location")
        val callbackUrl = "$host$callback"
        var succes = response.isSuccessful
        response.close()

        // Poll Livy for Statement result until either the waiting is canceled or there is a result
        while (succes && !isCanceled && !statementFinished(result)) {
            myProgress?.checkCanceled() // Will throw a ProcessCanceledException if cancel button was pressed
            myProgress?.text = "Waiting for Statement Result .."

            Thread.sleep(500)

            val request = Request.Builder()
                .url(callbackUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                succes = response.isSuccessful
                val responseText = response.body!!.string()
                if (config.showRawOutput) {
                    logText(responseText + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                }
                val jsonObject = JSONObject(responseText)
                val state = jsonObject.getString("state")
                val progress = jsonObject.getDouble("progress")
                if (!succes && jsonObject.has("output")) {
                    // log intermediary output. Seems to never to contain anything though.
                    logText(jsonObject.get("output").toString() + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                }
                myProgress?.fraction = progress
                WindowManager.getInstance().getStatusBar(myProject).info =
                    state + ", progress: " + ((progress * 100).roundToInt()) + "%"
                result = responseText
            }
        }

        if (succes) {
            val jsonObject = JSONObject(result)
            val status = jsonObject.getJSONObject("output").getString("status")
            if (status != "error") {
                val data = jsonObject.getJSONObject("output").getJSONObject("data").getString("text/plain")

                logText(data + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
            } else {
                val error = jsonObject.getJSONObject("output").getString("evalue")
                logText("Error:\n$error\n\n", ConsoleViewContentType.LOG_ERROR_OUTPUT)

                val traceback = jsonObject.getJSONObject("output").get("traceback") // can be a string or an array ..
                logText(traceback.toString() + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT)
            }
            myProgress?.let { it.text = "Statement Finished" }
        } else {
            logText("Error while fetching Livy statement result", ConsoleViewContentType.LOG_ERROR_OUTPUT)
            Utils.eventLog("Livy Error", result, NotificationType.ERROR)
            myProgress?.let { it.text = "Error" }
        }
    }

    // TODO might need better check than just the string available, maybe some statements are available and some not yet?
    private fun statementFinished(responseBody: String): Boolean = responseBody.contains("available")

    fun logText(text: String, type: ConsoleViewContentType) {
        // TODO find out what this Key is used for, setting it to Error or Info doesn't seem to make any difference
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
