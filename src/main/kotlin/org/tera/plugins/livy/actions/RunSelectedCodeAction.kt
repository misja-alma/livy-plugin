package org.tera.plugins.livy.actions

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.CaretModel

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import java.lang.Exception
import java.net.URL
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.intellij.execution.ui.ConsoleViewContentType

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.wm.ToolWindow

import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import org.tera.plugins.livy.run.LivyConfigurationFactory
import org.json.JSONObject



class RunSelectedCodeAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        // disable certificate verification. TODO think of better workaround
        val trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
                object: X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate?>? {
                        return null
                    }

                    override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {
                    }

                    override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {
                    }
                }
        )

        // Install the all-trusting trust manager
        try {
            val sc: SSLContext = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val runManager = RunManagerEx.getInstanceEx(e.project!!);
        val runConfig = runManager.createConfiguration("Livy from Selection", LivyConfigurationFactory())
        // TODO open and show Editor with this config and the selected code in it
        // TODO use RuntimeConfigurationProducer instead!
        // see https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html#creating-configurations-from-context
        // isConfigurationFromContext() can be implemented for reuse

        // TODO this throws an error when no editor is 'active'. Not sure what activates an editor
        val editor: Editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel: CaretModel = editor.getCaretModel()
        val selectedText = caretModel.currentCaret.selectedText
        val payload = JSONObject()
        payload.put("code", selectedText)

        val toolWindow: ToolWindow? = ToolWindowManager.getInstance(e.project!!).getToolWindow("Livy")
        if (toolWindow == null) throw RuntimeException("No toolwindow!")
        val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(e.project!!).console
        val content: Content = toolWindow.getContentManager().getFactory().createContent(consoleView.getComponent(), "MyPlugin Output", false)
        toolWindow.getContentManager().addContent(content)

        val url = URL("https://livy-dev.service.ckd.dns.teralytics.net/sessions/467/statements")

        with(url.openConnection() as HttpsURLConnection) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true

            val jsonInputString: String = payload.toString()
            outputStream.use { os ->
                val input: ByteArray = jsonInputString.toByteArray(Charset.forName("utf-8"))
                os.write(input, 0, input.size)
            }

            consoleView.print("\nSent 'POST' request: " + jsonInputString + " to URL : $url; Response Code : $responseCode", ConsoleViewContentType.LOG_INFO_OUTPUT)

            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    consoleView.print(line, ConsoleViewContentType.LOG_INFO_OUTPUT)
                }
            }
        }
    }
}
