package org.tera.plugins.livy.settings

import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.ui.components.JBTextField


class AppSettingsComponent {
    var panel: JPanel
    private val myHostNameText = JBTextField()
    private val mySessionPrefix = JBTextField()
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

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Default Livy host: "), myHostNameText, 1, false)
            .addLabeledComponent(JBLabel("Session name prefix: "), mySessionPrefix, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}