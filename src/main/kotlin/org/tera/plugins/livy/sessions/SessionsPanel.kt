package org.tera.plugins.livy.sessions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBScrollPane
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jdesktop.swingx.JXTable
import org.json.JSONArray
import org.json.JSONObject
import org.tera.plugins.livy.settings.Settings
import org.tera.plugins.livy.Utils
import org.tera.plugins.livy.settings.AppSettingsState
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

/**
 * The panel showing the list of running Livy sessions
 */
class SessionsPanel() {
    private val refreshToolWindowButton: JButton = JButton("Refresh")
    private val columnNames = Vector(listOf("Id", "Name", "State", "AppId", "SparkUIUrl"))
    private val deleteToolWindowButton: JButton = JButton("Delete Session")
    private val sessionsModel = object : DefaultTableModel(columnNames, 0) {
        override fun setValueAt(aValue: Any?, row: Int, column: Int) {
        }
    }
    private val sessionsTable = JXTable(sessionsModel)
    private val content: JPanel = JPanel(BorderLayout())

    private val client: OkHttpClient = Utils.getUnsafeOkHttpClient(60)

    init {
        deleteToolWindowButton.addActionListener { e: ActionEvent? -> deleteSelectedSessions() }
        refreshToolWindowButton.addActionListener { e: ActionEvent? -> refresh() }

        val bottomPanel = JPanel(FlowLayout())
        bottomPanel.add(refreshToolWindowButton)
        bottomPanel.add(deleteToolWindowButton)

        sessionsTable.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        sessionsTable.autoCreateColumnsFromModel = false

        val scroll = JBScrollPane()
        scroll.setViewportView(sessionsTable)
        content.add(scroll, BorderLayout.CENTER)
        content.add(bottomPanel, BorderLayout.SOUTH)
        content.updateUI()
    }

    fun getContent(): JPanel {
        return content
    }

    // TODO maybe show popup with are you sure or so, progress bar/ hour glass, ... Maybe less needed with autorefresh of sessions
    private fun deleteSelectedSessions(): Boolean {
        // TODO look up sessionId dynamically
        val selectedSessions = sessionsTable.selectedRows.map { sessionsModel.getValueAt(it, 0) as Int }

        val deleteAction = Runnable {
            selectedSessions.forEach { deleteThis ->
                deleteThis.run {
                    Utils.deleteSession(client, AppSettingsState.instance.livyHost, deleteThis)
                    if (Settings.activeSession == this) Settings.activeSession = null
                }
            }
        }

        ApplicationManager.getApplication().invokeLater(deleteAction)

        return refresh()
    }

    private fun refresh(): Boolean {
        val request = Request.Builder()
            .url(AppSettingsState.instance.livyHost + "/sessions")
            .get()
            .build()

        val refreshAction = Runnable {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val notification =
                        Notification(
                            "Livy",
                            null,
                            NotificationType.ERROR
                        ) // groupId is important for further settings
                    Notifications.Bus.notify(notification)
                } else {

                    val responseText = response.body!!.string()

                    val responseObject = JSONObject(responseText)
                    val jsonSessions: JSONArray = responseObject.getJSONArray("sessions")
                    val sessions: List<Session> = jsonSessions.map { s: Any ->
                        val jsonObject = s as JSONObject
                        val state = jsonObject.getString("state")
                        val id = jsonObject.getInt("id")
                        val name = jsonObject.optString("name")
                        val appId = jsonObject.optString("appId")
                        val sparkUIUrl = jsonObject.optJSONObject("appInfo")?.optString("sparkUiUrl")
                        Session(id, name, state, appId, sparkUIUrl)
                    }

                    sessionsModel.setDataVector(toRows(sessions), columnNames)
                    sessionsTable.packAll()
                }
            }
        }

        ApplicationManager.getApplication().invokeLater(refreshAction)

        return true
    }

    private fun toRows(sessions: List<Session>): Vector<Vector<Any>> {
        val rows = Vector<Vector<Any>>(sessions.size)
        for (session in sessions) {
            rows.add(toRow(session))
        }
        return rows
    }

    private fun toRow(session: Session): Vector<Any> {
        val row = Vector<Any>()
        // TODO look up column dynamically
        row.add(session.id)
        row.add(session.name)
        row.add(session.state)
        row.add(session.appId)
        row.add(session.sparkUIUrl)
        return row
    }
}
