package org.tera.plugins.livy.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.GuiUtils
import org.tera.plugins.livy.Settings
import java.awt.Dimension
import javax.swing.*

/**
 * TODO options to add:
 * - host url
 * - certificate (option)
 * - checkbox: start new session/ or session id to use
 * - if start new session: session config
 * - textbox containing code to execute
 */
class LivySettingsEditor: SettingsEditor<LivyConfiguration>() {
    private var myPanel: JPanel = JPanel()
    private var hostField: JTextField = GuiUtils.createUndoableTextField()
    private var sessionIdField: JTextField = GuiUtils.createUndoableTextField()
    private var codeField: JTextArea = JTextArea()
    private var sessionConfigField: JTextArea = JTextArea()

    override fun createEditor(): JComponent {
        myPanel.setLayout(null)
        myPanel.setSize(500, 630)
        myPanel.setPreferredSize(Dimension(500, 630))

        val hostLabel = JLabel("Host")
        hostLabel.setBounds(10, 10, 75, 25)
        hostLabel.setPreferredSize(Dimension(75, 25))
        myPanel.add(hostLabel)
        myPanel.add(hostField)
        hostField.setPreferredSize(Dimension(400, 25))
        hostField.setBounds(90, 10, 400, 25)

        val sessionLabel = JLabel("Session")
        sessionLabel.setBounds(10, 40, 75, 25)
        sessionLabel.setPreferredSize(Dimension(75, 25))
        myPanel.add(sessionLabel)
        myPanel.add(sessionIdField)
        sessionIdField.setBounds(90, 40, 100, 25)
        sessionIdField.setPreferredSize(Dimension(100, 25))

        val sessionConfigLabel = JLabel("Config")
        sessionConfigLabel.setBounds(10, 70, 75, 25)
        sessionConfigLabel.setPreferredSize(Dimension(75, 25))
        myPanel.add(sessionConfigLabel)
        myPanel.add(sessionConfigField)
        sessionConfigField.setBounds(90, 70, 400, 220)
        sessionConfigField.setPreferredSize(Dimension(400, 220))

        val codeLabel = JLabel("Code")
        codeLabel.setBounds(10, 400, 75, 25)
        codeLabel.setPreferredSize(Dimension(75, 25))
        myPanel.add(codeLabel)
        myPanel.add(codeField)
        codeField.setBounds(90, 400, 400, 220)
        codeField.setPreferredSize(Dimension(400, 220))

        return myPanel
    }

    override fun resetEditorFrom(configuration: LivyConfiguration) {
        hostField.text = configuration.host
        sessionIdField.text = configuration.sessionId
        codeField.text = configuration.code
        sessionConfigField.text = configuration.sessionConfig
    }

    override fun applyEditorTo(configuration: LivyConfiguration) {
        configuration.host = hostField.text
        configuration.sessionId = sessionIdField.text
        configuration.code = codeField.text
        configuration.sessionConfig = sessionConfigField.text

        Settings.activeSession = configuration.sessionId
        Settings.activeHost = configuration.host
    }
}