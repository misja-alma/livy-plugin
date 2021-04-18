package org.tera.plugins.livy.settings

import com.intellij.openapi.ui.LabeledComponent
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExpandableTextField
import org.tera.plugins.livy.Utils
import java.awt.Dimension
import javax.swing.JTextArea


class AppSettingsComponent {
    var panel: JPanel
    private val myHostNameText = JBTextField()
    private val mySessionPrefix = JBTextField()
    private val sessionConfigField = JBTextArea()
    val preferredFocusedComponent: JComponent
        get() = myHostNameText

    var hostNameText: String
        get() = myHostNameText.text
        set(newHostName) {
            myHostNameText.text = newHostName
        }
    var sessionPrefix: String
        get() = mySessionPrefix.text
        set(newPrefix) {
            mySessionPrefix.text = newPrefix
        }

    var sessionConfig: String
        get() = sessionConfigField.text
        set(newConfig) {
            sessionConfigField.text = newConfig
        }

    init {
        sessionConfigField.lineWrap = true
        val sessionConfigScr = JBScrollPane()
        sessionConfigScr.setViewportView(sessionConfigField)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Default Livy host: "), myHostNameText, 1, false)
            .addLabeledComponent(JBLabel("Session name prefix: "), mySessionPrefix, 1, false)
            .addLabeledComponent(JBLabel("Default session config:"), sessionConfigScr, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}