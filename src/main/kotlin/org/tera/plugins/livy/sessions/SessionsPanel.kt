package org.tera.plugins.livy.sessions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.tera.plugins.livy.Settings
import org.tera.plugins.livy.Utils
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class SessionsPanel(toolWindow: ToolWindow) {
    private val refreshToolWindowButton: JButton = JButton("Refresh")
    private val columnNames = Vector(listOf("Id", "Name", "State", "AppId", "SparkUIUrl"))
    private val deleteToolWindowButton: JButton = JButton("Delete Session")
    private val sessionsModel = object : DefaultTableModel(columnNames, 0) {
        override fun setValueAt(aValue: Any?, row: Int, column: Int) {
        }
    }
    private val sessionsTable = JBTable(sessionsModel)
    private val content: JPanel = JPanel(BorderLayout())

    private val client: OkHttpClient = Utils.getUnsafeOkHttpClient()

    init {
        deleteToolWindowButton.addActionListener { e: ActionEvent? -> deleteSelectedSessions() }
        refreshToolWindowButton.addActionListener { e: ActionEvent? -> refresh() }

        val bottomPanel = JPanel(FlowLayout())
        bottomPanel.add(refreshToolWindowButton)
        bottomPanel.add(deleteToolWindowButton)

        sessionsTable.columnModel.getColumn(0).setMaxWidth(60)
        sessionsTable.columnModel.getColumn(1).setMaxWidth(120)
        sessionsTable.columnModel.getColumn(2).setMaxWidth(60)
        sessionsTable.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        val scroll = JBScrollPane()
        scroll.setViewportView(sessionsTable)
        content.add(scroll, BorderLayout.CENTER)
        content.add(bottomPanel, BorderLayout.SOUTH)
        content.updateUI()
    }

    fun getContent(): JPanel {
        return content
    }

    // TODO maybe show popup with are you sure or so, progress bar, ... Maybe less needed with autorefresh of sessions
    private fun deleteSelectedSessions(): Boolean {
        sessionsTable.selectedRows.forEach { rowNr ->
            rowNr.run {
                // TODo look up col dynamically
                val victim = sessionsModel.getValueAt(rowNr, 0) as Int
                Utils.deleteSession(client, Settings.activeHost, victim)
                if (Settings.activeSession == victim) Settings.activeSession = null
            }
        }
        refresh()
        return true
    }

    private fun refresh(): Boolean {
        val request = Request.Builder()
            .url(Settings.activeHost + "/sessions")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // TODO check if this works, use it also to log info stuff like requests
                val notification = Notification("Livy", null, NotificationType.ERROR) // groupId is important for further settings
                Notifications.Bus.notify(notification)
                return true
            }

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

            // TODO run in thread and invoke in IntelliJ/SwingUtils.invokeLater?
            sessionsModel.setDataVector(toRows(sessions), columnNames)
        }

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
        // TODO make this dynamic
        row.add(session.id)
        row.add(session.name)
        row.add(session.state)
        row.add(session.appId)
        row.add(session.sparkUIUrl)
        return row
    }
}
