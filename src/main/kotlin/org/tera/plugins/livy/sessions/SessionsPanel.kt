package org.tera.plugins.livy.sessions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBList
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.tera.plugins.livy.Settings
import org.tera.plugins.livy.Utils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel

// See FileTemplateTabAsList
// TODO a table would be better. Would enable configuring columns, copying values and eliminate the need for field labels in the UI
class SessionsPanel(toolWindow: ToolWindow) {
    private val refreshToolWindowButton: JButton = JButton("Refresh")
    private val deleteToolWindowButton: JButton = JButton("Delete Session")
    private val sessionsModel = DefaultListModel<Session>()
    private val sessionsList = JBList(sessionsModel)
    private val content: JPanel = JPanel(BorderLayout())

    private val client: OkHttpClient = Utils.getUnsafeOkHttpClient()


    init {
        deleteToolWindowButton.addActionListener { e: ActionEvent? -> deleteSelectedSessions() }
        refreshToolWindowButton.addActionListener { e: ActionEvent? -> refresh() }

        content.setPreferredSize(Dimension(600, 600))
        val bottomPanel = JPanel(FlowLayout())
        bottomPanel.add(refreshToolWindowButton)
        bottomPanel.add(deleteToolWindowButton)
        content.add(sessionsList, BorderLayout.CENTER)
        content.add(bottomPanel, BorderLayout.SOUTH)
        content.updateUI()
    }

    fun getContent(): JPanel {
        return content
    }

    // TODO maybe show popup with are you sure or so, progress bar, ... Maybe less needed with autorefresh of sessions
    private fun deleteSelectedSessions(): Boolean {
        sessionsList.selectedValuesList.forEach { s ->
            s.run {
                Utils.deleteSession(client, Settings.activeHost, s.id)
            }
        }
        return true
    }

    private fun refresh(): Boolean {
        val request = Request.Builder()
            .url(Settings.activeHost+"/sessions")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // TODO check if this works, use it also to log info stuff like requests
                val notification = Notification("Livy", null, NotificationType.ERROR) //groupId is important for further settings
                Notifications.Bus.notify(notification)
                return true
            }

            val responseText = response.body!!.string()

            val responseObject = JSONObject(responseText)
            val jsonSessions: JSONArray = responseObject.getJSONArray("sessions")
            val sessions = jsonSessions.map { s: Any ->
                val jsonObject = s as JSONObject
                val state = jsonObject.getString("state")
                val id = jsonObject.getInt("id")
                val name = jsonObject.optString("name")
                val appId = jsonObject.getString("appId")
                val sparkUIUrl = jsonObject.getJSONObject("appInfo").optString("sparkUiUrl")
                Session(id, name, state, appId, sparkUIUrl)
            }

            // TODO run in thread and invoke in IntelliJ/SwingUtils.invokeLater?
            sessionsModel.removeAllElements()
            for (session in sessions) {
                sessionsModel.addElement(session)
            }
            // TODO how to fire contents changed? Also maybe be more precise with replacing/updating
        }

        return true
    }
}