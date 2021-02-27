package org.tera.plugins.livy.run

import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.NotificationType
import org.json.JSONObject
import java.io.OutputStream

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

import org.tera.plugins.livy.Settings
import org.tera.plugins.livy.Utils


class LivyProcessHandler(project: Project, config: LivyConfiguration): ProcessHandler() {
    private val myProject = project
    private var isCanceled = false

    companion object {
        val MEDIA_TYPE_JSON = "application/json".toMediaType()
    }

    fun startLivySession(client: OkHttpClient, config: LivyConfiguration): Int? {
        return Utils.startLivySession(
            client,
            config,
            myProject,
            { isCanceled },
            {text: String, type: ConsoleViewContentType -> logText(text, type)})
    }


    fun post(client: OkHttpClient, url: String, postBody: String): Response {
        return Utils.post(client, url, postBody, { text: String, type: ConsoleViewContentType -> logText(text, type) })
    }

    init {
        val task = object:Task.Backgroundable(project, "Livy job", false) {
            override fun run(indicator: ProgressIndicator) {
                val client = Utils.getUnsafeOkHttpClient()
                val myProgress = ProgressIndicatorProvider.getGlobalProgressIndicator()

                val host = config.host
                var sessionId = config.sessionId
                if (sessionId == null) {
                    // TODO we should also check here that the session is not dead
                    sessionId = startLivySession(client, config)
                    if (sessionId != null) {
                        config.setName("Livy session " + sessionId)
                        Settings.activeSession = sessionId
                        config.sessionId = sessionId
                    } else {
                        notifyProcessTerminated(1)
                        return
                    }

                    val runManager = RunManagerImpl.getInstanceImpl(project)
                    val config = runManager.findConfigurationByName(config.name)
                    if (config != null) {
                        runManager.fireRunConfigurationChanged(config)
                    }
                } else {
                    logText("Using existing session " + sessionId +  "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
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

                if (succes) {
                    val jsonObject = JSONObject(result)
                    val data = jsonObject.getJSONObject("output").getJSONObject("data").getString("text/plain")

                    logText(data + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                    myProgress!!.setText("Statement Finished")
                } else {
                    Utils.eventLog("Livy Error", result, NotificationType.ERROR)
                    myProgress!!.setText("Error")
                }

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